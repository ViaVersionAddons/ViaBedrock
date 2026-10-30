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

import com.vdurmont.semver4j.Semver;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.GameProfile;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.fastutil.ints.IntIntImmutablePair;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.v1_7.ClientboundBaseProtocol1_7;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundConfigurationPackets26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.entity.ClientPlayerEntity;
import net.raphimc.viabedrock.api.resourcepack.definition.ItemDefinitions;
import net.raphimc.viabedrock.api.util.BitSets;
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.api.util.StringUtil;
import net.raphimc.viabedrock.api.util.TextUtil;
import net.raphimc.viabedrock.platform.ViaBedrockConfig;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ClientboundBedrockPackets;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.DataValues;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.viabedrock.protocol.data.enums.Dimension;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.enums.java.GameEventType;
import net.raphimc.viabedrock.protocol.data.enums.java.Relative;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.PlayerInfoUpdateAction;
import net.raphimc.viabedrock.protocol.data.generated.java.Attributes;
import net.raphimc.viabedrock.protocol.model.*;
import net.raphimc.viabedrock.protocol.rewriter.BlockStateRewriter;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;
import net.raphimc.viabedrock.protocol.storage.*;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.*;
import java.util.logging.Level;

public final class JoinPackets {

    @SuppressWarnings("checkstyle:UnusedPrivateField")
    private static final PacketHandler BIOME_DEFINITION_LIST_HANDLER = wrapper -> {
        if (wrapper.isCancelled()) {
            return;
        }

        // TODO: Check namespaced
        wrapper.user().get(GameSessionStorage.class).setBedrockBiomeDefinitions((CompoundTag) wrapper.read(BedrockTypes.NETWORK_TAG)); // biome definitions
    };

    @SuppressWarnings("checkstyle:UnusedPrivateField")
    private static final PacketHandler COMPRESSED_BIOME_DEFINITION_LIST_HANDLER = wrapper -> {
        if (wrapper.isCancelled()) {
            return;
        }

        // Compressed biome definitions are used for the clientside generation of the world. Should not be sent as we tell the server that the client doesn't support it.
        BedrockProtocol.kickForIllegalState(wrapper.user(), "Compressed biome definitions are not supported.");
    };

