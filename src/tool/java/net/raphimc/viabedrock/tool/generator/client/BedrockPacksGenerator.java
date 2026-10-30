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
package net.raphimc.viabedrock.tool.generator.client;

import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonParser;
import net.raphimc.viabedrock.tool.ToolArgs;
import net.raphimc.viabedrock.tool.ToolPaths;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class BedrockPacksGenerator {

    private static final String MOJANG_LICENSE = """
        (c) Mojang AB. All rights reserved.

        By downloading the files in this repository, you agree to the [Minecraft End User License Agreement](https://www.minecraft.net/en-us/eula) and that these files are subject to its terms.
        """;

    public static void main(final String[] args) throws Throwable {
        final ToolArgs toolArgs = ToolArgs.parse(args);
        final File clientDataDir = ToolPaths.clientDataDir(toolArgs).toFile();
        final File resourcePacksDir = new File(clientDataDir, "resource_packs_unpacked");

        final File resourcePacksOutputDir = ToolPaths.RESOURCE_PACKS.toFile();
        clearDirectory(resourcePacksOutputDir);
        final Set<String> packKeys = new LinkedHashSet<>();
        for (File packDir : listSorted(resourcePacksDir)) {
            if (packDir.getName().equals("beta")) {
                continue;
            }
            if (new File(packDir, "manifest.json").exists()) {
                System.out.println("Processing pack: " + packDir.getName());
            } else {
                System.out.println("Skipping pack without manifest: " + packDir.getName());
                continue;
            }
            packKeys.add(readPackKey(new File(packDir, "manifest.json")));

            final File outputFile = new File(resourcePacksOutputDir, packDir.getName() + ".mcpack");

            try (FileSystem fs = FileSystems.newFileSystem(new URI("jar:" + outputFile.toURI()), Map.of("create", "true"))) {
                final Path fsRoot = fs.getRootDirectories().iterator().next();

                addLicense(fsRoot);
                copyFileIfExists(packDir, fsRoot, "manifest.json");
                copyFileIfExists(packDir, fsRoot, "texts/en_US.lang");
                copyFileIfExists(packDir, fsRoot, "font/glyph_E0.png");
                copyFileIfExists(packDir, fsRoot, "font/glyph_E1.png");
                copyFolder(packDir, fsRoot, "biomes");
                copyFolder(packDir, fsRoot, "fogs");
                copyFolder(packDir, fsRoot, "render_controllers");

                final File brArchiveDir = new File(packDir, "__brarchive");
                if (brArchiveDir.exists() && brArchiveDir.isDirectory()) {
                    copyFolder(brArchiveDir, fsRoot, "biomes");
                    copyFolder(brArchiveDir, fsRoot, "fogs");
                    copyFolder(brArchiveDir, fsRoot, "render_controllers");
                }

                removeTimestamps(fsRoot);
            }
        }

        final File skinPacksOutputDir = ToolPaths.SKIN_PACKS.toFile();
        clearDirectory(skinPacksOutputDir);
        try (FileSystem fs = FileSystems.newFileSystem(new URI("jar:" + new File(skinPacksOutputDir, "vanilla.mcpack").toURI()), Map.of("create", "true"))) {
            final Path fsRoot = fs.getRootDirectories().iterator().next();
            addLicense(fsRoot);
            copyFolder(new File(clientDataDir, "skin_packs/vanilla"), fsRoot, ".");

            removeTimestamps(fsRoot);
        }

        reportMissingPackKeys(packKeys);
    }

    /**
     * The load order of the vanilla packs is hand maintained, so new packs are only reported instead of being added blindly.
     */
    private static void reportMissingPackKeys(final Set<String> packKeys) throws IOException {
        final Path knownPacksFile = ToolPaths.CUSTOM_DATA.resolve("vanilla_resource_packs.json");
        final JsonArray knownPacks = JsonParser.parseString(Files.readString(knownPacksFile)).getAsJsonArray();
        final Set<String> knownKeys = StreamSupport.stream(knownPacks.spliterator(), false).map(JsonElement::getAsString).collect(Collectors.toSet());

        final List<String> missingKeys = packKeys.stream().filter(key -> !knownKeys.contains(key)).toList();
        if (missingKeys.isEmpty()) {
            return;
        }
        System.out.println();
        System.out.println("The following packs are missing from " + ToolPaths.describe(knownPacksFile) + ". Add them in the order the client loads them:");
        missingKeys.forEach(key -> System.out.println("  \"" + key + "\","));
    }

    private static String readPackKey(final File manifestFile) throws IOException {
        final JsonObject manifest = JsonParser.parseString(Files.readString(manifestFile.toPath())).getAsJsonObject();
        final JsonObject header = manifest.getAsJsonObject("header");
        final JsonElement version = header.get("version");
        final String versionString = version.isJsonArray()
            ? StreamSupport.stream(version.getAsJsonArray().spliterator(), false).map(JsonElement::getAsString).collect(Collectors.joining("."))
            : version.getAsString();
        return header.get("uuid").getAsString() + "_" + versionString;
    }

    private static List<File> listSorted(final File directory) {
        final File[] files = directory.listFiles();
        if (files == null) {
            throw new IllegalStateException("Could not list " + directory);
        }
        final List<File> sorted = new ArrayList<>(List.of(files));
        sorted.sort(File::compareTo);
        return sorted;
    }

    private static void clearDirectory(final File directory) throws IOException {
        Files.createDirectories(directory.toPath());
        for (File file : listSorted(directory)) {
            Files.delete(file.toPath());
        }
    }

    private static void addLicense(final Path targetRoot) throws IOException {
        final Path licensePath = targetRoot.resolve("LICENSE");
        Files.write(licensePath, MOJANG_LICENSE.getBytes());
    }

    private static void copyFileIfExists(final File packDir, final Path targetRoot, final String filePath) throws IOException {
        final File file = new File(packDir, filePath);
        if (file.exists()) {
            Files.createDirectories(targetRoot.resolve(filePath).getParent());
            Files.copy(file.toPath(), targetRoot.resolve(filePath));
        }
    }

    private static void copyFolder(final File packDir, final Path targetRoot, final String folderPath) throws IOException {
        final File folder = new File(packDir, folderPath);
        if (folder.exists()) {
            final Path sourcePath = folder.toPath();
            final Path targetPath = targetRoot.resolve(folderPath);
            Files.walk(sourcePath).forEach(path -> {
                try {
                    final Path resolvedTargetPath = targetPath.resolve(sourcePath.relativize(path).toString());
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(resolvedTargetPath);
                    } else {
                        Files.copy(path, resolvedTargetPath);
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static void removeTimestamps(final Path fsRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(fsRoot)) {
            paths.forEach(path -> {
                try {
                    final BasicFileAttributeView attributeView = Files.getFileAttributeView(path, BasicFileAttributeView.class);
                    attributeView.setTimes(FileTime.from(Instant.EPOCH), FileTime.from(Instant.EPOCH), FileTime.from(Instant.EPOCH));
                } catch (final NoSuchFileException ignored) {
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private BedrockPacksGenerator() {
    }

}
