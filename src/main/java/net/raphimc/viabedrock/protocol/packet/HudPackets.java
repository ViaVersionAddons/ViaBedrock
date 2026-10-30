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
package net.raphimc.viabedrock.protocol.packet;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.minecraft.GameProfile;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.util.Pair;
import net.lenni0451.mcstructs_bedrock.text.components.RootBedrockComponent;
import net.lenni0451.mcstructs_bedrock.text.components.TranslationBedrockComponent;
import net.lenni0451.mcstructs_bedrock.text.serializer.BedrockComponentSerializer;
import net.lenni0451.mcstructs_bedrock.text.utils.BedrockTranslator;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.entity.Entity;
import net.raphimc.viabedrock.api.model.scoreboard.ScoreboardEntry;
import net.raphimc.viabedrock.api.model.scoreboard.ScoreboardObjective;
import net.raphimc.viabedrock.api.util.*;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ClientboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.IdentityDefinition_Type;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.enums.java.ObjectiveAction;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.BossEventOperationType;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.CustomChatCompletionsAction;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.ObjectiveCriteriaRenderType;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.PlayerInfoUpdateAction;
import net.raphimc.viabedrock.protocol.model.SkinData;
import net.raphimc.viabedrock.protocol.provider.SkinProvider;
import net.raphimc.viabedrock.protocol.storage.*;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;

public final class HudPackets {

