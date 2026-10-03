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
package net.raphimc.viabedrock.api.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FileSystemUtil {

    public static void writeAtomically(final Path path, final byte[] bytes) throws IOException {
        final Path temporary = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (final AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static Map<Path, byte[]> getFilesInDirectory(final String assetPath) throws IOException, URISyntaxException {
        final Path path = getPath(FileSystemUtil.class.getClassLoader().getResource(assetPath).toURI());
        return getFilesInPath(path);
    }

    @SuppressWarnings({"DuplicateExpressions", "resource"})
    private static Path getPath(final URI uri) throws IOException {
        try {
            return Paths.get(uri);
        } catch (final FileSystemNotFoundException e) {
            FileSystems.newFileSystem(uri, Collections.emptyMap());
            return Paths.get(uri);
        }
    }

    private static Map<Path, byte[]> getFilesInPath(final Path path) throws IOException {
        try (Stream<Path> stream = Files.list(path)) {
            return stream
                .filter(Files::isRegularFile)
                .sorted(Comparator.comparing(Path::toString))
                .collect(Collectors.toMap(f -> f, f -> {
                    try {
                        return Files.readAllBytes(f);
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }, (u, v) -> {
                    throw new IllegalStateException("Duplicate key");
                }, LinkedHashMap::new));
        }
    }

    private FileSystemUtil() {
    }

}