    private static final PacketHandler REQUIRE_UNINITIALIZED_WORLD_HANDLER = wrapper -> {
        if (!wrapper.user().get(ChunkTracker.class).isEmpty()) {
            wrapper.cancel();
        } else if (!wrapper.user().get(EntityTracker.class).isEmpty()) {
            wrapper.cancel();
        }

        if (wrapper.isCancelled()) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Tried to change world properties after the world was already loaded");
        }
    };

    @SuppressWarnings("checkstyle:UnusedPrivateField")
    private static final PacketHandler RECONFIGURE_HANDLER = wrapper -> {
        if (wrapper.isCancelled()) {
            return;
        }
        wrapper.cancel();

        wrapper.user().put(new ChunkTracker(wrapper.user(), wrapper.user().get(ChunkTracker.class).getDimension()));
        if (wrapper.user().getProtocolInfo().protocolVersion().newerThanOrEqualTo(ProtocolVersion.v1_20_2)) {
            final PacketWrapper startConfiguration = PacketWrapper.create(ClientboundPackets26_3.START_CONFIGURATION, wrapper.user());
            startConfiguration.send(BedrockProtocol.class);
            wrapper.user().getProtocolInfo().setServerState(State.CONFIGURATION);

            handleJavaClientGameJoin(wrapper.user());
        } else {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Skipping reconfigure packet as it is not supported by the client. This may cause issues.");
        }
    };

    public static void register(final BedrockProtocol protocol) {
        protocol.registerClientboundTransition(ClientboundBedrockPackets.PLAY_STATUS,
            State.LOGIN, (PacketHandler) wrapper -> {
                final int rawStatus = wrapper.read(Types.INT); // status
                final PlayStatus status = PlayStatus.getByValue(rawStatus);
                if (status == null) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown PlayStatus: " + rawStatus);
                    wrapper.cancel();
                    return;
                }

                if (status == PlayStatus.LoginSuccess) {
                    final ProtocolInfo protocolInfo = wrapper.user().getProtocolInfo();
                    wrapper.setPacketType(ClientboundLoginPackets.LOGIN_FINISHED);
                    wrapper.write(Types.UUID, protocolInfo.getUuid()); // uuid
                    wrapper.write(Types.STRING, protocolInfo.getUsername()); // username
                    wrapper.write(Types.PROFILE_PROPERTY_ARRAY, new GameProfile.Property[0]); // properties
                    wrapper.write(Types.UUID, UUID.randomUUID()); // session id

                    ClientboundBaseProtocol1_7.onLoginSuccess(wrapper.user());
                    sendClientCacheStatus(wrapper.user());
                } else {
                    wrapper.setPacketType(ClientboundLoginPackets.LOGIN_DISCONNECT);
                    writePlayStatusKickMessage(wrapper, status);
                }
            }, State.PLAY, (PacketHandler) wrapper -> {
                final int rawStatus = wrapper.read(Types.INT); // status
                final PlayStatus status = PlayStatus.getByValue(rawStatus);
                if (status == null) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown PlayStatus: " + rawStatus);
                    wrapper.cancel();
                    return;
                }

                if (status == PlayStatus.LoginSuccess) {
                    wrapper.cancel();
                    sendClientCacheStatus(wrapper.user());
                } else if (status == PlayStatus.PlayerSpawn) {
                    wrapper.cancel();
                    final ClientPlayerEntity clientPlayer = wrapper.user().get(EntityTracker.class).getClientPlayer();
                    if (clientPlayer.isInitiallySpawned()) {
                        return;
                    }

                    final PacketWrapper interact = PacketWrapper.create(ServerboundBedrockPackets.INTERACT, wrapper.user());
                    interact.write(Types.UNSIGNED_BYTE, (short) InteractPacketPayload_Action.InteractUpdate.getValue()); // action
                    interact.write(BedrockTypes.UNSIGNED_VAR_LONG, 0L); // target entity runtime id
                    interact.write(BedrockTypes.OPTIONAL_POSITION_3F, null); // position
                    interact.sendToServer(BedrockProtocol.class);

                    clientPlayer.setRotation(new Position3f(clientPlayer.rotation().x(), clientPlayer.rotation().y(), clientPlayer.rotation().y()));
                    clientPlayer.setInitiallySpawned();

                    PacketFactory.sendBedrockLoadingScreen(wrapper.user(), ServerboundLoadingScreenPacketType.EndLoadingScreen, null);
                    final PacketWrapper setLocalPlayerAsInitialized = PacketWrapper.create(ServerboundBedrockPackets.SET_LOCAL_PLAYER_AS_INITIALIZED, wrapper.user());
                    setLocalPlayerAsInitialized.write(BedrockTypes.UNSIGNED_VAR_LONG, clientPlayer.runtimeId()); // entity runtime id
                    setLocalPlayerAsInitialized.sendToServer(BedrockProtocol.class);

                    PacketFactory.sendJavaGameEvent(wrapper.user(), GameEventType.LEVEL_CHUNKS_LOAD_START, 0F);
                } else {
                    wrapper.setPacketType(ClientboundPackets26_3.DISCONNECT);
                    writePlayStatusKickMessage(wrapper, status);
                }
            }, State.CONFIGURATION, (PacketHandler) wrapper -> {
                final int rawStatus = wrapper.read(Types.INT); // status
                final PlayStatus status = PlayStatus.getByValue(rawStatus);
                if (status == null) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown PlayStatus: " + rawStatus);
                    wrapper.cancel();
                    return;
                }

                if (status == PlayStatus.LoginSuccess) {
                    wrapper.cancel();
                    sendClientCacheStatus(wrapper.user());
                } else {
                    wrapper.setPacketType(ClientboundConfigurationPackets26_3.DISCONNECT);
                    writePlayStatusKickMessage(wrapper, status);
                }
            }
        );
        protocol.registerClientboundTransition(ClientboundBedrockPackets.START_GAME,
            State.CONFIGURATION, (PacketHandler) wrapper -> {
                wrapper.cancel();
                final GameSessionStorage gameSession = wrapper.user().get(GameSessionStorage.class);
                ResourcePackStorage resourcePackStorage = wrapper.user().get(ResourcePackStorage.class);
                if (resourcePackStorage == null) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Skipping resource pack negotiation");
                    wrapper.user().remove(ResourcePackLoadStateTracker.class);
                    resourcePackStorage = new ResourcePackStorage(Collections.emptyList());
                    wrapper.user().put(resourcePackStorage);
                }

                final long entityUniqueId = wrapper.read(BedrockTypes.VAR_LONG); // entity unique id
                final long entityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // entity runtime id
                final GameType playerGameType = GameType.getByValue(wrapper.read(BedrockTypes.VAR_INT), GameType.Undefined); // player game type
                final Position3f playerPosition = wrapper.read(BedrockTypes.POSITION_3F); // player position
                final Position2f playerRotation = wrapper.read(BedrockTypes.POSITION_2F); // player rotation

                // Level settings
                wrapper.read(BedrockTypes.LONG_LE); // seed
                wrapper.read(BedrockTypes.SHORT_LE); // spawn biome type
                wrapper.read(BedrockTypes.STRING); // custom biome name
                final Dimension dimension = Dimension.values()[wrapper.read(BedrockTypes.VAR_INT)]; // dimension
                final GeneratorType generatorType = GeneratorType.getByValue(wrapper.read(BedrockTypes.VAR_INT), GeneratorType.Undefined); // generator id
                final GameType levelGameType = GameType.getByValue(wrapper.read(BedrockTypes.VAR_INT), GameType.Undefined); // level game type
                final boolean hardcore = wrapper.read(Types.BOOLEAN); // hardcore
                final Difficulty difficulty = Difficulty.getByValue(wrapper.read(BedrockTypes.VAR_INT), Difficulty.Unknown); // difficulty
                wrapper.read(BedrockTypes.BLOCK_POSITION); // default spawn position
                wrapper.read(Types.BOOLEAN); // achievements disabled
                final EditorWorldType editorWorldType = EditorWorldType.getByValue(wrapper.read(BedrockTypes.VAR_INT)); // world editor type
                wrapper.read(Types.BOOLEAN); // created in world editor
                wrapper.read(Types.BOOLEAN); // exported from world editor
                wrapper.read(Types.BYTE); // editor migration version
                final int currentTime = wrapper.read(BedrockTypes.VAR_INT); // day cycle stop time
                wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // education edition offers
                wrapper.read(Types.BOOLEAN); // education features enabled
                wrapper.read(BedrockTypes.STRING); // education product id
                final float rainLevel = wrapper.read(BedrockTypes.FLOAT_LE); // rain level
                final float lightningLevel = wrapper.read(BedrockTypes.FLOAT_LE); // lightning level
                wrapper.read(Types.BOOLEAN); // platform locked content confirmed
                wrapper.read(Types.BOOLEAN); // multiplayer game
                wrapper.read(Types.BOOLEAN); // is broadcasting to lan
                wrapper.read(BedrockTypes.VAR_INT); // Xbox Live broadcast mode
                wrapper.read(BedrockTypes.VAR_INT); // platform broadcast mode
                final boolean commandsEnabled = wrapper.read(Types.BOOLEAN); // commands enabled
                wrapper.read(Types.BOOLEAN); // texture packs required
                final GameRule[] gameRules = wrapper.read(BedrockTypes.GAME_RULE_ARRAY); // game rules
                final Experiment[] experiments = wrapper.read(BedrockTypes.EXPERIMENT_ARRAY); // experiments
                wrapper.read(Types.BOOLEAN); // experiments previously toggled
                wrapper.read(Types.BOOLEAN); // bonus chest enabled
                wrapper.read(Types.BOOLEAN); // start with map enabled
                final int playerPermission = wrapper.read(Types.BYTE); // player permission
                final int chunkTickRange = wrapper.read(BedrockTypes.INT_LE); // server chunk tick range
                wrapper.read(Types.BOOLEAN); // behavior pack locked
                wrapper.read(Types.BOOLEAN); // resource pack locked
                wrapper.read(Types.BOOLEAN); // from locked world template
                wrapper.read(Types.BOOLEAN); // using msa gamer tags only
                wrapper.read(Types.BOOLEAN); // from world template
                wrapper.read(Types.BOOLEAN); // world template option locked
                wrapper.read(Types.BOOLEAN); // only spawn v1 villagers
                wrapper.read(Types.BOOLEAN); // disable personas
                wrapper.read(Types.BOOLEAN); // disable custom skins
                wrapper.read(Types.BOOLEAN); // mute emote chat
                final String vanillaVersion = wrapper.read(BedrockTypes.STRING); // vanilla version
                wrapper.read(BedrockTypes.INT_LE); // limited world width
                wrapper.read(BedrockTypes.INT_LE); // limited world height
                wrapper.read(Types.BOOLEAN); // nether type
                wrapper.read(BedrockTypes.EDUCATION_URI_RESOURCE); // education shared uri
                if (wrapper.read(Types.BOOLEAN)) { // optional experimental game play
                    wrapper.read(Types.BOOLEAN); // enable experimental game play
                }
                final ChatRestrictionLevel chatRestrictionLevel = ChatRestrictionLevel.getByValue(wrapper.read(Types.BYTE), ChatRestrictionLevel.Disabled); // chat restriction level
                wrapper.read(Types.BOOLEAN); // disabling player interactions
                wrapper.read(BedrockTypes.VAR_INT); // server editor connection policy
                wrapper.read(Types.BOOLEAN); // allow anonymous block drops in editor worlds

                // Continue reading start game packet
                wrapper.read(BedrockTypes.STRING); // level id
                final String levelName = wrapper.read(BedrockTypes.STRING); // level name
                wrapper.read(BedrockTypes.STRING); // premium world template id
                wrapper.read(Types.BOOLEAN); // is trial
                final int rewindHistorySize = wrapper.read(BedrockTypes.VAR_INT); // rewind history size
                final boolean blockBreakingServerAuthoritative = wrapper.read(Types.BOOLEAN); // server authoritative block breaking
                final long levelTime = wrapper.read(BedrockTypes.LONG_LE); // current level time
                wrapper.read(BedrockTypes.VAR_INT); // enchantment seed
                final BlockProperties[] blockProperties = wrapper.read(BedrockTypes.BLOCK_PROPERTIES_ARRAY); // block properties
                wrapper.read(BedrockTypes.STRING); // multiplayer correlation id
                final boolean inventoryServerAuthoritative = wrapper.read(Types.BOOLEAN); // server authoritative inventories
                final String serverEngine = wrapper.read(BedrockTypes.STRING); // server engine
                wrapper.read(BedrockTypes.NETWORK_TAG); // player property data
                wrapper.read(BedrockTypes.LONG_LE); // block registry checksum
                wrapper.read(BedrockTypes.UUID); // world template id
                wrapper.read(Types.BOOLEAN); // client side generation
                final boolean hashedRuntimeBlockIds = wrapper.read(Types.BOOLEAN); // use hashed block runtime ids
                wrapper.read(Types.BOOLEAN); // server authoritative sounds
                if (wrapper.read(Types.BOOLEAN)) { // has server join information
                    if (wrapper.read(Types.BOOLEAN)) { // has gathering join information
                        wrapper.read(BedrockTypes.UUID); // experience id
                        wrapper.read(BedrockTypes.STRING); // experience name
                        if (wrapper.read(Types.BOOLEAN)) { // has experience world id
                            wrapper.read(BedrockTypes.UUID); // experience world id
                        }
                        if (wrapper.read(Types.BOOLEAN)) { // has experience world name
                            wrapper.read(BedrockTypes.STRING); // experience world name
                        }
                        wrapper.read(BedrockTypes.STRING); // creator id
                        if (wrapper.read(Types.BOOLEAN)) { // has target id
                            wrapper.read(BedrockTypes.UUID); // target id
                        }
                        if (wrapper.read(Types.BOOLEAN)) {
                            wrapper.read(BedrockTypes.STRING); // scenario id
                        }
                        if (wrapper.read(Types.BOOLEAN)) { // has server id
                            wrapper.read(BedrockTypes.STRING); // server id
                        }
                    }
                    if (wrapper.read(Types.BOOLEAN)) { // has store entry point info
                        wrapper.read(BedrockTypes.STRING); // store id
                        wrapper.read(BedrockTypes.STRING); // store name
                    }
                    if (wrapper.read(Types.BOOLEAN)) { // has presence info
                        wrapper.read(BedrockTypes.STRING); // experience name
                        wrapper.read(BedrockTypes.STRING); // world name
                    }
                }
                wrapper.read(BedrockTypes.STRING); // server id
                wrapper.read(BedrockTypes.STRING); // scenario id
                wrapper.read(BedrockTypes.STRING); // world id
                wrapper.read(BedrockTypes.STRING); // owner id

                if (editorWorldType == EditorWorldType.EditorProject) {
                    final PacketWrapper disconnect = PacketWrapper.create(ClientboundConfigurationPackets26_3.DISCONNECT, wrapper.user());
                    PacketFactory.writeJavaDisconnect(wrapper, resourcePackStorage.getTexts().get("disconnectionScreen.editor.mismatchEditorWorld"));
                    disconnect.send(BedrockProtocol.class);
                    return;
                }

                ViaBedrock.getPlatform().getLogger().log(Level.INFO, "Server feature version: " + vanillaVersion);
                Semver version;
                try {
                    if (vanillaVersion.equals("*")) {
                        version = new Semver("99.99.99");
                    } else {
                        version = new Semver(vanillaVersion, Semver.SemverType.LOOSE);
                    }
                } catch (final Throwable e) {
                    ViaBedrock.getPlatform().getLogger().log(Level.SEVERE, "Invalid vanilla version: " + vanillaVersion);
                    version = new Semver("99.99.99");
                }

                final List<String> enabledFeatures = new ArrayList<>();
                for (Experiment experiment : experiments) {
                    if (experiment.enabled()) {
                        if (BedrockProtocol.MAPPINGS.getBedrockToJavaExperimentalFeatures().containsKey(experiment.name())) {
                            enabledFeatures.add(BedrockProtocol.MAPPINGS.getBedrockToJavaExperimentalFeatures().get(experiment.name()));
                        } else {
                            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "This server uses an unsupported experimental feature: " + experiment.name());
                        }
                    }
                }

                if (!inventoryServerAuthoritative) {
                    ViaBedrock.getPlatform().getLogger().log(Level.INFO, "This server uses client authoritative inventories. This is not supported yet.");
                }

                gameSession.setBedrockVanillaVersion(version);
                gameSession.setFlatGenerator(generatorType == GeneratorType.Flat);
                gameSession.setMovementRewindHistorySize(rewindHistorySize);
                gameSession.setLevelGameType(levelGameType);
                gameSession.setLevelTime(levelTime);
                gameSession.setHardcoreMode(hardcore);
                gameSession.setChatRestrictionLevel(chatRestrictionLevel);
                gameSession.setCommandsEnabled(commandsEnabled);
                gameSession.setInventoryServerAuthoritative(inventoryServerAuthoritative);
                gameSession.setBlockBreakingServerAuthoritative(blockBreakingServerAuthoritative);

                final PlayerAbilities playerAbilities = new PlayerAbilities(entityUniqueId, (byte) playerPermission, (byte) CommandPermissionLevel.Any.getValue());
                final ClientPlayerEntity clientPlayer = new ClientPlayerEntity(wrapper.user(), entityRuntimeId, wrapper.user().getProtocolInfo().getUuid(), playerAbilities);
                clientPlayer.setPosition(new Position3f(playerPosition.x(), playerPosition.y() + clientPlayer.eyeOffset(), playerPosition.z()));
                clientPlayer.setRotation(new Position3f(playerRotation.x(), playerRotation.y(), 0F));
                clientPlayer.setOnGround(false);
                clientPlayer.setGameType(playerGameType);
                clientPlayer.setName(wrapper.user().getProtocolInfo().getUsername());

                wrapper.user().put(new JoinGameStorage(levelName, difficulty, rainLevel, lightningLevel, currentTime, chunkTickRange));
                wrapper.user().put(new GameRulesStorage(wrapper.user(), gameRules));
                wrapper.user().put(new BlockStateRewriter(blockProperties, hashedRuntimeBlockIds));
                wrapper.user().put(new ItemRewriter(wrapper.user(), new ItemEntry[0]));
                wrapper.user().put(new ChunkTracker(wrapper.user(), dimension));
                final EntityTracker entityTracker = new EntityTracker(wrapper.user());
                entityTracker.addEntity(clientPlayer, false);
                wrapper.user().put(entityTracker);

                final PacketWrapper brandCustomPayload = PacketWrapper.create(ClientboundConfigurationPackets26_3.CUSTOM_PAYLOAD, wrapper.user());
                brandCustomPayload.write(Types.STRING, "minecraft:brand"); // channel
                brandCustomPayload.write(Types.STRING, "Bedrock" + (!serverEngine.isEmpty() ? " @" + serverEngine : "") + " v: " + vanillaVersion); // content
                brandCustomPayload.send(BedrockProtocol.class);

                if (!enabledFeatures.isEmpty()) {
                    enabledFeatures.add("minecraft:vanilla");
                    final PacketWrapper updateEnabledFeatures = PacketWrapper.create(ClientboundConfigurationPackets26_3.UPDATE_ENABLED_FEATURES, wrapper.user());
                    updateEnabledFeatures.write(Types.STRING_ARRAY, enabledFeatures.toArray(new String[0])); // enabled features
                    updateEnabledFeatures.send(BedrockProtocol.class);
                }

                handleJavaClientGameJoin(wrapper.user());

                final PacketWrapper requestChunkRadius = PacketWrapper.create(ServerboundBedrockPackets.REQUEST_CHUNK_RADIUS, wrapper.user());
                requestChunkRadius.write(BedrockTypes.VAR_INT, wrapper.user().get(ClientSettingsStorage.class).viewDistance()); // radius
                requestChunkRadius.write(Types.BYTE, ProtocolConstants.BEDROCK_REQUEST_CHUNK_RADIUS_MAX_RADIUS); // max radius
                requestChunkRadius.sendToServer(BedrockProtocol.class);
                PacketFactory.sendBedrockLoadingScreen(wrapper.user(), ServerboundLoadingScreenPacketType.StartLoadingScreen, null);
            }, State.PLAY, (PacketHandler) PacketWrapper::cancel // Bedrock client ignores multiple start game packets
        );
        protocol.registerClientboundTransition(ClientboundBedrockPackets.BIOME_DEFINITION_LIST,
            // Biomes are technically data driven, but the client seems to ignore most of the defined data and instead uses hardcoded values.
            State.CONFIGURATION, new PacketHandlers() {
                @Override
                protected void register() {
                    handler(PacketWrapper::cancel);
                }
            }, State.PLAY, new PacketHandlers() {
                @Override
                protected void register() {
                    handler(REQUIRE_UNINITIALIZED_WORLD_HANDLER);
                    handler(PacketWrapper::cancel);
                }
            }
        );
        protocol.registerClientboundTransition(ClientboundBedrockPackets.DIMENSION_DATA,
            State.CONFIGURATION, (PacketHandler) wrapper -> {
                wrapper.cancel();
                final GameSessionStorage gameSession = wrapper.user().get(GameSessionStorage.class);
                final int count = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // entry count
                for (int i = 0; i < count; i++) {
                    final String dimensionIdentifier = wrapper.read(BedrockTypes.STRING); // dimension identifier
                    final int minimumHeight = wrapper.read(BedrockTypes.VAR_INT); // minimum height
                    final int maximumHeight = minimumHeight + wrapper.read(BedrockTypes.VAR_INT); // maximum height
                    wrapper.read(BedrockTypes.VAR_INT); // generator type
                    wrapper.read(BedrockTypes.VAR_INT); // dimension type
                    wrapper.read(BedrockTypes.UUID); // pack id
                    wrapper.read(BedrockTypes.STRING); // default biome
                    wrapper.read(BedrockTypes.VAR_INT); // cloud height
                    wrapper.read(Types.BOOLEAN); // render clouds
                    if (dimensionIdentifier.equals(Dimension.OVERWORLD.getKey())) { // Bedrock client currently only supports overworld
                        gameSession.putBedrockDimensionDefinition(dimensionIdentifier, new IntIntImmutablePair(minimumHeight, maximumHeight));
                    }
                }
            }, State.PLAY, (PacketHandler) PacketWrapper::cancel // Bedrock client ignores dimension data after start game
        );
        protocol.registerClientbound(ClientboundBedrockPackets.ITEM_REGISTRY, null, wrapper -> {
            wrapper.cancel();
            if (wrapper.user().has(ItemRewriter.class) && !wrapper.user().get(ItemRewriter.class).getItems().isEmpty()) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received ITEM_REGISTRY after item rewriter was already initialized");
                return;
            }

            final ItemEntry[] itemEntries = wrapper.read(BedrockTypes.ITEM_ENTRY_ARRAY); // items
            final ItemRewriter itemRewriter = new ItemRewriter(wrapper.user(), itemEntries);
            wrapper.user().put(itemRewriter);
            final ItemDefinitions itemDefinitions = wrapper.user().get(ResourcePackStorage.class).getItems();

            // Component items are loaded from the item registry entries
            for (String identifier : itemRewriter.getComponentItems()) {
                itemDefinitions.remove(identifier);
            }
            for (ItemEntry itemEntry : itemEntries) {
                if (itemEntry.componentData() != null && itemEntry.version() == ItemVersion.DataDriven && itemRewriter.getComponentItems().contains(itemEntry.identifier())) {
                    itemDefinitions.addFromNetworkTag(itemEntry.identifier(), itemEntry.componentData());
                }
            }
        });
        protocol.registerClientboundTransition(ClientboundBedrockPackets.AVAILABLE_ENTITY_IDENTIFIERS,
            State.CONFIGURATION, (PacketHandler) PacketWrapper::cancel, // Bedrock client ignores entity identifiers before start game
            State.PLAY, (PacketHandler) wrapper -> {
                wrapper.cancel();
                final GameSessionStorage gameSession = wrapper.user().get(GameSessionStorage.class);
                final CompoundTag entityIdentifiers = (CompoundTag) wrapper.read(BedrockTypes.NETWORK_TAG); // entity identifiers
                for (CompoundTag entityIdentifier : entityIdentifiers.getListTag("idlist", CompoundTag.class)) {
                    final String identifier = entityIdentifier.getString("id");
                    if (identifier != null) {
                        gameSession.addEntityIdentifier(identifier);
                    }
                }
            }
        );
    }

    private static void sendClientCacheStatus(final UserConnection user) {
        final PacketWrapper clientCacheStatus = PacketWrapper.create(ServerboundBedrockPackets.CLIENT_CACHE_STATUS, user);
        clientCacheStatus.write(Types.BOOLEAN, !ViaBedrock.getConfig().getBlobCacheMode().equals(ViaBedrockConfig.BlobCacheMode.DISABLED)); // is supported
        clientCacheStatus.sendToServer(BedrockProtocol.class);
    }

    private static void writePlayStatusKickMessage(final PacketWrapper wrapper, final PlayStatus status) {
        final Map<String, String> translations = BedrockProtocol.MAPPINGS.getBedrockResourcePacks().get(DataValues.VANILLA_RESOURCE_PACK_KEY).content().getLang("texts/en_US.lang");

        switch (status) {
            case LoginFailed_ClientOld -> PacketFactory.writeJavaDisconnect(wrapper, translations.get("disconnectionScreen.outdatedClient"));
            case LoginFailed_ServerOld -> PacketFactory.writeJavaDisconnect(wrapper, translations.get("disconnectionScreen.outdatedServer"));
            case LoginFailed_InvalidTenant -> PacketFactory.writeJavaDisconnect(wrapper, translations.get("disconnectionScreen.invalidTenant"));
            case LoginFailed_EditionMismatchEduToVanilla -> PacketFactory.writeJavaDisconnect(wrapper, translations.get("disconnectionScreen.editionMismatchEduToVanilla"));
            case LoginFailed_EditionMismatchVanillaToEdu -> PacketFactory.writeJavaDisconnect(wrapper, translations.get("disconnectionScreen.editionMismatchVanillaToEdu"));
            case LoginFailed_ServerFullSubClient, LoginFailed_EditorMismatchVanillaToEditor -> PacketFactory.writeJavaDisconnect(wrapper, translations.get("disconnectionScreen.serverFull") + "\n\n\n\n" + translations.get("disconnectionScreen.serverFull.title"));
            case LoginFailed_EditorMismatchEditorToVanilla -> PacketFactory.writeJavaDisconnect(wrapper, translations.get("disconnectionScreen.editor.mismatchEditorToVanilla"));
            case PlayerSpawn, LoginSuccess -> wrapper.cancel();
            default -> throw new IllegalStateException("Unhandled PlayStatus: " + status);
        }
    }

    private static void handleJavaClientGameJoin(final UserConnection user) {
        final JoinGameStorage joinGameStorage = user.get(JoinGameStorage.class);
        final GameSessionStorage gameSession = user.get(GameSessionStorage.class);
        final ClientSettingsStorage clientSettingsStorage = user.get(ClientSettingsStorage.class);
        final GameRulesStorage gameRulesStorage = user.get(GameRulesStorage.class);
        final ChunkTracker chunkTracker = user.get(ChunkTracker.class);
        final CommandsStorage commandsStorage = user.get(CommandsStorage.class);
        final ClientPlayerEntity clientPlayer = user.get(EntityTracker.class).getClientPlayer();

        for (Map.Entry<String, Tag> registry : gameSession.getJavaRegistries().entrySet()) {
            final CompoundTag registryTag = (CompoundTag) registry.getValue();
            final PacketWrapper registryData = PacketWrapper.create(ClientboundConfigurationPackets26_3.REGISTRY_DATA, user);
            registryData.write(Types.STRING, registry.getKey()); // registry key
            final List<RegistryEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Tag> entry : registryTag.entrySet()) {
                entries.add(new RegistryEntry(entry.getKey(), entry.getValue()));
            }
            registryData.write(Types.REGISTRY_ENTRY_ARRAY, entries.toArray(new RegistryEntry[0])); // registry entries
            registryData.send(BedrockProtocol.class);
        }

        final PacketWrapper updateTags = PacketWrapper.create(ClientboundConfigurationPackets26_3.UPDATE_TAGS, user);
        updateTags.write(Types.VAR_INT, BedrockProtocol.MAPPINGS.getJavaTags().size()); // number of registries
        for (Map.Entry<String, Tag> registryEntry : BedrockProtocol.MAPPINGS.getJavaTags().entrySet()) {
            final CompoundTag tag = (CompoundTag) registryEntry.getValue();
            updateTags.write(Types.STRING, registryEntry.getKey()); // registry key
            updateTags.write(Types.VAR_INT, tag.size()); // number of tags
            for (Map.Entry<String, Tag> tagEntry : tag.entrySet()) {
                updateTags.write(Types.STRING, tagEntry.getKey()); // tag name
                updateTags.write(Types.VAR_INT_ARRAY_PRIMITIVE, ((IntArrayTag) tagEntry.getValue()).getValue().clone()); // tag ids
            }
        }
        updateTags.send(BedrockProtocol.class);

        final PacketWrapper finishConfiguration = PacketWrapper.create(ClientboundConfigurationPackets26_3.FINISH_CONFIGURATION, user);
        finishConfiguration.send(BedrockProtocol.class);
        user.getProtocolInfo().setServerState(State.PLAY);
        if (user.getProtocolInfo().protocolVersion().betweenInclusive(ProtocolVersion.v1_20_2, ProtocolVersion.v1_21_2)) { // VB compatibility
            // Problematic code: https://github.com/ViaVersion/ViaBackwards/blob/b90b573f1d6f4d59841a3243e5bd072a43ec78e5/common/src/main/java/com/viaversion/viabackwards/protocol/v1_21_4to1_21_2/rewriter/EntityPacketRewriter1_21_4.java#L109
            user.getProtocolInfo().setClientState(State.PLAY); // Wrong, but needed because ViaBackwards expects this and would otherwise send the player loaded packet in configuration state.
        }

        final PacketWrapper joinGame = PacketWrapper.create(ClientboundPackets26_3.LOGIN, user);
        joinGame.write(Types.INT, clientPlayer.javaId()); // entity id
        joinGame.write(Types.BOOLEAN, gameSession.isHardcoreMode()); // hardcore
        joinGame.write(Types.STRING_ARRAY, Dimension.getDimensionKeys()); // dimension types
        joinGame.write(Types.VAR_INT, 100); // max players
        joinGame.write(Types.VAR_INT, clientSettingsStorage.viewDistance()); // view distance
        joinGame.write(Types.VAR_INT, joinGameStorage.chunkTickRange()); // simulation distance
        joinGame.write(Types.BOOLEAN, ViaBedrock.getConfig().shouldTranslateShowCoordinatesGameRule() && !gameRulesStorage.<Boolean>getGameRule("showCoordinates")); // reduced debug info
        joinGame.write(Types.BOOLEAN, !gameRulesStorage.<Boolean>getGameRule("doImmediateRespawn")); // show death screen
        joinGame.write(Types.BOOLEAN, gameRulesStorage.getGameRule("doLimitedCrafting")); // limited crafting
        joinGame.write(Types.VAR_INT, chunkTracker.getDimension().ordinal()); // dimension id
        joinGame.write(Types.STRING, chunkTracker.getDimension().getKey()); // dimension name
        joinGame.write(Types.LONG, 0L); // hashed seed
        joinGame.write(Types.VAR_INT, clientPlayer.javaGameMode().ordinal()); // game mode
        joinGame.write(Types.OPTIONAL_VAR_INT, null); // previous game mode
        joinGame.write(Types.BOOLEAN, false); // is debug
        joinGame.write(Types.BOOLEAN, gameSession.isFlatGenerator()); // is flat
        joinGame.write(Types.OPTIONAL_GLOBAL_POSITION, null); // last death location
        joinGame.write(Types.VAR_INT, 0); // portal cooldown
        joinGame.write(Types.VAR_INT, 64); // sea level
        joinGame.write(Types.BOOLEAN, false); // online mode
        joinGame.write(Types.BOOLEAN, false); // enforces secure chat
        joinGame.send(BedrockProtocol.class);

        clientPlayer.createTeam();
        clientPlayer.sendInitialEntityData();
        clientPlayer.updateAttributes(clientPlayer.attributes().values().toArray(new EntityAttribute[0]));
        clientPlayer.setAbilities(clientPlayer.abilities());
        clientPlayer.sendPlayerPositionPacketToClient(Relative.NONE);
        if (commandsStorage != null) {
            commandsStorage.updateCommandTree();
        }

        final PacketWrapper initializeBorder = PacketWrapper.create(ClientboundPackets26_3.INITIALIZE_BORDER, user);
        initializeBorder.write(Types.DOUBLE, 0D); // center x
        initializeBorder.write(Types.DOUBLE, 0D); // center z
        initializeBorder.write(Types.DOUBLE, 0D); // old size
        initializeBorder.write(Types.DOUBLE, 60_000_000D); // new size
        initializeBorder.write(Types.VAR_LONG, 0L); // lerp time
        initializeBorder.write(Types.VAR_INT, 60_000_000); // new absolute max size
        initializeBorder.write(Types.VAR_INT, 0); // warning blocks
        initializeBorder.write(Types.VAR_INT, 0); // warning time
        initializeBorder.send(BedrockProtocol.class);

        final PacketWrapper updateAttributes = PacketWrapper.create(ClientboundPackets26_3.UPDATE_ATTRIBUTES, user);
        updateAttributes.write(Types.VAR_INT, clientPlayer.javaId()); // entity id
        updateAttributes.write(Types.VAR_INT, 1); // attribute count
        updateAttributes.write(Types.VAR_INT, BedrockProtocol.MAPPINGS.getJavaEntityAttributes().get(Attributes.ATTACK_SPEED)); // attribute id
        updateAttributes.write(Types.DOUBLE, 20D); // base value
        updateAttributes.write(Types.VAR_INT, 0); // modifier count
        updateAttributes.send(BedrockProtocol.class);

        final PacketWrapper serverDifficulty = PacketWrapper.create(ClientboundPackets26_3.CHANGE_DIFFICULTY, user);
        serverDifficulty.write(Types.VAR_INT, joinGameStorage.difficulty().getValue()); // difficulty
        serverDifficulty.write(Types.BOOLEAN, false); // locked
        serverDifficulty.send(BedrockProtocol.class);

        final PacketWrapper tabList = PacketWrapper.create(ClientboundPackets26_3.TAB_LIST, user);
        tabList.write(Types.TAG, TextUtil.stringToNbt(joinGameStorage.levelName() + "\n")); // header
        tabList.write(Types.TAG, TextUtil.stringToNbt("§aViaBedrock §3v" + ViaBedrock.VERSION + "\n§7https://github.com/RaphiMC/ViaBedrock")); // footer
        tabList.send(BedrockProtocol.class);

        final PacketWrapper playerInfoUpdate = PacketWrapper.create(ClientboundPackets26_3.PLAYER_INFO_UPDATE, user);
        playerInfoUpdate.write(Types.PROFILE_ACTIONS_ENUM1_21_4, BitSets.create(8, PlayerInfoUpdateAction.ADD_PLAYER, PlayerInfoUpdateAction.UPDATE_GAME_MODE)); // actions
        playerInfoUpdate.write(Types.VAR_INT, 1); // length
        playerInfoUpdate.write(Types.UUID, clientPlayer.javaUuid()); // uuid
        playerInfoUpdate.write(Types.STRING, StringUtil.encodeUuid(clientPlayer.javaUuid())); // username
        playerInfoUpdate.write(Types.PROFILE_PROPERTY_ARRAY, new GameProfile.Property[0]); // properties
        playerInfoUpdate.write(Types.VAR_INT, clientPlayer.javaGameMode().ordinal()); // game mode
        playerInfoUpdate.send(BedrockProtocol.class);

        if (joinGameStorage.rainLevel() > 0F || joinGameStorage.lightningLevel() > 0F) {
            PacketFactory.sendJavaGameEvent(user, GameEventType.START_RAINING, 0F);
            if (joinGameStorage.rainLevel() > 0F) {
                PacketFactory.sendJavaGameEvent(user, GameEventType.RAIN_LEVEL_CHANGE, joinGameStorage.rainLevel());
            }
            if (joinGameStorage.lightningLevel() > 0F) {
                PacketFactory.sendJavaGameEvent(user, GameEventType.THUNDER_LEVEL_CHANGE, joinGameStorage.lightningLevel());
            }
        }

        final PacketWrapper setTime = PacketWrapper.create(ClientboundBedrockPackets.SET_TIME, user);
        setTime.write(BedrockTypes.VAR_INT, joinGameStorage.currentTime()); // time of day
        setTime.send(BedrockProtocol.class, false);
    }

    private JoinPackets() {
    }

}
