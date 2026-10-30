/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2026 RK_01/RaphiMC and contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package net.raphimc.viabedrock.protocol.packet;

import net.raphimc.viabedrock.api.resourcepack.ResourcePack;
import net.raphimc.viabedrock.protocol.storage.ResourcePackDownloadTracker;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Verifies completed downloads in workers and publishes only the receiving session's current entry. */
final class ResourcePackDownloadCompletion {

    private ResourcePackDownloadCompletion() {
    }

    static void finish(final Function<Supplier<ResourcePack>, CompletableFuture<ResourcePack>> workers, final Executor eventLoop,
                       final BooleanSupplier sessionCurrent, final ResourcePackDownloadTracker tracker, final String key,
                       final ResourcePackDownloadTracker.Download download, final Consumer<ResourcePack> loaded, final Consumer<Throwable> failed) {
        ResourcePackPreparation.prepare(workers, eventLoop, () -> sessionCurrent.getAsBoolean() && tracker.get(key) == download,
                download::verifyAndParse, (pack, error) -> {
                    if (error != null) {
                        failed.accept(error);
                        return;
                    }
                    tracker.remove(key);
                    loaded.accept(pack);
                }, failed);
    }

}
