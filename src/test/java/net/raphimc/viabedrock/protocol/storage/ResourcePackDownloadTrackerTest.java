/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2026 RK_01/RaphiMC and contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package net.raphimc.viabedrock.protocol.storage;

import net.raphimc.viabedrock.api.resourcepack.content.InMemoryContent;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PackType;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePackDownloadTrackerTest {

    @Test
    void assemblesOrderedReversedAndShuffledChunksAndOnlyCompletesOnce() throws Exception {
        final byte[] archive = archive();
        for (int ordering = 0; ordering < 3; ordering++) {
            final var download = new ResourcePackDownloadTracker().add("pack", archive.length, 17, digest(archive), false, PackType.Resources);
            final var chunks = new ArrayList<Integer>();
            for (int chunk = 0; chunk < download.receivedChunks().length; chunk++) {
                chunks.add(chunk);
            }
            if (ordering == 1) {
                Collections.reverse(chunks);
            } else if (ordering == 2) {
                Collections.shuffle(chunks, new Random(2193));
            }
            assertFalse(download.isComplete());
            assertThrows(IllegalStateException.class, download::verifyAndParse);
            for (int index = 0; index < chunks.size(); index++) {
                final int chunk = chunks.get(index);
                final int offset = chunk * 17;
                final boolean complete = download.acceptDataChunk(chunk, Arrays.copyOfRange(archive, offset, Math.min(offset + 17, archive.length)));
                assertEquals(index == chunks.size() - 1, complete);
                assertEquals(complete, download.isComplete());
            }
            assertArrayEquals(archive, download.data());
            assertArrayEquals(new byte[]{1, 2, 3}, download.verifyAndParse().content().get("payload"));
            assertThrows(IllegalStateException.class, () -> download.acceptDataChunk(chunks.get(0), new byte[0]));
        }
    }

    @Test
    void duplicateOutOfBoundsAndFailedCopiesDoNotAdvanceReceiptCount() {
        final var download = new ResourcePackDownloadTracker().add("pack", 4, 2, new byte[32], false, PackType.Resources);
        assertFalse(download.acceptDataChunk(1, new byte[]{3, 4}));
        assertThrows(IllegalStateException.class, () -> download.acceptDataChunk(1, new byte[]{3, 4}));
        assertThrows(IllegalStateException.class, () -> download.acceptDataChunk(-1, new byte[0]));
        assertThrows(IllegalStateException.class, () -> download.acceptDataChunk(2, new byte[0]));
        assertThrows(IndexOutOfBoundsException.class, () -> download.acceptDataChunk(0, new byte[5]));
        assertFalse(download.isComplete());
        assertTrue(download.acceptDataChunk(0, new byte[]{1, 2}));
        assertArrayEquals(new byte[]{1, 2, 3, 4}, download.data());
    }

    @Test
    void rejectsHashMismatchBeforeParsing() throws Exception {
        final byte[] archive = archive();
        final var download = new ResourcePackDownloadTracker().add("pack", archive.length, archive.length, new byte[32], false, PackType.Resources);
        assertTrue(download.acceptDataChunk(0, archive));
        final RuntimeException error = assertThrows(RuntimeException.class, download::verifyAndParse);
        assertEquals("Failed to verify data hash", error.getMessage());
        assertEquals("Hash mismatch", error.getCause().getMessage());
    }

    @Test
    void rejectsTruncatedZipAndMissingManifestWithTheExistingParseError() throws Exception {
        final InMemoryContent missingManifest = new InMemoryContent();
        missingManifest.put("payload", new byte[]{1});
        final InMemoryContent truncatedContent = new InMemoryContent();
        truncatedContent.putStored("payload", new byte[1024]);
        final byte[] truncated = Arrays.copyOf(truncatedContent.toZip(), 100);
        for (final byte[] invalid : new byte[][]{truncated, missingManifest.toZip()}) {
            final var download = new ResourcePackDownloadTracker().add("pack", invalid.length, invalid.length, digest(invalid), false, PackType.Resources);
            assertTrue(download.acceptDataChunk(0, invalid));
            final RuntimeException error = assertThrows(RuntimeException.class, download::verifyAndParse);
            assertEquals("Failed to parse resource pack data", error.getMessage());
            if (invalid == truncated) {
                assertInstanceOf(IOException.class, error.getCause());
            } else {
                assertInstanceOf(IllegalStateException.class, error.getCause().getCause());
                assertEquals("Missing manifest.json", error.getCause().getCause().getMessage());
            }
        }
    }

    @Test
    void synchronousCompatibilityMethodStillReturnsTheDecodedPack() throws Exception {
        final byte[] archive = archive();
        final var download = new ResourcePackDownloadTracker().add("pack", archive.length, archive.length, digest(archive), false, PackType.Resources);
        assertArrayEquals(new byte[]{1, 2, 3}, download.processDataChunk(0, archive).content().get("payload"));
    }

    private static byte[] archive() throws Exception {
        final InMemoryContent content = new InMemoryContent();
        content.putString("manifest.json", "{\"format_version\":3,\"header\":{\"uuid\":\"" + UUID.randomUUID()
                + "\",\"version\":\"1.0.0\",\"name\":\"test\"}}");
        content.put("payload", new byte[]{1, 2, 3});
        return content.toZip();
    }

    private static byte[] digest(final byte[] data) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(data);
    }

}
