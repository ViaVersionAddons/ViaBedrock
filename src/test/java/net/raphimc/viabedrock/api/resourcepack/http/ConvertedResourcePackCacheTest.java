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
import net.raphimc.viabedrock.protocol.provider.impl.InMemoryResourcePackProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConvertedResourcePackCacheTest {

    @TempDir
    Path directory;

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

    private static ResourcePack pack(final UUID id, final byte[] data) {
        final InMemoryContent content = new InMemoryContent();
        content.putString("manifest.json", "{\"format_version\":3,\"header\":{\"uuid\":\"" + id + "\",\"version\":\"1.0.0\",\"name\":\"test\"}}");
        content.put("assets/example", data);
        return new ResourcePack(content);
    }

}
