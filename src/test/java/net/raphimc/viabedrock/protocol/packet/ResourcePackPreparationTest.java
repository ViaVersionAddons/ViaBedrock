/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2026 RK_01/RaphiMC and contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package net.raphimc.viabedrock.protocol.packet;

import io.netty.channel.DefaultEventLoop;
import net.raphimc.viabedrock.api.resourcepack.ResourcePackExecutor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePackPreparationTest {

    @Test
    void expensivePreparationDoesNotBlockTheEventLoopAndPublishesOnIt() throws Exception {
        final ResourcePackExecutor workers = new ResourcePackExecutor(1, 1, "prepare-test");
        final DefaultEventLoop loop = new DefaultEventLoop();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CompletableFuture<String> published = new CompletableFuture<>();
        try {
            loop.submit(() -> ResourcePackPreparation.prepare(workers::submit, loop, () -> {
                assertTrue(loop.inEventLoop());
                return true;
            }, () -> {
                assertFalse(loop.inEventLoop());
                started.countDown();
                try {
                    assertTrue(release.await(5, TimeUnit.SECONDS));
                } catch (final InterruptedException interrupted) {
                    throw new IllegalStateException(interrupted);
                }
                return "prepared";
            }, (value, error) -> {
                assertTrue(loop.inEventLoop());
                if (error == null) {
                    published.complete(value);
                } else {
                    published.completeExceptionally(error);
                }
            }, published::completeExceptionally)).get(5, TimeUnit.SECONDS);
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertEquals("heartbeat", loop.submit(() -> "heartbeat").get(5, TimeUnit.SECONDS));
            assertFalse(published.isDone());
            release.countDown();
            assertEquals("prepared", published.get(5, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            workers.stop();
            loop.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }
    }

    @Test
    void replacedOrDisconnectedOwnershipDiscardsWorkerCompletion() throws Exception {
        final ResourcePackExecutor workers = new ResourcePackExecutor(1, 1, "prepare-test");
        final DefaultEventLoop loop = new DefaultEventLoop();
        final AtomicBoolean current = new AtomicBoolean(true);
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger publications = new AtomicInteger();
        try {
            loop.submit(() -> ResourcePackPreparation.prepare(workers::submit, loop, current::get, () -> {
                started.countDown();
                try {
                    assertTrue(release.await(5, TimeUnit.SECONDS));
                } catch (final InterruptedException interrupted) {
                    throw new IllegalStateException(interrupted);
                }
                return "stale";
            }, (value, error) -> publications.incrementAndGet(), error -> fail("Stale preparation must discard failure"))).get(5, TimeUnit.SECONDS);
            assertTrue(started.await(5, TimeUnit.SECONDS));
            loop.submit(() -> current.set(false)).get(5, TimeUnit.SECONDS);
            release.countDown();
            workers.submit(() -> "worker-barrier").get(5, TimeUnit.SECONDS);
            loop.submit(() -> "publication-barrier").get(5, TimeUnit.SECONDS);
            assertEquals(0, publications.get());
        } finally {
            release.countDown();
            workers.stop();
            loop.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }
    }

    @Test
    void rejectedWorkReportsFailureOnTheEventLoop() throws Exception {
        final ResourcePackExecutor workers = new ResourcePackExecutor(1, 1, "prepare-test");
        final DefaultEventLoop loop = new DefaultEventLoop();
        final CompletableFuture<Throwable> failed = new CompletableFuture<>();
        workers.stop();
        try {
            loop.submit(() -> ResourcePackPreparation.prepare(workers::submit, loop, () -> true, () -> {
                fail("Rejected work must not run");
                return "unexpected";
            }, (value, error) -> {
                assertTrue(loop.inEventLoop());
                failed.complete(error);
            }, failed::complete)).get(5, TimeUnit.SECONDS);
            assertInstanceOf(java.util.concurrent.RejectedExecutionException.class, failed.get(5, TimeUnit.SECONDS));
        } finally {
            loop.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }
    }

    @Test
    void throwingPublicationReportsFailureOnTheEventLoop() throws Exception {
        final ResourcePackExecutor workers = new ResourcePackExecutor(1, 1, "prepare-test");
        final DefaultEventLoop loop = new DefaultEventLoop();
        final CompletableFuture<Throwable> failed = new CompletableFuture<>();
        final AtomicInteger successfulPublications = new AtomicInteger();
        final IllegalStateException publicationError = new IllegalStateException("Publishing failed");
        try {
            loop.submit(() -> ResourcePackPreparation.prepare(workers::submit, loop, () -> true, () -> "prepared", (value, error) -> {
                assertTrue(loop.inEventLoop());
                assertNull(error);
                if (value.equals("prepared")) {
                    throw publicationError;
                }
                successfulPublications.incrementAndGet();
            }, error -> {
                assertTrue(loop.inEventLoop());
                failed.complete(error);
            })).get(5, TimeUnit.SECONDS);
            assertSame(publicationError, failed.get(5, TimeUnit.SECONDS));
            assertEquals(0, successfulPublications.get());
        } finally {
            workers.stop();
            loop.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
        }
    }

}
