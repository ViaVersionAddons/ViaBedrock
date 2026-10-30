/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2026 RK_01/RaphiMC and contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package net.raphimc.viabedrock.protocol.packet;

import io.netty.channel.DefaultEventLoop;
import net.raphimc.viabedrock.api.resourcepack.ResourcePack;
import net.raphimc.viabedrock.api.resourcepack.ResourcePackExecutor;
import net.raphimc.viabedrock.api.resourcepack.content.InMemoryContent;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PackType;
import net.raphimc.viabedrock.protocol.storage.ResourcePackDownloadTracker;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePackDownloadCompletionTest {

    @Test
    void decodingWaitsForWorkersWithoutBlockingTheEventLoopOrPublishingEarly() throws Exception {
        final ResourcePackExecutor workers = new ResourcePackExecutor(1, 4, "download-test");
        final DefaultEventLoop loop = new DefaultEventLoop();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final ResourcePackDownloadTracker tracker = new ResourcePackDownloadTracker();
        final var download = completed(tracker, false);
        final CompletableFuture<ResourcePack> loaded = new CompletableFuture<>();
        try {
            workers.submit(() -> block(started, release));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            loop.submit(() -> ResourcePackDownloadCompletion.finish(workers::submit, loop, () -> {
                assertTrue(loop.inEventLoop());
                return true;
            }, tracker, "pack", download, pack -> {
                assertTrue(loop.inEventLoop());
                assertNull(tracker.get("pack"));
                loaded.complete(pack);
            }, loaded::completeExceptionally)).get(5, TimeUnit.SECONDS);
            assertEquals("heartbeat", loop.submit(() -> "heartbeat").get(5, TimeUnit.SECONDS));
            assertFalse(loaded.isDone());
            assertSame(download, tracker.get("pack"));
            release.countDown();
            assertArrayEquals(new byte[]{1, 2, 3}, loaded.get(5, TimeUnit.SECONDS).content().get("payload"));
        } finally {
            release.countDown();
            workers.stop();
            loop.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }
    }

    @Test
    void replacedDownloadTrackerLoadOwnerAndDisconnectedChannelDiscardCompletion() throws Exception {
        for (int stale = 0; stale < 4; stale++) {
            final ResourcePackExecutor workers = new ResourcePackExecutor(1, 4, "download-test");
            final DefaultEventLoop loop = new DefaultEventLoop();
            final CountDownLatch started = new CountDownLatch(1);
            final CountDownLatch release = new CountDownLatch(1);
            final ResourcePackDownloadTracker tracker = new ResourcePackDownloadTracker();
            final var download = completed(tracker, stale % 2 != 0);
            final AtomicReference<ResourcePackDownloadTracker> currentTracker = new AtomicReference<>(tracker);
            final Object expectedOwner = new Object();
            final AtomicReference<Object> currentOwner = new AtomicReference<>(expectedOwner);
            final AtomicBoolean active = new AtomicBoolean(true);
            final AtomicInteger publications = new AtomicInteger();
            final AtomicInteger failures = new AtomicInteger();
            try {
                workers.submit(() -> block(started, release));
                assertTrue(started.await(5, TimeUnit.SECONDS));
                loop.submit(() -> ResourcePackDownloadCompletion.finish(workers::submit, loop,
                        () -> active.get() && currentTracker.get() == tracker && currentOwner.get() == expectedOwner,
                        tracker, "pack", download, pack -> publications.incrementAndGet(), error -> failures.incrementAndGet())).get(5, TimeUnit.SECONDS);
                final int replacement = stale;
                loop.submit(() -> {
                    switch (replacement) {
                        case 0 -> tracker.add("pack", 2, 2, new byte[32], false, PackType.Resources);
                        case 1 -> currentTracker.set(new ResourcePackDownloadTracker());
                        case 2 -> currentOwner.set(new Object());
                        case 3 -> active.set(false);
                        default -> throw new AssertionError();
                    }
                }).get(5, TimeUnit.SECONDS);
                release.countDown();
                workers.submit(() -> "worker-barrier").get(5, TimeUnit.SECONDS);
                loop.submit(() -> "completion-barrier").get(5, TimeUnit.SECONDS);
                assertEquals(0, publications.get());
                assertEquals(0, failures.get());
                assertNotNull(tracker.get("pack"));
                if (stale == 0) {
                    assertNotSame(download, tracker.get("pack"));
                }
            } finally {
                release.countDown();
                workers.stop();
                loop.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
            }
        }
    }

    @Test
    void verificationFailureReportsOnTheEventLoopWithoutPublishing() throws Exception {
        final ResourcePackExecutor workers = new ResourcePackExecutor(1, 4, "download-test");
        final DefaultEventLoop loop = new DefaultEventLoop();
        final ResourcePackDownloadTracker tracker = new ResourcePackDownloadTracker();
        final var download = completed(tracker, true);
        final CompletableFuture<Throwable> failed = new CompletableFuture<>();
        final AtomicInteger publications = new AtomicInteger();
        try {
            loop.submit(() -> ResourcePackDownloadCompletion.finish(workers::submit, loop, () -> true, tracker, "pack", download,
                    pack -> publications.incrementAndGet(), error -> {
                        assertTrue(loop.inEventLoop());
                        failed.complete(error);
                    })).get(5, TimeUnit.SECONDS);
            assertEquals("Failed to verify data hash", failed.get(5, TimeUnit.SECONDS).getMessage());
            assertEquals(0, publications.get());
            assertSame(download, tracker.get("pack"));
        } finally {
            workers.stop();
            loop.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }
    }

    @Test
    void rejectedDecoderReportsFailureWithoutRunningOnTheCaller() throws Exception {
        final ResourcePackExecutor workers = new ResourcePackExecutor(1, 4, "download-test");
        final DefaultEventLoop loop = new DefaultEventLoop();
        final ResourcePackDownloadTracker tracker = new ResourcePackDownloadTracker();
        final var download = completed(tracker, true);
        final CompletableFuture<Throwable> failed = new CompletableFuture<>();
        workers.stop();
        try {
            loop.submit(() -> ResourcePackDownloadCompletion.finish(workers::submit, loop, () -> true, tracker, "pack", download,
                    pack -> fail("A rejected decoder cannot publish"), error -> {
                        assertTrue(loop.inEventLoop());
                        failed.complete(error);
                    })).get(5, TimeUnit.SECONDS);
            assertInstanceOf(RejectedExecutionException.class, failed.get(5, TimeUnit.SECONDS));
            assertSame(download, tracker.get("pack"));
        } finally {
            loop.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }
    }

    @Test
    void shutdownSettlesQueuedDecoderInsteadOfHangingNegotiation() throws Exception {
        final ResourcePackExecutor workers = new ResourcePackExecutor(1, 4, "download-test");
        final DefaultEventLoop loop = new DefaultEventLoop();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final ResourcePackDownloadTracker tracker = new ResourcePackDownloadTracker();
        final var download = completed(tracker, false);
        final CompletableFuture<Throwable> failed = new CompletableFuture<>();
        try {
            workers.submit(() -> block(started, release));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            loop.submit(() -> ResourcePackDownloadCompletion.finish(workers::submit, loop, () -> true, tracker, "pack", download,
                    pack -> fail("A cancelled decoder cannot publish"), failed::complete)).get(5, TimeUnit.SECONDS);
            workers.stop();
            assertInstanceOf(CancellationException.class, failed.get(5, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            workers.stop();
            loop.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }
    }

    private static ResourcePackDownloadTracker.Download completed(final ResourcePackDownloadTracker tracker, final boolean mismatch) throws Exception {
        final InMemoryContent content = new InMemoryContent();
        content.putString("manifest.json", "{\"format_version\":3,\"header\":{\"uuid\":\"" + UUID.randomUUID()
                + "\",\"version\":\"1.0.0\",\"name\":\"test\"}}");
        content.put("payload", new byte[]{1, 2, 3});
        final byte[] archive = content.toZip();
        final byte[] hash = mismatch ? new byte[32] : MessageDigest.getInstance("SHA-256").digest(archive);
        final var download = tracker.add("pack", archive.length, archive.length, hash, false, PackType.Resources);
        assertTrue(download.acceptDataChunk(0, archive));
        return download;
    }

    private static String block(final CountDownLatch started, final CountDownLatch release) {
        started.countDown();
        boolean interrupted = false;
        while (release.getCount() != 0) {
            try {
                release.await();
            } catch (final InterruptedException ignored) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return "released";
    }

}
