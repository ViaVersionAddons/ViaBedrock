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
package net.raphimc.viabedrock.protocol.storage;

import net.raphimc.viabedrock.api.resourcepack.ResourcePack;
import net.raphimc.viabedrock.api.resourcepack.content.InMemoryContent;
import net.raphimc.viabedrock.protocol.provider.impl.InMemoryResourcePackProvider;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePackLoadStateTrackerTest {

    @Test
    void sourceCacheIdentityTracksContentAndDecryptionKey() {
        final ResourcePack.Key key = new ResourcePack.Key(UUID.randomUUID(), "1.0.0");
        final ResourcePackLoadStateTracker.Info original = new ResourcePackLoadStateTracker.Info(key, new byte[]{1}, "first", null);
        final ResourcePackLoadStateTracker.Info same = new ResourcePackLoadStateTracker.Info(key, new byte[]{1}, "first", null);
        final ResourcePackLoadStateTracker.Info changedContent = new ResourcePackLoadStateTracker.Info(key, new byte[]{1}, "second", null);
        final ResourcePackLoadStateTracker.Info changedKey = new ResourcePackLoadStateTracker.Info(key, new byte[]{2}, "first", null);

        assertEquals(original.cacheIdentity(), same.cacheIdentity());
        assertNotEquals(original.cacheIdentity(), changedContent.cacheIdentity());
        assertNotEquals(original.cacheIdentity(), changedKey.cacheIdentity());
        assertNull(new ResourcePackLoadStateTracker.Info(key, new byte[0], "", null).cacheIdentity());
    }

    @Test
    void persistsDownloadedPacksAbsentFromTheSelectedStack() throws Exception {
        final ResourcePack selected = pack(new byte[]{1});
        final ResourcePack unused = pack(new byte[]{2});
        final var selectedInfo = new ResourcePackLoadStateTracker.Info(selected.key(), new byte[0], "selected", null);
        final var unusedInfo = new ResourcePackLoadStateTracker.Info(unused.key(), new byte[0], "unused", null);
        final InMemoryResourcePackProvider provider = new InMemoryResourcePackProvider();

        ResourcePackLoadStateTracker.saveRemoteResourcePacks(List.of(selected, unused),
                Map.of(selected.key(), selectedInfo, unused.key(), unusedInfo), provider,
                (pack, error) -> fail("Saving a valid downloaded pack must succeed", error));

        assertTrue(provider.has(selected.key(), selectedInfo.cacheIdentity()));
        assertTrue(provider.has(unused.key(), unusedInfo.cacheIdentity()));
        assertArrayEquals(new byte[]{2}, provider.load(unused.key(), unusedInfo.cacheIdentity()).content().get("payload"));
        assertArrayEquals(new byte[]{2}, unused.content().get("payload"));
    }

    @Test
    void failedSourceWriteDoesNotStopSavingOtherDownloadedPacks() throws Exception {
        final ResourcePack failed = pack(new byte[]{1});
        final ResourcePack unused = pack(new byte[]{2});
        final var failedInfo = new ResourcePackLoadStateTracker.Info(failed.key(), new byte[0], "failed", null);
        final var unusedInfo = new ResourcePackLoadStateTracker.Info(unused.key(), new byte[0], "unused", null);
        final List<ResourcePack.Key> failures = new ArrayList<>();
        final InMemoryResourcePackProvider provider = new InMemoryResourcePackProvider() {
            @Override
            public void save(final ResourcePack pack, final String identity) throws IOException {
                if (pack.key().equals(failed.key())) {
                    throw new IOException("Source cache is not writable");
                }
                super.save(pack, identity);
            }
        };

        assertDoesNotThrow(() -> ResourcePackLoadStateTracker.saveRemoteResourcePacks(List.of(failed, unused),
                Map.of(failed.key(), failedInfo, unused.key(), unusedInfo), provider, (pack, error) -> {
                    assertInstanceOf(IOException.class, error);
                    failures.add(pack.key());
                }));

        assertEquals(List.of(failed.key()), failures);
        assertFalse(provider.has(failed.key(), failedInfo.cacheIdentity()));
        assertTrue(provider.has(unused.key(), unusedInfo.cacheIdentity()));
        assertArrayEquals(new byte[]{2}, provider.load(unused.key(), unusedInfo.cacheIdentity()).content().get("payload"));
    }

    @Test
    void failedDecodeCompletesTheNegotiationFutureExceptionally() {
        final ResourcePackLoadStateTracker tracker = new ResourcePackLoadStateTracker(null, new ResourcePackLoadStateTracker.Info[0]);
        final var negotiation = tracker.loadedFuture();
        final IllegalStateException failure = new IllegalStateException("Bad downloaded pack");
        tracker.failLoading(failure);
        assertSame(failure, assertThrows(ExecutionException.class, () -> negotiation.get(5, TimeUnit.SECONDS)).getCause());
        tracker.onRemove();
    }

    private static ResourcePack pack(final byte[] payload) {
        final InMemoryContent content = new InMemoryContent();
        content.putString("manifest.json", "{\"format_version\":3,\"header\":{\"uuid\":\"" + UUID.randomUUID()
                + "\",\"version\":\"1.0.0\",\"name\":\"test\"}}");
        content.put("payload", payload);
        return new ResourcePack(content);
    }

}
