/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2026 RK_01/RaphiMC and contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package net.raphimc.viabedrock.api.resourcepack;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Bounded background work that never falls back to the connection's event loop. */
public final class ResourcePackExecutor {

    private final ThreadPoolExecutor executor;
    private final Set<CompletableFuture<?>> tasks = new HashSet<>();

    public ResourcePackExecutor(final int workers, final int queuedTasks, final String threadName) {
        final int threads = Math.max(1, Math.min(workers, Runtime.getRuntime().availableProcessors()));
        this.executor = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queuedTasks), task -> {
            final Thread thread = new Thread(task, threadName);
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    }

    public synchronized <T> CompletableFuture<T> submit(final Supplier<T> work) {
        final CompletableFuture<T> result = new CompletableFuture<>();
        this.tasks.add(result);
        result.whenComplete((value, error) -> {
            synchronized (this) {
                this.tasks.remove(result);
            }
        });
        try {
            this.executor.execute(() -> {
                if (result.isDone()) {
                    return;
                }
                try {
                    result.complete(work.get());
                } catch (final Throwable error) {
                    result.completeExceptionally(error);
                }
            });
        } catch (final RejectedExecutionException error) {
            result.completeExceptionally(error);
        }
        return result;
    }

    public void stop() {
        final Set<CompletableFuture<?>> waiting;
        synchronized (this) {
            this.executor.shutdownNow();
            waiting = Set.copyOf(this.tasks);
        }
        for (final CompletableFuture<?> task : waiting) {
            task.completeExceptionally(new CancellationException("Resource pack executor stopped"));
        }
    }

}
