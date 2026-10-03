/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2023-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viabedrock.api.resourcepack.http;

import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.resourcepack.ResourcePack;
import net.raphimc.viabedrock.api.util.FileSystemUtil;
import net.raphimc.viabedrock.platform.ViaBedrockConfig;
import net.raphimc.viabedrock.protocol.rewriter.ResourcePackRewriter;
import net.raphimc.viabedrock.protocol.storage.ResourcePackStorage;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class ConvertedResourcePackCache {

    private final Path directory;
    private final ViaBedrockConfig.PackCacheMode mode;
    private final ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors())), task -> {
        final Thread thread = new Thread(task, "ViaBedrock Resource Pack Converter");
        thread.setDaemon(true);
        return thread;
    });
    private final ConcurrentHashMap<String, CompletableFuture<Pack>> pending = new ConcurrentHashMap<>();

    public ConvertedResourcePackCache(final Path directory, final ViaBedrockConfig.PackCacheMode mode) {
        this.directory = directory;
        this.mode = mode;
    }

    public CompletableFuture<Pack> prepare(final ResourcePackStorage storage) {
        if (this.mode == ViaBedrockConfig.PackCacheMode.DISABLED) {
            return CompletableFuture.supplyAsync(() -> convert(storage), this.executor);
        }
        return CompletableFuture.supplyAsync(() -> fingerprint(storage.getPackStackTopToBottom()), this.executor).thenCompose(key -> {
            final CompletableFuture<Pack> future = this.pending.computeIfAbsent(key, ignored -> CompletableFuture.supplyAsync(() ->
                    this.mode == ViaBedrockConfig.PackCacheMode.DISK ? this.loadOrConvert(key, storage) : convert(storage), this.executor));
            return future.whenComplete((pack, error) -> {
                if (error != null) {
                    this.pending.remove(key, future);
                }
            });
        });
    }

    public void stop() {
        this.executor.shutdownNow();
    }

    private Pack loadOrConvert(final String key, final ResourcePackStorage storage) {
        try {
            Files.createDirectories(this.directory);
            final Path index = this.directory.resolve(key + ".sha1");
            if (Files.isRegularFile(index)) {
                final String expectedSha1 = Files.readString(index);
                if (expectedSha1.matches("[0-9a-f]{40}")) {
                    final Path cachedPath = this.directory.resolve(expectedSha1 + ".zip");
                    if (Files.isRegularFile(cachedPath)) {
                        final Pack cached = describe(cachedPath);
                        if (cached.sha1().equals(expectedSha1)) {
                            return cached;
                        }
                    }
                }
            }

            final Pack converted = convert(storage);
            final Path path = this.directory.resolve(converted.sha1() + ".zip");
            FileSystemUtil.writeAtomically(path, converted.bytes());
            FileSystemUtil.writeAtomically(index, converted.sha1().getBytes(StandardCharsets.US_ASCII));
            return describe(path);
        } catch (final Exception e) {
            throw new CompletionException("Failed to prepare converted resource pack", e);
        }
    }

    private static Pack convert(final ResourcePackStorage storage) {
        try {
            final long start = System.nanoTime();
            final byte[] bytes = ResourcePackRewriter.bedrockToJava(storage).toZip();
            ViaBedrock.getPlatform().getLogger().log(Level.INFO, "Converted resource packs in " + ((System.nanoTime() - start) / 1_000_000L) + "ms");
            return describe(bytes);
        } catch (final Exception e) {
            throw new CompletionException("Failed to convert resource packs", e);
        }
    }

    static Pack describe(final Path path) throws IOException {
        final MessageDigest digest = digest("SHA-1");
        try (var input = Files.newInputStream(path)) {
            final byte[] buffer = new byte[64 * 1024];
            int length;
            while ((length = input.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
            }
        }
        final String sha1 = HexFormat.of().formatHex(digest.digest());
        final UUID id = UUID.nameUUIDFromBytes(("ViaBedrock:" + sha1).getBytes(StandardCharsets.UTF_8));
        return new Pack(path, null, Files.size(path), sha1, id);
    }

    static Pack describe(final byte[] bytes) {
        final String sha1 = HexFormat.of().formatHex(digest("SHA-1").digest(bytes));
        final UUID id = UUID.nameUUIDFromBytes(("ViaBedrock:" + sha1).getBytes(StandardCharsets.UTF_8));
        return new Pack(null, bytes, bytes.length, sha1, id);
    }

    static String fingerprint(final Collection<ResourcePack> packs) {
        final MessageDigest digest = digest("SHA-256");
        try (var output = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), digest))) {
            writeString(output, ViaBedrock.IMPL_VERSION);
            for (ResourcePack pack : packs) {
                writeString(output, pack.key().toString());
                final List<String> paths = new ArrayList<>(pack.content().getFilesDeep("", ""));
                paths.sort(String::compareTo);
                output.writeInt(paths.size());
                for (String path : paths) {
                    writeString(output, path);
                    final byte[] bytes = pack.content().get(path);
                    output.writeInt(bytes.length);
                    output.write(bytes);
                }
            }
        } catch (final IOException e) {
            throw new CompletionException(e);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void writeString(final DataOutputStream output, final String value) throws IOException {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static MessageDigest digest(final String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " is not available", e);
        }
    }

    public record Pack(Path path, byte[] bytes, long size, String sha1, UUID id) {
    }

}
