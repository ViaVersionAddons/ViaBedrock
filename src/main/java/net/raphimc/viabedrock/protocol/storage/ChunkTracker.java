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

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viaversion.api.connection.StoredObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.ChunkPosition;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntityImpl;
import com.viaversion.viaversion.api.minecraft.chunks.*;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType26_1;
import com.viaversion.viaversion.libs.fastutil.ints.IntObjectImmutablePair;
import com.viaversion.viaversion.libs.fastutil.ints.IntObjectPair;
import com.viaversion.viaversion.libs.fastutil.ints.IntSet;
import com.viaversion.viaversion.libs.fastutil.longs.Long2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.longs.Long2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.fastutil.longs.LongOpenHashSet;
import com.viaversion.viaversion.libs.fastutil.longs.LongSet;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.util.CompactArrayUtil;
import com.viaversion.viaversion.util.MathUtil;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.chunk.BedrockBlockEntity;
import net.raphimc.viabedrock.api.chunk.BedrockChunk;
import net.raphimc.viabedrock.api.chunk.BlockEntityWithBlockState;
import net.raphimc.viabedrock.api.chunk.datapalette.BedrockBlockArray;
import net.raphimc.viabedrock.api.chunk.datapalette.BedrockDataPalette;
import net.raphimc.viabedrock.api.chunk.section.BedrockChunkSection;
import net.raphimc.viabedrock.api.chunk.section.BedrockChunkSectionImpl;
import net.raphimc.viabedrock.api.model.BedrockBlockState;
import net.raphimc.viabedrock.api.model.BlockState;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.viabedrock.protocol.data.enums.Dimension;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.HeightmapType;
import net.raphimc.viabedrock.protocol.data.generated.bedrock.CustomBlockTags;
import net.raphimc.viabedrock.protocol.data.generated.java.RegistryKeys;
import net.raphimc.viabedrock.protocol.model.Position3f;
import net.raphimc.viabedrock.protocol.rewriter.BlockEntityRewriter;
import net.raphimc.viabedrock.protocol.rewriter.BlockStateRewriter;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

// TODO: Feature: Block connections
// TODO: Feature: Lighting
public class ChunkTracker extends StoredObject {

    private static final byte[] FULL_LIGHT = new byte[ChunkSectionLight.LIGHT_LENGTH];

    static {
        Arrays.fill(FULL_LIGHT, (byte) 0xFF);
    }

    private final Dimension dimension;
    private final int minY;
    private final int worldHeight;
    private final Type<Chunk> chunkType;

    private final Long2ObjectMap<BedrockChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final LongSet dirtyChunks = new LongOpenHashSet();

    private final Set<SubChunkPosition> subChunkRequests = new HashSet<>();
    private final Set<SubChunkPosition> pendingSubChunks = new HashSet<>();

    private int centerX = 0;
    private int centerZ = 0;
    private int radius;

    public ChunkTracker(final UserConnection user, final Dimension dimension) {
        super(user);
        this.dimension = dimension;

        final GameSessionStorage gameSession = user.get(GameSessionStorage.class);
        final CompoundTag registries = gameSession.getJavaRegistries();
        final String dimensionKey = this.dimension.getKey();
        final CompoundTag dimensionRegistry = registries.getCompoundTag(RegistryKeys.DIMENSION_TYPE);
        final CompoundTag biomeRegistry = registries.getCompoundTag(RegistryKeys.WORLDGEN_BIOME);
        final CompoundTag dimensionTag = dimensionRegistry.getCompoundTag(dimensionKey);
        this.minY = dimensionTag.getNumberTag("min_y").asInt();
        this.worldHeight = dimensionTag.getNumberTag("height").asInt();
        this.chunkType = new ChunkType26_1(this.worldHeight >> 4, MathUtil.ceilLog2(BedrockProtocol.MAPPINGS.getJavaBlockStates().size()), MathUtil.ceilLog2(biomeRegistry.size()));

        final ChunkTracker oldChunkTracker = user.get(ChunkTracker.class);
        this.radius = oldChunkTracker != null ? oldChunkTracker.radius : user.get(ClientSettingsStorage.class).viewDistance();
    }

    public void setCenter(final int x, final int z) {
        this.centerX = x;
        this.centerZ = z;
        this.removeOutOfLoadDistanceChunks();
    }

    public void setRadius(final int radius) {
        this.radius = radius;
        this.removeOutOfLoadDistanceChunks();
    }

