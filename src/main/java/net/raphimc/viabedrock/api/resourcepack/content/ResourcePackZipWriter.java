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
package net.raphimc.viabedrock.api.resourcepack.content;

import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.parallel.FileBasedScatterGatherBackingStore;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Compresses large archives in parallel while preserving entry order and bounded working storage. */
public final class ResourcePackZipWriter {

    private static final long PARALLEL_THRESHOLD = 8 * 1024 * 1024;

    private ResourcePackZipWriter() {
    }

    static void write(final Content content, final OutputStream output) throws IOException {
        try (output) {
            final List<Entry> entries = new ArrayList<>();
            for (final String path : content.getFilesDeep("", "").stream().sorted().toList()) {
                entries.add(new Entry(path, content.get(path), content.isStored(path)));
            }
            write(entries, output);
        }
    }

    /** Writes entries in caller order, bounds compression workers and closes the supplied output. */
    public static void write(final List<Entry> entries, final OutputStream output) throws IOException {
        try (output) {
            final long bytes = entries.stream().mapToLong(entry -> entry.data().length).sum();
            final int workers = Math.min(4, Runtime.getRuntime().availableProcessors());
            if (workers > 1 && entries.size() > 1 && bytes >= PARALLEL_THRESHOLD) {
                writeParallel(entries, output, Path.of(System.getProperty("java.io.tmpdir")), workers);
            } else {
                try (ZipOutputStream zip = new ZipOutputStream(output)) {
                    zip.setLevel(Deflater.BEST_SPEED);
                    for (final Entry file : entries) {
                        final ZipEntry entry = new ZipEntry(file.path());
                        entry.setTime(0L);
                        if (file.stored()) {
                            final CRC32 crc = new CRC32();
                            crc.update(file.data());
                            entry.setMethod(ZipEntry.STORED);
                            entry.setSize(file.data().length);
                            entry.setCompressedSize(file.data().length);
                            entry.setCrc(crc.getValue());
                        }
                        zip.putNextEntry(entry);
                        zip.write(file.data());
                        zip.closeEntry();
                    }
                }
            }
        }
    }

    static void writeParallel(final List<Entry> entries, final OutputStream output,
                              final Path temporaryRoot, final int workers) throws IOException {
        try (TemporaryDirectory directory = new TemporaryDirectory(Files.createTempDirectory(temporaryRoot, "viabedrock-zip-"), new ConcurrentLinkedQueue<>());
             ZipArchiveOutputStream zip = new ZipArchiveOutputStream(output)) {
            final AtomicInteger workerIndex = new AtomicInteger();
            final var executor = Executors.newFixedThreadPool(workers, task -> {
                final Thread thread = new Thread(task, "ViaBedrock ZIP Compressor-" + workerIndex.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            });
            try {
                final ParallelScatterZipCreator scatter = new ParallelScatterZipCreator(executor,
                        directory::createStore, Deflater.BEST_SPEED);
                zip.setEncoding("UTF-8");
                for (final Entry file : entries) {
                    final ZipArchiveEntry entry = new ZipArchiveEntry(file.path());
                    entry.setTime(0L);
                    entry.setMethod(file.stored() ? ZipEntry.STORED : ZipEntry.DEFLATED);
                    scatter.addArchiveEntry(entry, () -> new ByteArrayInputStream(file.data()));
                }
                scatter.writeTo(zip);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted resource-pack compression", e);
            } catch (final ExecutionException e) {
                throw new IOException("Failed to compress resource-pack entries", e.getCause());
            } finally {
                executor.shutdownNow();
                boolean interrupted = Thread.interrupted();
                while (!executor.isTerminated()) {
                    try {
                        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                    } catch (final InterruptedException e) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /** Entry bytes remain owned by the caller and must not change during the write. */
    public record Entry(String path, byte[] data, boolean stored) {
        public Entry(final String path, final byte[] data) {
            this(path, data, false);
        }
    }

    /** Removes late-created worker files after the executor has terminated, including on failed writes. */
    private record TemporaryDirectory(Path path, ConcurrentLinkedQueue<FileBasedScatterGatherBackingStore> stores) implements AutoCloseable {
        FileBasedScatterGatherBackingStore createStore() throws IOException {
            final var store = new FileBasedScatterGatherBackingStore(Files.createTempFile(this.path, "compressed-", ".tmp"));
            this.stores.add(store);
            return store;
        }

        @Override
        public void close() throws IOException {
            IOException failure = null;
            for (final var store : this.stores) {
                try {
                    store.close();
                } catch (final IOException e) {
                    if (failure == null) {
                        failure = e;
                    } else {
                        failure.addSuppressed(e);
                    }
                }
            }
            try (var files = Files.list(this.path)) {
                for (final Path file : files.toList()) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (final IOException e) {
                        if (failure == null) {
                            failure = e;
                        } else {
                            failure.addSuppressed(e);
                        }
                    }
                }
            }
            try {
                Files.delete(this.path);
            } catch (final IOException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }

}
