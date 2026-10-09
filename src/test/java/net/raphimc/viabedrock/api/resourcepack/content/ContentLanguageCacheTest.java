/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2026 RK_01/RaphiMC and contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package net.raphimc.viabedrock.api.resourcepack.content;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ContentLanguageCacheTest {

    @Test
    void concurrentConnectionsShareOneImmutableParsedTable() throws Exception {
        final int connections = 8;
        final CountDownLatch started = new CountDownLatch(connections);
        final AtomicInteger reads = new AtomicInteger();
        final InMemoryContent content = new InMemoryContent() {
            @Override
            public List<String> getLines(final String path) {
                reads.incrementAndGet();
                try {
                    if (!started.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException();
                    }
                } catch (final InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
                return List.of("a=first", "a=last", "b=value ## comment", "## ignored");
            }
        };
        final var executor = Executors.newFixedThreadPool(connections);
        try {
            final List<Future<Map<String, String>>> tables = new ArrayList<>();
            for (int index = 0; index < connections; index++) {
                tables.add(executor.submit(() -> {
                    started.countDown();
                    return content.getLang("texts/en_US.lang");
                }));
            }
            final Map<String, String> first = tables.get(0).get(5, TimeUnit.SECONDS);
            assertEquals(Map.of("a", "last", "b", "value"), first);
            for (final var table : tables) {
                assertSame(first, table.get(5, TimeUnit.SECONDS));
            }
            assertEquals(1, reads.get());
            assertThrows(UnsupportedOperationException.class, () -> first.clear());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

}