    public BedrockChunk createChunk(final int chunkX, final int chunkZ, final int nonNullSectionCount) {
        if (!this.isInLoadDistance(chunkX, chunkZ)) {
            return null;
        }
        if (!this.isInRenderDistance(chunkX, chunkZ)) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received chunk outside of render distance, but within load distance: " + chunkX + ", " + chunkZ);
            final EntityTracker entityTracker = this.user().get(EntityTracker.class);
            final PacketWrapper setChunkCacheCenter = PacketWrapper.create(ClientboundPackets26_3.SET_CHUNK_CACHE_CENTER, this.user());
            setChunkCacheCenter.write(Types.VAR_INT, (int) Math.floor(entityTracker.getClientPlayer().position().x()) >> 4); // chunk x
            setChunkCacheCenter.write(Types.VAR_INT, (int) Math.floor(entityTracker.getClientPlayer().position().z()) >> 4); // chunk z
            setChunkCacheCenter.send(BedrockProtocol.class);
        }

        final BedrockChunk chunk = new BedrockChunk(chunkX, chunkZ, new BedrockChunkSection[this.worldHeight >> 4]);
        for (int i = 0; i < nonNullSectionCount && i < chunk.getSections().length; i++) {
            chunk.getSections()[i] = new BedrockChunkSectionImpl();
        }
        for (int i = 0; i < chunk.getSections().length; i++) {
            if (chunk.getSections()[i] == null) {
                chunk.getSections()[i] = new BedrockChunkSectionImpl(true);
            }
        }
        this.chunks.put(ChunkPosition.chunkKey(chunk.getX(), chunk.getZ()), chunk);
        return chunk;
    }

    public void unloadChunk(final ChunkPosition chunkPos) {
        this.chunks.remove(chunkPos.chunkKey());
        this.user().get(EntityTracker.class).removeItemFrame(chunkPos);

        final PacketWrapper unloadChunk = PacketWrapper.create(ClientboundPackets26_3.FORGET_LEVEL_CHUNK, this.user());
        unloadChunk.write(Types.CHUNK_POSITION, chunkPos); // chunk position
        unloadChunk.send(BedrockProtocol.class);
    }

    public BedrockChunk getChunk(final int chunkX, final int chunkZ) {
        if (!this.isInLoadDistance(chunkX, chunkZ)) {
            return null;
        }
        return this.chunks.get(ChunkPosition.chunkKey(chunkX, chunkZ));
    }

    public BedrockChunkSection getChunkSection(final int chunkX, final int subChunkY, final int chunkZ) {
        final BedrockChunk chunk = this.getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return null;
        }

        final int sectionIndex = subChunkY + Math.abs(this.minY >> 4);
        if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
            return null;
        }

        return chunk.getSections()[sectionIndex];
    }

    public BedrockChunkSection getChunkSection(final BlockPosition blockPosition) {
        return this.getChunkSection(blockPosition.x() >> 4, blockPosition.y() >> 4, blockPosition.z() >> 4);
    }

    public int getBlockState(final BlockPosition blockPosition) {
        return this.getBlockState(0, blockPosition);
    }

    public int getBlockState(final int layer, final BlockPosition blockPosition) {
        final BedrockChunkSection chunkSection = this.getChunkSection(blockPosition);
        if (chunkSection == null) {
            return this.bedrockAirId();
        }
        if (chunkSection.palettesCount(PaletteType.BLOCKS) <= layer) {
            return this.bedrockAirId();
        }
        return chunkSection.palettes(PaletteType.BLOCKS).get(layer).idAt(blockPosition.x() & 15, blockPosition.y() & 15, blockPosition.z() & 15);
    }

    public int getJavaBlockState(final BlockPosition blockPosition) {
        final BedrockChunkSection chunkSection = this.getChunkSection(blockPosition);
        if (chunkSection == null) {
            return ProtocolConstants.JAVA_AIR_ID;
        }

        final int sectionX = blockPosition.x() & 15;
        final int sectionY = blockPosition.y() & 15;
        final int sectionZ = blockPosition.z() & 15;

        return this.getJavaBlockState(chunkSection, sectionX, sectionY, sectionZ);
    }

    public int getJavaBlockState(final BedrockChunkSection section, final int sectionX, final int sectionY, final int sectionZ) {
        final BlockStateRewriter blockStateRewriter = this.user().get(BlockStateRewriter.class);
        final List<DataPalette> blockPalettes = section.palettes(PaletteType.BLOCKS);

        final int blockState0 = blockPalettes.get(0).idAt(sectionX, sectionY, sectionZ);
        int remappedBlockState = blockStateRewriter.javaId(blockState0);
        if (remappedBlockState == -1) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing block state: " + blockState0);
            remappedBlockState = ProtocolConstants.JAVA_AIR_ID;
        }

        final int airId = this.bedrockAirId();
        if (blockState0 != airId && blockPalettes.size() > 1) {
            final int blockState1 = blockPalettes.get(1).idAt(sectionX, sectionY, sectionZ);
            if (blockState1 != airId) {
                return this.remapSecondBlockLayer(blockStateRewriter, blockState0, blockState1, remappedBlockState);
            }
        }

        return remappedBlockState;
    }

    private int remapSecondBlockLayer(final BlockStateRewriter blockStateRewriter, final int blockState0, final int blockState1, final int javaBlockState) {
        if (CustomBlockTags.WATER.equals(blockStateRewriter.tag(blockState1))) {
            final int waterloggedBlockState = blockStateRewriter.waterlog(javaBlockState);
            if (waterloggedBlockState != -1) {
                return waterloggedBlockState;
            }
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing waterlogged block state: " + blockState0);
        } else {
            final BlockState primaryState = blockStateRewriter.blockState(blockState0);
            // Snowlogged blocks are stored under snow in Bedrock. Keep the snow depth in Java's single block state.
            if (primaryState == null || !primaryState.namespacedIdentifier().equals("minecraft:snow_layer") || blockStateRewriter.blockState(blockState1) == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Invalid layer 2 block state. L1: " + blockState0 + ", L2: " + blockState1);
            }
        }
        return javaBlockState;
    }

    public BedrockBlockEntity getBlockEntity(final BlockPosition blockPosition) {
        final BedrockChunk chunk = this.getChunk(blockPosition.x() >> 4, blockPosition.z() >> 4);
        if (chunk == null) {
            return null;
        }
        return chunk.getBlockEntityAt(blockPosition);
    }

    public void addBlockEntity(final BedrockBlockEntity bedrockBlockEntity) {
        final BedrockChunk chunk = this.getChunk(bedrockBlockEntity.position().x() >> 4, bedrockBlockEntity.position().z() >> 4);
        if (chunk == null) {
            return;
        }

        chunk.removeBlockEntityAt(bedrockBlockEntity.position());
        chunk.blockEntities().add(bedrockBlockEntity);
    }

    public boolean isChunkLoaded(final ChunkPosition chunkPos) {
        if (!this.isInLoadDistance(chunkPos.chunkX(), chunkPos.chunkZ())) {
            return false;
        }
        return this.chunks.containsKey(chunkPos.chunkKey());
    }

    public boolean isInUnloadedChunkSection(final Position3f playerPosition) {
        final BlockPosition chunkSectionPosition = new BlockPosition((int) Math.floor(playerPosition.x()) >> 4, (int) Math.floor((playerPosition.y() - 1.62F)) >> 4, (int) Math.floor(playerPosition.z()) >> 4);
        final ChunkPosition chunkPos = new ChunkPosition(chunkSectionPosition.x(), chunkSectionPosition.z());
        if (!this.isChunkLoaded(chunkPos)) {
            return true;
        }
        final BedrockChunkSection chunkSection = this.getChunkSection(chunkSectionPosition.x(), chunkSectionPosition.y(), chunkSectionPosition.z());
        if (chunkSection == null) {
            return false;
        }
        if (chunkSection.hasPendingBlockUpdates()) {
            return true;
        }
        return this.dirtyChunks.contains(chunkPos.chunkKey());
    }

    public boolean isInLoadDistance(final int chunkX, final int chunkZ) {
        if (!this.isInRenderDistance(chunkX, chunkZ)) { // Bedrock accepts chunks outside the chunk render range and uses the player position as a center to determine if a chunk is allowed to be loaded
            final EntityTracker entityTracker = this.user().get(EntityTracker.class);
            if (entityTracker == null) {
                return false;
            }
            final int centerX = (int) Math.floor(entityTracker.getClientPlayer().position().x()) >> 4;
            final int centerZ = (int) Math.floor(entityTracker.getClientPlayer().position().z()) >> 4;
            return Math.abs(chunkX - centerX) <= this.radius && Math.abs(chunkZ - centerZ) <= this.radius;
        }

        return true;
    }

    public boolean isInRenderDistance(final int chunkX, final int chunkZ) {
        return Math.abs(chunkX - this.centerX) <= this.radius && Math.abs(chunkZ - this.centerZ) <= this.radius;
    }

    public void removeOutOfLoadDistanceChunks() {
        final Set<ChunkPosition> chunksToRemove = new HashSet<>();
        for (long chunkKey : this.chunks.keySet()) {
            final ChunkPosition chunkPos = new ChunkPosition(chunkKey);
            if (this.isInLoadDistance(chunkPos.chunkX(), chunkPos.chunkZ())) {
                continue;
            }

            chunksToRemove.add(chunkPos);
        }
        for (ChunkPosition chunkPos : chunksToRemove) {
            this.unloadChunk(chunkPos);
        }
    }

    public void requestSubChunks(final int chunkX, final int chunkZ, final int from, final int to) {
        for (int i = from; i < to; i++) {
            this.requestSubChunk(chunkX, i, chunkZ);
        }
    }

    public void requestSubChunk(final int chunkX, final int subChunkY, final int chunkZ) {
        if (!this.isInLoadDistance(chunkX, chunkZ)) {
            return;
        }
        this.subChunkRequests.add(new SubChunkPosition(chunkX, subChunkY, chunkZ));
    }

    public boolean mergeSubChunk(final int chunkX, final int subChunkY, final int chunkZ, final BedrockChunkSection other, final List<BedrockBlockEntity> blockEntities) {
        if (!this.isInLoadDistance(chunkX, chunkZ)) {
            return false;
        }

        final SubChunkPosition position = new SubChunkPosition(chunkX, subChunkY, chunkZ);
        if (!this.pendingSubChunks.contains(position)) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received sub chunk that was not requested: " + position);
            return false;
        }
        this.pendingSubChunks.remove(position);

        final BedrockChunk chunk = this.getChunk(chunkX, chunkZ);
        if (chunk == null) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received sub chunk for unloaded chunk: " + position);
            return false;
        }

        final BedrockChunkSection section = chunk.getSections()[subChunkY + Math.abs(this.minY >> 4)];
        section.mergeWith(this.handleBlockPalette(other));
        section.applyPendingBlockUpdates(this.bedrockAirId());
        blockEntities.forEach(blockEntity -> chunk.removeBlockEntityAt(blockEntity.position()));
        chunk.blockEntities().addAll(blockEntities);
        return true;
    }

    public IntObjectPair<BlockEntity> handleBlockChange(final BlockPosition blockPosition, final int layer, final int blockState) {
        final BedrockChunkSection section = this.getChunkSection(blockPosition);
        if (section == null) {
            return null;
        }

        final BlockStateRewriter blockStateRewriter = this.user().get(BlockStateRewriter.class);
        final EntityTracker entityTracker = this.user().get(EntityTracker.class);
        final int sectionX = blockPosition.x() & 15;
        final int sectionY = blockPosition.y() & 15;
        final int sectionZ = blockPosition.z() & 15;

        if (section.hasPendingBlockUpdates()) {
            section.addPendingBlockUpdate(sectionX, sectionY, sectionZ, layer, blockState);
            return null;
        }

        while (section.palettesCount(PaletteType.BLOCKS) <= layer) {
            final BedrockDataPalette palette = new BedrockDataPalette();
            palette.addId(this.bedrockAirId());
            section.addPalette(PaletteType.BLOCKS, palette);
        }
        final DataPalette palette = section.palettes(PaletteType.BLOCKS).get(layer);
        final int prevBlockState = palette.idAt(sectionX, sectionY, sectionZ);
        final String prevTag = blockStateRewriter.tag(prevBlockState);
        palette.setIdAt(sectionX, sectionY, sectionZ, blockState);
        final String tag = blockStateRewriter.tag(blockState);

        int remappedBlockState = this.getJavaBlockState(section, sectionX, sectionY, sectionZ);
        if (!Objects.equals(prevTag, tag)) {
            this.getChunk(blockPosition.x() >> 4, blockPosition.z() >> 4).removeBlockEntityAt(blockPosition);
            entityTracker.removeItemFrame(blockPosition);
        }

        if (prevBlockState != blockState) {
            if (BlockEntityRewriter.isBlockEntity(tag)) {
                final BedrockBlockEntity bedrockBlockEntity = this.getBlockEntity(blockPosition);
                BlockEntity javaBlockEntity = null;
                if (bedrockBlockEntity != null) {
                    javaBlockEntity = BlockEntityRewriter.toJava(this.user(), blockState, bedrockBlockEntity);
                    if (javaBlockEntity instanceof BlockEntityWithBlockState blockEntityWithBlockState) {
                        remappedBlockState = blockEntityWithBlockState.blockState();
                    }
                } else if (BedrockProtocol.MAPPINGS.getJavaBlockEntities().containsKey(tag)) {
                    final int javaType = BedrockProtocol.MAPPINGS.getJavaBlockEntities().get(tag);
                    javaBlockEntity = new BlockEntityImpl(BlockEntity.pack(sectionX, sectionZ), (short) blockPosition.y(), javaType, new CompoundTag());
                }

                if (javaBlockEntity != null && javaBlockEntity.tag() != null) {
                    return new IntObjectImmutablePair<>(remappedBlockState, javaBlockEntity);
                }
            } else if (CustomBlockTags.ITEM_FRAME.equals(tag)) {
                entityTracker.spawnItemFrame(blockPosition, blockStateRewriter.blockState(blockState));
            }
        }

        return new IntObjectImmutablePair<>(remappedBlockState, null);
    }

    public BedrockChunkSection handleBlockPalette(final BedrockChunkSection section) {
        this.replaceLegacyBlocks(section);
        this.resolvePersistentIds(section);
        return section;
    }

    public void sendChunkInNextTick(final int chunkX, final int chunkZ) {
        this.dirtyChunks.add(ChunkPosition.chunkKey(chunkX, chunkZ));
    }

    public void sendChunk(final int chunkX, final int chunkZ) {
        final BedrockChunk chunk = this.getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return;
        }

        final Chunk remappedChunk = this.remapChunk(chunk);

        final PacketWrapper levelChunkWithLight = PacketWrapper.create(ClientboundPackets26_3.LEVEL_CHUNK_WITH_LIGHT, this.user());
        final BitSet lightMask = new BitSet();
        lightMask.set(0, remappedChunk.getSections().length + 2);
        levelChunkWithLight.write(this.chunkType, remappedChunk); // chunk
        levelChunkWithLight.write(Types.BIT_SET, lightMask); // sky light mask
        levelChunkWithLight.write(Types.BIT_SET, new BitSet()); // block light mask
        levelChunkWithLight.write(Types.BIT_SET, new BitSet()); // empty sky light mask
        levelChunkWithLight.write(Types.BIT_SET, lightMask); // empty block light mask
        levelChunkWithLight.write(Types.VAR_INT, remappedChunk.getSections().length + 2); // sky light length
        for (int i = 0; i < remappedChunk.getSections().length + 2; i++) {
            levelChunkWithLight.write(Types.BYTE_ARRAY_PRIMITIVE, FULL_LIGHT.clone()); // sky light
        }
        levelChunkWithLight.write(Types.VAR_INT, 0); // block light length
        levelChunkWithLight.send(BedrockProtocol.class);
    }

    public Dimension getDimension() {
        return this.dimension;
    }

    public int getMinY() {
        return this.minY;
    }

    public int getMaxY() {
        return this.worldHeight - Math.abs(this.minY);
    }

    public int getWorldHeight() {
        return this.worldHeight;
    }

    public int bedrockAirId() {
        return this.user().get(BlockStateRewriter.class).bedrockId(BedrockBlockState.AIR);
    }

    public boolean isEmpty() {
        boolean empty = true;
        empty &= this.chunks.isEmpty();
        empty &= this.subChunkRequests.isEmpty() && this.pendingSubChunks.isEmpty();
        return empty;
    }

    public void tick() {
        for (long dirtyChunk : this.dirtyChunks) {
            final ChunkPosition chunkPos = new ChunkPosition(dirtyChunk);
            this.sendChunk(chunkPos.chunkX(), chunkPos.chunkZ());
        }
        this.dirtyChunks.clear();

        if (this.user().get(EntityTracker.class) == null || !this.user().get(EntityTracker.class).getClientPlayer().isInitiallySpawned()) {
            return;
        }

        this.subChunkRequests.removeIf(s -> !this.isInLoadDistance(s.chunkX, s.chunkZ));
        final BlockPosition basePosition = new BlockPosition(this.centerX, 0, this.centerZ);
        while (!this.subChunkRequests.isEmpty()) {
            final Set<SubChunkPosition> group = this.subChunkRequests.stream().limit(256).collect(Collectors.toSet());
            this.subChunkRequests.removeAll(group);
            this.pendingSubChunks.addAll(group);

            final PacketWrapper subChunkRequest = PacketWrapper.create(ServerboundBedrockPackets.SUB_CHUNK_REQUEST, this.user());
            subChunkRequest.write(BedrockTypes.VAR_INT, this.dimension.ordinal()); // dimension id
            subChunkRequest.write(BedrockTypes.UNSIGNED_VAR_INT, group.size()); // sub chunk offset count
            for (SubChunkPosition subChunkPosition : group) {
                final BlockPosition offset = new BlockPosition(subChunkPosition.chunkX - basePosition.x(), subChunkPosition.subChunkY, subChunkPosition.chunkZ - basePosition.z());
                subChunkRequest.write(BedrockTypes.SUB_CHUNK_OFFSET, offset); // offset
            }
            // base position
            subChunkRequest.write(BedrockTypes.INT_LE, basePosition.x()); // x
            subChunkRequest.write(BedrockTypes.INT_LE, basePosition.y()); // y
            subChunkRequest.write(BedrockTypes.INT_LE, basePosition.z()); // z

            subChunkRequest.sendToServer(BedrockProtocol.class);
        }
    }

    private Chunk remapChunk(final BedrockChunk chunk) {
        final BlockStateRewriter blockStateRewriter = this.user().get(BlockStateRewriter.class);
        final int airId = this.bedrockAirId();

        final Chunk remappedChunk = new Chunk1_21_5(chunk.getX(), chunk.getZ(), new ChunkSection[chunk.getSections().length], new Heightmap[2], new ArrayList<>());

        final BedrockChunkSection[] bedrockSections = chunk.getSections();
        final ChunkSection[] remappedSections = remappedChunk.getSections();
        for (int idx = 0; idx < bedrockSections.length; idx++) {
            final BedrockChunkSection bedrockSection = bedrockSections[idx];
            final List<DataPalette> blockPalettes = bedrockSection.palettes(PaletteType.BLOCKS);
            final ChunkSection remappedSection = remappedSections[idx] = new ChunkSectionImpl(false);
            final DataPalette remappedBlockPalette = remappedSection.palette(PaletteType.BLOCKS);

            if (!blockPalettes.isEmpty()) {
                final DataPalette layer0 = blockPalettes.get(0);
                if (layer0.size() == 1) {
                    remappedBlockPalette.addId(layer0.idByIndex(0));
                } else {
                    this.transferPaletteData(layer0, remappedBlockPalette);
                }

                final String[] paletteIndexBlockStateTags = new String[remappedBlockPalette.size()];
                for (int i = 0; i < remappedBlockPalette.size(); i++) {
                    paletteIndexBlockStateTags[i] = blockStateRewriter.tag(remappedBlockPalette.idByIndex(i));
                }
                remappedBlockPalette.replaceIds(bedrockBlockState -> {
                    final int javaBlockState = blockStateRewriter.javaId(bedrockBlockState);
                    if (javaBlockState != -1) {
                        return javaBlockState;
                    } else {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing block state: " + bedrockBlockState);
                        return ProtocolConstants.JAVA_AIR_ID;
                    }
                });

                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            final String tag = paletteIndexBlockStateTags[remappedBlockPalette.paletteIndexAt(remappedBlockPalette.index(x, y, z))];
                            if (tag != null) {
                                if (BlockEntityRewriter.isBlockEntity(tag)) {
                                    final int absY = this.minY + (idx << 4) + y;
                                    final BlockPosition position = new BlockPosition((chunk.getX() << 4) + x, absY, (chunk.getZ() << 4) + z);
                                    final BedrockBlockEntity bedrockBlockEntity = chunk.getBlockEntityAt(position);
                                    if (bedrockBlockEntity != null) {
                                        final BlockEntity javaBlockEntity = BlockEntityRewriter.toJava(this.user(), layer0.idAt(x, y, z), bedrockBlockEntity);
                                        if (javaBlockEntity instanceof BlockEntityWithBlockState blockEntityWithBlockState) {
                                            remappedBlockPalette.setIdAt(x, y, z, blockEntityWithBlockState.blockState());
                                        }
                                        if (javaBlockEntity != null && javaBlockEntity.tag() != null) {
                                            remappedChunk.blockEntities().add(javaBlockEntity);
                                        }
                                    } else if (BedrockProtocol.MAPPINGS.getJavaBlockEntities().containsKey(tag)) {
                                        final int javaType = BedrockProtocol.MAPPINGS.getJavaBlockEntities().get(tag);
                                        final BlockEntity javaBlockEntity = new BlockEntityImpl(BlockEntity.pack(x, z), (short) absY, javaType, new CompoundTag());
                                        remappedChunk.blockEntities().add(javaBlockEntity);
                                    }
                                } else if (tag.equals(CustomBlockTags.ITEM_FRAME)) {
                                    final BlockPosition position = new BlockPosition((chunk.getX() << 4) + x, this.minY + (idx << 4) + y, (chunk.getZ() << 4) + z);
                                    this.user().get(EntityTracker.class).spawnItemFrame(position, blockStateRewriter.blockState(layer0.idAt(x, y, z)));
                                }
                            }
                        }
                    }
                }

                if (blockPalettes.size() > 1) {
                    final DataPalette layer1 = blockPalettes.get(1);
                    if (layer1.size() != 1 || layer1.idByIndex(0) != airId) {
                        for (int x = 0; x < 16; x++) {
                            for (int z = 0; z < 16; z++) {
                                for (int y = 0; y < 16; y++) {
                                    final int blockState1 = layer1.idAt(x, y, z);
                                    if (blockState1 == airId) {
                                        continue;
                                    }
                                    final int blockState0 = layer0.idAt(x, y, z);
                                    if (blockState0 == airId) {
                                        continue;
                                    }

                                    final int javaBlockState = remappedBlockPalette.idAt(x, y, z);
                                    final int combinedBlockState = this.remapSecondBlockLayer(blockStateRewriter, blockState0, blockState1, javaBlockState);
                                    if (combinedBlockState != javaBlockState) {
                                        remappedBlockPalette.setIdAt(x, y, z, combinedBlockState);
                                    }
                                }
                            }
                        }
                    }
                }

                int nonAirBlockCount = 0;
                int fluidCount = 0;
                for (int i = 0; i < ChunkSection.SIZE; i++) {
                    final int javaBlockState = remappedBlockPalette.idAt(i);
                    if (javaBlockState != ProtocolConstants.JAVA_AIR_ID) {
                        nonAirBlockCount++;
                    }
                    if (BedrockProtocol.MAPPINGS.getJavaFluidBlockStates().contains(javaBlockState)) {
                        fluidCount++;
                    }
                }
                remappedSection.setNonAirBlocksCount(nonAirBlockCount);
                remappedSection.setFluidCount(fluidCount);
            } else {
                remappedBlockPalette.addId(ProtocolConstants.JAVA_AIR_ID);
            }

            final DataPalette biomePalette = bedrockSection.palette(PaletteType.BIOMES);
            final DataPalette remappedBiomePalette = new DataPaletteImpl(ChunkSection.BIOME_SIZE);
            remappedSection.addPalette(PaletteType.BIOMES, remappedBiomePalette);

            if (biomePalette != null) {
                if (biomePalette.size() == 1) {
                    remappedBiomePalette.addId(biomePalette.idByIndex(0));
                } else {
                    for (int x = 0; x < 4; x++) {
                        for (int z = 0; z < 4; z++) {
                            for (int y = 0; y < 4; y++) {
                                final BiomeAggregator subBiomes = new BiomeAggregator(4);
                                for (int subX = 0; subX < 4; subX++) {
                                    for (int subZ = 0; subZ < 4; subZ++) {
                                        for (int subY = 0; subY < 4; subY++) {
                                            subBiomes.record(biomePalette.idAt((x << 2) + subX, (y << 2) + subY, (z << 2) + subZ));
                                        }
                                    }
                                }
                                remappedBiomePalette.setIdAt(x, y, z, subBiomes.getMaxBiome());
                            }
                        }
                    }
                }

                remappedBiomePalette.replaceIds(bedrockBiome -> {
                    final String bedrockBiomeName = BedrockProtocol.MAPPINGS.getBedrockBiomes().inverse().get(bedrockBiome);
                    if (bedrockBiomeName != null) {
                        return BedrockProtocol.MAPPINGS.getJavaBiomes().get(bedrockBiomeName);
                    } else {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing biome: " + bedrockBiome);
                        return BedrockProtocol.MAPPINGS.getJavaBiomes().get("the_void");
                    }
                });
            } else {
                remappedBiomePalette.addId(BedrockProtocol.MAPPINGS.getJavaBiomes().get("the_void"));
            }
        }

        final IntSet motionBlockingBlockStates = BedrockProtocol.MAPPINGS.getJavaHeightMapBlockStates().get("motion_blocking");
        final int[] worldSurface = new int[16 * 16];
        final int[] motionBlocking = new int[16 * 16];
        Arrays.fill(worldSurface, Integer.MIN_VALUE);
        Arrays.fill(motionBlocking, Integer.MIN_VALUE);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                final int index = (z << 4) + x;
                FIND_Y:
                for (int idx = remappedSections.length - 1; idx >= 0; idx--) {
                    final DataPalette blockPalette = remappedSections[idx].palette(PaletteType.BLOCKS);
                    if (blockPalette.size() == 1 && blockPalette.idByIndex(0) == ProtocolConstants.JAVA_AIR_ID) {
                        continue;
                    }

                    for (int y = 15; y >= 0; y--) {
                        final int blockState = blockPalette.idAt(x, y, z);
                        if (blockState != ProtocolConstants.JAVA_AIR_ID) {
                            final int value = (idx << 4) + y + 1;

                            if (worldSurface[index] == Integer.MIN_VALUE) {
                                worldSurface[index] = value;
                            }
                            if (motionBlocking[index] == Integer.MIN_VALUE && motionBlockingBlockStates.contains(blockState)) {
                                motionBlocking[index] = value;
                                break FIND_Y;
                            }
                        }
                    }
                }

                if (worldSurface[index] == Integer.MIN_VALUE) {
                    worldSurface[index] = this.minY;
                }
                if (motionBlocking[index] == Integer.MIN_VALUE) {
                    motionBlocking[index] = this.minY;
                }
            }
        }

        final int bitsPerEntry = MathUtil.ceilLog2(this.worldHeight + 1);
        remappedChunk.heightmaps()[0] = new Heightmap(HeightmapType.WORLD_SURFACE.ordinal(), CompactArrayUtil.createCompactArrayWithPadding(bitsPerEntry, worldSurface.length, i -> worldSurface[i]));
        remappedChunk.heightmaps()[1] = new Heightmap(HeightmapType.MOTION_BLOCKING.ordinal(), CompactArrayUtil.createCompactArrayWithPadding(bitsPerEntry, motionBlocking.length, i -> motionBlocking[i]));

        return remappedChunk;
    }

    private void resolvePersistentIds(final BedrockChunkSection bedrockSection) {
        final BlockStateRewriter blockStateRewriter = this.user().get(BlockStateRewriter.class);

        final List<DataPalette> palettes = bedrockSection.palettes(PaletteType.BLOCKS);
        for (DataPalette palette : palettes) {
            if (palette instanceof BedrockDataPalette bedrockPalette) {
                if (bedrockPalette.usesPersistentIds()) {
                    bedrockPalette.resolvePersistentIds(bedrockBlockStateTag -> {
                        if (!(bedrockBlockStateTag instanceof CompoundTag)) {
                            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Invalid block state tag: Palette=" + bedrockPalette + " Tag=" + bedrockBlockStateTag);
                            return blockStateRewriter.bedrockId(BedrockBlockState.INFO_UPDATE);
                        }
                        final int bedrockBlockState = blockStateRewriter.bedrockId((CompoundTag) bedrockBlockStateTag);
                        if (bedrockBlockState != -1) {
                            return bedrockBlockState;
                        } else {
                            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing block state: " + bedrockBlockStateTag);
                            return blockStateRewriter.bedrockId(BedrockBlockState.INFO_UPDATE);
                        }
                    });
                }
            }
        }
    }

    private void replaceLegacyBlocks(final BedrockChunkSection bedrockSection) {
        final BlockStateRewriter blockStateRewriter = this.user().get(BlockStateRewriter.class);

        final List<DataPalette> palettes = bedrockSection.palettes(PaletteType.BLOCKS);
        for (DataPalette palette : palettes) {
            if (palette instanceof BedrockBlockArray) {
                final BedrockDataPalette newPalette = new BedrockDataPalette();
                this.transferPaletteData(palette, newPalette);
                newPalette.replaceIds(legacyBlockState -> {
                    final int bedrockBlockState = blockStateRewriter.bedrockId(legacyBlockState);
                    if (bedrockBlockState != -1) {
                        return bedrockBlockState;
                    } else {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing legacy block state: " + legacyBlockState);
                        return this.bedrockAirId();
                    }
                });
                palettes.set(palettes.indexOf(palette), newPalette);
            }
        }
    }

    /**
     * Transfers the palette data between two different palette types.
     *
     * @param source The source palette
     * @param target The target palette
     */
    private void transferPaletteData(final DataPalette source, final DataPalette target) {
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    target.setIdAt(x, y, z, source.idAt(x, y, z));
                }
            }
        }
    }

    private record SubChunkPosition(int chunkX, int subChunkY, int chunkZ) {
    }

    private static final class BiomeAggregator {

        private int[] biome;
        private int[] count;
        private int size;

        private BiomeAggregator(final int capacity) {
            this.biome = new int[capacity];
            this.count = new int[capacity];
        }

        private void record(final int biome) {
            for (int i = 0; i < this.size; i++) {
                if (this.biome[i] == biome) {
                    this.count[i]++;
                    return;
                }
            }
            this.init(biome);
        }

        private int getMaxBiome() {
            int maxBiome = Integer.MIN_VALUE;
            int maxCount = Integer.MIN_VALUE;
            for (int i = 0; i < this.size; i++) {
                if (this.count[i] > maxCount) {
                    maxCount = this.count[i];
                    maxBiome = this.biome[i];
                }
            }
            return maxBiome;
        }

        private void init(final int biome) {
            if (this.size == this.biome.length) {
                final int[] newBiome = new int[this.size == 0 ? 2 : this.size * 2];
                final int[] newCount = new int[this.size == 0 ? 2 : this.size * 2];
                System.arraycopy(this.biome, 0, newBiome, 0, this.size);
                System.arraycopy(this.count, 0, newCount, 0, this.size);
                this.biome = newBiome;
                this.count = newCount;
            }
            this.biome[this.size] = biome;
            this.count[this.size] = 1;
            this.size++;
        }

    }

}
