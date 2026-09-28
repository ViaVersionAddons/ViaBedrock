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

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.FloatTag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.util.Key;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.BlockState;
import net.raphimc.viabedrock.api.model.entity.Entity;
import net.raphimc.viabedrock.api.resourcepack.definition.SoundDefinitions;
import net.raphimc.viabedrock.api.resourcepack.definition.TextDefinitions;
import net.raphimc.viabedrock.api.util.EnumUtil;
import net.raphimc.viabedrock.api.util.MathUtil;
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.api.util.TextUtil;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ClientboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.BedrockMappingData;
import net.raphimc.viabedrock.protocol.data.enums.Dimension;
import net.raphimc.viabedrock.protocol.data.enums.Direction;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.LevelEvent;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.NoteBlockInstrument;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ParticleType;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.SharedTypes_Legacy_LevelSoundEvent;
import net.raphimc.viabedrock.protocol.data.enums.java.GameEventType;
import net.raphimc.viabedrock.protocol.data.enums.java.PositionSourceType;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.SoundSource;
import net.raphimc.viabedrock.protocol.data.generated.bedrock.CustomBlockTags;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.Position3f;
import net.raphimc.viabedrock.protocol.rewriter.BlockStateRewriter;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;
import net.raphimc.viabedrock.protocol.storage.BreakingTracker;
import net.raphimc.viabedrock.protocol.storage.ChunkTracker;
import net.raphimc.viabedrock.protocol.storage.EntityTracker;
import net.raphimc.viabedrock.protocol.storage.ResourcePackStorage;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class WorldEffectPackets {

    // Only log warnings about missing mappings if explicitly enabled for debugging
    // The Bedrock Dedicated Server sends a lot of unknown sound events which are expected to be ignored in most cases (Resource packs could add custom sounds for certain events)
    private static final boolean LEVEL_SOUND_DEBUG_LOG = false;

    public static void register(final BedrockProtocol protocol) {
        protocol.registerClientbound(ClientboundBedrockPackets.PLAY_SOUND, ClientboundPackets26_1.SOUND, wrapper -> {
            final String name = wrapper.read(BedrockTypes.STRING); // sound name
            final BlockPosition position = wrapper.read(BedrockTypes.BLOCK_POSITION); // position
            final float volume = wrapper.read(BedrockTypes.FLOAT_LE); // volume
            final float pitch = wrapper.read(BedrockTypes.FLOAT_LE); // pitch
            wrapper.read(BedrockTypes.OPTIONAL_UNSIGNED_LONG_LE); // server sound handle

            final BedrockMappingData.JavaSound javaSound = BedrockProtocol.MAPPINGS.getBedrockToJavaSounds().get(name);
            if (javaSound == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown bedrock sound: " + name);
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.SOUND_EVENT, Holder.of(javaSound.id())); // sound id
            wrapper.write(Types.VAR_INT, javaSound.category().ordinal()); // category
            wrapper.write(Types.INT, position.x()); // x
            wrapper.write(Types.INT, position.y()); // y
            wrapper.write(Types.INT, position.z()); // z
            wrapper.write(Types.FLOAT, volume); // volume
            wrapper.write(Types.FLOAT, pitch); // pitch
            wrapper.write(Types.LONG, ThreadLocalRandom.current().nextLong()); // seed
        });
        protocol.registerClientbound(ClientboundBedrockPackets.STOP_SOUND, ClientboundPackets26_1.STOP_SOUND, wrapper -> {
            final String name = wrapper.read(BedrockTypes.STRING); // sound name
            final boolean stopAll = wrapper.read(Types.BOOLEAN); // stop all
            wrapper.read(Types.BOOLEAN); // stop music | Ignored because it seems to do nothing

            if (stopAll) {
                wrapper.write(Types.BYTE, (byte) 0); // flags
            } else {
                final BedrockMappingData.JavaSound javaSound = BedrockProtocol.MAPPINGS.getBedrockToJavaSounds().get(name);
                if (javaSound == null) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown bedrock sound: " + name);
                    wrapper.cancel();
                    return;
                }

                wrapper.write(Types.BYTE, (byte) 2); // flags
                wrapper.write(Types.STRING, javaSound.identifier()); // sound identifier
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.SPAWN_PARTICLE_EFFECT, ClientboundPackets26_1.LEVEL_PARTICLES, wrapper -> {
            final Dimension dimension = Dimension.getByValue(wrapper.read(Types.BYTE)); // dimension
            if (dimension != wrapper.user().get(ChunkTracker.class).getDimension()) {
                wrapper.cancel();
                return;
            }
            wrapper.read(BedrockTypes.VAR_LONG); // entity unique id
            final Position3f position = wrapper.read(BedrockTypes.POSITION_3F); // position
            final String effectIdentifier = wrapper.read(BedrockTypes.STRING); // effect name
            if (wrapper.read(Types.BOOLEAN)) { // has molang variables
                wrapper.read(BedrockTypes.STRING); // molang variables json
            }

            final BedrockMappingData.JavaParticle javaParticle = BedrockProtocol.MAPPINGS.getBedrockToJavaParticles().get(effectIdentifier);
            if (javaParticle == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown bedrock particle: " + effectIdentifier);
                wrapper.cancel();
                return;
            }
            PacketFactory.writeJavaLevelParticles(wrapper, position, switch (effectIdentifier) {
                case "minecraft:eyeblossom_close", "minecraft:eyeblossom_open" -> {
                    final Particle particle = javaParticle.particle().copy();
                    particle.set(0, Types.DOUBLE, (double) position.x() + ThreadLocalRandom.current().nextFloat() - 0.5F); // target x
                    particle.set(1, Types.DOUBLE, (double) position.y() + ThreadLocalRandom.current().nextFloat() + 1.5F); // target y
                    particle.set(2, Types.DOUBLE, (double) position.z() + ThreadLocalRandom.current().nextFloat() - 0.5F); // target z
                    yield javaParticle.withParticle(particle);
                }
                default -> javaParticle;
            });
        });
        protocol.registerClientbound(ClientboundBedrockPackets.LEVEL_SOUND_EVENT, ClientboundPackets26_1.SOUND, wrapper -> {

            final String soundEvent = wrapper.read(BedrockTypes.STRING);
            if (soundEvent == null ||  soundEvent.isEmpty()) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown SoundEvent");
                wrapper.cancel();
                return;
            }
            final Position3f position = wrapper.read(BedrockTypes.POSITION_3F); // position
            final int data = wrapper.read(BedrockTypes.VAR_INT); // data
            final String entityIdentifier = wrapper.read(BedrockTypes.STRING); // entity identifier
            final boolean isBabyMob = wrapper.read(Types.BOOLEAN); // is baby mob
            final boolean isGlobal = wrapper.read(Types.BOOLEAN); // is global sound
            wrapper.read(BedrockTypes.LONG_LE); // entity unique id
            wrapper.read(BedrockTypes.OPTIONAL_POSITION_3F); // fire at position

            final boolean globalSound = isGlobal || Float.isNaN(position.x()) || Float.isNaN(position.y()) || Float.isNaN(position.z());
            SoundDefinitions.ConfiguredSound configuredSound;
            switch (soundEvent) {
                case "record.null" -> {
                    wrapper.setPacketType(ClientboundPackets26_1.STOP_SOUND);
                    wrapper.write(Types.BYTE, (byte) 1); // flags
                    wrapper.write(Types.VAR_INT, SoundSource.RECORDS.ordinal()); // category id
                    return;
                }
                case "note" -> {
                    final NoteBlockInstrument noteBlockInstrument = NoteBlockInstrument.getByValue(data >> 8, NoteBlockInstrument.Harp);
                    final String noteBlockSound = BedrockProtocol.MAPPINGS.getBedrockNoteBlockInstrumentSounds().get(noteBlockInstrument);
                    final int key = data & 0xFF;
                    final float pitch = (float) Math.pow(2D, (double) (key - 12) / 12);
                    configuredSound = new SoundDefinitions.ConfiguredSound(noteBlockSound, 1F, 1F, pitch, pitch);
                }
                default -> {
                    configuredSound = tryFindSound(wrapper.user(), soundEvent, data, entityIdentifier, isBabyMob);
                    if (configuredSound == null) { // Fallback for some special handled sounds
                        /*switch (soundEvent) {
                            case AmbientBaby, MobWarningBaby, HurtBaby, DeathBaby, StepBaby -> {
                                final SharedTypes_Legacy_LevelSoundEvent soundEventAdult = EnumUtil.getEnumConstantOrNull(SharedTypes_Legacy_LevelSoundEvent.class, soundEvent.name().replace("Baby", ""));
                                configuredSound = tryFindSound(wrapper.user(), soundEventAdult, data, entityIdentifier, true);
                            }
                            case AmbientInWater, AmbientInAir -> configuredSound = tryFindSound(wrapper.user(), SharedTypes_Legacy_LevelSoundEvent.Ambient, data, entityIdentifier, isBabyMob);
                        }*/
                    }
                    if (configuredSound == null) {
                        if (LEVEL_SOUND_DEBUG_LOG) {
                            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing level sound event mapping for " + soundEvent + " with entity identifier '" + entityIdentifier + "' and data " + data);
                        }
                        wrapper.cancel();
                        return;
                    }
                }
            }
            final BedrockMappingData.JavaSound javaSound = BedrockProtocol.MAPPINGS.getBedrockToJavaSounds().get(configuredSound.sound());
            if (javaSound == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown bedrock sound: " + configuredSound.sound());
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.SOUND_EVENT, Holder.of(javaSound.id())); // sound id
            wrapper.write(Types.VAR_INT, javaSound.category().ordinal()); // category
            wrapper.write(Types.INT, (int) (position.x() * 8F)); // x
            wrapper.write(Types.INT, (int) (position.y() * 8F)); // y
            wrapper.write(Types.INT, (int) (position.z() * 8F)); // z
            wrapper.write(Types.FLOAT, globalSound ? Integer.MAX_VALUE : MathUtil.randomFloatInclusive(configuredSound.minVolume(), configuredSound.maxVolume())); // volume
            wrapper.write(Types.FLOAT, MathUtil.randomFloatInclusive(configuredSound.minPitch(), configuredSound.maxPitch())); // pitch
            wrapper.write(Types.LONG, ThreadLocalRandom.current().nextLong()); // seed
        });
        protocol.registerClientbound(ClientboundBedrockPackets.LEVEL_EVENT, ClientboundPackets26_1.LEVEL_EVENT, wrapper -> {
            final int rawLevelEvent = wrapper.read(BedrockTypes.VAR_INT); // event id
            final Position3f position = wrapper.read(BedrockTypes.POSITION_3F); // position
            int data = wrapper.read(BedrockTypes.VAR_INT); // data

            if ((rawLevelEvent & LevelEvent.ParticleLegacyEvent.getValue()) != 0 || rawLevelEvent == LevelEvent.ParticleGenericSpawn.getValue()) {
                wrapper.setPacketType(ClientboundPackets26_1.LEVEL_PARTICLES);
                final int rawParticleType = rawLevelEvent == LevelEvent.ParticleGenericSpawn.getValue() ? data : rawLevelEvent & ~LevelEvent.ParticleLegacyEvent.getValue();
                final ParticleType particleType = ParticleType.getByValue(rawParticleType);
                if (particleType == null) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown particle type: " + rawParticleType);
                    wrapper.cancel();
                    return;
                }
                if (rawLevelEvent == LevelEvent.ParticleGenericSpawn.getValue()) {
                    data = 0;
                }
                final BedrockMappingData.JavaParticle javaParticle = BedrockProtocol.MAPPINGS.getBedrockToJavaLevelEventParticles().get(particleType);
                if (javaParticle != null) {
                    PacketFactory.writeJavaLevelParticles(wrapper, position, switch (particleType) {
                        case IconCrack, Food -> {
                            final BedrockItem bedrockItem = new BedrockItem(data >> 16, (short) (data & 0xFFFF), (byte) 1);
                            final Particle particle = new Particle(javaParticle.particle().id());
                            particle.add(VersionedTypes.V26_2.item, wrapper.user().get(ItemRewriter.class).javaItem(bedrockItem)); // item
                            yield javaParticle.withParticle(particle);
                        }
                        case Terrain, BrushDust -> {
                            final int javaBlockState = wrapper.user().get(BlockStateRewriter.class).javaId(data);
                            if (javaBlockState != -1) {
                                final Particle particle = new Particle(javaParticle.particle().id());
                                particle.add(Types.VAR_INT, javaBlockState); // block state
                                yield javaParticle.withParticle(particle);
                            } else {
                                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing block state: " + data);
                                wrapper.cancel();
                                yield javaParticle;
                            }
                        }
                        case FallingDust -> {
                            final Particle particle = new Particle(javaParticle.particle().id());
                            particle.add(Types.INT, (0xFF << 24) | data); // from color
                            particle.add(Types.INT, (0xFF << 24) | data); // to color
                            particle.add(Types.FLOAT, 1F); // scale
                            yield javaParticle.withParticle(particle);
                        }
                        case MobSpell, MobSpellAmbient, MobSpellInstantaneous -> {
                            final Particle particle = new Particle(javaParticle.particle().id());
                            particle.add(Types.INT, (0xFF << 24) | data); // color
                            yield javaParticle.withParticle(particle);
                        }
                        case EyeblossomOpen, EyeblossomClose -> {
                            final Particle particle = javaParticle.particle().copy();
                            particle.set(0, Types.DOUBLE, (double) position.x() + ThreadLocalRandom.current().nextFloat() - 0.5F); // target x
                            particle.set(1, Types.DOUBLE, (double) position.y() + ThreadLocalRandom.current().nextFloat() + 1.5F); // target y
                            particle.set(2, Types.DOUBLE, (double) position.z() + ThreadLocalRandom.current().nextFloat() - 0.5F); // target z
                            yield javaParticle.withParticle(particle);
                        }
                        default -> javaParticle;
                    });
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing level event particle mapping for " + particleType);
                    wrapper.cancel();
                }
                return;
            }

            final LevelEvent levelEvent = LevelEvent.getByValue(rawLevelEvent);
            if (levelEvent == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown LevelEvent: " + rawLevelEvent);
                wrapper.cancel();
                return;
            }
            switch (levelEvent) {
                case ParticleSoundGuardianGhost -> {
                    PacketFactory.sendJavaGameEvent(wrapper.user(), GameEventType.GUARDIAN_ELDER_EFFECT, 1F);
                    wrapper.cancel();
                }
                case StartRaining -> {
                    PacketFactory.sendJavaGameEvent(wrapper.user(), GameEventType.START_RAINING, 0F);
                    PacketFactory.sendJavaGameEvent(wrapper.user(), GameEventType.RAIN_LEVEL_CHANGE, data / 65535F);
                    wrapper.cancel();
                }
                case StopRaining -> {
                    PacketFactory.sendJavaGameEvent(wrapper.user(), GameEventType.STOP_RAINING, 0F);
                    PacketFactory.sendJavaGameEvent(wrapper.user(), GameEventType.RAIN_LEVEL_CHANGE, 0F);
                    wrapper.cancel();
                }
                case StartThunderstorm -> {
                    PacketFactory.sendJavaGameEvent(wrapper.user(), GameEventType.START_RAINING, 0F);
                    PacketFactory.sendJavaGameEvent(wrapper.user(), GameEventType.THUNDER_LEVEL_CHANGE, data / 65535F);
                    wrapper.cancel();
                }
                case StopThunderstorm -> {
                    PacketFactory.sendJavaGameEvent(wrapper.user(), GameEventType.THUNDER_LEVEL_CHANGE, 0F);
                    wrapper.cancel();
                }
                case GlobalPause -> {
                    if (data != 0) {
                        ViaBedrock.getPlatform().getLogger().log(Level.SEVERE, "Server paused the game. This is not supported by ViaBedrock.");
                    }
                    wrapper.cancel();
                }
                case SimTimeStep -> {
                    ViaBedrock.getPlatform().getLogger().log(Level.SEVERE, "Server tick stepped the game. This is not supported by ViaBedrock.");
                    wrapper.cancel();
                }
                case SimTimeScale -> {
                    ViaBedrock.getPlatform().getLogger().log(Level.SEVERE, "Server sped up the game. This is not supported by ViaBedrock.");
                    wrapper.cancel();
                }
                case StartBlockCracking, UpdateBlockCracking -> {
                    wrapper.cancel();
                    wrapper.user().get(BreakingTracker.class).updateCrackingInfo(new Position3f(
                            MathUtil.floor(position.x()), MathUtil.floor(position.y()), MathUtil.floor(position.z())
                    ), data, levelEvent == LevelEvent.UpdateBlockCracking);
                }
                case StopBlockCracking -> {
                    wrapper.cancel();
                    wrapper.user().get(BreakingTracker.class).stopCracking(new Position3f(MathUtil.floor(position.x()), MathUtil.floor(position.y()), MathUtil.floor(position.z())));
                }
                default -> {
                    BedrockMappingData.LevelEventMapping levelEventMapping = BedrockProtocol.MAPPINGS.getBedrockToJavaLevelEvents().get(levelEvent);
                    if (levelEventMapping instanceof BedrockMappingData.JavaSoundLevelEvent javaSoundLevelEvent) {
                        if (data == 0) {
                            levelEventMapping = javaSoundLevelEvent.levelEvent();
                        } else {
                            levelEventMapping = javaSoundLevelEvent.sound();
                        }
                    }
                    if (levelEventMapping instanceof BedrockMappingData.JavaLevelEvent javaLevelEvent) {
                        wrapper.write(Types.INT, javaLevelEvent.levelEvent().getValue()); // event id
                        wrapper.write(Types.BLOCK_POSITION1_14, new BlockPosition((int) position.x(), (int) position.y(), (int) position.z())); // position
                        wrapper.write(Types.INT, switch (levelEvent) {
                            case ParticlesShoot, ParticlesShootWhiteSmoke -> switch (data % 9) {
                                case 3, 0 -> Direction.WEST.ordinal();
                                case 4 -> Direction.UP.ordinal();
                                case 5, 8 -> Direction.EAST.ordinal();
                                case 7, 6 -> Direction.SOUTH.ordinal();
                                default /* 1, 2 */ -> Direction.NORTH.ordinal();
                            };
                            case ParticlesDestroyBlock, ParticlesDestroyBlockNoSound -> {
                                final int javaBlockState = wrapper.user().get(BlockStateRewriter.class).javaId(data);
                                if (javaBlockState != -1) {
                                    yield javaBlockState;
                                } else {
                                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing block state: " + data);
                                    wrapper.cancel();
                                    yield 0;
                                }
                            }
                            default -> javaLevelEvent.data() != null ? javaLevelEvent.data() : data;
                        }); // data
                        wrapper.write(Types.BOOLEAN, false); // global
                    } else if (levelEventMapping instanceof BedrockMappingData.JavaSound javaSound) {
                        wrapper.setPacketType(ClientboundPackets26_1.SOUND);
                        wrapper.write(Types.SOUND_EVENT, Holder.of(javaSound.id())); // sound id
                        wrapper.write(Types.VAR_INT, javaSound.category().ordinal()); // category
                        wrapper.write(Types.INT, (int) (position.x() * 8F)); // x
                        wrapper.write(Types.INT, (int) (position.y() * 8F)); // y
                        wrapper.write(Types.INT, (int) (position.z() * 8F)); // z
                        wrapper.write(Types.FLOAT, 1F); // volume
                        wrapper.write(Types.FLOAT, switch (levelEvent) {
                            case SoundClick, SoundClickFail, SoundOpenDoor, SoundFizz, SoundInfinityArrowPickup, SoundAmethystResonate -> {
                                yield data > 0 && data <= 256_000 ? (data / 1000F) : 1F;
                            }
                            default -> 1F;
                        }); // pitch
                        wrapper.write(Types.LONG, ThreadLocalRandom.current().nextLong()); // seed
                    } else if (levelEventMapping instanceof BedrockMappingData.JavaParticle javaParticle) {
                        wrapper.setPacketType(ClientboundPackets26_1.LEVEL_PARTICLES);
                        PacketFactory.writeJavaLevelParticles(wrapper, switch (levelEvent) {
                            case ParticlesCrackBlockDown -> new Position3f(MathUtil.floor(position.x()) + 0.5F, MathUtil.floor(position.y()), MathUtil.floor(position.z()) + 0.5F);
                            case ParticlesCrackBlockUp -> new Position3f(MathUtil.floor(position.x()) + 0.5F, MathUtil.floor(position.y()) + 1F, MathUtil.floor(position.z()) + 0.5F);
                            case ParticlesCrackBlockNorth -> new Position3f(MathUtil.floor(position.x()) + 0.5F, MathUtil.floor(position.y()) + 0.5F, MathUtil.floor(position.z()));
                            case ParticlesCrackBlockSouth -> new Position3f(MathUtil.floor(position.x()) + 0.5F, MathUtil.floor(position.y()) + 0.5F, MathUtil.floor(position.z()) + 1F);
                            case ParticlesCrackBlockWest -> new Position3f(MathUtil.floor(position.x()), MathUtil.floor(position.y()) + 0.5F, MathUtil.floor(position.z()) + 0.5F);
                            case ParticlesCrackBlockEast -> new Position3f(MathUtil.floor(position.x()) + 1F, MathUtil.floor(position.y()) + 0.5F, MathUtil.floor(position.z()) + 0.5F);
                            default -> position;
                        }, switch (levelEvent) {
                            case ParticlesCrit -> javaParticle.withCount(data);
                            case ParticlesCrackBlock, ParticlesCrackBlockDown, ParticlesCrackBlockUp, ParticlesCrackBlockNorth,
                                 ParticlesCrackBlockSouth, ParticlesCrackBlockWest, ParticlesCrackBlockEast -> {
                                final int javaBlockState = wrapper.user().get(BlockStateRewriter.class).javaId(data);
                                if (javaBlockState != -1) {
                                    final Particle particle = new Particle(javaParticle.particle().id());
                                    particle.add(Types.VAR_INT, javaBlockState); // block state
                                    yield javaParticle.withParticle(particle);
                                } else {
                                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing block state: " + data);
                                    wrapper.cancel();
                                    yield javaParticle;
                                }
                            }
                            default -> javaParticle;
                        });
                    } else if (levelEventMapping == null) {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing level event mapping for " + levelEvent);
                        wrapper.cancel();
                    } else {
                        throw new IllegalStateException("Unknown level event mapping type: " + levelEventMapping.getClass().getName());
                    }
                }
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.LEVEL_EVENT_GENERIC, ClientboundPackets26_1.LEVEL_PARTICLES, wrapper -> {
            final int rawLevelEvent = wrapper.read(BedrockTypes.VAR_INT); // event id
            final CompoundTag data = (CompoundTag) wrapper.read(BedrockTypes.COMPOUND_TAG_VALUE); // data

            final LevelEvent levelEvent = LevelEvent.getByValue(rawLevelEvent);
            if (levelEvent == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown LevelEvent: " + rawLevelEvent);
                wrapper.cancel();
                return;
            }
            switch (levelEvent) {
                case ParticlesTeleportTrail -> {
                    wrapper.cancel();
                    final Position3f startPosition = new Position3f(data.getFloat("Startx"), data.getFloat("Starty"), data.getFloat("Startz"));
                    final Position3f endPosition = new Position3f(data.getFloat("Endx"), data.getFloat("Endy"), data.getFloat("Endz"));
                    final float dirScale = data.getFloat("DirScale");
                    final float variationX = data.getFloat("Variationx");
                    final float variationY = data.getFloat("Variationy");
                    final int count = data.getInt("Count");
                    for (int i = 0; i < count; i++) {
                        final float progress = i / (count - 1F);
                        final Position3f position = new Position3f(
                                MathUtil.lerp(progress, startPosition.x(), endPosition.x()) + (ThreadLocalRandom.current().nextFloat() - 0.5F) * variationX * 2F,
                                MathUtil.lerp(progress, startPosition.y(), endPosition.y()) + (ThreadLocalRandom.current().nextFloat() - 0.5F) * variationY * 2F - 1F,
                                MathUtil.lerp(progress, startPosition.z(), endPosition.z()) + (ThreadLocalRandom.current().nextFloat() - 0.5F) * variationX * 2F
                        );
                        final BedrockMappingData.JavaParticle javaParticle = new BedrockMappingData.JavaParticle(
                                new Particle(BedrockProtocol.MAPPINGS.getJavaParticles().get("minecraft:portal")),
                                (ThreadLocalRandom.current().nextFloat() - 0.5F) * dirScale,
                                (ThreadLocalRandom.current().nextFloat() - 0.5F) * dirScale,
                                (ThreadLocalRandom.current().nextFloat() - 0.5F) * dirScale,
                                1F,
                                0
                        );
                        PacketFactory.sendJavaLevelParticles(wrapper.user(), position, javaParticle);
                    }
                }
                case ParticlesBlockExplosion -> {
                    wrapper.cancel();
                    final Position3f originPosition = new Position3f(data.getFloat("originX"), data.getFloat("originY"), data.getFloat("originZ"));
                    final float radius = data.getFloat("radius");
                    final int blockCount = data.getInt("size");
                    for (int i = 0; i < blockCount; i++) {
                        final Position3f position = new Position3f(data.getFloat("pos" + i + "x"), data.getFloat("pos" + i + "y"), data.getFloat("pos" + i + "z"));
                        final Particle particle = new Particle(BedrockProtocol.MAPPINGS.getJavaParticles().get("minecraft:smoke"));
                        PacketFactory.sendJavaLevelParticles(wrapper.user(), position, new BedrockMappingData.JavaParticle(particle, 0F, 0F, 0F, 0F, 0));
                    }
                }
                case ParticlesVibrationSignal -> {
                    if (data.get("origin") instanceof CompoundTag origin && data.get("target") instanceof CompoundTag target && data.get("timeToLive") instanceof FloatTag timeToLive) {
                        final Position3f originPosition = new Position3f(origin.getFloat("x"), origin.getFloat("y"), origin.getFloat("z"));
                        final Particle particle = new Particle(BedrockProtocol.MAPPINGS.getJavaParticles().get("minecraft:vibration"));
                        switch (target.getString("type", "")) {
                            case "vec3" -> {
                                final Position3f targetPosition = new Position3f(target.getFloat("x"), target.getFloat("y"), target.getFloat("z"));
                                particle.add(Types.VAR_INT, PositionSourceType.BLOCK.ordinal()); // destination type
                                particle.add(Types.BLOCK_POSITION1_14, new BlockPosition((int) targetPosition.x(), (int) targetPosition.y(), (int) targetPosition.z())); // destination block pos
                            }
                            case "actor" -> {
                                final Entity entity = wrapper.user().get(EntityTracker.class).getEntityByUid(target.getLong("uniqueID"));
                                if (entity == null) {
                                    wrapper.cancel();
                                    return;
                                }
                                particle.add(Types.VAR_INT, PositionSourceType.ENTITY.ordinal()); // destination type
                                particle.add(Types.VAR_INT, entity.javaId()); // destination entity
                                particle.add(Types.FLOAT, 0F); // y offset
                            }
                            default -> {
                                wrapper.cancel();
                                return;
                            }
                        }
                        particle.add(Types.VAR_INT, (int) (timeToLive.asFloat() * 20F)); // arrival in ticks
                        PacketFactory.writeJavaLevelParticles(wrapper, originPosition, new BedrockMappingData.JavaParticle(particle, 0F, 0F, 0F, 0F, 0));
                    } else {
                        wrapper.cancel();
                    }
                }
                case ParticlesSculkShriek -> {
                    wrapper.cancel();
                    final Position3f position = new Position3f(data.getInt("originX") + 0.5F, data.getInt("originY") + 0.5F, data.getInt("originZ") + 0.5F);
                    for (int i = 0; i < 15; i++) {
                        final Particle particle = new Particle(BedrockProtocol.MAPPINGS.getJavaParticles().get("minecraft:shriek"));
                        particle.add(Types.VAR_INT, i * 5); // delay
                        PacketFactory.sendJavaLevelParticles(wrapper.user(), position, new BedrockMappingData.JavaParticle(particle, 0F, 0F, 0F, 0F, 0));
                    }
                }
                case SculkCatalystBloom -> {
                    final Position3f position = new Position3f(data.getFloat("originX"), data.getFloat("originY"), data.getFloat("originZ"));
                    final Particle particle = new Particle(BedrockProtocol.MAPPINGS.getJavaParticles().get("minecraft:sculk_soul"));
                    PacketFactory.writeJavaLevelParticles(wrapper, new Position3f(position.x() + 0.5F, position.y() + 1.15F, position.z() + 0.5F), new BedrockMappingData.JavaParticle(particle, 0F, 0F, 0F, 0F, 0));
                    final PacketWrapper sound = PacketWrapper.create(ClientboundPackets26_1.SOUND, wrapper.user());
                    sound.write(Types.SOUND_EVENT, Holder.of((int) BedrockProtocol.MAPPINGS.getJavaSounds().get("minecraft:block.sculk_catalyst.bloom"))); // sound id
                    sound.write(Types.VAR_INT, SoundSource.BLOCKS.ordinal()); // category
                    sound.write(Types.INT, (int) (position.x() * 8F)); // x
                    sound.write(Types.INT, (int) (position.y() * 8F)); // y
                    sound.write(Types.INT, (int) (position.z() * 8F)); // z
                    sound.write(Types.FLOAT, 2F); // volume
                    sound.write(Types.FLOAT, 0.6F + ThreadLocalRandom.current().nextFloat() * 0.4F); // pitch
                    sound.write(Types.LONG, ThreadLocalRandom.current().nextLong()); // seed
                    sound.send(BedrockProtocol.class);
                }
                case SculkCharge -> {
                    wrapper.setPacketType(ClientboundPackets26_1.LEVEL_EVENT);
                    wrapper.write(Types.INT, net.raphimc.viabedrock.protocol.data.enums.java.LevelEvent.PARTICLES_SCULK_CHARGE.getValue()); // event id
                    wrapper.write(Types.BLOCK_POSITION1_14, new BlockPosition(data.getInt("x"), data.getInt("y"), data.getInt("z"))); // position
                    wrapper.write(Types.INT, (data.getShort("charge") << 6) | (data.getShort("facing") & 0x3F)); // data
                    wrapper.write(Types.BOOLEAN, false); // global
                }
                case SculkChargePop -> {
                    final Position3f position = new Position3f(data.getInt("x") + 0.5F, data.getInt("y") + 0.5F, data.getInt("z") + 0.5F);
                    final Particle particle = new Particle(BedrockProtocol.MAPPINGS.getJavaParticles().get("minecraft:sculk_charge_pop"));
                    PacketFactory.writeJavaLevelParticles(wrapper, position, new BedrockMappingData.JavaParticle(particle, 0F, 0F, 0F, 0.04F, 20));
                }
                case SonicExplosion -> {
                    final Position3f position = new Position3f(data.getFloat("x"), data.getFloat("y"), data.getFloat("z"));
                    final Particle particle = new Particle(BedrockProtocol.MAPPINGS.getJavaParticles().get("minecraft:sonic_boom"));
                    PacketFactory.writeJavaLevelParticles(wrapper, position, new BedrockMappingData.JavaParticle(particle, 0F, 0F, 0F, 0F, 0));
                }
                case DustPlume -> {
                    final Position3f position = new Position3f(data.getFloat("x"), data.getFloat("y"), data.getFloat("z"));
                    final Particle particle = new Particle(BedrockProtocol.MAPPINGS.getJavaParticles().get("minecraft:dust_plume"));
                    PacketFactory.writeJavaLevelParticles(wrapper, position, new BedrockMappingData.JavaParticle(particle, 0F, 0F, 0F, 0F, 7));
                }
                case SleepingPlayers -> {
                    wrapper.setPacketType(ClientboundPackets26_1.SYSTEM_CHAT);
                    final TextDefinitions textDefinitions = wrapper.user().get(ResourcePackStorage.class).getTexts();
                    if (data.getInt("ableToSleep") != 0) {
                        final int playerCount = data.getInt("overworldPlayerCount");
                        final int sleepingPlayerCount = data.getInt("sleepingPlayerCount");
                        if (sleepingPlayerCount < playerCount) {
                            wrapper.write(Types.TAG, TextUtil.stringToNbt(textDefinitions.translate("multiplayer.playersSleeping", sleepingPlayerCount, playerCount))); // message
                        } else {
                            wrapper.write(Types.TAG, TextUtil.stringToNbt(textDefinitions.get("multiplayer.playersSkippingNight"))); // message
                        }
                    } else {
                        wrapper.write(Types.TAG, TextUtil.stringToNbt(textDefinitions.get("multiplayer.playersSleepingNotPossible"))); // message
                    }
                    wrapper.write(Types.BOOLEAN, true); // overlay
                }
                case ParticleCreakingHeartTrail -> {
                    wrapper.cancel();
                    final Position3f creakingPosition = new Position3f(data.getFloat("CreakingX"), data.getFloat("CreakingY"), data.getFloat("CreakingZ"));
                    final Position3f heartPosition = new Position3f(data.getFloat("HeartX"), data.getFloat("HeartY"), data.getFloat("HeartZ"));
                    final int heartAmount = data.getInt("HeartAmount");
                    final int creakingAmount = data.getInt("CreakingAmount");

                    for (int i = 0; i < heartAmount; i++) {
                        final Particle particle = new Particle(BedrockProtocol.MAPPINGS.getJavaParticles().get("minecraft:trail"));
                        particle.add(Types.DOUBLE, (double) heartPosition.x() + ThreadLocalRandom.current().nextFloat()); // target x
                        particle.add(Types.DOUBLE, (double) heartPosition.y() + ThreadLocalRandom.current().nextFloat()); // target y
                        particle.add(Types.DOUBLE, (double) heartPosition.z() + ThreadLocalRandom.current().nextFloat()); // target z
                        particle.add(Types.INT, 6250335); // color
                        particle.add(Types.VAR_INT, ThreadLocalRandom.current().nextInt(40) + 10); // duration
                        PacketFactory.sendJavaLevelParticles(wrapper.user(), creakingPosition, new BedrockMappingData.JavaParticle(particle, 0.5F, 1F, 0.5F, 0F, 1));
                    }
                    for (int i = 0; i < creakingAmount; i++) {
                        final Particle particle = new Particle(BedrockProtocol.MAPPINGS.getJavaParticles().get("minecraft:trail"));
                        particle.add(Types.DOUBLE, (double) creakingPosition.x() + ThreadLocalRandom.current().nextFloat() * 2F); // target x
                        particle.add(Types.DOUBLE, (double) creakingPosition.y() + ThreadLocalRandom.current().nextFloat() * 2F); // target y
                        particle.add(Types.DOUBLE, (double) creakingPosition.z() + ThreadLocalRandom.current().nextFloat() * 2F); // target z
                        particle.add(Types.INT, 16545810); // color
                        particle.add(Types.VAR_INT, ThreadLocalRandom.current().nextInt(40) + 10); // duration
                        PacketFactory.sendJavaLevelParticles(wrapper.user(), heartPosition, new BedrockMappingData.JavaParticle(particle, 0.5F, 0.5F, 0.5F, 0F, 1));
                    }
                }
                default -> {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unhandled generic level event: " + levelEvent + " (" + data + ")");
                    wrapper.cancel();
                }
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.BLOCK_EVENT, ClientboundPackets26_1.BLOCK_EVENT, wrapper -> {
            final ChunkTracker chunkTracker = wrapper.user().get(ChunkTracker.class);
            final BlockStateRewriter blockStateRewriter = wrapper.user().get(BlockStateRewriter.class);
            final BlockPosition position = wrapper.passthroughAndMap(BedrockTypes.BLOCK_POSITION, Types.BLOCK_POSITION1_14); // position
            final int type = wrapper.read(BedrockTypes.VAR_INT); // event type
            final int data = wrapper.read(BedrockTypes.VAR_INT); // event data

            final int blockState = chunkTracker.getBlockState(position);
            final String tag = blockStateRewriter.tag(blockState);
            if (tag == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unhandled block event for " + blockState + ": " + type + " " + data);
                wrapper.cancel();
                return;
            }
            switch (tag) {
                case CustomBlockTags.CHEST, CustomBlockTags.TRAPPED_CHEST, CustomBlockTags.ENDER_CHEST, CustomBlockTags.SHULKER_BOX, CustomBlockTags.BARREL -> {
                    if (type == 1) { // open / close
                        wrapper.write(Types.UNSIGNED_BYTE, (short) type); // event type
                        wrapper.write(Types.UNSIGNED_BYTE, (short) MathUtil.clamp(data, 0, 255)); // event data
                    } else {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unhandled chest block event: " + type + " " + data);
                        wrapper.cancel();
                        return;
                    }
                }
                case CustomBlockTags.NOTE_BLOCK -> {
                    final NoteBlockInstrument noteBlockInstrument = NoteBlockInstrument.getByValue(type);
                    if (noteBlockInstrument != null) {
                        wrapper.write(Types.UNSIGNED_BYTE, (short) type); // event type
                        wrapper.write(Types.UNSIGNED_BYTE, (short) MathUtil.clamp(data, 0, 255)); // event data
                    } else {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unhandled note block block event: " + type + " " + data);
                        wrapper.cancel();
                        return;
                    }
                }
                case CustomBlockTags.MOB_SPAWNER -> {
                    if (type == 1) { // set spawn delay
                        wrapper.write(Types.UNSIGNED_BYTE, (short) type); // event type
                        wrapper.write(Types.UNSIGNED_BYTE, (short) MathUtil.clamp(data, 0, 255)); // event data
                    } else {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unhandled mob spawner block event: " + type + " " + data);
                        wrapper.cancel();
                        return;
                    }
                }
                case CustomBlockTags.END_GATEWAY -> {
                    if (type == 1) { // set cooldown
                        wrapper.write(Types.UNSIGNED_BYTE, (short) type); // event type
                        wrapper.write(Types.UNSIGNED_BYTE, (short) MathUtil.clamp(data, 0, 255)); // event data
                    } else {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unhandled end gateway block event: " + type + " " + data);
                        wrapper.cancel();
                        return;
                    }
                }
                default -> {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unhandled block event for " + blockState + ": " + type + " " + data);
                    wrapper.cancel();
                    return;
                }
            }
            wrapper.write(Types.VAR_INT, BedrockProtocol.MAPPINGS.getJavaBlocks().get(BedrockProtocol.MAPPINGS.getJavaBlockStates().inverse().get(blockStateRewriter.javaId(blockState)).namespacedIdentifier())); // block
        });
    }

    private static SoundDefinitions.ConfiguredSound tryFindSound(final UserConnection user, final String soundEvent, final int data, final String entityIdentifier, final boolean isBabyMob) {
        final Map<String, SoundDefinitions.ConfiguredSound> soundEvents = BedrockProtocol.MAPPINGS.getBedrockLevelSoundEvents().get(soundEvent);
        if (soundEvents == null) {
            return null;
        }
        SoundDefinitions.ConfiguredSound configuredSound = null;
        if (!entityIdentifier.isEmpty()) { // entity specific sound
            configuredSound = soundEvents.get(Key.namespaced(entityIdentifier));
            if (isBabyMob && configuredSound != null) {
                configuredSound = new SoundDefinitions.ConfiguredSound(configuredSound.sound(), configuredSound.minVolume(), configuredSound.maxVolume(), configuredSound.minPitch() + 0.5F, configuredSound.maxPitch() + 0.5F);
            }
        }
        if (configuredSound == null && data != -1) { // block specific sound
            final BlockState blockState = user.get(BlockStateRewriter.class).blockState(data);
            if (blockState != null) {
                final String blockSound = BedrockProtocol.MAPPINGS.getBedrockBlockSounds().get(blockState.namespacedIdentifier());
                if (blockSound != null) {
                    configuredSound = soundEvents.get(blockSound);
                } else {
                    if (LEVEL_SOUND_DEBUG_LOG) {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing " + soundEvent + " sound for " + blockState.namespacedIdentifier());
                    }
                    configuredSound = soundEvents.get("stone");
                }
            } else {
                if (LEVEL_SOUND_DEBUG_LOG) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing block state (" + soundEvent + " level sound event): " + data);
                }
                configuredSound = soundEvents.get("stone");
            }
        }
        if (configuredSound == null) {
            configuredSound = soundEvents.get(null); // generic sound
        }
        return configuredSound;
    }

}
