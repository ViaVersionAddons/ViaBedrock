/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2026 RK_01/RaphiMC and contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package net.raphimc.viabedrock.api.resourcepack;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePackExecutorTest {

    @Test
    void saturationRejectsWithoutRunningOnTheCaller() throws Exception {
        final ResourcePackExecutor workers = new ResourcePackExecutor(1, 1, "pack-test");
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicBoolean rejectedWorkRan = new AtomicBoolean();
        try {
            final var running = workers.submit(() -> await(started, release));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            final var queued = workers.submit(() -> "queued");
            final var rejected = workers.submit(() -> {
                rejectedWorkRan.set(true);
                return "rejected";
            });
            assertInstanceOf(RejectedExecutionException.class, assertThrows(ExecutionException.class,
                    () -> rejected.get(5, TimeUnit.SECONDS)).getCause());
            assertFalse(rejectedWorkRan.get());
            release.countDown();
            assertEquals("running", running.get(5, TimeUnit.SECONDS));
            assertEquals("queued", queued.get(5, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            workers.stop();
        }
    }

    @Test
    void shutdownCompletesQueuedAndRunningFuturesAndRejectsNewWork() throws Exception {
        final ResourcePackExecutor workers = new ResourcePackExecutor(1, 1, "pack-test");
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicBoolean queuedWorkRan = new AtomicBoolean();
        final var running = workers.submit(() -> await(started, release));
        assertTrue(started.await(5, TimeUnit.SECONDS));
        final var queued = workers.submit(() -> {
            queuedWorkRan.set(true);
            return "queued";
        });
        workers.stop();
        try {
            assertThrows(CancellationException.class, () -> running.get(5, TimeUnit.SECONDS));
            assertThrows(CancellationException.class, () -> queued.get(5, TimeUnit.SECONDS));
            assertFalse(queuedWorkRan.get());
            assertInstanceOf(RejectedExecutionException.class, assertThrows(ExecutionException.class,
                    () -> workers.submit(() -> "late").get(5, TimeUnit.SECONDS)).getCause());
        } finally {
            release.countDown();
        }
    }

    private static String await(final CountDownLatch started, final CountDownLatch release) {
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
        return "running";
    }

}
