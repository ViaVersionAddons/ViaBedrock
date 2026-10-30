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

import com.google.common.collect.Lists;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.PaintingVariant;
import com.viaversion.viaversion.api.minecraft.Vector3d;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes26_3;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.item.data.EnumTypes;
import com.viaversion.viaversion.api.minecraft.item.data.SwingAnimation;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.util.Key;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.entity.ClientPlayerEntity;
import net.raphimc.viabedrock.api.model.entity.CustomEntity;
import net.raphimc.viabedrock.api.model.entity.Entity;
import net.raphimc.viabedrock.api.model.entity.LivingEntity;
import net.raphimc.viabedrock.api.model.entity.properties.EntityProperty;
import net.raphimc.viabedrock.api.model.entity.properties.EntityPropertyList;
import net.raphimc.viabedrock.api.resourcepack.definition.EntityDefinitions;
import net.raphimc.viabedrock.api.util.MathUtil;
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.api.util.RegistryUtil;
import net.raphimc.viabedrock.api.util.TextUtil;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ClientboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.Direction;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.enums.java.AnimateAction;
import net.raphimc.viabedrock.protocol.data.enums.java.Relative;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.EquipmentSlot;
import net.raphimc.viabedrock.protocol.data.generated.java.EntityDataFields;
import net.raphimc.viabedrock.protocol.data.generated.java.RegistryKeys;
import net.raphimc.viabedrock.protocol.model.*;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;
import net.raphimc.viabedrock.protocol.storage.EntityPropertyTracker;
import net.raphimc.viabedrock.protocol.storage.EntityTracker;
import net.raphimc.viabedrock.protocol.storage.GameSessionStorage;
import net.raphimc.viabedrock.protocol.storage.ResourcePackStorage;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class EntityPackets {

    private static final int INTERPOLATION_STEP_TICKS = 3;
    private static final float PAINTING_POS_OFFSET = -0.46875F;
    private static final SwingAnimation DEFAULT_SWING_ANIMATION = new SwingAnimation(EnumTypes.SWING_ANIMATION1_21_11.idFromName("whack"), 6);

    public static void register(final BedrockProtocol protocol) {
        protocol.registerClientbound(ClientboundBedrockPackets.ADD_ENTITY, ClientboundPackets26_3.ADD_ENTITY, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);
            final GameSessionStorage gameSession = wrapper.user().get(GameSessionStorage.class);

            final long entityUniqueId = wrapper.read(BedrockTypes.VAR_LONG); // entity unique id
            final long entityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // entity runtime id
            final String type = Key.namespaced(wrapper.read(BedrockTypes.STRING)); // type
            final Position3f position = wrapper.read(BedrockTypes.POSITION_3F); // position
            final Position3f motion = wrapper.read(BedrockTypes.POSITION_3F); // motion
            final Position3f rotation = wrapper.read(BedrockTypes.POSITION_3F); // rotation
            wrapper.read(BedrockTypes.FLOAT_LE); // body rotation
            final EntityAttribute[] attributes = new EntityAttribute[wrapper.read(BedrockTypes.UNSIGNED_VAR_INT)]; // attribute count
            for (int i = 0; i < attributes.length; i++) {
                final String name = wrapper.read(BedrockTypes.STRING); // name
                final float minValue = wrapper.read(BedrockTypes.FLOAT_LE); // min
                final float currentValue = wrapper.read(BedrockTypes.FLOAT_LE); // current
                final float maxValue = wrapper.read(BedrockTypes.FLOAT_LE); // max
                attributes[i] = new EntityAttribute(name, currentValue, minValue, maxValue);
            }
            final EntityData[] entityData = wrapper.read(BedrockTypes.ENTITY_DATA_ARRAY); // entity data
            wrapper.read(BedrockTypes.ENTITY_PROPERTIES); // entity properties
            wrapper.read(BedrockTypes.ENTITY_LINK_ARRAY); // entity links
            wrapper.read(BedrockTypes.OPTIONAL_PASSENGER_OF_BLOCKS); // passenger of blocks

            final Entity entity;
            final EntityTypes26_3 javaEntityType = BedrockProtocol.MAPPINGS.getBedrockToJavaEntities().get(type);
            if (javaEntityType != null) {
                entity = entityTracker.addEntity(entityUniqueId, entityRuntimeId, type, javaEntityType);
            } else if (gameSession.getAvailableEntityIdentifiers().contains(type)) {
                final ResourcePackStorage resourcePackStorage = wrapper.user().get(ResourcePackStorage.class);
                final EntityDefinitions.EntityDefinition entityDefinition = resourcePackStorage.getEntities().get(type);
                if (entityDefinition != null) {
                    if (resourcePackStorage.isLoadedOnJavaClient()) {
                        entity = new CustomEntity(wrapper.user(), entityUniqueId, entityRuntimeId, type, entityTracker.getNextJavaEntityId(), entityDefinition);
                        entityTracker.addEntity(entity);
                    } else {
                        entity = entityTracker.addEntity(entityUniqueId, entityRuntimeId, type, EntityTypes26_3.PIG);
                    }
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing bedrock entity type: " + type);
                    wrapper.cancel();
                    return;
                }
            } else {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown bedrock entity type: " + type);
                wrapper.cancel();
                return;
            }
            entity.setPosition(position);
            entity.setRotation(rotation);

            wrapper.write(Types.VAR_INT, entity.javaId()); // entity id
            wrapper.write(Types.UUID, entity.javaUuid()); // uuid
            wrapper.write(Types.VAR_INT, entity.javaType().getId()); // type id
            wrapper.write(Types.DOUBLE, (double) position.x()); // x
            wrapper.write(Types.DOUBLE, (double) position.y()); // y
            wrapper.write(Types.DOUBLE, (double) position.z()); // z
            wrapper.write(Types.LOW_PRECISION_VECTOR, new Vector3d(motion.x(), motion.y(), motion.z())); // velocity
            wrapper.write(Types.BYTE, MathUtil.float2Byte(rotation.x())); // pitch
            wrapper.write(Types.BYTE, MathUtil.float2Byte(rotation.y())); // yaw
            wrapper.write(Types.BYTE, MathUtil.float2Byte(rotation.z())); // head yaw
            wrapper.write(Types.VAR_INT, 0); // data
            wrapper.send(BedrockProtocol.class);
            wrapper.cancel();

            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.updateAttributes(attributes);
            }
            entity.updateEntityData(entityData);
        });
        protocol.registerClientbound(ClientboundBedrockPackets.ADD_ITEM_ENTITY, ClientboundPackets26_3.ADD_ENTITY, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);
            final ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);

            final long entityUniqueId = wrapper.read(BedrockTypes.VAR_LONG); // entity unique id
            final long entityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // entity runtime id
            final BedrockItem item = wrapper.read(itemRewriter.newItemType()); // item
            final Position3f position = wrapper.read(BedrockTypes.POSITION_3F); // position
            final Position3f motion = wrapper.read(BedrockTypes.POSITION_3F); // motion
            final EntityData[] entityData = wrapper.read(BedrockTypes.ENTITY_DATA_ARRAY); // entity data
            wrapper.read(Types.BOOLEAN); // from fishing

            final Entity entity = entityTracker.addEntity(entityUniqueId, entityRuntimeId, "minecraft:item", EntityTypes26_3.ITEM);
            entity.setPosition(position);

            wrapper.write(Types.VAR_INT, entity.javaId()); // entity id
            wrapper.write(Types.UUID, entity.javaUuid()); // uuid
            wrapper.write(Types.VAR_INT, entity.javaType().getId()); // type id
            wrapper.write(Types.DOUBLE, (double) position.x()); // x
            wrapper.write(Types.DOUBLE, (double) position.y()); // y
            wrapper.write(Types.DOUBLE, (double) position.z()); // z
            wrapper.write(Types.LOW_PRECISION_VECTOR, new Vector3d(motion.x(), motion.y(), motion.z())); // velocity
            wrapper.write(Types.BYTE, (byte) 0); // pitch
            wrapper.write(Types.BYTE, (byte) 0); // yaw
            wrapper.write(Types.BYTE, (byte) 0); // head yaw
            wrapper.write(Types.VAR_INT, 0); // data
            wrapper.send(BedrockProtocol.class);
            wrapper.cancel();

            final List<EntityData> javaEntityData = new ArrayList<>();
            entity.updateEntityData(entityData, javaEntityData);
            javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.ITEM), VersionedTypes.V26_3.entityDataTypes.itemType, itemRewriter.javaItem(item)));
            final PacketWrapper setEntityData = PacketWrapper.create(ClientboundPackets26_3.SET_ENTITY_DATA, wrapper.user());
            setEntityData.write(Types.VAR_INT, entity.javaId()); // entity id
            setEntityData.write(VersionedTypes.V26_3.entityDataList, javaEntityData); // entity data
            setEntityData.send(BedrockProtocol.class);
        });
        protocol.registerClientbound(ClientboundBedrockPackets.MOVE_ENTITY_ABSOLUTE, ClientboundPackets26_3.ENTITY_POSITION_SYNC, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);

            final long entityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // entity runtime id
            final short flags = wrapper.read(Types.UNSIGNED_BYTE); // flags
            final Position3f position = wrapper.read(BedrockTypes.POSITION_3F); // position
            final float pitch = MathUtil.byte2Float(wrapper.read(Types.BYTE)); // pitch
            final float yaw = MathUtil.byte2Float(wrapper.read(Types.BYTE)); // yaw
            final float headYaw = MathUtil.byte2Float(wrapper.read(Types.BYTE)); // head yaw
            final boolean onGround = (flags & 1) != 0;
            final boolean teleported = (flags & 2) != 0; // If the position shouldn't be interpolated
            final boolean forceMoveLocalEntity = (flags & 4) != 0;

            final Entity entity = entityTracker.getEntityByRid(entityRuntimeId);
            if (entity == null) {
                wrapper.cancel();
                return;
            }

            if (entity == entityTracker.getClientPlayer()) {
                if (!teleported && !forceMoveLocalEntity) {
                    wrapper.cancel();
                    return;
                }
                entity.setPosition(position);

                if (teleported) {
                    wrapper.setPacketType(ClientboundPackets26_3.PLAYER_POSITION);
                    entityTracker.getClientPlayer().writePlayerPositionPacketToClient(wrapper, Relative.union(Relative.ROTATION, Relative.VELOCITY), true);
                } else { // force move local entity
                    wrapper.write(Types.VAR_INT, entity.javaId()); // entity id
                    wrapper.write(Types.VAR_INT, 1); // Stepped
                    wrapper.write(Types.VAR_INT, 1); // 1 step
                    wrapper.write(Types.DOUBLE, (double) entity.position().x()); // x
                    wrapper.write(Types.DOUBLE, (double) entity.position().y() - entity.eyeOffset()); // y
                    wrapper.write(Types.DOUBLE, (double) entity.position().z()); // z
                    wrapper.write(Types.VAR_INT, INTERPOLATION_STEP_TICKS);
                    wrapper.write(Types.FLOAT, entity.rotation().y()); // yaw
                    wrapper.write(Types.FLOAT, entity.rotation().x()); // pitch
                    wrapper.write(Types.BOOLEAN, entity.isOnGround()); // on ground
                }
                return;
            }

            entity.setPosition(position);
            entity.setRotation(new Position3f(pitch, yaw, headYaw));
            entity.setOnGround(onGround);

            wrapper.write(Types.VAR_INT, entity.javaId()); // entity id
            wrapper.write(Types.VAR_INT, 1); // Stepped
            wrapper.write(Types.VAR_INT, 1); // 1 step
            wrapper.write(Types.DOUBLE, (double) position.x()); // x
            wrapper.write(Types.DOUBLE, (double) position.y() - entity.eyeOffset()); // y
            wrapper.write(Types.DOUBLE, (double) position.z()); // z
            wrapper.write(Types.VAR_INT, INTERPOLATION_STEP_TICKS);
            wrapper.write(Types.FLOAT, yaw); // yaw
            wrapper.write(Types.FLOAT, pitch); // pitch
            wrapper.write(Types.BOOLEAN, entity.isOnGround()); // on ground

            PacketFactory.sendJavaRotateHead(wrapper.user(), entity);
        });
        protocol.registerClientbound(ClientboundBedrockPackets.MOVE_ENTITY_DELTA, ClientboundPackets26_3.ENTITY_POSITION_SYNC, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);

            final long entityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // entity runtime id
            final boolean hasX = wrapper.read(Types.BOOLEAN);
            final float x = hasX ? wrapper.read(BedrockTypes.FLOAT_LE) : 0;
            final boolean hasY = wrapper.read(Types.BOOLEAN);
            final float y = hasY ? wrapper.read(BedrockTypes.FLOAT_LE) : 0;
            final boolean hasZ = wrapper.read(Types.BOOLEAN);
            final float z = hasZ ? wrapper.read(BedrockTypes.FLOAT_LE) : 0;
            final boolean hasPitch = wrapper.read(Types.BOOLEAN);
            final byte pitch = hasPitch ? wrapper.read(Types.BYTE) : 0;
            final boolean hasYaw = wrapper.read(Types.BOOLEAN);
            final byte yaw = hasYaw ? wrapper.read(Types.BYTE) : 0;
            final boolean hasHeadYaw = wrapper.read(Types.BOOLEAN);
            final byte headYaw = hasHeadYaw ? wrapper.read(Types.BYTE) : 0;
            final boolean onGround = wrapper.read(Types.BOOLEAN);
            final boolean teleported = wrapper.read(Types.BOOLEAN); // If the position shouldn't be interpolated
            final boolean forceMoveLocalEntity = wrapper.read(Types.BOOLEAN);
            wrapper.read(Types.BOOLEAN); // force completion
            wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // ticks

            final Entity entity = entityTracker.getEntityByRid(entityRuntimeId);
            if (entity == null) {
                wrapper.cancel();
                return;
            }

            if (entity == entityTracker.getClientPlayer()) {
                if (!teleported && !forceMoveLocalEntity) {
                    wrapper.cancel();
                    return;
                }

                entity.setPosition(new Position3f(x, y, z));

                if (teleported) {
                    wrapper.setPacketType(ClientboundPackets26_3.PLAYER_POSITION);
                    entityTracker.getClientPlayer().writePlayerPositionPacketToClient(wrapper, Relative.union(Relative.ROTATION, Relative.VELOCITY), true);
                } else { // force move local entity
                    wrapper.write(Types.VAR_INT, entity.javaId()); // entity id
                    wrapper.write(Types.VAR_INT, 1); // Stepped
                    wrapper.write(Types.VAR_INT, 1); // 1 step
                    wrapper.write(Types.DOUBLE, (double) entity.position().x()); // x
                    wrapper.write(Types.DOUBLE, (double) entity.position().y() - entity.eyeOffset()); // y
                    wrapper.write(Types.DOUBLE, (double) entity.position().z()); // z
                    wrapper.write(Types.VAR_INT, INTERPOLATION_STEP_TICKS);
                    wrapper.write(Types.FLOAT, entity.rotation().y()); // yaw
                    wrapper.write(Types.FLOAT, entity.rotation().x()); // pitch
                    wrapper.write(Types.BOOLEAN, entity.isOnGround()); // on ground
                }
                return;
            }

            if (hasX) {
                entity.setPosition(new Position3f(x, entity.position().y(), entity.position().z()));
            }
            if (hasY) {
                entity.setPosition(new Position3f(entity.position().x(), y, entity.position().z()));
            }
            if (hasZ) {
                entity.setPosition(new Position3f(entity.position().x(), entity.position().y(), z));
            }
            if (hasPitch) {
                entity.setRotation(new Position3f(MathUtil.byte2Float(pitch), entity.rotation().y(), entity.rotation().z()));
            }
            if (hasYaw) {
                entity.setRotation(new Position3f(entity.rotation().x(), MathUtil.byte2Float(yaw), entity.rotation().z()));
            }
            if (hasHeadYaw) {
                entity.setRotation(new Position3f(entity.rotation().x(), entity.rotation().y(), MathUtil.byte2Float(headYaw)));
                PacketFactory.sendJavaRotateHead(wrapper.user(), entity);
            }
            entity.setOnGround(onGround);

            wrapper.write(Types.VAR_INT, entity.javaId()); // entity id
            wrapper.write(Types.VAR_INT, 1); // Stepped
            wrapper.write(Types.VAR_INT, 1); // 1 step
            wrapper.write(Types.DOUBLE, (double) entity.position().x()); // x
            wrapper.write(Types.DOUBLE, (double) entity.position().y() - entity.eyeOffset()); // y
            wrapper.write(Types.DOUBLE, (double) entity.position().z()); // z
            wrapper.write(Types.VAR_INT, INTERPOLATION_STEP_TICKS);
            wrapper.write(Types.FLOAT, entity.rotation().y()); // yaw
            wrapper.write(Types.FLOAT, entity.rotation().x()); // pitch
            wrapper.write(Types.BOOLEAN, entity.isOnGround()); // on ground
        });
        protocol.registerClientbound(ClientboundBedrockPackets.SET_ENTITY_MOTION, ClientboundPackets26_3.SET_ENTITY_MOTION, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);

            final long entityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // entity runtime id
            final Position3f motion = wrapper.read(BedrockTypes.POSITION_3F); // motion
            wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // tick

            final Entity entity = entityTracker.getEntityByRid(entityRuntimeId);
            if (entity == null) {
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.VAR_INT, entity.javaId()); // entity id
            wrapper.write(Types.LOW_PRECISION_VECTOR, new Vector3d(motion.x(), motion.y(), motion.z())); // velocity
        });
        protocol.registerClientbound(ClientboundBedrockPackets.REMOVE_ENTITY, ClientboundPackets26_3.REMOVE_ENTITIES, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);
            final long entityUniqueId = wrapper.read(BedrockTypes.VAR_LONG); // entity unique id

            final Entity entity = entityTracker.getEntityByUid(entityUniqueId);
            if (entity == null) {
                wrapper.cancel();
                return;
            }
            entityTracker.removeEntity(entity);

            wrapper.write(Types.VAR_INT_ARRAY_PRIMITIVE, new int[]{entity.javaId()}); // entity ids
        });
        protocol.registerClientbound(ClientboundBedrockPackets.ADD_PAINTING, ClientboundPackets26_3.ADD_ENTITY, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);
            final GameSessionStorage gameSession = wrapper.user().get(GameSessionStorage.class);
            final CompoundTag paintingRegistry = gameSession.getJavaRegistries().getCompoundTag(RegistryKeys.PAINTING_VARIANT);

            final long entityUniqueId = wrapper.read(BedrockTypes.VAR_LONG); // entity unique id
            final long entityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // entity runtime id
            final Position3f position = wrapper.read(BedrockTypes.POSITION_3F); // position
            final Direction direction = Direction.getFromHorizontalId(wrapper.read(BedrockTypes.VAR_INT), Direction.NORTH); // direction
            final String motive = wrapper.read(BedrockTypes.STRING); // motive

            String javaIdentifier = BedrockProtocol.MAPPINGS.getBedrockToJavaPaintings().get(motive);
            if (javaIdentifier == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown bedrock painting motive: " + motive);
                javaIdentifier = "minecraft:kebab";
            }
            final CompoundTag paintingEntry = paintingRegistry.getCompoundTag(javaIdentifier);
            final Holder<PaintingVariant> paintingHolder = Holder.of(RegistryUtil.getRegistryIndex(paintingRegistry, paintingEntry));
            final int width = paintingEntry.getInt("width");
            final int height = paintingEntry.getInt("height");
            final float widthOffset = width % 2 == 0 ? 0.5F : 0;
            final float heightOffset = height % 2 == 0 ? 0.5F : 0;
            Position3f positionOffset = new Position3f(-0.5F, -0.5F, -0.5F);
            positionOffset = switch (direction) {
                case NORTH -> positionOffset.subtract(-widthOffset, heightOffset, -PAINTING_POS_OFFSET);
                case EAST -> positionOffset.subtract(PAINTING_POS_OFFSET, heightOffset, -widthOffset);
                case SOUTH -> positionOffset.subtract(widthOffset, heightOffset, PAINTING_POS_OFFSET);
                case WEST -> positionOffset.subtract(-PAINTING_POS_OFFSET, heightOffset, widthOffset);
                default -> positionOffset;
            };

            final Entity entity = entityTracker.addEntity(entityUniqueId, entityRuntimeId, "minecraft:painting", EntityTypes26_3.PAINTING);
            entity.setPosition(position);

            wrapper.write(Types.VAR_INT, entity.javaId()); // entity id
            wrapper.write(Types.UUID, entity.javaUuid()); // uuid
            wrapper.write(Types.VAR_INT, entity.javaType().getId()); // type id
            wrapper.write(Types.DOUBLE, (double) position.x() + positionOffset.x()); // x
            wrapper.write(Types.DOUBLE, (double) position.y() + positionOffset.y()); // y
            wrapper.write(Types.DOUBLE, (double) position.z() + positionOffset.z()); // z
            wrapper.write(Types.LOW_PRECISION_VECTOR, Vector3d.ZERO); // velocity
            wrapper.write(Types.BYTE, (byte) 0); // pitch
            wrapper.write(Types.BYTE, (byte) 0); // yaw
            wrapper.write(Types.BYTE, (byte) 0); // head yaw
            wrapper.write(Types.VAR_INT, direction.verticalId()); // data
            wrapper.send(BedrockProtocol.class);
            wrapper.cancel();

            final PacketWrapper setEntityData = PacketWrapper.create(ClientboundPackets26_3.SET_ENTITY_DATA, wrapper.user());
            setEntityData.write(Types.VAR_INT, entity.javaId()); // entity id
            setEntityData.write(VersionedTypes.V26_3.entityDataList, Lists.newArrayList(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.PAINTING_VARIANT), VersionedTypes.V26_3.entityDataTypes.paintingVariantType, paintingHolder))); // entity data
            setEntityData.send(BedrockProtocol.class);
        });
        protocol.registerClientbound(ClientboundBedrockPackets.ENTITY_EVENT, ClientboundPackets26_3.ENTITY_EVENT, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);
            final GameSessionStorage gameSession = wrapper.user().get(GameSessionStorage.class);

            final long entityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // entity runtime id
            final short rawEvent = wrapper.read(Types.UNSIGNED_BYTE); // event
            final ActorEvent event = ActorEvent.getByValue(rawEvent);
            if (event == null) {
                wrapper.cancel();
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown ActorEvent: " + rawEvent);
                return;
            }
            final int data = wrapper.read(BedrockTypes.VAR_INT); // data
            wrapper.read(BedrockTypes.OPTIONAL_POSITION_3F); // fire at position

            final Entity entity = entityTracker.getEntityByRid(entityRuntimeId);
            if (entity == null) {
                wrapper.cancel();
                return;
            }
            switch (event) {
                case HURT -> {
                    final CompoundTag damageTypeRegistry = gameSession.getJavaRegistries().getCompoundTag(RegistryKeys.DAMAGE_TYPE);
                    final SharedTypes_Legacy_ActorDamageCause damageCause = SharedTypes_Legacy_ActorDamageCause.getByValue(data, SharedTypes_Legacy_ActorDamageCause.Override);
                    final CompoundTag damageTypeEntry = damageTypeRegistry.getCompoundTag(BedrockProtocol.MAPPINGS.getBedrockToJavaDamageCauses().get(damageCause));

                    wrapper.setPacketType(ClientboundPackets26_3.DAMAGE_EVENT);
                    wrapper.write(Types.VAR_INT, entity.javaId()); // entity id
                    wrapper.write(Types.VAR_INT, RegistryUtil.getRegistryIndex(damageTypeRegistry, damageTypeEntry)); // source type
                    wrapper.write(Types.OPTIONAL_VAR_INT, null); // source cause id
                    wrapper.write(Types.OPTIONAL_VAR_INT, null); // source direct id
                    wrapper.write(Types.BOOLEAN, false); // has source position
                    if (entity != entityTracker.getClientPlayer()) {
                        entity.playSound("hurt");
                    }
                }
                case DEATH -> {
                    wrapper.cancel();
                    if (entity instanceof LivingEntity livingEntity) {
                        livingEntity.setHealth(0F);
                        livingEntity.sendAttribute("minecraft:health");
                    }
                    if (entity == entityTracker.getClientPlayer() && entityTracker.getClientPlayer().isDead() && gameSession.getDeathMessage() != null) {
                        final PacketWrapper playerCombatKill = PacketWrapper.create(ClientboundPackets26_3.PLAYER_COMBAT_KILL, wrapper.user());
                        playerCombatKill.write(Types.VAR_INT, entityTracker.getClientPlayer().javaId()); // entity id
                        playerCombatKill.write(Types.TAG, TextUtil.textComponentToNbt(gameSession.getDeathMessage())); // message
                        playerCombatKill.send(BedrockProtocol.class);
                    }
                    if (entity != entityTracker.getClientPlayer()) {
                        entity.playSound("death");
                    }
                }
                default -> {
                    wrapper.cancel();
                    // TODO: Handle remaining events
                    // throw new IllegalStateException("Unhandled ActorEvent: " + event);
                }
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.UPDATE_ATTRIBUTES, ClientboundPackets26_3.UPDATE_ATTRIBUTES, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);

            final long entityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // entity runtime id
            final EntityAttribute[] attributes = new EntityAttribute[wrapper.read(BedrockTypes.UNSIGNED_VAR_INT)]; // attribute count
            for (int i = 0; i < attributes.length; i++) {
                final float minValue = wrapper.read(BedrockTypes.FLOAT_LE); // min value
                final float maxValue = wrapper.read(BedrockTypes.FLOAT_LE); // max value
                final float currentValue = wrapper.read(BedrockTypes.FLOAT_LE); // current value
                final float defaultMinValue = wrapper.read(BedrockTypes.FLOAT_LE); // default min value
                final float defaultMaxValue = wrapper.read(BedrockTypes.FLOAT_LE); // default max value
                final float defaultValue = wrapper.read(BedrockTypes.FLOAT_LE); // default value
                final String name = wrapper.read(BedrockTypes.STRING); // name
                final EntityAttribute.Modifier[] modifiers = new EntityAttribute.Modifier[wrapper.read(BedrockTypes.UNSIGNED_VAR_INT)]; // modifier count
                for (int j = 0; j < modifiers.length; j++) {
                    final String id = wrapper.read(BedrockTypes.STRING); // id
                    final String modifierName = wrapper.read(BedrockTypes.STRING); // name
                    final float amount = wrapper.read(BedrockTypes.FLOAT_LE); // amount
                    final AttributeModifierOperation operation = AttributeModifierOperation.getByValue(wrapper.read(BedrockTypes.INT_LE), AttributeModifierOperation.OPERATION_INVALID); // operation
                    final AttributeOperands operand = AttributeOperands.getByValue(wrapper.read(BedrockTypes.INT_LE), AttributeOperands.OPERAND_INVALID); // operand
                    final boolean isSerializable = wrapper.read(Types.BOOLEAN); // is serializable
                    modifiers[j] = new EntityAttribute.Modifier(id, modifierName, amount, operation, operand, isSerializable);
                }
                attributes[i] = new EntityAttribute(name, currentValue, minValue, maxValue, defaultValue, defaultMinValue, defaultMaxValue, modifiers);
            }
            wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // tick

            final Entity entity = entityTracker.getEntityByRid(entityRuntimeId);
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.updateAttributes(attributes, wrapper);
            } else {
                wrapper.cancel();
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.SET_ENTITY_DATA, ClientboundPackets26_3.SET_ENTITY_DATA, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);

            final long entityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // entity runtime id
            final EntityData[] entityData = wrapper.read(BedrockTypes.ENTITY_DATA_ARRAY); // entity data
            final EntityProperties entityProperties = wrapper.read(BedrockTypes.ENTITY_PROPERTIES); // entity properties
            wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // tick

            final Entity entity = entityTracker.getEntityByRid(entityRuntimeId);
            if (entity == null) {
                wrapper.cancel();
                return;
            }

            final List<EntityData> javaEntityData = new ArrayList<>();
            entity.updateEntityData(entityData, javaEntityData);
            entity.translateEntityProperties(entityProperties, javaEntityData);
            wrapper.write(Types.VAR_INT, entity.javaId()); // entity id
            wrapper.write(VersionedTypes.V26_3.entityDataList, javaEntityData); // entity data
        });
        protocol.registerClientbound(ClientboundBedrockPackets.SYNC_ENTITY_PROPERTY, null, wrapper -> {
            wrapper.cancel();
            final Tag tag = wrapper.read(BedrockTypes.NETWORK_TAG);

            final EntityPropertyTracker epTracker = wrapper.user().get(EntityPropertyTracker.class);

            final String entityType = ((CompoundTag) tag).getString("type");
            final List<EntityProperty> intEntityProperties = new ArrayList<>();
            final List<EntityProperty.FloatProperty> floatEntityProperties = new ArrayList<>();

            final ListTag<CompoundTag> properties = ((CompoundTag) tag).getListTag("properties", CompoundTag.class);
            for (CompoundTag property : properties) {
                final EntityProperty newProperty = switch (property.getInt("type")) {
                    case 0 -> new EntityProperty.IntProperty(
                        property.getString("name"),
                        property.getInt("min"),
                        property.getInt("max")
                    );
                    case 1 -> new EntityProperty.FloatProperty(
                        property.getString("name"),
                        property.getFloat("min"),
                        property.getFloat("max")
                    );
                    case 2 -> new EntityProperty.BooleanProperty(
                        property.getString("name")
                    );
                    case 3 -> new EntityProperty.EnumProperty(
                        property.getString("name"),
                        property.getListTag("enum", StringTag.class).stream().map(StringTag::getValue).toArray(String[]::new)
                    );
                    default -> {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received entity property with unknown type: " + property.getInt("type"));
                        yield null;
                    }
                };

                if (newProperty != null) {
                    if (newProperty instanceof EntityProperty.FloatProperty floatProperty) {
                        floatEntityProperties.add(floatProperty);
                    } else {
                        intEntityProperties.add(newProperty);
                    }
                }
            }

            epTracker.addEntity(entityType, new EntityPropertyList(intEntityProperties, floatEntityProperties));
        });
        protocol.registerClientbound(ClientboundBedrockPackets.MOB_EFFECT, ClientboundPackets26_3.UPDATE_MOB_EFFECT, wrapper -> {
            final long entityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // entity runtime id
            final MobEffectPacketPayload_Event event = MobEffectPacketPayload_Event.getByValue(wrapper.read(Types.BYTE), MobEffectPacketPayload_Event.Invalid); // event id
            final int effectId = wrapper.read(BedrockTypes.VAR_INT); // effect id
            final int amplifier = wrapper.read(BedrockTypes.VAR_INT); // amplifier
            final boolean showParticles = wrapper.read(Types.BOOLEAN); // show particles
            final int duration = wrapper.read(BedrockTypes.VAR_INT); // duration
            wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // tick
            final boolean ambient = wrapper.read(Types.BOOLEAN); // ambient

            final Entity entity = wrapper.user().get(EntityTracker.class).getEntityByRid(entityRuntimeId);
            if (!(entity instanceof LivingEntity livingEntity) || effectId == 0) {
                wrapper.cancel();
                return;
            }

            final String bedrockIdentifier = BedrockProtocol.MAPPINGS.getBedrockEffects().inverse().get(effectId);
            if (bedrockIdentifier == null) { // Bedrock client crashes
                throw new IllegalStateException("Unknown bedrock effect: " + effectId);
            }
            final EntityEffect effect = new EntityEffect(bedrockIdentifier, amplifier, duration, showParticles, ambient);
            switch (event) {
                case Invalid -> wrapper.cancel();
                case Add, Update -> livingEntity.updateEffect(effect, wrapper);
                case Remove -> {
                    wrapper.setPacketType(ClientboundPackets26_3.REMOVE_MOB_EFFECT);
                    livingEntity.removeEffect(bedrockIdentifier, wrapper);
                }
                default -> throw new IllegalStateException("Unhandled MobEffectPacketPayload_Event: " + event);
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.ANIMATE, ClientboundPackets26_3.ANIMATE, wrapper -> {
            final AnimatePacketPayload_Action action = AnimatePacketPayload_Action.getByValue(wrapper.read(Types.UNSIGNED_BYTE), AnimatePacketPayload_Action.NoAction); // action
            final long entityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // entity runtime id
            wrapper.read(BedrockTypes.FLOAT_LE); // data
            wrapper.read(BedrockTypes.OPTIONAL_STRING); // swing source
            final HandSlot hand = HandSlot.getByValue(wrapper.read(Types.BYTE), HandSlot.Mainhand); // hand

            final Entity entity = wrapper.user().get(EntityTracker.class).getEntityByRid(entityRuntimeId);
            if (entity == null) {
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.VAR_INT, entity.javaId()); // entity id
            switch (action) {
                case NoAction -> wrapper.cancel();
                case Swing -> {
                    wrapper.setPacketType(ClientboundPackets26_3.SWING_ANIMATION);
                    wrapper.write(Types.VAR_INT, hand.getValue()); // hand
                    wrapper.write(SwingAnimation.TYPE, DEFAULT_SWING_ANIMATION);
                }
                case WakeUp -> wrapper.write(Types.UNSIGNED_BYTE, (short) AnimateAction.WAKE_UP.ordinal()); // action
                case CriticalHit -> wrapper.write(Types.UNSIGNED_BYTE, (short) AnimateAction.CRITICAL_HIT.ordinal()); // action
                case MagicCriticalHit -> wrapper.write(Types.UNSIGNED_BYTE, (short) AnimateAction.MAGIC_CRITICAL_HIT.ordinal()); // action
                default -> throw new IllegalStateException("Unhandled AnimatePacket_Action: " + action);
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.MOB_ARMOR_EQUIPMENT, ClientboundPackets26_3.SET_EQUIPMENT, wrapper -> {
            final ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);
            final long entityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // entity runtime id
            final BedrockItem head = wrapper.read(itemRewriter.newItemType()); // head
            final BedrockItem chest = wrapper.read(itemRewriter.newItemType()); // chest
            final BedrockItem legs = wrapper.read(itemRewriter.newItemType()); // legs
            final BedrockItem feet = wrapper.read(itemRewriter.newItemType()); // feet
            final BedrockItem body = wrapper.read(itemRewriter.newItemType()); // body

            final Entity entity = wrapper.user().get(EntityTracker.class).getEntityByRid(entityRuntimeId);
            if (entity == null || entity instanceof ClientPlayerEntity) {
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.VAR_INT, entity.javaId()); // entity id
            wrapper.write(Types.BYTE, (byte) (EquipmentSlot.FEET.ordinal() | Byte.MIN_VALUE)); // slot
            wrapper.write(VersionedTypes.V26_3.item, itemRewriter.javaItem(feet)); // item
            wrapper.write(Types.BYTE, (byte) (EquipmentSlot.LEGS.ordinal() | Byte.MIN_VALUE)); // slot
            wrapper.write(VersionedTypes.V26_3.item, itemRewriter.javaItem(legs)); // item
            wrapper.write(Types.BYTE, (byte) (EquipmentSlot.CHEST.ordinal() | Byte.MIN_VALUE)); // slot
            wrapper.write(VersionedTypes.V26_3.item, itemRewriter.javaItem(chest)); // item
            wrapper.write(Types.BYTE, (byte) (EquipmentSlot.HEAD.ordinal() | Byte.MIN_VALUE)); // slot
            wrapper.write(VersionedTypes.V26_3.item, itemRewriter.javaItem(head)); // item
            wrapper.write(Types.BYTE, (byte) EquipmentSlot.BODY.ordinal()); // slot
            wrapper.write(VersionedTypes.V26_3.item, itemRewriter.javaItem(body)); // item
        });
        protocol.registerClientbound(ClientboundBedrockPackets.MOB_EQUIPMENT, ClientboundPackets26_3.SET_EQUIPMENT, wrapper -> {
            final ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);
            final long entityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // entity runtime id
            final BedrockItem item = wrapper.read(itemRewriter.newItemType()); // item
            final byte slot = wrapper.read(Types.BYTE); // slot
            final byte selectedSlot = wrapper.read(Types.BYTE); // selected slot
            final byte containerId = wrapper.read(Types.BYTE); // container id

            final Entity entity = wrapper.user().get(EntityTracker.class).getEntityByRid(entityRuntimeId);
            if (entity == null || entity instanceof ClientPlayerEntity) {
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.VAR_INT, entity.javaId()); // entity id
            if (containerId == ContainerID.CONTAINER_ID_INVENTORY.getValue() && slot >= 0 && slot < 9 && (slot == selectedSlot || selectedSlot < 0)) {
                wrapper.write(Types.BYTE, (byte) EquipmentSlot.MAINHAND.ordinal()); // slot
                wrapper.write(VersionedTypes.V26_3.item, itemRewriter.javaItem(item)); // item
            } else if (containerId == ContainerID.CONTAINER_ID_OFFHAND.getValue()) {
                wrapper.write(Types.BYTE, (byte) EquipmentSlot.OFFHAND.ordinal()); // slot
                wrapper.write(VersionedTypes.V26_3.item, itemRewriter.javaItem(item)); // item
            } else {
                wrapper.cancel();
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.TAKE_ITEM_ENTITY, ClientboundPackets26_3.TAKE_ITEM_ENTITY, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);
            final long itemEntityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // item entity runtime id
            final long collectorEntityRuntimeId = wrapper.read(BedrockTypes.UNSIGNED_VAR_LONG); // collector entity runtime id

            final Entity itemEntity = entityTracker.getEntityByRid(itemEntityRuntimeId);
            final Entity collectorEntity = entityTracker.getEntityByRid(collectorEntityRuntimeId);
            if (itemEntity == null || collectorEntity == null || itemEntity.javaType() != EntityTypes26_3.ITEM) {
                wrapper.cancel();
                return;
            }
            wrapper.write(Types.VAR_INT, itemEntity.javaId()); // item entity id
            wrapper.write(Types.VAR_INT, collectorEntity.javaId()); // collector entity id
            wrapper.write(Types.VAR_INT, 0); // amount
        });
    }

    private EntityPackets() {
    }

}
