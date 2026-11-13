/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2023-2025 RK_01/RaphiMC and contributors
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
package net.raphimc.viabedrock.protocol.rewriter;

import com.google.common.collect.Iterables;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.EulerAngle;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes26_3;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityDataType;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.util.Key;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.entity.Entity;
import net.raphimc.viabedrock.api.model.entity.LivingEntity;
import net.raphimc.viabedrock.api.model.entity.properties.EntityProperty;
import net.raphimc.viabedrock.api.model.entity.properties.EntityPropertyList;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ActorDataIds;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ActorFlags;
import net.raphimc.viabedrock.protocol.data.generated.java.EntityDataFields;
import net.raphimc.viabedrock.protocol.data.generated.java.RegistryKeys;
import net.raphimc.viabedrock.protocol.model.EntityAttribute;
import net.raphimc.viabedrock.protocol.model.EntityProperties;
import net.raphimc.viabedrock.protocol.storage.EntityPropertyTracker;
import net.raphimc.viabedrock.protocol.storage.EntityTracker;
import net.raphimc.viabedrock.protocol.storage.GameSessionStorage;
import net.raphimc.viabedrock.protocol.types.entitydata.EntityDataTypesBedrock;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public final class EntityMetadataRewriter {

    public static boolean rewrite(final UserConnection user, final Entity entity, final ActorDataIds id, final EntityData entityData, final List<EntityData> javaEntityData) {
        final EntityTracker entityTracker = user.get(EntityTracker.class);

        switch (id) {
            case RESERVED_0, RESERVED_092 -> { // Entity flags mask
                final Set<ActorFlags> bedrockFlags = entity.entityFlags();
                byte javaBitMask = 0; // https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata#Entity
                if (bedrockFlags.contains(ActorFlags.ONFIRE)) {
                    javaBitMask |= (1 << 0);
                }
                if (bedrockFlags.contains(ActorFlags.SNEAKING)) {
                    javaBitMask |= (1 << 1);
                }
                if (bedrockFlags.contains(ActorFlags.RIDING)) {
                    javaBitMask |= (1 << 2);
                }
                if (bedrockFlags.contains(ActorFlags.SPRINTING)) {
                    javaBitMask |= (1 << 3);
                }
                if (bedrockFlags.contains(ActorFlags.SWIMMING)) {
                    javaBitMask |= (1 << 4);
                }
                if (bedrockFlags.contains(ActorFlags.INVISIBLE)) {
                    javaBitMask |= (1 << 5);
                }
                if (bedrockFlags.contains(ActorFlags.GLIDING)) {
                    javaBitMask |= (byte) (1 << 7);
                }

                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.SHARED_FLAGS), VersionedTypes.V26_3.entityDataTypes().byteType, javaBitMask));

                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.SILENT), VersionedTypes.V26_3.entityDataTypes().booleanType, bedrockFlags.contains(ActorFlags.SILENT)));
                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.NO_GRAVITY), VersionedTypes.V26_3.entityDataTypes().booleanType, !bedrockFlags.contains(ActorFlags.HAS_GRAVITY)));

                if (entity.javaType().is(EntityTypes26_3.END_CRYSTAL)) {
                    final boolean showBottom = bedrockFlags.contains(ActorFlags.SHOW_BOTTOM);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.SHOW_BOTTOM), VersionedTypes.V26_3.entityDataTypes().booleanType, showBottom));
                }

                if (entity.javaType().isOrHasParent(EntityTypes26_3.ABSTRACT_ARROW)) {
                    byte arrowBitMask = 0;
                    if (bedrockFlags.contains(ActorFlags.CRITICAL)) {
                        arrowBitMask |= 0x01;
                    }
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.FLAGS), VersionedTypes.V26_3.entityDataTypes().byteType, arrowBitMask));
                }

                if (entity.javaType().isOrHasParent(EntityTypes26_3.MOB)) {
                    byte mobBitMask = 0;
                    if (bedrockFlags.contains(ActorFlags.NOAI)) {
                        mobBitMask |= 0x01;
                    }

                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.MOB_FLAGS), VersionedTypes.V26_3.entityDataTypes().byteType, mobBitMask));
                }

                if (entity.javaType().is(EntityTypes26_3.GHAST)) {
                    final boolean attacking = bedrockFlags.contains(ActorFlags.CHARGED);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.IS_CHARGING), VersionedTypes.V26_3.entityDataTypes().booleanType, attacking));
                }

                if (entity.javaType().is(EntityTypes26_3.SNOW_GOLEM)) {
                    final byte hasPumpkin = (byte) (bedrockFlags.contains(ActorFlags.SHEARED) ? 0 : 0x10);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.FLAGS), VersionedTypes.V26_3.entityDataTypes().byteType, hasPumpkin));
                }

                if (entity.javaType().is(EntityTypes26_3.ALLAY)) {
                    final boolean dancing = bedrockFlags.contains(ActorFlags.DANCING);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.DANCING), VersionedTypes.V26_3.entityDataTypes().booleanType, dancing));
                }

                if (entity.javaType().isOrHasParent(EntityTypes26_3.ABSTRACT_AGEABLE)) {
                    final boolean isBaby = bedrockFlags.contains(ActorFlags.BABY);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.BABY), VersionedTypes.V26_3.entityDataTypes().booleanType, isBaby));
                }

                if (entity.javaType().is(EntityTypes26_3.AXOLOTL)) {
                    final boolean playingDead = bedrockFlags.contains(ActorFlags.PLAYING_DEAD);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.PLAYING_DEAD), VersionedTypes.V26_3.entityDataTypes().booleanType, playingDead));
                }

                if (entity.javaType().is(EntityTypes26_3.BEE)) {
                    byte beeBitMask = 0;
                    if (bedrockFlags.contains(ActorFlags.ANGRY)) {
                        beeBitMask |= 0x02;
                    }

                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.FLAGS), VersionedTypes.V26_3.entityDataTypes().byteType, beeBitMask));
                }

                if (entity.javaType().is(EntityTypes26_3.OCELOT)) {
                    final boolean isTrusting = bedrockFlags.contains(ActorFlags.TRUSTING);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.TRUSTING), VersionedTypes.V26_3.entityDataTypes().booleanType, isTrusting));
                }

                if (entity.javaType().is(EntityTypes26_3.SHEEP)) {
                    byte sheepBitMask = 0;
                    if (bedrockFlags.contains(ActorFlags.SHEARED)) {
                        sheepBitMask |= 0x10;
                    }
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.WOOL), VersionedTypes.V26_3.entityDataTypes().byteType, sheepBitMask));
                }

                if (entity.javaType().is(EntityTypes26_3.SNIFFER)) {
                    int sniffingState = 0;
                    if (bedrockFlags.contains(ActorFlags.IDLING)) {
                        sniffingState = 0;
                    } else if (bedrockFlags.contains(ActorFlags.DEPRECATED_3)) { // FEELING_HAPPY
                        sniffingState = 1;
                    } else if (bedrockFlags.contains(ActorFlags.DEPRECATED_1)) { // SCENTING
                        sniffingState = 2;
                    } else if (bedrockFlags.contains(ActorFlags.SNIFFING)) {
                        sniffingState = 3;
                    } else if (bedrockFlags.contains(ActorFlags.SEARCHING)) {
                        sniffingState = 4;
                    } else if (bedrockFlags.contains(ActorFlags.DIGGING)) {
                        sniffingState = 5;
                    } else if (bedrockFlags.contains(ActorFlags.DEPRECATED_2)) { // RISING
                        sniffingState = 6;
                    } else {
                        sniffingState = 0;
                        //TODO: Currently spams a bit but thats probably because we are missing states
                        //ViaBedrock.getPlatform().getLogger().warning("Unknown sniffer state, defaulting to IDLING.");
                    }

                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.STATE), VersionedTypes.V26_3.entityDataTypes().snifferState, sniffingState));
                }

                if (entity.javaType().is(EntityTypes26_3.STRIDER)) {
                    final boolean shaking = bedrockFlags.contains(ActorFlags.SHAKING);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.SUFFOCATING), VersionedTypes.V26_3.entityDataTypes().booleanType, shaking));
                }

                if (entity.javaType().is(EntityTypes26_3.TURTLE)) { //TODO: Test
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.HAS_EGG), VersionedTypes.V26_3.entityDataTypes().booleanType, bedrockFlags.contains(ActorFlags.IS_PREGNANT)));
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.LAYING_EGG), VersionedTypes.V26_3.entityDataTypes().booleanType, bedrockFlags.contains(ActorFlags.LAYING_EGG)));
                }

                if (entity.javaType().isOrHasParent(EntityTypes26_3.ABSTRACT_CHESTED_HORSE)) {
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.CHEST), VersionedTypes.V26_3.entityDataTypes().booleanType, bedrockFlags.contains(ActorFlags.CHESTED)));
                }

                if (entity.javaType().isOrHasParent(EntityTypes26_3.TAMABLE_ANIMAL)) {
                    byte tamableBitMask = 0;
                    if (bedrockFlags.contains(ActorFlags.SITTING)) {
                        tamableBitMask |= 0x01;
                    }
                    if (bedrockFlags.contains(ActorFlags.TAMED)) {
                        tamableBitMask |= 0x04;
                    }

                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.FLAGS), VersionedTypes.V26_3.entityDataTypes().byteType, tamableBitMask));
                }

                if (entity.javaType().is(EntityTypes26_3.CAT)) {
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.IS_LYING), VersionedTypes.V26_3.entityDataTypes().booleanType, bedrockFlags.contains(ActorFlags.RESTING)));
                }

                if (entity.javaType().is(EntityTypes26_3.BLAZE)) {
                    final byte onFire = (byte) (bedrockFlags.contains(ActorFlags.CHARGED) ? 0x01 : 0);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.FLAGS), VersionedTypes.V26_3.entityDataTypes().byteType, onFire));
                }

                if (entity.javaType().is(EntityTypes26_3.BOGGED)) {
                    final boolean isSheared = bedrockFlags.contains(ActorFlags.SHEARED);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.SHEARED), VersionedTypes.V26_3.entityDataTypes().booleanType, isSheared));
                }

                if (entity.javaType().is(EntityTypes26_3.CREEPER)) {
                    final boolean charged = bedrockFlags.contains(ActorFlags.CHARGED);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.IS_POWERED), VersionedTypes.V26_3.entityDataTypes().booleanType, charged));

                    final boolean ignited = bedrockFlags.contains(ActorFlags.IGNITED);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.IS_IGNITED), VersionedTypes.V26_3.entityDataTypes().booleanType, ignited));
                }

                if (entity.javaType().is(EntityTypes26_3.SPIDER)) {
                    final boolean climbing = bedrockFlags.contains(ActorFlags.WALLCLIMBING);
                    final byte mask = (byte) (climbing ? 1 : 0);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.FLAGS), VersionedTypes.V26_3.entityDataTypes().byteType, mask));
                }

                if (entity.javaType().is(EntityTypes26_3.ZOGLIN)) {
                    final boolean isBaby = bedrockFlags.contains(ActorFlags.BABY);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.BABY), VersionedTypes.V26_3.entityDataTypes().booleanType, isBaby));
                }

                if (entity.javaType().is(EntityTypes26_3.ZOMBIE)) {
                    final boolean isBaby = bedrockFlags.contains(ActorFlags.BABY);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.BABY), VersionedTypes.V26_3.entityDataTypes().booleanType, isBaby));
                }

                if (entity.javaType().is(EntityTypes26_3.PIGLIN)) {
                    final boolean isBaby = bedrockFlags.contains(ActorFlags.BABY);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.BABY), VersionedTypes.V26_3.entityDataTypes().booleanType, isBaby));

                    final boolean isDancing = bedrockFlags.contains(ActorFlags.DANCING);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.IS_DANCING), VersionedTypes.V26_3.entityDataTypes().booleanType, isDancing));
                }

                if (entity.javaType().isOrHasParent(EntityTypes26_3.ABSTRACT_RAIDER)) { //TODO: Test
                    final boolean isCelebrating = bedrockFlags.contains(ActorFlags.CELEBRATING);
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.IS_CELEBRATING), VersionedTypes.V26_3.entityDataTypes().booleanType, isCelebrating));
                }

            }
            case VARIANT -> {
                final int variant = readNumber(entityData).intValue();

                switch (entity.javaType()) {
                    case WOLF -> {
                        final int javaVariant = switch (variant) {
                            case 0 -> 4; // PALE
                            case 1 -> 7; // ASHEN
                            case 2 -> 6; // BLACK
                            case 3 -> 2; // CHESTNUT
                            case 4 -> 1; // RUSTY
                            case 5 -> 8; // SNOWY
                            case 6 -> 0; // SPOTTED
                            case 7 -> 3; // STRIPED
                            case 8 -> 5; // WOODS
                            default -> {
                                ViaBedrock.getPlatform().getLogger().warning("Unknown wolf variant " + variant + ", defaulting to PALE.");
                                yield 4;
                            }
                        };
                        javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.VARIANT), VersionedTypes.V26_3.entityDataTypes().wolfVariantType, javaVariant));
                    }
                    case HORSE -> {
                        javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.TYPE_VARIANT), VersionedTypes.V26_3.entityDataTypes().varIntType, variant));
                    }
                    case FROG -> {
                        final int javaVariant = switch (variant) {
                            case 0 -> 1; // TEMPERATE
                            case 1 -> 2; // COLD
                            case 2 -> 0; // WARM
                            default -> {
                                ViaBedrock.getPlatform().getLogger().warning("Unknown frog variant " + variant + ", defaulting to TEMPERATE.");
                                yield 1;
                            }
                        };
                        javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.VARIANT), VersionedTypes.V26_3.entityDataTypes().frogVariantType, javaVariant));
                    }
                    case TROPICAL_FISH -> {
                        //TODO: Remap tropical fish variants properly
                        //javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.TYPE_VARIANT), VersionedTypes.V26_3.entityDataTypes().varIntType, variant));
                    }
                    case PUFFERFISH -> {
                    } // For some reason bedrock sends the puffed state here as well as in the PUFFED_STATE Actor ID so we ignore this one
                    case SHULKER -> {
                        final byte color = (byte) variant;
                        javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.COLOR), VersionedTypes.V26_3.entityDataTypes().byteType, color));
                    }
                    case AXOLOTL -> {
                        final int javaVariant = switch (variant) {
                            case 0 -> 0; // LUCY
                            case 1 -> 3; // CYAN
                            case 2 -> 2; // GOLD
                            case 3 -> 1; // WILD
                            case 4 -> 4; // BLUE
                            default -> {
                                ViaBedrock.getPlatform().getLogger().warning("Unknown axolotl variant " + variant + ", defaulting to LUCY.");
                                yield 2;
                            }
                        };
                        javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.VARIANT), VersionedTypes.V26_3.entityDataTypes().varIntType, javaVariant));
                    }
                    case MOOSHROOM -> {
                        final int javaVariant = switch (variant) {
                            case 0 -> 0; // RED
                            case 1 -> 1; // BROWN
                            default -> {
                                ViaBedrock.getPlatform().getLogger().warning("Unknown mooshroom variant " + variant + ", defaulting to RED.");
                                yield 0;
                            }
                        };
                        javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.TYPE), VersionedTypes.V26_3.entityDataTypes().varIntType, javaVariant));
                    }
                    case SLIME, MAGMA_CUBE -> {
                        javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.SIZE), VersionedTypes.V26_3.entityDataTypes().varIntType, variant));
                    }
                    case RABBIT -> { // TODO: Test when I can
                        final int javaVariant = variant;
                        javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.TYPE), VersionedTypes.V26_3.entityDataTypes().varIntType, javaVariant));
                    }
                    case PARROT -> { // TODO: Test when I can
                        final int javaVariant = variant;
                        javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.VARIANT), VersionedTypes.V26_3.entityDataTypes().varIntType, javaVariant));
                    }
                    case CAT -> {
                        final String javaVariant = switch (variant) {
                            case 0 -> "minecraft:white";
                            case 1 -> "minecraft:black";
                            case 2 -> "minecraft:red";
                            case 3 -> "minecraft:siamese";
                            case 4 -> "minecraft:british_shorthair";
                            case 5 -> "minecraft:calico";
                            case 6 -> "minecraft:persian";
                            case 7 -> "minecraft:ragdoll";
                            case 8 -> "minecraft:tabby";
                            case 9 -> "minecraft:all_black";
                            case 10 -> "minecraft:jellie";
                            default -> {
                                ViaBedrock.getPlatform().getLogger().warning("Unknown cat variant " + variant + ", defaulting to BLACK.");
                                yield "minecraft:black";
                            }
                        };
                        final CompoundTag catVariants = user.get(GameSessionStorage.class).getJavaRegistries().getCompoundTag(RegistryKeys.CAT_VARIANT);
                        final int javaVariantId = Iterables.indexOf(catVariants.keySet(), javaVariant::equals);
                        if (javaVariantId == -1) {
                            ViaBedrock.getPlatform().getLogger().warning("Missing cat variant " + javaVariant + " in the Java registry.");
                            break;
                        }
                        javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.VARIANT), VersionedTypes.V26_3.entityDataTypes().catVariantType, javaVariantId));
                    }
                    default -> {
                        if (variant != 0) { // For some reason bedrock seems to send variant 0 for many entities that don't have variants
                            ViaBedrock.getPlatform().getLogger().warning("Received non-zero VARIANT " + variant + " for unsupported entity " + entity.type());
                        }
                    }
                }

            }
            case COLOR_INDEX -> {
                final int javaColorIndex = readNumber(entityData).intValue();

                switch (entity.javaType()) {
                    case WOLF, CAT -> {
                        javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.COLLAR_COLOR), VersionedTypes.V26_3.entityDataTypes().varIntType, javaColorIndex));
                    }
                    case SHEEP -> { // TODO: This seems to get overwritten by the entity flags sheared value, need to combine both
                        byte sheepBitMask = 0;
                        sheepBitMask |= (byte) (javaColorIndex & 0x0F); // Lower 4 bits for color
                        javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.WOOL), VersionedTypes.V26_3.entityDataTypes().byteType, sheepBitMask));
                    }
                    default -> {
                        if (javaColorIndex != 0) { // For some reason bedrock seems to send color index 0 for many entities that don't have colors
                            ViaBedrock.getPlatform().getLogger().warning("Received non-zero COLOR_INDEX " + javaColorIndex + " for unsupported entity " + entity.type());
                        }
                    }
                }
            }
            case OWNER -> {
                final long ownerId = readNumber(entityData).longValue();
                if (ownerId == -1) {
                    break; // No owner
                }
                final Entity ownerEntity = entityTracker.getEntityByUid(ownerId);
                if (ownerEntity == null) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to find owner entity with id " + ownerId + " for entity " + entity.type());
                    break;
                }
                if (entity.javaType().isOrHasParent(EntityTypes26_3.TAMABLE_ANIMAL)) {
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.OWNERUUID), VersionedTypes.V26_3.entityDataTypes().optionalUUIDType, ownerEntity.javaUuid()));
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received OWNER for non-TAMEABLE_ANIMAL entity " + entity.type());
                }
            }
            case FUSE_TIME -> {
                final int fuseTime = readNumber(entityData).intValue();
                if (entity.javaType().is(EntityTypes26_3.TNT)) {
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.FUSE), VersionedTypes.V26_3.entityDataTypes().varIntType, fuseTime));
                } else {
                    //ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received FUSE_TIME for non-TNT entity " + entity.type());
                }
            }
            case AIR_SUPPLY -> { // Air supply is stored as a short in Bedrock, but an int in Java (Bedrock also has a max air supply value we ignore for now)
                final int airSupply = readNumber(entityData).intValue();
                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.AIR_SUPPLY), VersionedTypes.V26_3.entityDataTypes().varIntType, airSupply));
            }
            case POSE_INDEX -> {
                if (!entity.javaType().is(EntityTypes26_3.ARMOR_STAND)) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received POSE_INDEX for non-ARMOR_STAND entity " + entity.type());
                    break;
                }

                byte javaBitMask = 0;
                javaBitMask |= 0x04; // Has arms

                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.CLIENT_FLAGS), VersionedTypes.V26_3.entityDataTypes().byteType, javaBitMask));

                final int poseIndex = readNumber(entityData).intValue();

                final EulerAngle headPose;
                final EulerAngle bodyPose;
                final EulerAngle leftArmPose;
                final EulerAngle rightArmPose;
                final EulerAngle leftLegPose;
                final EulerAngle rightLegPose;

                //Poses from https://github.com/lpsmods/armor-stand-poses/blob/1.21/datapack/datapack/data/poses/function/armor_stand/defaults.mcfunction
                switch (poseIndex) {
                    case 0 -> { // DEFAULT
                        headPose = new EulerAngle(0f, 0f, 0f);
                        bodyPose = new EulerAngle(0f, 0f, 0f);
                        leftArmPose = new EulerAngle(-10f, 0f, -10f);
                        rightArmPose = new EulerAngle(-15f, 0f, 10f);
                        leftLegPose = new EulerAngle(-1f, 0f, -1f);
                        rightLegPose = new EulerAngle(1f, 0f, 1f);
                    }
                    case 1 -> { // NONE
                        headPose = new EulerAngle(0f, 0f, 0f);
                        bodyPose = new EulerAngle(0f, 0f, 0f);
                        leftArmPose = new EulerAngle(0f, 0f, 0f);
                        rightArmPose = new EulerAngle(0f, 0f, 0f);
                        leftLegPose = new EulerAngle(0f, 0f, 0f);
                        rightLegPose = new EulerAngle(0f, 0f, 0f);
                    }
                    case 2 -> { // SOLEMN
                        headPose = new EulerAngle(15f, 0f, 0f);
                        bodyPose = new EulerAngle(0f, 0f, 2f);
                        leftArmPose = new EulerAngle(-30f, 15f, 15f);
                        rightArmPose = new EulerAngle(-60f, -20f, -10f);
                        leftLegPose = new EulerAngle(-1f, 0f, -1f);
                        rightLegPose = new EulerAngle(1f, 0f, 1f);
                    }
                    case 3 -> { // ATHENA
                        headPose = new EulerAngle(-5f, 0f, 0f);
                        bodyPose = new EulerAngle(0f, 0f, 2f);
                        leftArmPose = new EulerAngle(10f, 0f, -5f);
                        rightArmPose = new EulerAngle(-60f, 20f, -10f);
                        leftLegPose = new EulerAngle(-3f, -3f, -3f);
                        rightLegPose = new EulerAngle(3f, 3f, 3f);
                    }
                    case 4 -> { // BRANDISH
                        headPose = new EulerAngle(-15f, 0f, 0f);
                        bodyPose = new EulerAngle(0f, 0f, -2f);
                        leftArmPose = new EulerAngle(20f, 0f, -10f);
                        rightArmPose = new EulerAngle(-110f, 50f, 0f);
                        leftLegPose = new EulerAngle(5f, -3f, -3f);
                        rightLegPose = new EulerAngle(-5f, 3f, 3f);
                    }
                    case 5 -> { // HONOR
                        headPose = new EulerAngle(-15f, 0f, 0f);
                        bodyPose = new EulerAngle(0f, 0f, 0f);
                        leftArmPose = new EulerAngle(-110f, 35f, 0f);
                        rightArmPose = new EulerAngle(-110f, -35f, 0f);
                        leftLegPose = new EulerAngle(5f, -3f, -3f);
                        rightLegPose = new EulerAngle(-5f, 3f, 3f);
                    }
                    case 6 -> { // ENTERTAIN
                        headPose = new EulerAngle(-15f, 0f, 0f);
                        bodyPose = new EulerAngle(0f, 0f, 0f);
                        leftArmPose = new EulerAngle(-110f, -35f, 0f);
                        rightArmPose = new EulerAngle(-110f, 35f, 0f);
                        leftLegPose = new EulerAngle(5f, -3f, -3f);
                        rightLegPose = new EulerAngle(-5f, 3f, 3f);
                    }
                    case 7 -> { // SALUTE
                        headPose = new EulerAngle(0f, 0f, 0f);
                        bodyPose = new EulerAngle(0f, 0f, 0f);
                        leftArmPose = new EulerAngle(10f, 0f, -5f);
                        rightArmPose = new EulerAngle(-70f, -40f, 0f);
                        leftLegPose = new EulerAngle(-1f, 0f, -1f);
                        rightLegPose = new EulerAngle(1f, 0f, 1f);
                    }
                    case 8 -> { // RIPOSTE
                        headPose = new EulerAngle(16f, 20f, 0f);
                        bodyPose = new EulerAngle(0f, 0f, 0f);
                        leftArmPose = new EulerAngle(4f, 8f, 237f);
                        rightArmPose = new EulerAngle(246f, 0f, 89f);
                        leftLegPose = new EulerAngle(-14f, -18f, -16f);
                        rightLegPose = new EulerAngle(8f, 20f, 4f);
                    }
                    case 9 -> { // ZOMBIE
                        headPose = new EulerAngle(-10f, 0f, -5f);
                        bodyPose = new EulerAngle(0f, 0f, 0f);
                        leftArmPose = new EulerAngle(-105f, 0f, 0f);
                        rightArmPose = new EulerAngle(-100f, 0f, 0f);
                        leftLegPose = new EulerAngle(7f, 0f, 0f);
                        rightLegPose = new EulerAngle(-46f, 0f, 0f);
                    }
                    case 10 -> { // CAN_CAN_A
                        headPose = new EulerAngle(-5f, 18f, 0f);
                        bodyPose = new EulerAngle(0f, 22f, 0f);
                        leftArmPose = new EulerAngle(8f, 0f, -114f);
                        rightArmPose = new EulerAngle(0f, 84f, 111f);
                        leftLegPose = new EulerAngle(-111f, 55f, 0f);
                        rightLegPose = new EulerAngle(0f, 23f, -13f);
                    }
                    case 11 -> { // CAN_CAN_B
                        headPose = new EulerAngle(-10f, -20f, 0f);
                        bodyPose = new EulerAngle(0f, -18f, 0f);
                        leftArmPose = new EulerAngle(0f, 0f, -112f);
                        rightArmPose = new EulerAngle(8f, 90f, 111f);
                        leftLegPose = new EulerAngle(0f, 0f, 13f);
                        rightLegPose = new EulerAngle(-119f, -42f, 0f);
                    }
                    case 12 -> { // HERO
                        headPose = new EulerAngle(-4f, 67f, 0f);
                        bodyPose = new EulerAngle(0f, 8f, 0f);
                        leftArmPose = new EulerAngle(16f, 32f, -8f);
                        rightArmPose = new EulerAngle(-99f, 63f, 0f);
                        leftLegPose = new EulerAngle(0f, -75f, -8f);
                        rightLegPose = new EulerAngle(4f, 63f, 8f);
                    }
                    default -> {
                        // Fallback to none
                        headPose = new EulerAngle(0f, 0f, 0f);
                        bodyPose = new EulerAngle(0f, 0f, 0f);
                        leftArmPose = new EulerAngle(0f, 0f, 0f);
                        rightArmPose = new EulerAngle(0f, 0f, 0f);
                        leftLegPose = new EulerAngle(0f, 0f, 0f);
                        rightLegPose = new EulerAngle(0f, 0f, 0f);
                        ViaBedrock.getPlatform().getLogger().warning("Unknown armor stand pose index " + poseIndex + ", defaulting to NONE.");
                    }
                }

                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.HEAD_POSE), VersionedTypes.V26_3.entityDataTypes().rotationsType, headPose));
                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.BODY_POSE), VersionedTypes.V26_3.entityDataTypes().rotationsType, bodyPose));
                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.LEFT_ARM_POSE), VersionedTypes.V26_3.entityDataTypes().rotationsType, leftArmPose));
                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.RIGHT_ARM_POSE), VersionedTypes.V26_3.entityDataTypes().rotationsType, rightArmPose));
                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.LEFT_LEG_POSE), VersionedTypes.V26_3.entityDataTypes().rotationsType, leftLegPose));
                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.RIGHT_LEG_POSE), VersionedTypes.V26_3.entityDataTypes().rotationsType, rightLegPose));
            }

            case PUFFED_STATE -> {
                final int javaPuffedState = readNumber(entityData).intValue();
                if (entity.javaType().is(EntityTypes26_3.PUFFERFISH)) {
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.PUFF_STATE), VersionedTypes.V26_3.entityDataTypes().varIntType, javaPuffedState));
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received PUFFED_STATE for non-PUFFERFISH entity " + entity.type());
                }
            }
            case FREEZING_EFFECT_STRENGTH -> {
                final float freezingStrength = readNumber(entityData).floatValue();

                // Java freezing strength is from 0-140 whereas Bedrock is from 0.0-1.0
                final int javaStrength = Math.round(freezingStrength * 140f);
                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.TICKS_FROZEN), VersionedTypes.V26_3.entityDataTypes().varIntType, javaStrength));
            }
            case GOAT_HORN_COUNT -> {
                if (entity.javaType().is(EntityTypes26_3.GOAT)) {
                    // In bedrock the goat always loses its right horn first, whereas in java its random
                    final int hornCount = readNumber(entityData).intValue();
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.HAS_LEFT_HORN), VersionedTypes.V26_3.entityDataTypes().booleanType, hornCount != 0));
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.HAS_RIGHT_HORN), VersionedTypes.V26_3.entityDataTypes().booleanType, hornCount == 2));
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received GOAT_HORN_COUNT for non-GOAT entity " + entity.type());
                }
            }
            case EATING_COUNTER -> {
                final int eatingCounter = readNumber(entityData).intValue();
                if (entity.javaType().is(EntityTypes26_3.PANDA)) {
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.EAT_COUNTER), VersionedTypes.V26_3.entityDataTypes().varIntType, eatingCounter));
                } else if (eatingCounter != 0) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received EATING_COUNTER for non-PANDA entity " + entity.type() + " with non-zero value " + eatingCounter);
                }
            }
            case ATTACH_FACE -> {
                if (entity.javaType().is(EntityTypes26_3.SHULKER)) {
                    final int javaAttachFace = readNumber(entityData).intValue();
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.ATTACH_FACE), VersionedTypes.V26_3.entityDataTypes().directionType, javaAttachFace));
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received ATTACH_FACE for non-SHULKER entity " + entity.type());
                }
            }
            case PEEK_ID -> {
                if (entity.javaType().is(EntityTypes26_3.SHULKER)) {
                    final byte peek = readNumber(entityData).byteValue();
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.PEEK), VersionedTypes.V26_3.entityDataTypes().byteType, peek));
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received PEEK_ID for non-SHULKER entity " + entity.type());
                }
            }
            case ATTACHED, ATTACH_POS -> { // Not needed in java
                if (!entity.javaType().is(EntityTypes26_3.SHULKER)) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received ATTACH for non-SHULKER entity " + entity.type());
                }
            }
            case DATA_RADIUS -> {
                if (entity.javaType().isOrHasParent(EntityTypes26_3.AREA_EFFECT_CLOUD)) {
                    final float radius = readNumber(entityData).floatValue();
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.RADIUS), VersionedTypes.V26_3.entityDataTypes().floatType, radius));
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received DATA_RADIUS for non-AREA_EFFECT_CLOUD entity " + entity.type());
                }
            }
            case DATA_WAITING -> {
                if (entity.javaType().is(EntityTypes26_3.AREA_EFFECT_CLOUD)) {
                    final int isWaiting = readNumber(entityData).intValue();
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.WAITING), VersionedTypes.V26_3.entityDataTypes().booleanType, isWaiting != 0));
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received DATA_WAITING for non-AREA_EFFECT_CLOUD entity " + entity.type());
                }
            }
            case DATA_PARTICLE -> {
                if (entity.javaType().is(EntityTypes26_3.AREA_EFFECT_CLOUD)) {
                    // final int particleIdOrColour = readNumber(entityData).intValue(); //TODO: not sure what this is exactly
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received DATA_PARTICLE for non-AREA_EFFECT_CLOUD entity " + entity.type());
                }
            }
            case VALUE -> { // TODO: Test
                if (entity.javaType().is(EntityTypes26_3.EXPERIENCE_ORB)) {
                    final int exp = readNumber(entityData).intValue();
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.VALUE), VersionedTypes.V26_3.entityDataTypes().varIntType, exp));
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received VALUE for non-EXPERIENCE_ORB entity " + entity.type());
                }
            }
            case AUX_POWER -> {
                if (entity.javaType().is(EntityTypes26_3.ARROW)) {
                    // Potion aux value used for an Arrow's trail. (Equal to the potion ID - 1)
                    //int potionId = readNumber(entityData).byteValue() + 1;

                    // TODO: Find potion by id and get color
                    //javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.EFFECT_COLOR), VersionedTypes.V26_3.entityDataTypes().varIntType, to_do));
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received AUX_POWER for non-ARROW entity " + entity.type());
                }
            }
            case INV -> {
                if (entity.javaType().is(EntityTypes26_3.WITHER)) {
                    final int invulnerabilityTicks = readNumber(entityData).intValue();
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.INV), VersionedTypes.V26_3.entityDataTypes().varIntType, invulnerabilityTicks));
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received INV for non-WITHER entity " + entity.type());
                }
            }
            case TARGET_A -> {
                if (entity.javaType().is(EntityTypes26_3.WITHER)) {
                    final long targetFirstId = readNumber(entityData).longValue();
                    if (targetFirstId == -1) {
                        break; // No target
                    }
                    final Entity targetFirstEntity = entityTracker.getEntityByUid(targetFirstId);
                    if (targetFirstEntity == null) {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to find TARGET_A entity with id " + targetFirstId + " for entity " + entity.type());
                        break;
                    }
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.TARGET_A), VersionedTypes.V26_3.entityDataTypes().varIntType, targetFirstEntity.javaId()));
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received TARGET_A for non-WITHER entity " + entity.type());
                }
            }
            case TARGET_B -> {
                if (entity.javaType().is(EntityTypes26_3.WITHER)) {
                    final long targetSecondId = readNumber(entityData).longValue();
                    if (targetSecondId == -1) {
                        break; // No target
                    }
                    final Entity targetSecondEntity = entityTracker.getEntityByUid(targetSecondId);
                    if (targetSecondEntity == null) {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to find TARGET_B entity with id " + targetSecondId + " for entity " + entity.type());
                        break;
                    }
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.TARGET_B), VersionedTypes.V26_3.entityDataTypes().varIntType, targetSecondEntity.javaId()));
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received TARGET_B for non-WITHER entity " + entity.type());
                }
            }
            case TARGET_C -> {
                if (entity.javaType().is(EntityTypes26_3.WITHER)) {
                    final long targetThirdId = readNumber(entityData).longValue();
                    if (targetThirdId == -1) {
                        break; // No target
                    }
                    final Entity targetThirdEntity = entityTracker.getEntityByUid(targetThirdId);
                    if (targetThirdEntity == null) {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to find TARGET_C entity with id " + targetThirdId + " for entity " + entity.type());
                        break;
                    }
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.TARGET_C), VersionedTypes.V26_3.entityDataTypes().varIntType, targetThirdEntity.javaId()));
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received TARGET_C for non-WITHER entity " + entity.type());
                }
            }
            case TARGET -> {
                final long targetId = readNumber(entityData).longValue();
                if (entity.javaType().is(EntityTypes26_3.GUARDIAN)) {
                    if (targetId == 0) {
                        break; // No target
                    }
                    final Entity targetEntity = entityTracker.getEntityByUid(targetId);
                    if (targetEntity == null) {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to find TARGET entity with id " + targetId + " for entity " + entity.type());
                        break;
                    }
                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.ATTACK_TARGET), VersionedTypes.V26_3.entityDataTypes().varIntType, targetEntity.javaId()));
                } else if (targetId != 0) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received TARGET for non-GUARDIAN entity " + entity.type() + " with non-zero value " + targetId);
                }
            }
            case BLOCK_TARGET -> {
                if (entity.javaType().is(EntityTypes26_3.END_CRYSTAL)) {
                    final BlockPosition blockPosition = (BlockPosition) entityData.getValue(); // TODO: Test

                    javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.BEAM_TARGET), VersionedTypes.V26_3.entityDataTypes().optionalBlockPositionType, blockPosition));
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received BLOCK_TARGET for non-END_CRYSTAL entity " + entity.type());
                }
            }
            case RESERVED_038 -> { // SCALE
                final float scale = readNumber(entityData).floatValue();

                // TODO: Armor stands might have special handling

                if (entity.javaType().isOrHasParent(EntityTypes26_3.LIVING_ENTITY) && scale != 0f) {
                    final LivingEntity livingEntity = (LivingEntity) entity;

                    livingEntity.updateAttributes(new EntityAttribute[]{new EntityAttribute("minecraft:scale", scale, (float) 1 / 16, 16)});
                }
            }
            case RESERVED_053 -> { } // BOUNDING_BOX_WIDTH
            case RESERVED_054 -> { } // BOUNDING_BOX_HEIGHT
            case AGENT, BALLOON_ANCHOR -> {
            } // Education edition only, ignore
            default -> {
                return false;
            }
        }

        return true;
    }

    public static void rewriteProperties(final UserConnection user, final Entity entity, final EntityProperties properties, final List<EntityData> javaEntityData) {
        final EntityPropertyTracker epTracker = user.get(EntityPropertyTracker.class);
        final CompoundTag registries = BedrockProtocol.MAPPINGS.getJavaRegistries();
        final EntityPropertyList expectedProperties = epTracker.getEntityProperties(entity.type());
        if (expectedProperties == null) {
            //ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to find expected properties for entity " + entity.type());
            return;
        }

        final Iterator<EntityProperty> intIterator = expectedProperties.intProperties().iterator();

        for (int i : properties.intProperties().values()) {
            final EntityProperty property = intIterator.next();

            if (property instanceof EntityProperty.BooleanProperty booleanProperty) {
                final boolean value = i != 0;

                switch (booleanProperty.identifier()) {
                    case "minecraft:can_move" -> {
                        if (entity.javaType().is(EntityTypes26_3.HAPPY_GHAST)) {
                            javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.STAYS_STILL), VersionedTypes.V26_3.entityDataTypes().booleanType, !value));
                        } else {
                            ViaBedrock.getPlatform().getLogger().warning("Received can_move property for non-HAPPY_GHAST entity " + entity.type());
                        }
                    }
                    case "minecraft:has_nectar" -> {
                        if (entity.javaType().is(EntityTypes26_3.BEE)) {
                            final byte bitmask = value ? (byte) 0x08 : 0; // TODO

                            javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.FLAGS), VersionedTypes.V26_3.entityDataTypes().byteType, bitmask));
                        } else {
                            ViaBedrock.getPlatform().getLogger().warning("Received has_nectar property for non-BEE entity " + entity.type());
                        }
                    }
                    case "minecraft:has_flower" -> {
                        if (entity.javaType().is(EntityTypes26_3.COPPER_GOLEM)) {
                            // TODO
                        } else {
                            ViaBedrock.getPlatform().getLogger().warning("Received has_flower property for non-COPPER_GOLEM entity " + entity.type());
                        }
                    }
                    default -> ViaBedrock.getPlatform().getLogger().warning("Received boolean property " + booleanProperty.identifier() + " for entity " + entity.type() + " with value " + value);
                }
            } else if (property instanceof EntityProperty.IntProperty integerProperty) {
                final int value = i;

                ViaBedrock.getPlatform().getLogger().warning("Received int property " + integerProperty.identifier() + " for entity " + entity.type() + " with value " + value);
            } else if (property instanceof EntityProperty.EnumProperty enumProperty) {
                final String value = enumProperty.possibleValues()[i];

                switch (enumProperty.identifier()) {
                    case "minecraft:climate_variant" -> {
                        final String namespacedVariant = Key.namespaced(value);

                        final String registryName = switch (entity.javaType()) {
                            case CHICKEN -> "minecraft:chicken_variant";
                            case COW -> "minecraft:cow_variant";
                            case PIG -> "minecraft:pig_variant";
                            default -> {
                                ViaBedrock.getPlatform().getLogger().warning("Received climate_variant for unsupported entity " + entity.type());
                                yield null;
                            }
                        };
                        final EntityDataType variantType = switch (entity.javaType()) {
                            case CHICKEN -> VersionedTypes.V26_3.entityDataTypes().chickenVariantType;
                            case COW -> VersionedTypes.V26_3.entityDataTypes().cowVariantType;
                            case PIG -> VersionedTypes.V26_3.entityDataTypes().pigVariantType;
                            default -> null;
                        };

                        if (registryName != null && variantType != null) {
                            final CompoundTag variantRegistry = registries.getCompoundTag(registryName);
                            // TODO: Could be cleaned up
                            int javaVariant = -1;
                            for (String key : variantRegistry.keySet()) {
                                javaVariant++;
                                if (key.equals(namespacedVariant)) {
                                    break;
                                }
                            }

                            if (javaVariant != -1) {
                                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.VARIANT), variantType, javaVariant));
                            } else {
                                ViaBedrock.getPlatform().getLogger().warning("Failed to find variant " + namespacedVariant + " in registry " + registryName + " for entity " + entity.type());
                            }
                        }
                    }
                    case "minecraft:sound_variant" -> {
                        final String namespacedVariant = Key.namespaced(value);

                        final String registryName = switch (entity.javaType()) {
                            case CHICKEN -> "minecraft:chicken_sound_variant";
                            case COW -> "minecraft:cow_sound_variant";
                            case PIG -> "minecraft:pig_sound_variant";
                            case WOLF -> "minecraft:wolf_sound_variant";
                            case CAT -> "minecraft:cat_sound_variant";
                            default -> {
                                ViaBedrock.getPlatform().getLogger().warning("Received sound_variant for unsupported entity " + entity.type());
                                yield null;
                            }
                        };
                        final EntityDataType variantType = switch (entity.javaType()) {
                            case CHICKEN -> VersionedTypes.V26_3.entityDataTypes().chickenSoundVariant;
                            case COW -> VersionedTypes.V26_3.entityDataTypes().cowSoundVariant;
                            case PIG -> VersionedTypes.V26_3.entityDataTypes().pigSoundVariant;
                            case WOLF -> VersionedTypes.V26_3.entityDataTypes().wolfSoundVariantType;
                            case CAT -> VersionedTypes.V26_3.entityDataTypes().catSoundVariant;
                            default -> null;
                        };

                        if (registryName != null && variantType != null) {
                            final CompoundTag variantRegistry = registries.getCompoundTag(registryName);
                            // TODO: Could be cleaned up
                            int javaVariant = -1;
                            for (String key : variantRegistry.keySet()) {
                                javaVariant++;
                                if (key.equals(namespacedVariant)) {
                                    break;
                                }
                            }

                            if (javaVariant != -1) {
                                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.SOUND_VARIANT), variantType, javaVariant));
                            } else {
                                ViaBedrock.getPlatform().getLogger().warning("Failed to find sound variant " + namespacedVariant + " in registry " + registryName + " for entity " + entity.type());
                            }
                        }
                    }
                    case "minecraft:chest_interaction" -> {
                        if (entity.javaType().is(EntityTypes26_3.COPPER_GOLEM)) {
                            final int state = switch (value) {
                                case "none" -> 0; // IDLE
                                case "take" -> 1; // GETTING_ITEM
                                case "take_fail" -> 2; // GETTING_NO_ITEM
                                case "put" -> 3; // DROPPING_ITEM
                                case "put_fail" -> 4; // DROPPING_NO_ITEM
                                default -> -1;
                            };

                            if (state != -1) {
                                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.COPPER_GOLEM_STATE), VersionedTypes.V26_3.entityDataTypes().copperGolemState, state));
                            } else {
                                ViaBedrock.getPlatform().getLogger().warning("Received unknown chest_interaction state " + value + " for COPPER_GOLEM entity " + entity.type());
                            }
                        } else {
                            ViaBedrock.getPlatform().getLogger().warning("Received chest_interaction for non-COPPER_GOLEM entity " + entity.type());
                        }
                    }
                    case "minecraft:oxidation_level" -> {
                        if (entity.javaType().is(EntityTypes26_3.COPPER_GOLEM)) {
                            final int state = switch (value) {
                                case "unoxidized" -> 0; // UNAFFECTED
                                case "exposed" -> 1; // EXPOSED
                                case "weathered" -> 2; // WEATHERED
                                case "oxidized" -> 3; // OXIDIZED
                                default -> -1;
                            };

                            if (state != -1) {
                                javaEntityData.add(new EntityData(entity.getJavaEntityDataIndex(EntityDataFields.WEATHER_STATE), VersionedTypes.V26_3.entityDataTypes().weatheringCopperState, state));
                            } else {
                                ViaBedrock.getPlatform().getLogger().warning("Received unknown oxidation_level state " + value + " for COPPER_GOLEM entity " + entity.type());
                            }
                        } else {
                            ViaBedrock.getPlatform().getLogger().warning("Received oxidation_level for non-COPPER_GOLEM entity " + entity.type());
                        }
                    }
                    default -> ViaBedrock.getPlatform().getLogger().warning("Received enum property " + enumProperty.identifier() + " for entity " + entity.type() + " with value " + value);
                }
            }
        }

        final Iterator<EntityProperty.FloatProperty> floatIterator = expectedProperties.floatProperties().iterator();

        for (float value : properties.floatProperties().values()) {
            final EntityProperty.FloatProperty property = floatIterator.next();

            ViaBedrock.getPlatform().getLogger().warning("Received float property " + property.identifier() + " for entity " + entity.type() + " with value " + value);
        }
    }

    private static Number readNumber(final EntityData data) {
        if (data.dataType() == null || data.getValue() == null) {
            throw new IllegalArgumentException("EntityData " + data.id() + " has null data type or value");
        }
        return switch ((EntityDataTypesBedrock) data.dataType()) {
            case BYTE -> (byte) data.getValue();
            case SHORT -> (short) data.getValue();
            case INT -> (int) data.getValue();
            case FLOAT -> (float) data.getValue();
            case LONG -> (long) data.getValue();
            default -> throw new IllegalArgumentException("Unsupported number type: " + data.dataType());
        };
    }

    private EntityMetadataRewriter() {
    }

}
