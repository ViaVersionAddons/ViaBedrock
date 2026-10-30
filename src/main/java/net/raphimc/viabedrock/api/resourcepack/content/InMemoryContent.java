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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InMemoryContent extends Content {

    protected final Map<String, byte[]> content = new HashMap<>();

    @Override
    public List<String> getFilesShallow(final String path, final String extension) {
        return this.content.keySet().stream().filter(file -> file.startsWith(path) && !file.substring(path.length()).contains("/") && file.endsWith(extension)).collect(Collectors.toList());
    }

    @Override
    public List<String> getFilesDeep(final String path, final String extension) {
        return this.content.keySet().stream().filter(file -> file.startsWith(path) && file.endsWith(extension)).collect(Collectors.toList());
    }

    @Override
    public boolean contains(final String path) {
        return this.content.containsKey(path);
    }

    @Override
    public byte[] get(final String path) {
        return this.content.get(path);
    }

    @Override
    protected boolean putBytes(final String path, final byte[] data) {
        return this.content.put(path, data) != null;
    }

    public int size() {
        return this.content.size();
    }

}
