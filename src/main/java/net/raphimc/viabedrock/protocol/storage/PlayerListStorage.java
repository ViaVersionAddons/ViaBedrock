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
import com.viaversion.viaversion.util.Pair;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.GameType;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.GameMode;
import net.raphimc.viabedrock.protocol.rewriter.GameTypeRewriter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListStorage implements StorableObject {

    private final Map<UUID, Pair<Long, String>> playerList = new HashMap<>();

    private final Map<UUID, PlayerGameType> gameTypes = new HashMap<>();

    public Pair<Long, String> addPlayer(final UUID uuid, final long entityUniqueId, final String name) {
        final Pair<Long, String> previous = this.playerList.put(uuid, new Pair<>(entityUniqueId, name));
        final PlayerGameType gameType = this.gameTypes.get(uuid);
        if (gameType != null && gameType.entityUniqueId != entityUniqueId) {
            this.gameTypes.remove(uuid);
        }
        return previous;
    }

    public Pair<Long, String> removePlayer(final UUID uuid) {
        this.gameTypes.remove(uuid);
        return this.playerList.remove(uuid);
    }

    public void setGameType(final UUID uuid, final long entityUniqueId, final GameType gameType) {
        this.gameTypes.put(uuid, new PlayerGameType(entityUniqueId, gameType));
    }

    public GameMode getJavaGameMode(final UUID uuid, final GameType levelGameType) {
        final PlayerGameType gameType = this.gameTypes.get(uuid);
        return gameType == null ? GameMode.SURVIVAL : GameTypeRewriter.getEffectiveGameMode(gameType.value, levelGameType);
    }

    public Map<UUID, GameMode> getGameModeUpdates(final GameType previousDefault, final GameType nextDefault) {
        final Map<UUID, GameMode> updates = new HashMap<>();
        this.gameTypes.forEach((uuid, gameType) -> {
            final GameMode previous = GameTypeRewriter.getEffectiveGameMode(gameType.value, previousDefault);
            final GameMode next = GameTypeRewriter.getEffectiveGameMode(gameType.value, nextDefault);
            if (previous != next) {
                updates.put(uuid, next);
            }
        });
        return updates;
    }

    public boolean containsPlayer(final UUID uuid) {
        return this.playerList.containsKey(uuid);
    }

    public Pair<Long, String> getPlayer(final UUID uuid) {
        return this.playerList.get(uuid);
    }

    public Pair<UUID, String> getPlayer(final long entityUniqueId) {
        for (Map.Entry<UUID, Pair<Long, String>> entry : this.playerList.entrySet()) {
            if (entry.getValue().key() == entityUniqueId) {
                return new Pair<>(entry.getKey(), entry.getValue().value());
            }
        }

        return null;
    }

    private record PlayerGameType(long entityUniqueId, GameType value) {
    }

}