    public static void register(final BedrockProtocol protocol) {
        protocol.registerClientbound(ClientboundBedrockPackets.PLAYER_LIST, null, wrapper -> {
            final PlayerListStorage playerListStorage = wrapper.user().get(PlayerListStorage.class);
            final ScoreboardTracker scoreboardTracker = wrapper.user().get(ScoreboardTracker.class);

            wrapper.cancel();

            final int size = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // size

            final List<GameProfile> profiles = new ArrayList<>();

            final List<UUID> uuidsToRemove = new ArrayList<>();
            final List<UUID> uuidsToRemoveSafe = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                final PlayerListPacketType action = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT) == 1 ? PlayerListPacketType.Add : PlayerListPacketType.Remove; // action
                wrapper.read(Types.UNSIGNED_BYTE); // ordinal #blameMojang

                switch (action) {
                    case Add -> {
                        final UUID uuid = wrapper.read(BedrockTypes.UUID); // uuid
                        final long entityUniqueId = wrapper.read(BedrockTypes.VAR_LONG); // actorUniqueId
                        final String name = wrapper.read(BedrockTypes.STRING); // name
                        final String xuid = wrapper.read(BedrockTypes.STRING); // xuid
                        final String playFabId = wrapper.read(BedrockTypes.STRING); // skin PlayFab id
                        final String platformOnlineId = wrapper.read(BedrockTypes.STRING); // platform online id
                        final int deviceOs = wrapper.read(BedrockTypes.INT_LE); // device os
                        final SkinData skin = wrapper.read(BedrockTypes.SKIN); // skin
                        final boolean isTeacher = wrapper.read(Types.BOOLEAN); // is teacher
                        final boolean isHost = wrapper.read(Types.BOOLEAN); // is host
                        final boolean isSubClient = wrapper.read(Types.BOOLEAN); // is sub client
                        wrapper.read(BedrockTypes.INT_LE); // color (argb)

                        profiles.add(new GameProfile(name, uuid, new GameProfile.Property[]{
                            new GameProfile.Property("xuid", xuid),
                            new GameProfile.Property("playfab_id", playFabId),
                            new GameProfile.Property("platform_online_id", platformOnlineId),
                            new GameProfile.Property("device_os", String.valueOf(deviceOs)),
                            new GameProfile.Property("is_teacher", String.valueOf(isTeacher)),
                            new GameProfile.Property("is_host", String.valueOf(isHost)),
                            new GameProfile.Property("is_subclient", String.valueOf(isSubClient))
                        }));

                        Via.getManager().getProviders().get(SkinProvider.class).setSkin(wrapper.user(), uuid, skin);

                        final Pair<Long, String> entry = playerListStorage.addPlayer(uuid, entityUniqueId, name);
                        if (entry != null) {
                            uuidsToRemoveSafe.add(uuid);
                        }

                        final Pair<ScoreboardObjective, ScoreboardEntry> scoreboardEntry = scoreboardTracker.getEntryForPlayer(entityUniqueId);
                        if (scoreboardEntry != null) {
                            scoreboardEntry.key().updateEntry(wrapper.user(), scoreboardEntry.value());
                        }
                    }
                    case Remove -> {
                        final UUID uuid = wrapper.read(BedrockTypes.UUID); // uuid
                        uuidsToRemove.add(uuid);
                    }
                    default -> throw new IllegalStateException("Unhandled PlayerListPacketType: " + action);
                }
            }

            uuidsToRemoveSafe.addAll(uuidsToRemove); // TODO: This might be a little jank
            if (!uuidsToRemoveSafe.isEmpty()) {
                final PacketWrapper infoRemovePacket = PacketWrapper.create(ClientboundPackets26_3.PLAYER_INFO_REMOVE, wrapper.user());
                infoRemovePacket.write(Types.UUID_ARRAY, uuidsToRemoveSafe.toArray(new UUID[0])); // uuids
                infoRemovePacket.send(BedrockProtocol.class);

                if (!uuidsToRemove.isEmpty()) {
                    final List<String> removeNames = new ArrayList<>();
                    for (UUID uuid : uuidsToRemove) {
                        final Pair<Long, String> entry = playerListStorage.removePlayer(uuid);
                        if (entry != null) {
                            removeNames.add(entry.value());
                            final Pair<ScoreboardObjective, ScoreboardEntry> scoreboardEntry = scoreboardTracker.getEntryForPlayer(entry.key());
                            if (scoreboardEntry != null) {
                                scoreboardEntry.key().updateEntry(wrapper.user(), scoreboardEntry.value());
                            }
                        }
                    }

                    PacketFactory.sendJavaCustomChatCompletions(wrapper.user(), CustomChatCompletionsAction.REMOVE, removeNames.toArray(new String[0]));
                }
            }

            if (!profiles.isEmpty()) {
                final PacketWrapper infoAddPacket = PacketWrapper.create(ClientboundPackets26_3.PLAYER_INFO_UPDATE, wrapper.user());
                infoAddPacket.write(Types.PROFILE_ACTIONS_ENUM1_21_4, BitSets.create(8, PlayerInfoUpdateAction.ADD_PLAYER, PlayerInfoUpdateAction.UPDATE_LISTED, PlayerInfoUpdateAction.UPDATE_DISPLAY_NAME)); // actions
                infoAddPacket.write(Types.VAR_INT, profiles.size()); // length
                for (GameProfile profile : profiles) {
                    infoAddPacket.write(Types.UUID, profile.id()); // uuid
                    infoAddPacket.write(Types.STRING, StringUtil.encodeUuid(profile.id())); // username
                    infoAddPacket.write(Types.PROFILE_PROPERTY_ARRAY, profile.properties()); // properties
                    infoAddPacket.write(Types.BOOLEAN, true); // listed
                    infoAddPacket.write(Types.OPTIONAL_TAG, TextUtil.stringToNbt(profile.name())); // display name
                }
                infoAddPacket.send(BedrockProtocol.class);

                final List<String> names = new ArrayList<>();
                for (GameProfile profile : profiles) {
                    names.add(profile.name());
                }

                PacketFactory.sendJavaCustomChatCompletions(wrapper.user(), CustomChatCompletionsAction.ADD, names.toArray(new String[0]));
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.SET_TITLE, null, wrapper -> {
            final int rawType = wrapper.read(BedrockTypes.VAR_INT); // type
            final SetTitlePacket_TitleType type = SetTitlePacket_TitleType.getByValue(rawType);
            if (type == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown SetTitlePacketPayload_TitleType: " + rawType);
                wrapper.cancel();
                return;
            }
            String text = wrapper.read(BedrockTypes.STRING); // text
            final int fadeInTicks = wrapper.read(BedrockTypes.VAR_INT); // fade in ticks
            final int stayTicks = wrapper.read(BedrockTypes.VAR_INT); // stay ticks
            final int fadeOutTicks = wrapper.read(BedrockTypes.VAR_INT); // fade out ticks
            wrapper.read(BedrockTypes.STRING); // xuid
            wrapper.read(BedrockTypes.STRING); // platform online id
            wrapper.read(BedrockTypes.STRING); // filtered text

            final Function<String, String> translator = wrapper.user().get(ResourcePackStorage.class).getTexts().lookup();
            final String originalText = text;
            try {
                if (type.getValue() >= SetTitlePacket_TitleType.TitleTextObject.getValue() && type.getValue() <= SetTitlePacket_TitleType.ActionbarTextObject.getValue()) {
                    final RootBedrockComponent rootComponent = BedrockComponentSerializer.deserialize(text);
                    rootComponent.forEach(c -> {
                        if (c instanceof TranslationBedrockComponent) {
                            ((TranslationBedrockComponent) c).setTranslator(translator);
                        }
                    });
                    text = rootComponent.asString();
                }

                switch (type) {
                    case Clear, Reset -> {
                        wrapper.setPacketType(ClientboundPackets26_3.CLEAR_TITLES);
                        wrapper.write(Types.BOOLEAN, type == SetTitlePacket_TitleType.Reset); // reset
                    }
                    case Title, TitleTextObject -> {
                        wrapper.setPacketType(ClientboundPackets26_3.SET_TITLE_TEXT);
                        wrapper.write(Types.TAG, TextUtil.stringToNbt(text)); // text
                    }
                    case Subtitle, SubtitleTextObject -> {
                        wrapper.setPacketType(ClientboundPackets26_3.SET_SUBTITLE_TEXT);
                        wrapper.write(Types.TAG, TextUtil.stringToNbt(text)); // text
                    }
                    case Actionbar, ActionbarTextObject -> {
                        wrapper.setPacketType(ClientboundPackets26_3.SET_ACTION_BAR_TEXT);
                        wrapper.write(Types.TAG, TextUtil.stringToNbt(text)); // text
                    }
                    case Times -> {
                        wrapper.setPacketType(ClientboundPackets26_3.SET_TITLES_ANIMATION);
                        wrapper.write(Types.INT, fadeInTicks); // fade in ticks
                        wrapper.write(Types.INT, stayTicks); // stay ticks
                        wrapper.write(Types.INT, fadeOutTicks); // fade out ticks
                    }
                    default -> throw new IllegalStateException("Unhandled SetTitlePacketPayload_TitleType: " + type);
                }
            } catch (final Throwable e) { // Bedrock client silently ignores errors
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Error while translating '" + originalText + "'", e);
                wrapper.cancel();
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.SET_DISPLAY_OBJECTIVE, ClientboundPackets26_3.SET_DISPLAY_OBJECTIVE, wrapper -> {
            final ScoreboardTracker scoreboardTracker = wrapper.user().get(ScoreboardTracker.class);

            final String displaySlot = wrapper.read(BedrockTypes.STRING); // display slot
            final String objectiveName = wrapper.read(BedrockTypes.STRING); // objective name
            final String displayName = wrapper.read(BedrockTypes.STRING); // display name
            wrapper.read(BedrockTypes.STRING); // criteria
            final ObjectiveSortOrder sortOrder = ObjectiveSortOrder.getByValue(wrapper.read(BedrockTypes.VAR_INT), ObjectiveSortOrder.Descending); // sort order | Any invalid value is treated as no sorting, but Java Edition doesn't support that

            switch (displaySlot) {
                case "sidebar" -> wrapper.write(Types.VAR_INT, 1); // position
                case "belowname" -> wrapper.write(Types.VAR_INT, 2); // position
                case "list" -> wrapper.write(Types.VAR_INT, 0); // position
                default -> {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown bedrock scoreboard display slot: " + displaySlot);
                    wrapper.cancel();
                    return;
                }
            }
            wrapper.write(Types.STRING, objectiveName); // objective name

            if (objectiveName.isEmpty()) {
                return;
            }

            if (!scoreboardTracker.hasObjective(objectiveName)) {
                scoreboardTracker.addObjective(objectiveName, new ScoreboardObjective(objectiveName, sortOrder));

                final PacketWrapper scoreboardObjective = PacketWrapper.create(ClientboundPackets26_3.SET_OBJECTIVE, wrapper.user());
                scoreboardObjective.write(Types.STRING, objectiveName); // objective name
                scoreboardObjective.write(Types.BYTE, (byte) ObjectiveAction.ADD.ordinal()); // mode
                scoreboardObjective.write(Types.TAG, TextUtil.stringToNbt(wrapper.user().get(ResourcePackStorage.class).getTexts().translate(displayName))); // display name
                scoreboardObjective.write(Types.VAR_INT, ObjectiveCriteriaRenderType.INTEGER.ordinal()); // display mode
                scoreboardObjective.write(Types.BOOLEAN, false); // has number format
                scoreboardObjective.send(BedrockProtocol.class);
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.SET_SCORE, null, wrapper -> {
            wrapper.cancel();
            final ScoreboardTracker scoreboardTracker = wrapper.user().get(ScoreboardTracker.class);

            final int count = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // count
            for (int i = 0; i < count; i++) {
                final ScorePacketEntryAction action = ScorePacketEntryAction.getByValue(wrapper.read(BedrockTypes.UNSIGNED_VAR_INT)); // action
                wrapper.read(BedrockTypes.STRING); // type name #blameMojang
                final long scoreboardId = wrapper.read(BedrockTypes.VAR_LONG); // scoreboard id

                final String objectiveName;
                final ScoreboardEntry entry;
                boolean resetAll = false;

                switch (action) {
                    case Remove -> {
                        if (wrapper.read(Types.BOOLEAN)) {
                            objectiveName = wrapper.read(BedrockTypes.STRING);
                        } else {
                            objectiveName = null;
                            resetAll = true;
                        }
                        entry = null;
                    }
                    case ChangePlayer, ChangeEntity -> {
                        objectiveName = wrapper.read(BedrockTypes.STRING);
                        final int score = wrapper.read(BedrockTypes.INT_LE); // score
                        final long entityId = wrapper.read(BedrockTypes.VAR_LONG); // Entity Id
                        final IdentityDefinition_Type type = action == ScorePacketEntryAction.ChangePlayer ? IdentityDefinition_Type.Player : IdentityDefinition_Type.Entity;
                        entry = new ScoreboardEntry(score, type, entityId, null);
                    }
                    case ChangeFakePlayer -> {
                        objectiveName = wrapper.read(BedrockTypes.STRING);
                        final int score = wrapper.read(BedrockTypes.INT_LE); // score
                        final String name = wrapper.read(BedrockTypes.STRING); // Fake Player Name
                        entry = new ScoreboardEntry(score, IdentityDefinition_Type.FakePlayer, null, name);
                    }
                    default -> throw new IllegalStateException("Unhandled ScorePacketEntryAction: " + action);
                }

                if (resetAll) {
                    // TODO
                    return;
                }

                final ScoreboardObjective objective = scoreboardTracker.getObjective(objectiveName);
                final Pair<ScoreboardObjective, ScoreboardEntry> existingEntry = scoreboardTracker.getEntry(scoreboardId);
                if (existingEntry != null) {
                    existingEntry.key().removeEntry(wrapper.user(), scoreboardId);
                    if (entry != null && objective != null) {
                        existingEntry.value().setScore(entry.score());
                        objective.addEntry(wrapper.user(), scoreboardId, existingEntry.value());
                    }
                } else if (entry != null && objective != null) {
                    final ScoreboardEntry sameTargetEntry = objective.getEntryWithSameTarget(entry);
                    if (sameTargetEntry != null) {
                        sameTargetEntry.setScore(entry.score());
                        objective.updateEntryInPlace(wrapper.user(), sameTargetEntry);
                    } else if (entry.isValid()) {
                        objective.addEntry(wrapper.user(), scoreboardId, entry);
                    }
                }
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.SET_SCOREBOARD_IDENTITY, null, wrapper -> {
            wrapper.cancel();
            final ScoreboardTracker scoreboardTracker = wrapper.user().get(ScoreboardTracker.class);

            final byte rawAction = wrapper.read(Types.BYTE); // action
            final ScoreboardIdentityPacketType action = ScoreboardIdentityPacketType.getByValue(rawAction);
            if (action == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown ScoreboardIdentityPacketType: " + rawAction);
                return;
            }
            final int count = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // count
            for (int i = 0; i < count; i++) {
                final long scoreboardId = wrapper.read(BedrockTypes.VAR_LONG); // scoreboard id
                if (wrapper.read(Types.BOOLEAN)) {
                    wrapper.read(BedrockTypes.VAR_LONG); // playerId
                    // TODO: Only update if it points to the client player? wtf is this for mojang
                }

                final Pair<ScoreboardObjective, ScoreboardEntry> entry = scoreboardTracker.getEntry(scoreboardId);
                switch (action) {
                    case Update -> {
                        final long entityUniqueId = wrapper.read(BedrockTypes.VAR_LONG); // entity unique id
                        if (entry == null) {
                            continue;
                        }
                        final ScoreboardEntry scoreboardEntry = entry.value();

                        if (scoreboardEntry.entityUniqueId() == null) {
                            scoreboardEntry.updateTarget(IdentityDefinition_Type.Player, entityUniqueId, scoreboardEntry.fakePlayerName());
                            entry.key().updateEntry(wrapper.user(), scoreboardEntry);
                        }
                    }
                    case Remove -> {
                        if (entry == null) {
                            continue;
                        }
                        final ScoreboardEntry scoreboardEntry = entry.value();

                        if (scoreboardEntry.fakePlayerName() != null) {
                            scoreboardEntry.updateTarget(IdentityDefinition_Type.FakePlayer, null, scoreboardEntry.fakePlayerName());
                            entry.key().updateEntry(wrapper.user(), scoreboardEntry);
                        }
                    }
                    default -> throw new IllegalStateException("Unhandled ScoreboardIdentityPacketType: " + action);
                }
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.REMOVE_OBJECTIVE, ClientboundPackets26_3.SET_OBJECTIVE, new PacketHandlers() {
            @Override
            protected void register() {
                map(BedrockTypes.STRING, Types.STRING); // objective name
                create(Types.BYTE, (byte) ObjectiveAction.REMOVE.ordinal()); // mode
                handler(wrapper -> wrapper.user().get(ScoreboardTracker.class).removeObjective(wrapper.get(Types.STRING, 0)));
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.BOSS_EVENT, ClientboundPackets26_3.BOSS_EVENT, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);
            final long bossEntityUniqueId = wrapper.read(BedrockTypes.VAR_LONG); // boss entity unique id
            final int rawUpdateType = wrapper.read(Types.BYTE); // update type
            final BossEventUpdateType updateType = BossEventUpdateType.getByValue(rawUpdateType);
            if (updateType == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown BossEventUpdateType: " + rawUpdateType);
                wrapper.cancel();
                return;
            }

            final String bossName = wrapper.read(BedrockTypes.STRING);
            wrapper.read(BedrockTypes.STRING); // filtered boss name
            final float healthPercent = wrapper.read(BedrockTypes.FLOAT_LE);
            final int color = wrapper.read(Types.BYTE);
            final int overlay = wrapper.read(Types.BYTE);

            final Entity entity = entityTracker.getEntityByUid(bossEntityUniqueId);
            if (entity == null) {
                wrapper.cancel();
                return;
            }
            wrapper.write(Types.UUID, entity.javaUuid()); // uuid
            switch (updateType) {
                case Add -> {
                    if (!entity.hasBossBar()) {
                        entity.setHasBossBar(true);
                        wrapper.write(Types.VAR_INT, BossEventOperationType.ADD.ordinal()); // operation
                        wrapper.write(Types.TAG, TextUtil.stringToNbt(wrapper.user().get(ResourcePackStorage.class).getTexts().translate(bossName))); // name
                        wrapper.write(Types.FLOAT, healthPercent); // progress
                        wrapper.write(Types.VAR_INT, MathUtil.getOrFallback(color, 0, 5, 0)); // color
                        wrapper.write(Types.VAR_INT, overlay); // overlay
                        wrapper.write(Types.UNSIGNED_BYTE, (short) 0); // flags
                    } else {
                        wrapper.cancel();
                    }
                }
                case Remove -> {
                    entity.setHasBossBar(false);
                    wrapper.write(Types.VAR_INT, BossEventOperationType.REMOVE.ordinal()); // operation
                }
                case Update_Percent -> {
                    wrapper.write(Types.VAR_INT, BossEventOperationType.UPDATE_PROGRESS.ordinal()); // operation
                    wrapper.write(Types.FLOAT, healthPercent); // progress
                }
                case Update_Name -> {
                    wrapper.write(Types.VAR_INT, BossEventOperationType.UPDATE_NAME.ordinal()); // operation
                    wrapper.write(Types.TAG, TextUtil.stringToNbt(wrapper.user().get(ResourcePackStorage.class).getTexts().translate(bossName))); // name
                }
                case Update_Properties, Update_Style -> {
                    wrapper.write(Types.VAR_INT, BossEventOperationType.UPDATE_STYLE.ordinal()); // operation
                    wrapper.write(Types.VAR_INT, MathUtil.getOrFallback(color, 0, 5, 0)); // color
                    wrapper.write(Types.VAR_INT, overlay); // overlay
                }
                case PlayerAdded, PlayerRemoved, Query -> wrapper.cancel();
                default -> throw new IllegalStateException("Unhandled BossEventUpdateType: " + updateType);
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.DEATH_INFO, null, wrapper -> {
            wrapper.cancel();
            final GameSessionStorage gameSession = wrapper.user().get(GameSessionStorage.class);
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);
            final String message = wrapper.read(BedrockTypes.STRING); // death cause message
            final String[] parameters = wrapper.read(BedrockTypes.STRING_ARRAY); // parameters

            final Function<String, String> translator = wrapper.user().get(ResourcePackStorage.class).getTexts().lookup();
            gameSession.setDeathMessage(TextUtil.stringToTextComponent(BedrockTranslator.translate(message, translator, parameters)));
            if (entityTracker.getClientPlayer().isDead()) {
                final PacketWrapper playerCombatKill = PacketWrapper.create(ClientboundPackets26_3.PLAYER_COMBAT_KILL, wrapper.user());
                playerCombatKill.write(Types.VAR_INT, entityTracker.getClientPlayer().javaId()); // entity id
                playerCombatKill.write(Types.TAG, TextUtil.textComponentToNbt(gameSession.getDeathMessage())); // message
                playerCombatKill.send(BedrockProtocol.class);
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.GUI_DATA_PICK_ITEM, ClientboundPackets26_3.SYSTEM_CHAT, wrapper -> {
            final String itemName = wrapper.read(BedrockTypes.STRING); // item name
            final String itemEffects = wrapper.read(BedrockTypes.STRING); // item effects
            wrapper.read(BedrockTypes.INT_LE); // hotbar slot (Unused by the vanilla client)

            if (!itemEffects.isEmpty()) {
                wrapper.write(Types.TAG, TextUtil.stringToNbt(itemName + "\n" + itemEffects)); // message
            } else {
                wrapper.write(Types.TAG, TextUtil.stringToNbt(itemName)); // message
            }
            wrapper.write(Types.BOOLEAN, true); // overlay
        });
    }

    private HudPackets() {
    }

}
