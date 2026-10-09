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
package net.raphimc.viabedrock.protocol.provider.impl;

import net.raphimc.viabedrock.api.resourcepack.ResourcePack;
import net.raphimc.viabedrock.protocol.provider.ResourcePackProvider;

public class NoOpResourcePackProvider extends ResourcePackProvider {

    @Override
    public boolean has(final ResourcePack.Key key, final String contentIdentity) {
        return false;
    }

    @Override
    public ResourcePack load(final ResourcePack.Key key, final String contentIdentity) {
        throw new UnsupportedOperationException("NoOpResourcePackProvider cannot load packs");
    }

    @Override
    public ResourcePack loadAny(final ResourcePack.Key key) {
        throw new UnsupportedOperationException("NoOpResourcePackProvider cannot load packs");
    }

    @Override
    public void save(final ResourcePack resourcePack, final String contentIdentity) {
    }

}
