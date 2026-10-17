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
import net.raphimc.viabedrock.api.resourcepack.content.Content;
import net.raphimc.viabedrock.api.resourcepack.content.ZipContent;
import net.raphimc.viabedrock.api.util.FileSystemUtil;
import net.raphimc.viabedrock.platform.ViaBedrockConfig;
import net.raphimc.viabedrock.protocol.rewriter.ResourcePackRewriter;
import net.raphimc.viabedrock.protocol.storage.ResourcePackStorage;

import java.io.BufferedOutputStream;
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
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipFile;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class ConvertedResourcePackCache {

    private static final String CONVERSION_VERSION = "resolved-model-textures-and-appearance-6";
    // Advance when the set or format of emitted resources changes independently of converter profiles.
    private static final int RESOURCE_FORMAT_VERSION = 5;

    private final Path directory;
    private final ViaBedrockConfig.PackCacheMode mode;
    private final Function<ResourcePackStorage, Content> converter;
    private final ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, Math.min(2, Runtime.getRuntime().availableProcessors())), task -> {
        final Thread thread = new Thread(task, "ViaBedrock Resource Pack Converter");
        thread.setDaemon(true);
        return thread;
    });
    private final ConcurrentHashMap<String, CompletableFuture<Pack>> pending = new ConcurrentHashMap<>();

    public ConvertedResourcePackCache(final Path directory, final ViaBedrockConfig.PackCacheMode mode) {
        this(directory, mode, storage -> {
            try {
                final long start = System.nanoTime();
                final Content content = ResourcePackRewriter.bedrockToJava(storage);
                ViaBedrock.getPlatform().getLogger().log(Level.INFO, "Converted resource packs in " + ((System.nanoTime() - start) / 1_000_000L) + "ms");
                return content;
            } catch (final Exception e) {
                throw new CompletionException("Failed to convert resource packs", e);
            }
        });
    }

    ConvertedResourcePackCache(final Path directory, final ViaBedrockConfig.PackCacheMode mode, final Function<ResourcePackStorage, Content> converter) {
        this.directory = directory;
        this.mode = mode;
        this.converter = converter;
    }

    public CompletableFuture<Pack> prepare(final ResourcePackStorage storage) {
        if (this.mode == ViaBedrockConfig.PackCacheMode.DISABLED) {
            return CompletableFuture.supplyAsync(() -> this.convert(storage), this.executor);
        }
        return CompletableFuture.supplyAsync(() -> fingerprint(storage.getPackStackTopToBottom()), this.executor).thenCompose(key -> {
            final CompletableFuture<Pack> future = this.pending.computeIfAbsent(key, ignored -> CompletableFuture.supplyAsync(() ->
                    this.mode == ViaBedrockConfig.PackCacheMode.DISK ? this.loadOrConvert(key, storage) : this.convert(storage), this.executor));
            return future.thenApply(pack -> {
                storage.getConverterData().putAll(pack.converterData());
                return pack;
            }).whenComplete((pack, error) -> {
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
                        try {
                            final Pack cached = describe(cachedPath);
                            if (cached.sha1().equals(expectedSha1)) {
                                return cached;
                            }
                        } catch (final IOException | RuntimeException invalid) {
                            // Rebuild incomplete ZIPs and obsolete or malformed converter metadata.
                        }
                    }
                }
            }

            final Content content = this.convertContent(storage);
            final Map<String, Object> metadata = ConverterDataManifest.decode(content.getString(ConverterDataManifest.PATH));
            final Path temporary = Files.createTempFile(this.directory, "conversion-", ".tmp");
            try {
                final MessageDigest digest = digest("SHA-1");
                try (var output = new BufferedOutputStream(new DigestOutputStream(Files.newOutputStream(temporary), digest), 64 * 1024)) {
                    content.writeZip(output);
                }
                final String sha1 = HexFormat.of().formatHex(digest.digest());
                final UUID id = UUID.nameUUIDFromBytes(("ViaBedrock:" + sha1).getBytes(StandardCharsets.UTF_8));
                final long size = Files.size(temporary);
                final Path path = this.directory.resolve(sha1 + ".zip");
                FileSystemUtil.moveAtomically(temporary, path);
                FileSystemUtil.writeAtomically(index, sha1.getBytes(StandardCharsets.US_ASCII));
                return new Pack(path, null, size, sha1, id, metadata);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (final Exception e) {
            throw new CompletionException("Failed to prepare converted resource pack", e);
        }
    }

    private Pack convert(final ResourcePackStorage storage) {
        try {
            final Content content = this.convertContent(storage);
            return describe(content.toZip(), ConverterDataManifest.decode(content.getString(ConverterDataManifest.PATH)));
        } catch (final Exception e) {
            throw new CompletionException("Failed to convert resource packs", e);
        }
    }

    private Content convertContent(final ResourcePackStorage storage) {
        final Content content = this.converter.apply(storage);
        content.putJson(ConverterDataManifest.PATH, ConverterDataManifest.encode(storage.getConverterData()));
        return content;
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
        final Map<String, Object> converterData;
        try (ZipFile zip = new ZipFile(path.toFile())) {
            final var entry = zip.getEntry(ConverterDataManifest.PATH);
            converterData = entry == null ? Map.of() : ConverterDataManifest.decode(new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8));
        }
        return new Pack(path, null, Files.size(path), sha1, id, converterData);
    }

    private static Pack describe(final byte[] bytes, final Map<String, Object> converterData) {
        final String sha1 = HexFormat.of().formatHex(digest("SHA-1").digest(bytes));
        final UUID id = UUID.nameUUIDFromBytes(("ViaBedrock:" + sha1).getBytes(StandardCharsets.UTF_8));
        return new Pack(null, bytes, bytes.length, sha1, id, converterData);
    }

    static Pack describe(final byte[] bytes) {
        final String sha1 = HexFormat.of().formatHex(digest("SHA-1").digest(bytes));
        final UUID id = UUID.nameUUIDFromBytes(("ViaBedrock:" + sha1).getBytes(StandardCharsets.UTF_8));
        try {
            final Content content = new ZipContent(bytes);
            final Map<String, Object> converterData = content.contains(ConverterDataManifest.PATH)
                    ? ConverterDataManifest.decode(content.getString(ConverterDataManifest.PATH)) : Map.of();
            return new Pack(null, bytes, bytes.length, sha1, id, converterData);
        } catch (final IOException e) {
            throw new CompletionException("Invalid converted resource pack", e);
        }
    }

    static String fingerprint(final Collection<ResourcePack> packs) {
        final MessageDigest digest = digest("SHA-256");
        try (var output = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), digest))) {
            writeString(output, ViaBedrock.IMPL_VERSION);
            output.writeInt(RESOURCE_FORMAT_VERSION);
            writeString(output, CONVERSION_VERSION);
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

    public record Pack(Path path, byte[] bytes, long size, String sha1, UUID id, Map<String, Object> converterData) {

        public Pack {
            converterData = Map.copyOf(converterData);
        }
    }

}
