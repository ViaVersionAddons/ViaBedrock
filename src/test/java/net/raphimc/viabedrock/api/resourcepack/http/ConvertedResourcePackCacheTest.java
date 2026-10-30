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

import net.raphimc.viabedrock.api.resourcepack.ResourcePack;
import net.raphimc.viabedrock.api.resourcepack.content.InMemoryContent;
import net.raphimc.viabedrock.api.resourcepack.content.Content;
import net.raphimc.viabedrock.api.resourcepack.content.ZipContent;
import net.raphimc.viabedrock.protocol.provider.impl.InMemoryResourcePackProvider;
import net.raphimc.viabedrock.protocol.storage.ResourcePackStorage;
import net.raphimc.viabedrock.platform.ViaBedrockConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.function.Function;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConvertedResourcePackCacheTest {

    @TempDir
    Path directory;

    @Test
    void streamedDiskOutputMatchesMemoryArchiveAndMetadata() throws Exception {
        final byte[] payload = new byte[2 * 1024 * 1024];
        new Random(2193).nextBytes(payload);
        final Map<String, Object> metadata = Map.of("ce_example_scale", 2.5F);
        final Function<ResourcePackStorage, Content> converter = storage -> {
            storage.getConverterData().putAll(metadata);
            final InMemoryContent content = new InMemoryContent();
            content.put("assets/example/payload", payload);
            return content;
        };
        final ConvertedResourcePackCache memory = new ConvertedResourcePackCache(this.directory, ViaBedrockConfig.PackCacheMode.MEMORY, converter);
        final ConvertedResourcePackCache disk = new ConvertedResourcePackCache(this.directory, ViaBedrockConfig.PackCacheMode.DISK, converter);
        try {
            final var expected = memory.prepare(new ResourcePackStorage(List.of())).join();
            final var streamed = disk.prepare(new ResourcePackStorage(List.of())).join();
            assertArrayEquals(expected.bytes(), Files.readAllBytes(streamed.path()));
            assertEquals(expected.sha1(), streamed.sha1());
            assertEquals(expected.id(), streamed.id());
            assertEquals(expected.size(), streamed.size());
            assertEquals(expected.converterData(), streamed.converterData());
            assertNull(streamed.bytes());
            assertEquals(streamed.sha1(), ConvertedResourcePackCache.describe(streamed.path()).sha1());
        } finally {
            memory.stop();
            disk.stop();
        }
    }

    @Test
    void failedStreamingRemovesTemporaryFilesAndAllowsRetry() throws Exception {
        final AtomicInteger conversions = new AtomicInteger();
        final ConvertedResourcePackCache cache = new ConvertedResourcePackCache(this.directory, ViaBedrockConfig.PackCacheMode.DISK, storage -> {
            if (conversions.getAndIncrement() == 0) {
                return new InMemoryContent() {
                    @Override
                    public void writeZip(final OutputStream output) throws IOException {
                        output.write(new byte[]{1, 2, 3});
                        throw new IOException("Interrupted archive write");
                    }
                };
            }
            final InMemoryContent content = new InMemoryContent();
            content.put("assets/example/payload", new byte[]{4, 5, 6});
            return content;
        });
        try {
            assertThrows(CompletionException.class, () -> cache.prepare(new ResourcePackStorage(List.of())).join());
            try (var files = Files.list(this.directory)) {
                assertEquals(0, files.count());
            }
            final var complete = cache.prepare(new ResourcePackStorage(List.of())).join();
            assertEquals(2, conversions.get());
            assertArrayEquals(new byte[]{4, 5, 6}, new ZipContent(Files.readAllBytes(complete.path())).get("assets/example/payload"));
            try (var files = Files.list(this.directory)) {
                assertEquals(2, files.count());
            }
        } finally {
            cache.stop();
        }
    }

    @Test
    void damagedDiskArchivesRebuildAndRestoreTheSameMetadata() throws Exception {
        final AtomicInteger conversions = new AtomicInteger();
        final Map<String, Object> metadata = Map.of("ce_example_scale", 2.5F);
        final Function<ResourcePackStorage, Content> converter = storage -> {
            conversions.incrementAndGet();
            storage.getConverterData().putAll(metadata);
            final InMemoryContent content = new InMemoryContent();
            content.put("assets/example/texture", new byte[]{1, 2, 3});
            return content;
        };
        final ConvertedResourcePackCache first = new ConvertedResourcePackCache(this.directory, ViaBedrockConfig.PackCacheMode.DISK, converter);
        final ConvertedResourcePackCache.Pack original;
        try {
            original = first.prepare(new ResourcePackStorage(List.of())).join();
        } finally {
            first.stop();
        }
        final byte[] complete = Files.readAllBytes(original.path());
        Files.write(original.path(), Arrays.copyOf(complete, complete.length / 2));
        final ConvertedResourcePackCache replacement = new ConvertedResourcePackCache(this.directory, ViaBedrockConfig.PackCacheMode.DISK, converter);
        try {
            final ResourcePackStorage connection = new ResourcePackStorage(List.of());
            final var repaired = replacement.prepare(connection).join();
            assertEquals(2, conversions.get());
            assertEquals(original.sha1(), repaired.sha1());
            assertEquals(original.id(), repaired.id());
            assertEquals(metadata, connection.getConverterData());
            assertEquals(metadata, repaired.converterData());
            assertArrayEquals(complete, Files.readAllBytes(repaired.path()));
            assertEquals(repaired.sha1(), ConvertedResourcePackCache.describe(repaired.path()).sha1());
        } finally {
            replacement.stop();
        }
    }

    @Test
    void effectivePackOrderChangesCacheIdentity() {
        final ResourcePack first = pack(UUID.randomUUID(), new byte[]{1});
        final ResourcePack second = pack(UUID.randomUUID(), new byte[]{2});

        assertNotEquals(ConvertedResourcePackCache.fingerprint(List.of(first, second)), ConvertedResourcePackCache.fingerprint(List.of(second, first)));
    }

    @Test
    void changedSourceBytesChangeCacheIdentityWithoutChangingPackId() {
        final UUID id = UUID.randomUUID();
        final ResourcePack original = pack(id, new byte[]{1});
        final ResourcePack updated = pack(id, new byte[]{2});

        assertNotEquals(ConvertedResourcePackCache.fingerprint(List.of(original)), ConvertedResourcePackCache.fingerprint(List.of(updated)));
    }

    @Test
    void sourceCacheDoesNotReuseAnotherAdvertisedContentIdentity() throws Exception {
        final UUID id = UUID.randomUUID();
        final InMemoryResourcePackProvider provider = new InMemoryResourcePackProvider();
        provider.save(pack(id, new byte[]{1}), "first");

        assertTrue(provider.has(new ResourcePack.Key(id, "1.0.0"), "first"));
        assertFalse(provider.has(new ResourcePack.Key(id, "1.0.0"), "second"));
        assertArrayEquals(new byte[]{1}, provider.load(new ResourcePack.Key(id, "1.0.0"), "first").content().get("assets/example"));
    }

    @Test
    void zipEntryInsertionOrderDoesNotChangeTheAdvertisedHash() throws Exception {
        final InMemoryContent first = new InMemoryContent();
        first.put("assets/b", new byte[]{2});
        first.put("assets/a", new byte[]{1});
        final InMemoryContent second = new InMemoryContent();
        second.put("assets/a", new byte[]{1});
        second.put("assets/b", new byte[]{2});

        final Path firstPath = this.directory.resolve("first.zip");
        final Path secondPath = this.directory.resolve("second.zip");
        Files.write(firstPath, first.toZip());
        Files.write(secondPath, second.toZip());

        assertArrayEquals(Files.readAllBytes(firstPath), Files.readAllBytes(secondPath));
        assertEquals(ConvertedResourcePackCache.describe(firstPath).sha1(), ConvertedResourcePackCache.describe(secondPath).sha1());
        assertEquals(ConvertedResourcePackCache.describe(firstPath).id(), ConvertedResourcePackCache.describe(secondPath).id());
        assertEquals(ConvertedResourcePackCache.describe(firstPath).id(), ConvertedResourcePackCache.describe(first.toZip()).id());
    }

    @Test
    void restoresConverterMetadataAcrossMissesAndSharedMemoryAndDiskHits() {
        final Map<String, Object> expected = Map.of("ce_example:actor_default_scale", 2.5F, "ca_example:armor_default", true,
                "model_missing_entities/example:actor/default_unused", "missing texture textures/unused");
        for (ViaBedrockConfig.PackCacheMode mode : List.of(ViaBedrockConfig.PackCacheMode.MEMORY, ViaBedrockConfig.PackCacheMode.DISK)) {
            final AtomicInteger conversions = new AtomicInteger();
            final Path cachePath = this.directory.resolve(mode.name());
            final ConvertedResourcePackCache cache = new ConvertedResourcePackCache(cachePath, mode, storage -> {
                conversions.incrementAndGet();
                storage.getConverterData().putAll(expected);
                return new InMemoryContent();
            });
            try {
                final ResourcePackStorage first = new ResourcePackStorage(List.of());
                final var converted = cache.prepare(first).join();
                final ResourcePackStorage second = new ResourcePackStorage(List.of());
                final var cached = cache.prepare(second).join();
                assertEquals(converted.sha1(), cached.sha1());
                assertEquals(expected, first.getConverterData());
                assertEquals(expected, second.getConverterData());
                assertEquals(1, conversions.get());
                assertThrows(UnsupportedOperationException.class, () -> cached.converterData().clear());
            } finally {
                cache.stop();
            }
            if (mode == ViaBedrockConfig.PackCacheMode.DISK) {
                final ConvertedResourcePackCache reopened = new ConvertedResourcePackCache(cachePath, mode, ignored -> {
                    fail("A disk hit must not invoke conversion");
                    return new InMemoryContent();
                });
                try {
                    final ResourcePackStorage fresh = new ResourcePackStorage(List.of());
                    reopened.prepare(fresh).join();
                    assertEquals(expected, fresh.getConverterData());
                } finally {
                    reopened.stop();
                }
            }
        }
    }

    @Test
    void rejectsUnsupportedObjectsAndInvalidScalesInConverterMetadata() {
        assertThrows(IllegalArgumentException.class, () -> ConverterDataManifest.encode(Map.of("ce_example_scale", new Object())));
        assertThrows(IllegalArgumentException.class, () -> ConverterDataManifest.encode(Map.of("ce_example_scale", Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> ConverterDataManifest.encode(Map.of("ce_example_scale", Double.MAX_VALUE)));
        final var manifest = ConverterDataManifest.encode(Map.of("ce_example_scale", 1F));
        manifest.getAsJsonObject("entity_scales").addProperty("ce_example_scale", Double.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> ConverterDataManifest.decode(manifest.toString()));
        assertThrows(IllegalArgumentException.class, () -> ConverterDataManifest.encode(Map.of("arbitrary_key", true)));
    }

    @Test
    void evictsLeastRecentlyUsedCompletedConversionsWithoutInvalidatingReturnedPacks() {
        final AtomicInteger conversions = new AtomicInteger();
        final ConvertedResourcePackCache cache = new ConvertedResourcePackCache(this.directory,
                ViaBedrockConfig.PackCacheMode.MEMORY, storage -> {
                    conversions.incrementAndGet();
                    return new InMemoryContent();
                });
        try {
            final var sources = java.util.stream.IntStream.range(0, 5)
                    .mapToObj(index -> new ResourcePackStorage(List.of(pack(UUID.randomUUID(), new byte[]{(byte) index}))))
                    .toList();
            final var first = cache.prepare(sources.get(0)).join();
            for (int index = 1; index < 4; index++) {
                cache.prepare(sources.get(index)).join();
            }
            assertSame(first, cache.prepare(sources.get(0)).join());
            cache.prepare(sources.get(sources.size() - 1)).join();
            assertSame(first, cache.prepare(sources.get(0)).join());
            assertEquals(5, conversions.get());
            cache.prepare(sources.get(1)).join();
            assertEquals(6, conversions.get());
            assertTrue(first.bytes().length > 0);
        } finally {
            cache.stop();
        }
    }

    @Test
    void completesCacheBookkeepingBeforeCallerContinuations() throws Exception {
        final CountDownLatch finishConversion = new CountDownLatch(1);
        final AtomicInteger conversions = new AtomicInteger();
        final ConvertedResourcePackCache cache = new ConvertedResourcePackCache(this.directory,
                ViaBedrockConfig.PackCacheMode.MEMORY, storage -> {
                    if (conversions.incrementAndGet() == 1) {
                        try {
                            if (!finishConversion.await(5, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("Conversion was not released");
                            }
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new CompletionException(e);
                        }
                    }
                    return new InMemoryContent();
                }, 0, 0);
        try {
            final ResourcePackStorage storage = new ResourcePackStorage(List.of());
            final var first = cache.prepare("uncached", storage);
            final var retry = first.thenApply(pack -> cache.prepare("uncached", storage));
            finishConversion.countDown();
            final var next = retry.get(5, TimeUnit.SECONDS);
            assertNotSame(first, next);
            next.get(5, TimeUnit.SECONDS);
            assertEquals(2, conversions.get());
        } finally {
            finishConversion.countDown();
            cache.stop();
        }
    }

    @Test
    void memoryBudgetEvictsArchivesAndSkipsOversizedResults() {
        final AtomicInteger conversions = new AtomicInteger();
        final Function<ResourcePackStorage, Content> converter = storage -> {
            conversions.incrementAndGet();
            final InMemoryContent content = new InMemoryContent();
            content.putStored("assets/payload", new byte[2048]);
            return content;
        };
        final ResourcePack first = pack(UUID.randomUUID(), new byte[]{1});
        final ResourcePack second = pack(UUID.randomUUID(), new byte[]{2});
        final ConvertedResourcePackCache bounded = new ConvertedResourcePackCache(this.directory, ViaBedrockConfig.PackCacheMode.MEMORY, converter, 32, 3000);
        final ConvertedResourcePackCache oversized = new ConvertedResourcePackCache(this.directory, ViaBedrockConfig.PackCacheMode.MEMORY, converter, 32, 1024);
        try {
            bounded.prepare(new ResourcePackStorage(List.of(first))).join();
            bounded.prepare(new ResourcePackStorage(List.of(second))).join();
            bounded.prepare(new ResourcePackStorage(List.of(first))).join();
            assertEquals(3, conversions.get());
            final var served = oversized.prepare(new ResourcePackStorage(List.of(first))).join();
            assertTrue(served.bytes().length > 1024);
            oversized.prepare(new ResourcePackStorage(List.of(first))).join();
            assertEquals(5, conversions.get());
        } finally {
            bounded.stop();
            oversized.stop();
        }
    }

    @Test
    void evictedDiskDescriptorsReloadWithoutReconversion() {
        final AtomicInteger conversions = new AtomicInteger();
        final ConvertedResourcePackCache cache = new ConvertedResourcePackCache(this.directory, ViaBedrockConfig.PackCacheMode.DISK, storage -> {
            conversions.incrementAndGet();
            storage.getConverterData().put("model_missing_large", "x".repeat(2048));
            return new InMemoryContent();
        }, 1, 1024);
        try {
            final ResourcePackStorage first = new ResourcePackStorage(List.of());
            final var original = cache.prepare(first).join();
            final ResourcePackStorage second = new ResourcePackStorage(List.of());
            final var restored = cache.prepare(second).join();
            assertNotSame(original, restored);
            assertEquals(1, conversions.get());
            assertEquals(original.sha1(), restored.sha1());
            assertEquals(first.getConverterData(), second.getConverterData());
        } finally {
            cache.stop();
        }
    }

    @Test
    void concurrentRequestsShareAnOversizedInFlightConversion() throws Exception {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger conversions = new AtomicInteger();
        final ConvertedResourcePackCache cache = new ConvertedResourcePackCache(this.directory, ViaBedrockConfig.PackCacheMode.MEMORY, storage -> {
            conversions.incrementAndGet();
            started.countDown();
            try {
                assertTrue(release.await(5, TimeUnit.SECONDS));
            } catch (final InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(interrupted);
            }
            return new InMemoryContent();
        }, 1, 1);
        try {
            final var first = cache.prepare("shared", new ResourcePackStorage(List.of()));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            final var second = cache.prepare("shared", new ResourcePackStorage(List.of()));
            assertSame(first, second);
            release.countDown();
            assertSame(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
            assertEquals(1, conversions.get());
        } finally {
            release.countDown();
            cache.stop();
        }
    }

    private static ResourcePack pack(final UUID id, final byte[] data) {
        final InMemoryContent content = new InMemoryContent();
        content.putString("manifest.json", "{\"format_version\":3,\"header\":{\"uuid\":\"" + id + "\",\"version\":\"1.0.0\",\"name\":\"test\"}}");
        content.put("assets/example", data);
        return new ResourcePack(content);
    }

}
