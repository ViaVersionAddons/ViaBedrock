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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.CRC32;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePackZipWriterTest {

    @TempDir
    Path directory;

    private static List<ResourcePackZipWriter.Entry> entries() {
        final byte[] random = new byte[3 * 1024 * 1024];
        new Random(2193).nextBytes(random);
        return List.of(new ResourcePackZipWriter.Entry("assets/pack/a", random, true),
                new ResourcePackZipWriter.Entry("assets/pack/b", new byte[random.length]),
                new ResourcePackZipWriter.Entry("assets/pack/c", Arrays.copyOf(random, random.length)),
                new ResourcePackZipWriter.Entry("assets/pack/é", new byte[0]));
    }

    @Test
    void parallelWorkersPreserveBytesOrderTimestampsAndDeterminism() throws Exception {
        final var entries = entries();
        final ByteArrayOutputStream one = new ByteArrayOutputStream();
        final ByteArrayOutputStream four = new ByteArrayOutputStream();
        ResourcePackZipWriter.writeParallel(entries, one, this.directory, 1);
        ResourcePackZipWriter.writeParallel(entries, four, this.directory, 4);
        assertArrayEquals(one.toByteArray(), four.toByteArray());
        try (var files = Files.list(this.directory)) {
            assertEquals(0, files.count());
        }
        final Path archive = this.directory.resolve("result.zip");
        Files.write(archive, four.toByteArray());
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            final var actual = zip.stream().toList();
            assertEquals(entries.stream().map(ResourcePackZipWriter.Entry::path).toList(), actual.stream().map(java.util.zip.ZipEntry::getName).toList());
            for (int i = 0; i < entries.size(); i++) {
                assertEquals(0L, actual.get(i).getTime());
                try (var input = zip.getInputStream(actual.get(i))) {
                    assertArrayEquals(entries.get(i).data(), input.readAllBytes());
                }
            }
        }
    }

    @Test
    void orderedArchivesKeepTheHeaderBeforeIndexedEntries() throws Exception {
        final var entries = new ArrayList<ResourcePackZipWriter.Entry>();
        entries.add(new ResourcePackZipWriter.Entry("header", new byte[]{1}));
        entries.addAll(entries());
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        ResourcePackZipWriter.write(entries, output);
        try (var zip = new ZipInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            for (final var entry : entries) {
                assertEquals(entry.path(), zip.getNextEntry().getName());
                assertArrayEquals(entry.data(), zip.readAllBytes());
            }
            assertNull(zip.getNextEntry());
        }
    }

    @Test
    void memoryAndStreamedArchivesRemainIdenticalAcrossBufferGrowth() throws Exception {
        final Content content = new InMemoryContent();
        for (final var entry : entries()) {
            content.put(entry.path(), entry.data());
        }
        final ByteArrayOutputStream streamed = new ByteArrayOutputStream();
        content.writeZip(streamed);
        assertTrue(streamed.size() > 4 * 1024 * 1024);
        assertArrayEquals(streamed.toByteArray(), content.toZip());
    }

    @Test
    void mixedStoredAndDeflatedEntriesWorkInBothBackends() throws Exception {
        final var payloads = entries();
        final var mixed = List.of(new ResourcePackZipWriter.Entry(payloads.get(0).path(), payloads.get(0).data(), true),
                payloads.get(1), new ResourcePackZipWriter.Entry(payloads.get(3).path(), payloads.get(3).data(), true));
        final ByteArrayOutputStream sequential = new ByteArrayOutputStream();
        final ByteArrayOutputStream parallel = new ByteArrayOutputStream();
        // A single entry forces the ordinary writer regardless of processor count or payload size.
        ResourcePackZipWriter.write(List.of(mixed.get(0)), sequential);
        ResourcePackZipWriter.writeParallel(mixed, parallel, this.directory, 4);
        for (final var result : List.of(java.util.Map.entry(sequential.toByteArray(), 1), java.util.Map.entry(parallel.toByteArray(), mixed.size()))) {
            try (var zip = new ZipInputStream(new ByteArrayInputStream(result.getKey()))) {
                for (int index = 0; index < result.getValue(); index++) {
                    final var entry = zip.getNextEntry();
                    assertNotNull(entry);
                    final var expected = mixed.get(index);
                    assertEquals(expected.path(), entry.getName());
                    assertEquals(expected.stored() ? ZipEntry.STORED : ZipEntry.DEFLATED, entry.getMethod());
                    assertArrayEquals(expected.data(), zip.readAllBytes());
                    final CRC32 crc = new CRC32();
                    crc.update(expected.data());
                    assertEquals(crc.getValue(), entry.getCrc());
                    if (expected.stored()) {
                        assertEquals(expected.data().length, entry.getSize());
                        assertEquals(entry.getSize(), entry.getCompressedSize());
                    }
                }
                assertNull(zip.getNextEntry());
            }
        }
    }

    @Test
    void storedOutputHintsSurviveMergingAndResetAfterReplacement() throws Exception {
        final String path = entries().get(0).path();
        final byte[] data = entries().get(0).data();
        Files.createDirectories(this.directory.resolve(path).getParent());
        for (final Content source : List.of(new InMemoryContent(), new DirectoryContent(this.directory))) {
            source.putStored(path, data);
            final Content merged = new InMemoryContent();
            merged.putAll(source);
            assertTrue(merged.isStored(path));
            try (var zip = new ZipInputStream(new ByteArrayInputStream(merged.toZip()))) {
                assertEquals(ZipEntry.STORED, zip.getNextEntry().getMethod());
                assertArrayEquals(data, zip.readAllBytes());
            }
            merged.put(path, new byte[data.length]);
            assertFalse(merged.isStored(path));
            try (var zip = new ZipInputStream(new ByteArrayInputStream(merged.toZip()))) {
                assertEquals(ZipEntry.DEFLATED, zip.getNextEntry().getMethod());
                assertArrayEquals(new byte[data.length], zip.readAllBytes());
            }
        }
    }

    @Test
    void failedReplacementRetainsTheExistingCompressionHint() {
        final String path = entries().get(0).path();
        final InMemoryContent content = new InMemoryContent() {
            @Override
            protected boolean putBytes(final String entryPath, final byte[] data) {
                if (data.length == 0) {
                    throw new IllegalStateException();
                }
                return super.putBytes(entryPath, data);
            }
        };
        content.putStored(path, new byte[1]);
        assertThrows(IllegalStateException.class, () -> content.put(path, new byte[0]));
        assertTrue(content.isStored(path));
        assertArrayEquals(new byte[1], content.get(path));
    }

    @Test
    void outputFailureRemovesAllWorkerFilesAndAllowsRetry() throws Exception {
        final var entries = entries();
        final OutputStream failing = new OutputStream() {
            private int remaining = 1024;

            @Override
            public void write(final int value) throws IOException {
                if (--this.remaining < 0) {
                    throw new IOException();
                }
            }
        };
        assertThrows(IOException.class, () -> ResourcePackZipWriter.writeParallel(entries, failing, this.directory, 4));
        try (var files = Files.list(this.directory)) {
            assertEquals(0, files.count());
        }
        final ByteArrayOutputStream retry = new ByteArrayOutputStream();
        ResourcePackZipWriter.writeParallel(entries, retry, this.directory, 4);
        assertTrue(retry.size() > 0);
        try (var files = Files.list(this.directory)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void interruptionPreservesTheSignalAndRemovesWorkerStorage() throws Exception {
        final var entries = entries();
        try {
            Thread.currentThread().interrupt();
            final IOException error = assertThrows(IOException.class, () -> ResourcePackZipWriter.writeParallel(entries, OutputStream.nullOutputStream(), this.directory, 4));
            assertInstanceOf(InterruptedException.class, error.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        try (var files = Files.list(this.directory)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void contentClosesItsOutputWhenReadingFails() {
        final boolean[] closed = {false};
        final InMemoryContent content = new InMemoryContent() {
            @Override
            public byte[] get(final String path) {
                throw new IllegalStateException();
            }
        };
        content.put("payload", new byte[1]);
        final ByteArrayOutputStream output = new ByteArrayOutputStream() {
            @Override
            public void close() {
                closed[0] = true;
            }
        };
        assertThrows(IllegalStateException.class, () -> content.writeZip(output));
        assertTrue(closed[0]);
    }

}
