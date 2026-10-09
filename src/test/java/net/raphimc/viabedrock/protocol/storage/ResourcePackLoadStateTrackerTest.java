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
import org.junit.jupiter.api.Test;

import java.util.UUID;

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

}
