/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2026 RK_01/RaphiMC and contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package net.raphimc.viabedrock.protocol.packet;

import java.util.concurrent.Executor;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.Consumer;

/** Keeps connection ownership checks and publication on the event loop while preparing packs in workers. */
final class ResourcePackPreparation {

    private ResourcePackPreparation() {
    }

    static <T> void prepare(final Function<Supplier<T>, CompletableFuture<T>> workers, final Executor eventLoop, final BooleanSupplier current,
                            final Supplier<T> work, final BiConsumer<T, Throwable> complete, final Consumer<Throwable> failed) {
        if (!current.getAsBoolean()) {
            return;
        }
        workers.apply(work).whenComplete((value, error) -> eventLoop.execute(() -> {
            if (current.getAsBoolean()) {
                try {
                    complete.accept(value, error);
                } catch (final Throwable publicationError) {
                    failed.accept(publicationError);
                }
            }
        }));
    }

}
