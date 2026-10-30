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
package net.raphimc.viabedrock.api.model.entity;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.util.Pair;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.util.EnumUtil;
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.PlayerActionPacketFactory;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.Direction;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.AbilitiesIndex;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.enums.java.*;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.GameMode;
import net.raphimc.viabedrock.protocol.model.EntityAttribute;
import net.raphimc.viabedrock.protocol.model.PlayerAbilities;
import net.raphimc.viabedrock.protocol.model.Position3f;
import net.raphimc.viabedrock.protocol.rewriter.GameTypeRewriter;
import net.raphimc.viabedrock.protocol.storage.ChunkTracker;
import net.raphimc.viabedrock.protocol.storage.CommandsStorage;
import net.raphimc.viabedrock.protocol.storage.GameSessionStorage;
import net.raphimc.viabedrock.protocol.storage.PlayerListStorage;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class ClientPlayerEntity extends PlayerEntity {

    private final AtomicInteger teleportId = new AtomicInteger(1);
    private final GameSessionStorage gameSession;

    // Initial spawn and respawning
    private boolean initiallySpawned;
    private DimensionChangeInfo dimensionChangeInfo;
    private boolean wasInsideUnloadedChunk;

    // Position syncing
    private int pendingTeleportId;
    private boolean waitingForPositionSync;
    private boolean serverSideTeleportConfirmed;

    // Server Authoritative Movement
    private Position3f prevPosition;
    private boolean prevOnGround;
    private final Set<PlayerAuthInputData> authInputData = EnumSet.noneOf(PlayerAuthInputData.class);
    private final List<AuthInputBlockAction> authInputBlockActions = new ArrayList<>();
    private Set<InputFlag> inputFlags = EnumSet.noneOf(InputFlag.class);
    private Set<InputFlag> prevInputFlags = EnumSet.noneOf(InputFlag.class);
    private boolean horizontalCollision;
    private boolean sneaking;
    private boolean sprinting;
    private boolean gliding;

    // Riding
    private boolean requestedDismount = false;

    // Misc data
    private GameType gameType;
    private GameMode javaGameMode;
    private boolean cancelNextSwingPacket;
    private BlockBreakingInfo blockBreakingInfo;

    public ClientPlayerEntity(final UserConnection user, final long runtimeId, final UUID javaUuid, final PlayerAbilities abilities) {
        super(user, runtimeId, 1, javaUuid, abilities);
        this.attributes.put("minecraft:movement", new EntityAttribute("minecraft:movement", 0.7F, 0F, Float.MAX_VALUE));
        this.attributes.put("minecraft:player.hunger", new EntityAttribute("minecraft:player.hunger", 20F, 0F, 20F));
        this.attributes.put("minecraft:player.saturation", new EntityAttribute("minecraft:player.saturation", 5F, 0F, 20F));
        this.attributes.put("minecraft:player.experience", new EntityAttribute("minecraft:player.experience", 0F, 0F, 1F));
        this.attributes.put("minecraft:player.level", new EntityAttribute("minecraft:player.level", 0F, 0F, 24791F));

        this.gameSession = user.get(GameSessionStorage.class);
    }

    @Override
    public void tick() {
        super.tick();

        this.prevPosition = this.position;
        this.prevOnGround = this.onGround;
        this.prevInputFlags = this.inputFlags;

        if (this.mountRuntimeId != -1 && this.sneaking && !this.requestedDismount) {
            PlayerActionPacketFactory.sendBedrockDismount(this.user, this.mountRuntimeId);
            this.requestedDismount = true;
        }
    }

    public void sendPlayerPositionPacketToClient(final Set<Relative> relatives) {
        final PacketWrapper playerPosition = PacketWrapper.create(ClientboundPackets26_3.PLAYER_POSITION, this.user);
        this.writePlayerPositionPacketToClient(playerPosition, relatives, true);
        playerPosition.send(BedrockProtocol.class);
    }

    public void writePlayerPositionPacketToClient(final PacketWrapper wrapper, final Set<Relative> relatives, final boolean fakeTeleport) {
        this.pendingTeleportId = this.teleportId.getAndIncrement();

        wrapper.write(Types.VAR_INT, this.pendingTeleportId * (fakeTeleport ? -1 : 1)); // teleport id
        wrapper.write(Types.DOUBLE, relatives.contains(Relative.X) ? 0D : (double) this.position.x()); // x
        wrapper.write(Types.DOUBLE, relatives.contains(Relative.Y) ? 0D : (double) (this.position.y() - this.eyeOffset())); // y
        wrapper.write(Types.DOUBLE, relatives.contains(Relative.Z) ? 0D : (double) this.position.z()); // z
        wrapper.write(Types.DOUBLE, 0D); // velocity x
        wrapper.write(Types.DOUBLE, 0D); // velocity y
        wrapper.write(Types.DOUBLE, 0D); // velocity z
        wrapper.write(Types.FLOAT, relatives.contains(Relative.Y_ROT) ? 0F : this.rotation.y()); // yaw
        wrapper.write(Types.FLOAT, relatives.contains(Relative.X_ROT) ? 0F : this.rotation.x()); // pitch
        wrapper.write(Types.INT, EnumUtil.getIntBitmaskFromEnumSet(relatives, Relative::ordinal)); // flags
    }

    public void sendPlayerActionPacketToServer(final PlayerActionType action) {
        this.sendPlayerActionPacketToServer(action, 0);
    }

    public void sendPlayerActionPacketToServer(final PlayerActionType action, final int direction) {
        this.sendPlayerActionPacketToServer(action, new BlockPosition(0, 0, 0), direction);
    }

    public void sendPlayerActionPacketToServer(final PlayerActionType action, final BlockPosition blockPosition, final int direction) {
        final PacketWrapper playerAction = PacketWrapper.create(ServerboundBedrockPackets.PLAYER_ACTION, this.user);
        playerAction.write(BedrockTypes.UNSIGNED_VAR_LONG, this.runtimeId); // entity runtime id
        playerAction.write(BedrockTypes.VAR_INT, action.getValue()); // action
        playerAction.write(BedrockTypes.BLOCK_POSITION, blockPosition); // block position
        playerAction.write(BedrockTypes.BLOCK_POSITION, new BlockPosition(0, 0, 0)); // result position
        playerAction.write(BedrockTypes.VAR_INT, direction); // facing
        playerAction.sendToServer(BedrockProtocol.class);
    }

    public void sendSwingPacketToServer() {
        final PacketWrapper animate = PacketWrapper.create(ServerboundBedrockPackets.ANIMATE, this.user);
        animate.write(Types.UNSIGNED_BYTE, (short) AnimatePacketPayload_Action.Swing.getValue()); // action
        animate.write(BedrockTypes.UNSIGNED_VAR_LONG, this.runtimeId); // entity runtime id
        animate.write(BedrockTypes.FLOAT_LE, 0F); // data
        animate.write(BedrockTypes.OPTIONAL_STRING, ActorSwingSource.Attack.name().toLowerCase(Locale.ROOT)); // swing source
        animate.write(Types.BYTE, (byte) HandSlot.Mainhand.getValue()); // hand slot
        animate.sendToServer(BedrockProtocol.class);
    }

    public void updatePlayerPosition(final short flags) {
        final boolean newOnGround = (flags & MovePlayerFlag.ON_GROUND.getBit()) != 0;

        if (!this.preMove(null, null, newOnGround)) {
            return;
        }

        this.onGround = newOnGround;
        this.horizontalCollision = (flags & MovePlayerFlag.HORIZONTAL_COLLISION.getBit()) != 0;
    }

    public void updatePlayerPosition(final double x, final double y, final double z, final short flags) {
        final Position3f newPosition = new Position3f((float) x, (float) y + this.eyeOffset(), (float) z);
        final boolean newOnGround = (flags & MovePlayerFlag.ON_GROUND.getBit()) != 0;

        if (!this.preMove(newPosition, null, newOnGround)) {
            return;
        }

        this.position = newPosition;
        this.onGround = newOnGround;
        this.horizontalCollision = (flags & MovePlayerFlag.HORIZONTAL_COLLISION.getBit()) != 0;
    }

    public void updatePlayerPosition(final double x, final double y, final double z, final float yaw, final float pitch, final short flags) {
        final Position3f newPosition = new Position3f((float) x, (float) y + this.eyeOffset(), (float) z);
        final Position3f newRotation = new Position3f(pitch, yaw, yaw);
        final boolean newOnGround = (flags & MovePlayerFlag.ON_GROUND.getBit()) != 0;

        if (!this.preMove(newPosition, newRotation, newOnGround)) {
            return;
        }

        this.position = newPosition;
        this.rotation = newRotation;
        this.onGround = newOnGround;
        this.horizontalCollision = (flags & MovePlayerFlag.HORIZONTAL_COLLISION.getBit()) != 0;
    }

    public void updatePlayerPosition(final float yaw, final float pitch, final short flags) {
        final Position3f newRotation = new Position3f(pitch, yaw, yaw);
        final boolean newOnGround = (flags & MovePlayerFlag.ON_GROUND.getBit()) != 0;

        if (!this.preMove(null, newRotation, newOnGround)) {
            return;
        }

        this.rotation = newRotation;
        this.onGround = newOnGround;
        this.horizontalCollision = (flags & MovePlayerFlag.HORIZONTAL_COLLISION.getBit()) != 0;
    }

    public void confirmTeleport(final int teleportId) {
        if (teleportId < 0) { // Fake teleport
            if (this.pendingTeleportId == -teleportId) {
                this.pendingTeleportId = 0;
                this.waitingForPositionSync = false;
            }
        } else {
            this.serverSideTeleportConfirmed = true;
            if (!this.initiallySpawned) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received teleport confirm for teleport id " + teleportId + " but player is not spawned yet");
            }
            this.authInputData.add(PlayerAuthInputData.HandledTeleport);
        }
    }

    public Position3f prevPosition() {
        return this.prevPosition;
    }

    public boolean prevOnGround() {
        return this.prevOnGround;
    }

    public Set<PlayerAuthInputData> authInputData() {
        return this.authInputData;
    }

    public void addAuthInputData(final PlayerAuthInputData data) {
        this.authInputData.add(data);
    }

    public void addAuthInputData(final PlayerAuthInputData... data) {
        this.authInputData.addAll(Arrays.asList(data));
    }

    public List<AuthInputBlockAction> authInputBlockActions() {
        return this.authInputBlockActions;
    }

    public void addAuthInputBlockAction(final AuthInputBlockAction blockAction) {
        this.authInputData.add(PlayerAuthInputData.PerformBlockActions);
        this.authInputBlockActions.add(blockAction);
    }

    @Override
    public void setPosition(final Position3f position) {
        super.setPosition(position);
        this.prevPosition = position;
    }

    @Override
    public void setOnGround(final boolean onGround) {
        super.setOnGround(onGround);
        this.prevOnGround = onGround;
    }

    @Override
    public String name() {
        final PlayerListStorage playerList = this.user.get(PlayerListStorage.class);
        final Pair<Long, String> entry = playerList.getPlayer(this.javaUuid);
        if (entry != null) {
            return entry.value();
        }

        return this.name;
    }

    @Override
    public void setAbilities(final PlayerAbilities abilities) {
        final PacketWrapper playerAbilities = PacketWrapper.create(ClientboundPackets26_3.PLAYER_ABILITIES, this.user);
        this.setAbilities(abilities, playerAbilities);
        playerAbilities.send(BedrockProtocol.class);
    }

    public void setAbilities(final PlayerAbilities abilities, final PacketWrapper javaAbilities) {
        final PlayerAbilities prevAbilities = this.abilities;
        super.setAbilities(abilities);

        if (abilities.commandPermission() != prevAbilities.commandPermission()) {
            final CommandsStorage commandsStorage = this.user.get(CommandsStorage.class);
            if (commandsStorage != null) {
                commandsStorage.updateCommandTree();
            }
        }

        final PlayerPermissionLevel playerPermission = PlayerPermissionLevel.getByValue(abilities.playerPermission());
        if (playerPermission == PlayerPermissionLevel.Operator || (playerPermission == PlayerPermissionLevel.Custom && abilities.getBooleanValue(AbilitiesIndex.OperatorCommands))) {
            PacketFactory.sendJavaEntityEvent(this.user, this, EntityEvent.PERMISSION_LEVEL_OWNERS);
        } else {
            PacketFactory.sendJavaEntityEvent(this.user, this, EntityEvent.PERMISSION_LEVEL_ALL);
        }

        byte flags = 0;
        if (abilities.getBooleanValue(AbilitiesIndex.Invulnerable)) {
            flags |= AbilitiesFlag.INVULNERABLE.getBit();
        }
        if (abilities.getBooleanValue(AbilitiesIndex.Flying)) {
            flags |= AbilitiesFlag.FLYING.getBit();
        }
        if (abilities.getBooleanValue(AbilitiesIndex.MayFly)) {
            flags |= AbilitiesFlag.CAN_FLY.getBit();
        }
        if (abilities.getBooleanValue(AbilitiesIndex.Instabuild)) {
            flags |= AbilitiesFlag.INSTABUILD.getBit();
        }
        javaAbilities.write(Types.BYTE, flags); // flags
        javaAbilities.write(Types.FLOAT, abilities.getFloatValue(AbilitiesIndex.FlySpeed)); // fly speed
        javaAbilities.write(Types.FLOAT, abilities.getFloatValue(AbilitiesIndex.WalkSpeed)); // walk speed
    }

    public boolean isInitiallySpawned() {
        return this.initiallySpawned;
    }

    public void setInitiallySpawned() {
        this.initiallySpawned = true;
    }

    public DimensionChangeInfo dimensionChangeInfo() {
        return this.dimensionChangeInfo;
    }

    public void setDimensionChangeInfo(final DimensionChangeInfo dimensionChangeInfo) {
        this.dimensionChangeInfo = dimensionChangeInfo;
    }

    public Set<InputFlag> inputFlags() {
        return this.inputFlags;
    }

    public void setInputFlags(final Set<InputFlag> inputFlags) {
        this.inputFlags = inputFlags;
    }

    public Set<InputFlag> prevInputFlags() {
        return this.prevInputFlags;
    }

    public boolean horizontalCollision() {
        return this.horizontalCollision;
    }

    public void setHorizontalCollision(final boolean horizontalCollision) {
        this.horizontalCollision = horizontalCollision;
    }

    public boolean isSneaking() {
        return this.sneaking;
    }

    public void setSneaking(final boolean sneaking) {
        this.sneaking = sneaking;
    }

    public boolean isSprinting() {
        return this.sprinting;
    }

    public void setSprinting(final boolean sprinting) {
        this.sprinting = sprinting;

        final EntityAttribute oldMovementAttribute = this.attributes.get("minecraft:movement");
        final List<EntityAttribute.Modifier> modifiers = new ArrayList<>(Arrays.asList(oldMovementAttribute.modifiers()));
        modifiers.removeIf(modifier -> modifier.id().equals("d208fc00-42aa-4aad-9276-d5446530de43") && modifier.name().equals("Sprinting speed boost") && modifier.operation() == AttributeModifierOperation.OPERATION_MULTIPLY_TOTAL);
        if (this.sprinting) {
            modifiers.add(new EntityAttribute.Modifier("d208fc00-42aa-4aad-9276-d5446530de43", "Sprinting speed boost", 0.3F, AttributeModifierOperation.OPERATION_MULTIPLY_TOTAL, AttributeOperands.OPERAND_CURRENT, false));
        }
        final EntityAttribute newMovementAttribute = oldMovementAttribute.withModifiers(modifiers.toArray(new EntityAttribute.Modifier[0]));
        // Compute the current value, as the client only updates it when a modifier is changed by itself
        this.updateAttributes(new EntityAttribute[]{newMovementAttribute.withValue(newMovementAttribute.computeCurrentValue())});
    }

    public boolean isGliding() {
        return this.gliding;
    }

    public void setGliding(final boolean gliding) {
        this.gliding = gliding;
    }

    public GameType gameType() {
        return this.gameType;
    }

    public void setGameType(final GameType gameType) {
        this.gameType = gameType;
        this.updateJavaGameMode();
    }

    public GameMode javaGameMode() {
        return this.javaGameMode;
    }

    public void updateJavaGameMode() {
        this.javaGameMode = GameTypeRewriter.getEffectiveGameMode(this.gameType, this.gameSession.getLevelGameType());

        final PlayerAbilities.AbilitiesLayer abilitiesLayer = this.abilities.getOrCreateCacheLayer();
        switch (this.javaGameMode) {
            case CREATIVE -> {
                abilitiesLayer.setAbility(AbilitiesIndex.Invulnerable, true);
                abilitiesLayer.setAbility(AbilitiesIndex.MayFly, true);
                abilitiesLayer.setAbility(AbilitiesIndex.Instabuild, true);
                abilitiesLayer.setAbility(AbilitiesIndex.NoClip, false);
            }
            case SPECTATOR -> {
                abilitiesLayer.setAbility(AbilitiesIndex.Invulnerable, true);
                abilitiesLayer.setAbility(AbilitiesIndex.Flying, true);
                abilitiesLayer.setAbility(AbilitiesIndex.MayFly, true);
                abilitiesLayer.setAbility(AbilitiesIndex.Instabuild, false);
                abilitiesLayer.setAbility(AbilitiesIndex.NoClip, true);
            }
            default -> {
                abilitiesLayer.setAbility(AbilitiesIndex.Invulnerable, false);
                abilitiesLayer.setAbility(AbilitiesIndex.Flying, false);
                abilitiesLayer.setAbility(AbilitiesIndex.MayFly, false);
                abilitiesLayer.setAbility(AbilitiesIndex.Instabuild, false);
                abilitiesLayer.setAbility(AbilitiesIndex.NoClip, false);
            }
        }
    }

    public boolean checkCancelSwingPacket() {
        final boolean cancel = this.cancelNextSwingPacket;
        this.cancelNextSwingPacket = false;
        return cancel;
    }

    public void cancelNextSwingPacket() {
        this.cancelNextSwingPacket = true;
    }

    public BlockBreakingInfo blockBreakingInfo() {
        return this.blockBreakingInfo;
    }

    public void setBlockBreakingInfo(final BlockBreakingInfo blockBreakingInfo) {
        this.blockBreakingInfo = blockBreakingInfo;
    }

    public void setRequestedDismount(final boolean requestedDismount) {
        this.requestedDismount = requestedDismount;
    }

    @Override
    protected boolean translateAttribute(final EntityAttribute attribute, final PacketWrapper javaAttributes, final AtomicInteger attributeCount, final List<EntityData> javaEntityData) {
        return switch (attribute.name()) {
            case "minecraft:health", "minecraft:player.hunger", "minecraft:player.saturation" -> {
                final EntityAttribute health = attribute.name().equals("minecraft:health") ? attribute : this.attributes.get("minecraft:health");
                final EntityAttribute hunger = attribute.name().equals("minecraft:player.hunger") ? attribute : this.attributes.get("minecraft:player.hunger");
                final EntityAttribute saturation = attribute.name().equals("minecraft:player.saturation") ? attribute : this.attributes.get("minecraft:player.saturation");
                final PacketWrapper setHealth = PacketWrapper.create(ClientboundPackets26_3.SET_HEALTH, this.user);
                setHealth.write(Types.FLOAT, health.computeClampedValue()); // health
                setHealth.write(Types.VAR_INT, (int) hunger.computeClampedValue()); // food
                setHealth.write(Types.FLOAT, saturation.computeClampedValue()); // saturation
                setHealth.send(BedrockProtocol.class);

                if (attribute.name().equals("minecraft:health")) { // Call super to translate max health
                    yield super.translateAttribute(attribute, javaAttributes, attributeCount, javaEntityData);
                } else {
                    yield true;
                }
            }
            case "minecraft:player.experience", "minecraft:player.level" -> {
                final EntityAttribute experience = attribute.name().equals("minecraft:player.experience") ? attribute : this.attributes.get("minecraft:player.experience");
                final EntityAttribute level = attribute.name().equals("minecraft:player.level") ? attribute : this.attributes.get("minecraft:player.level");
                final PacketWrapper setExperience = PacketWrapper.create(ClientboundPackets26_3.SET_EXPERIENCE, this.user);
                setExperience.write(Types.FLOAT, experience.computeClampedValue()); // bar progress
                setExperience.write(Types.VAR_INT, (int) level.computeClampedValue()); // experience level
                setExperience.write(Types.VAR_INT, 0); // total experience
                setExperience.send(BedrockProtocol.class);
                yield true;
            }
            case "minecraft:player.exhaustion" -> true; // Ignore exhaustion
            default -> super.translateAttribute(attribute, javaAttributes, attributeCount, javaEntityData);
        };
    }

    private boolean preMove(final Position3f newPosition, final Position3f newRotation, final boolean newOnGround) {
        final ChunkTracker chunkTracker = this.user.get(ChunkTracker.class);

        // Allow position packet which is sent immediately after confirming a teleport
        if (this.serverSideTeleportConfirmed) {
            this.serverSideTeleportConfirmed = false;
            return true;
        }
        // Waiting for position sync
        if (this.waitingForPositionSync) {
            return false;
        }
        // Not spawned yet or respawning
        if (!this.initiallySpawned || this.dimensionChangeInfo != null) {
            if (!this.position.equals(newPosition)) {
                this.sendPlayerPositionPacketToClient(Relative.NONE);
            }
            return false;
        }
        // Is in unloaded chunk
        if (chunkTracker.isInUnloadedChunkSection(this.position)) {
            this.wasInsideUnloadedChunk = true;
            if (!this.position.equals(newPosition)) {
                this.waitingForPositionSync = true;
                this.sendPlayerPositionPacketToClient(Relative.ROTATION);
            }
            return false;
        } else if (this.wasInsideUnloadedChunk) {
            this.wasInsideUnloadedChunk = false;
            this.waitingForPositionSync = true;
            this.sendPlayerPositionPacketToClient(Relative.ROTATION);
            return false;
        }
        // Loaded -> Unloaded chunk
        if (newPosition != null && chunkTracker.isInUnloadedChunkSection(newPosition)) {
            this.waitingForPositionSync = true;
            this.sendPlayerPositionPacketToClient(Relative.ROTATION);
            return false;
        }

        if (newPosition != null && !this.position.equals(newPosition)) {
            return true;
        } else if (newRotation != null && !this.rotation.equals(newRotation)) {
            return true;
        } else if (this.onGround != newOnGround) {
            return true;
        }

        return false;
    }

    public record DimensionChangeInfo(Long loadingScreenId) {
    }

    public record BlockBreakingInfo(BlockPosition position, Direction direction) {
    }

    public record AuthInputBlockAction(PlayerActionType action, BlockPosition position, int direction) {

        public AuthInputBlockAction(final PlayerActionType action) {
            this(action, null, -1);
        }

    }

}
