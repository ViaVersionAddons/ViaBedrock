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

import com.viaversion.viaversion.api.connection.StorableObject;
import net.raphimc.viabedrock.api.resourcepack.ResourcePack;
import net.raphimc.viabedrock.api.resourcepack.content.ZipContent;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PackType;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ResourcePackDownloadTracker implements StorableObject {

    private final Map<String, Download> downloads = new HashMap<>();

    public Download add(final String key, final long size, final long chunkSize, final byte[] hash, final boolean premium, final PackType type) {
        final Download download = new Download(hash, premium, type, chunkSize, new boolean[Math.toIntExact((long) Math.ceil((double) size / chunkSize))], new byte[Math.toIntExact(size)]);
        this.downloads.put(key, download);
        return download;
    }

    public Download get(final String key) {
        return this.downloads.get(key);
    }

    public void remove(final String key) {
        this.downloads.remove(key);
    }

    /** Chunk assembly is owned by the event loop; a completed buffer is read only by its decoder. */
    public static final class Download {

        private final byte[] hash;
        private final boolean premium;
        private final PackType type;
        private final long chunkSize;
        private final boolean[] receivedChunks;
        private final byte[] data;
        private int remainingChunks;

        public Download(final byte[] hash, final boolean premium, final PackType type, final long chunkSize, final boolean[] receivedChunks, final byte[] data) {
            this.hash = hash;
            this.premium = premium;
            this.type = type;
            this.chunkSize = chunkSize;
            this.receivedChunks = receivedChunks;
            this.data = data;
            for (final boolean received : receivedChunks) {
                if (!received) {
                    this.remainingChunks++;
                }
            }
        }

        /** Copies one chunk without hashing or inflating the complete archive. */
        public boolean acceptDataChunk(final long chunk, final byte[] data) {
            if (chunk < 0 || chunk >= this.receivedChunks.length) {
                throw new IllegalStateException("Received out of bounds chunk");
            }
            final int index = Math.toIntExact(chunk);
            if (this.receivedChunks[index]) {
                throw new IllegalStateException("Received duplicate chunk");
            }

            System.arraycopy(data, 0, this.data, Math.toIntExact(chunk * this.chunkSize), data.length);
            this.receivedChunks[index] = true;
            return --this.remainingChunks == 0;
        }

        /** Retained for integrations using synchronous decoding; packet handlers use acceptDataChunk. */
        public ResourcePack processDataChunk(final long chunk, final byte[] data) {
            return this.acceptDataChunk(chunk, data) ? this.verifyAndParse() : null;
        }

        public ResourcePack verifyAndParse() {
            if (!this.isComplete()) {
                throw new IllegalStateException("Resource pack data is incomplete");
            }
            try {
                final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                final byte[] hash = sha256.digest(this.data);
                if (!Arrays.equals(hash, this.hash)) {
                    throw new IllegalStateException("Hash mismatch");
                }
            } catch (final Throwable e) {
                throw new RuntimeException("Failed to verify data hash", e);
            }
            try {
                return new ResourcePack(new ZipContent(this.data));
            } catch (final Throwable e) {
                throw new RuntimeException("Failed to parse resource pack data", e);
            }
        }

        public boolean isComplete() {
            return this.remainingChunks == 0;
        }

        public byte[] hash() {
            return this.hash;
        }

        public boolean premium() {
            return this.premium;
        }

        public PackType type() {
            return this.type;
        }

        public long chunkSize() {
            return this.chunkSize;
        }

        public boolean[] receivedChunks() {
            return this.receivedChunks;
        }

        public byte[] data() {
            return this.data;
        }

    }

}
