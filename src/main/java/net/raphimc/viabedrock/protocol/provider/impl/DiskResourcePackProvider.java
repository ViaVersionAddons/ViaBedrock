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

import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.resourcepack.ResourcePack;
import net.raphimc.viabedrock.api.resourcepack.content.ZipContent;
import net.raphimc.viabedrock.api.util.FileSystemUtil;
import net.raphimc.viabedrock.protocol.provider.ResourcePackProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class DiskResourcePackProvider extends ResourcePackProvider {

    @Override
    public boolean has(final ResourcePack.Key key, final String contentIdentity) {
        return contentIdentity != null && Files.isRegularFile(this.getPath(key, contentIdentity));
    }

    @Override
    public ResourcePack load(final ResourcePack.Key key, final String contentIdentity) throws IOException {
        if (!this.has(key, contentIdentity)) {
            throw new IOException("Resource pack not found");
        }
        return new ResourcePack(new ZipContent(Files.readAllBytes(this.getPath(key, contentIdentity))));
    }

    @Override
    public ResourcePack loadAny(final ResourcePack.Key key) throws IOException {
        return new ResourcePack(new ZipContent(Files.readAllBytes(this.getLegacyPath(key))));
    }

    @Override
    public void save(final ResourcePack resourcePack, final String contentIdentity) throws IOException {
        final byte[] bytes = resourcePack.content().toZip();
        if (contentIdentity != null) {
            Files.createDirectories(this.getSourcePath());
            FileSystemUtil.writeAtomically(this.getPath(resourcePack.key(), contentIdentity), bytes);
        }
        FileSystemUtil.writeAtomically(this.getLegacyPath(resourcePack.key()), bytes);
    }

    private Path getPath(final ResourcePack.Key key, final String contentIdentity) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(key.toString().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(contentIdentity.getBytes(StandardCharsets.UTF_8));
            return this.getSourcePath().resolve(HexFormat.of().formatHex(digest.digest()) + ".mcpack");
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private Path getSourcePath() {
        return ViaBedrock.getPlatform().getServerPacksFolder().toPath().resolve("source");
    }

    private Path getLegacyPath(final ResourcePack.Key key) {
        final Path basePath = ViaBedrock.getPlatform().getServerPacksFolder().toPath();
        final Path resolvedPath = basePath.resolve(key.toString() + ".mcpack").normalize();
        if (!resolvedPath.startsWith(basePath)) {
            throw new IllegalArgumentException("Path traversal attempt: " + key);
        }
        return resolvedPath;
    }

}
