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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class DirectoryContent extends Content {

    private final Path dir;

    public DirectoryContent(final Path dir) {
        this.dir = dir;
    }

    @Override
    public List<String> getFilesShallow(final String path, final String extension) {
        final Path resolvedPath = this.resolvePath(this.dir, Path.of(path));
        if (!Files.exists(resolvedPath)) {
            return List.of();
        }
        try {
            return Files.list(resolvedPath)
                .filter(Files::isRegularFile)
                .map(this.dir::relativize)
                .map(Path::toString)
                .map(s -> s.replace('\\', '/'))
                .filter(file -> !file.contains("/") && file.endsWith(extension))
                .collect(Collectors.toList());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<String> getFilesDeep(final String path, final String extension) {
        final Path resolvedPath = this.resolvePath(this.dir, Path.of(path));
        if (!Files.exists(resolvedPath)) {
            return List.of();
        }
        try {
            return Files.walk(resolvedPath)
                .filter(Files::isRegularFile)
                .map(this.dir::relativize)
                .map(Path::toString)
                .map(s -> s.replace('\\', '/'))
                .filter(file -> file.endsWith(extension))
                .collect(Collectors.toList());
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean contains(final String path) {
        return Files.exists(this.resolvePath(this.dir, Path.of(path)));
    }

    @Override
    public byte[] get(final String path) {
        final Path resolvedPath = this.resolvePath(this.dir, Path.of(path));
        if (!Files.exists(resolvedPath)) {
            return null;
        }
        try {
            return Files.readAllBytes(resolvedPath);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    protected boolean putBytes(final String path, final byte[] data) {
        final boolean exists = this.contains(path);
        try {
            Files.write(this.resolvePath(this.dir, Path.of(path)), data);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return exists;
    }

    private Path resolvePath(final Path basePath, final Path userPath) {
        if (!basePath.isAbsolute()) {
            throw new IllegalArgumentException("Base path must be absolute");
        }
        if (userPath.isAbsolute()) {
            throw new IllegalArgumentException("User path must be relative");
        }
        final Path resolvedPath = basePath.resolve(userPath).normalize();
        if (!resolvedPath.startsWith(basePath)) {
            throw new IllegalArgumentException("Path traversal attempt: " + userPath);
        }
        return resolvedPath;
    }

}
