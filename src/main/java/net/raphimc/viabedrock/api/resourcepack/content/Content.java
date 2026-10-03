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

import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.util.GsonUtil;
import net.raphimc.viabedrock.api.util.JsonUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class Content {

    private final Map<String, Map<String, String>> langCache = new HashMap<>();

    public abstract List<String> getFilesShallow(final String path, final String extension);

    public abstract List<String> getFilesDeep(final String path, final String extension);

    public String getFullPath(final String shortNamePath, final String... extensions) {
        if (this.contains(shortNamePath)) {
            return shortNamePath;
        }
        for (String extension : extensions) {
            final String path = shortNamePath + "." + extension;
            if (this.contains(path)) {
                return path;
            }
        }
        return null;
    }

    public abstract boolean contains(final String path);

    public abstract byte[] get(final String path);

    public abstract boolean put(final String path, final byte[] data);

    public String getString(final String path) {
        final byte[] bytes = this.get(path);
        if (bytes == null) {
            return null;
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    public boolean putString(final String path, final String string) {
        return this.put(path, string.getBytes(StandardCharsets.UTF_8));
    }

    public List<String> getLines(final String path) {
        final String string = this.getString(path);
        if (string == null) {
            return null;
        }

        return List.of(string.split("\\n"));
    }

    public boolean putLines(final String path, final List<String> lines) {
        return this.putString(path, String.join("\\n", lines));
    }

    public Map<String, String> getLang(final String path) {
        return this.langCache.computeIfAbsent(path, k -> {
            final List<String> lines = this.getLines(k);
            return Collections.unmodifiableMap(lines.stream()
                .filter(line -> !line.startsWith("##"))
                .filter(line -> line.contains("="))
                .map(line -> line.contains("##") ? line.substring(0, line.indexOf("##")) : line)
                .map(String::trim)
                .map(line -> line.split("=", 2))
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1], (o, n) -> n)));
        });
    }

    public JsonObject getJson(final String path) {
        final String string = this.getString(path);
        if (string == null) {
            return null;
        }

        return GsonUtil.getGson().fromJson(string.trim(), JsonObject.class);
    }

    public JsonObject getSortedJson(final String path) {
        return JsonUtil.sort(this.getJson(path), Comparator.naturalOrder());
    }

    public boolean putJson(final String path, final JsonObject json) {
        return this.putString(path, GsonUtil.getGson().toJson(json));
    }

    public LazyImage getShortnameImage(final String path) {
        return this.getImage(this.getFullPath(path, "png", "jpg"));
    }

    public LazyImage getImage(final String path) {
        final byte[] bytes = this.get(path);
        if (bytes == null) {
            return null;
        }

        final boolean isPng = bytes.length > 8 && bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50 && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47 && bytes[4] == (byte) 0x0D && bytes[5] == (byte) 0x0A && bytes[6] == (byte) 0x1A && bytes[7] == (byte) 0x0A;
        final boolean isJpg = bytes.length > 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF;
        if (!isPng && !isJpg) {
            return null;
        }

        return new LazyImage(bytes, isPng ? "png" : "jpg");
    }

    public boolean putPngImage(final String path, final LazyImage image) {
        return this.put(path, image.getPngBytes());
    }

    public boolean putPngImage(final String path, final BufferedImage image) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return this.put(path, baos.toByteArray());
    }

    public void putAll(final Content content) {
        for (String path : content.getFilesDeep("", "")) {
            this.copyFrom(content, path, path);
        }
    }

    public void copyFrom(final Content content, final String sourcePath, final String targetPath) {
        this.put(targetPath, content.get(sourcePath));
    }

    public byte[] toZip() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(4 * 1024 * 1024);
        final ZipOutputStream zipOutputStream = new ZipOutputStream(baos);
        zipOutputStream.setLevel(Deflater.BEST_SPEED);
        for (String path : this.getFilesDeep("", "").stream().sorted().toList()) {
            final ZipEntry entry = new ZipEntry(path);
            entry.setTime(0L);
            zipOutputStream.putNextEntry(entry);
            zipOutputStream.write(this.get(path));
            zipOutputStream.closeEntry();
        }
        zipOutputStream.close();
        return baos.toByteArray();
    }

    public static class LazyImage {

        private final byte[] bytes;
        private final String format;
        private BufferedImage image;

        public LazyImage(final byte[] bytes, final String format) {
            this.bytes = bytes;
            this.format = format;
        }

        public BufferedImage getImage() {
            if (this.image == null) {
                try {
                    this.image = ImageIO.read(new ByteArrayInputStream(this.bytes));
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return this.image;
        }

        public byte[] getPngBytes() {
            return this.getPngBytes(false);
        }

        public byte[] getPngBytes(final boolean forceWrite) {
            if (this.format.equals("png") && !forceWrite) {
                return this.bytes;
            } else {
                final BufferedImage image = this.getImage();
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    ImageIO.write(image, "png", baos);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
                return baos.toByteArray();
            }
        }

    }

}
