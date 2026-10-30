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
package net.raphimc.viabedrock.protocol.types;

import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.type.OptionalType;
import com.viaversion.viaversion.api.type.Type;
import net.raphimc.viabedrock.api.chunk.datapalette.BedrockDataPalette;
import net.raphimc.viabedrock.api.chunk.section.BedrockChunkSection;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.Tag_Type;
import net.raphimc.viabedrock.protocol.model.*;
import net.raphimc.viabedrock.protocol.types.array.ArrayType;
import net.raphimc.viabedrock.protocol.types.array.ByteArrayType;
import net.raphimc.viabedrock.protocol.types.chunk.ChunkSectionType;
import net.raphimc.viabedrock.protocol.types.chunk.DataPaletteType;
import net.raphimc.viabedrock.protocol.types.entitydata.EntityDataType;
import net.raphimc.viabedrock.protocol.types.entitydata.EntityPropertiesType;
import net.raphimc.viabedrock.protocol.types.entitydata.PassengerOfBlocksType;
import net.raphimc.viabedrock.protocol.types.model.*;
import net.raphimc.viabedrock.protocol.types.position.BlockPositionType;
import net.raphimc.viabedrock.protocol.types.position.Position2fType;
import net.raphimc.viabedrock.protocol.types.position.Position3fType;
import net.raphimc.viabedrock.protocol.types.position.SubChunkOffsetType;
import net.raphimc.viabedrock.protocol.types.primitive.*;

import java.awt.image.BufferedImage;
import java.util.UUID;

public final class BedrockTypes {

    public static final ShortLeType SHORT_LE = new ShortLeType();
    public static final UnsignedShortLeType UNSIGNED_SHORT_LE = new UnsignedShortLeType();
    public static final IntLeType INT_LE = new IntLeType();
    public static final UnsignedIntLeType UNSIGNED_INT_LE = new UnsignedIntLeType();
    public static final FloatLeType FLOAT_LE = new FloatLeType();
    public static final LongLeType LONG_LE = new LongLeType();
    public static final LongLeType UNSIGNED_LONG_LE = LONG_LE;

    public static final VarIntType VAR_INT = new VarIntType();
    public static final UnsignedVarIntType UNSIGNED_VAR_INT = new UnsignedVarIntType();
    public static final VarLongType VAR_LONG = new VarLongType();
    public static final UnsignedVarLongType UNSIGNED_VAR_LONG = new UnsignedVarLongType();
    public static final Type<Long[]> LONG_ARRAY = new ArrayType<>(LONG_LE, UNSIGNED_VAR_INT);
    public static final Type<byte[]> BYTE_ARRAY = new ByteArrayType();
    public static final Type<String> ASCII_STRING = new AsciiStringType();
    public static final Type<String> STRING = new StringType();
    public static final Type<String> OPTIONAL_STRING = new OptionalType<>(STRING);
    public static final Type<String[]> STRING_ARRAY = new ArrayType<>(STRING, UNSIGNED_VAR_INT);
    public static final Type<String> UTF8_STRING = new Utf8StringType();
    public static final Type<String[]> UTF8_STRING_ARRAY = new ArrayType<>(UTF8_STRING, UNSIGNED_INT_LE);
    public static final Type<UUID> UUID = new UuidType();
    public static final Type<UUID[]> UUID_ARRAY = new ArrayType<>(UUID, UNSIGNED_VAR_INT);
    public static final Type<BufferedImage> IMAGE = new ImageType();

    public static final Type<Tag> NETWORK_TAG = new TagType();
    public static final Type<Tag> TAG_LE = new TagLeType();
    public static final Type<Tag> COMPOUND_TAG_VALUE = new TagValueType(Tag_Type.Compound);
    public static final Type<BlockPosition> BLOCK_POSITION = new BlockPositionType();
    public static final Type<Position3f> POSITION_3F = new Position3fType();
    public static final Type<Position3f> OPTIONAL_POSITION_3F = new OptionalType<>(POSITION_3F);
    public static final Type<Position2f> POSITION_2F = new Position2fType();
    public static final Type<GameRule> GAME_RULE = new GameRuleType();
    public static final Type<GameRule[]> GAME_RULE_ARRAY = new ArrayType<>(GAME_RULE, UNSIGNED_VAR_INT);
    public static final Type<Experiment> EXPERIMENT = new ExperimentType();
    public static final Type<Experiment[]> EXPERIMENT_ARRAY = new ArrayType<>(EXPERIMENT, UNSIGNED_INT_LE);
    public static final Type<EducationUriResource> EDUCATION_URI_RESOURCE = new EducationUriResourceType();
    public static final Type<BlockProperties> BLOCK_PROPERTIES = new BlockPropertiesType();
    public static final Type<BlockProperties[]> BLOCK_PROPERTIES_ARRAY = new ArrayType<>(BLOCK_PROPERTIES, UNSIGNED_VAR_INT);
    public static final Type<ItemEntry> ITEM_ENTRY = new ItemEntryType();
    public static final Type<ItemEntry[]> ITEM_ENTRY_ARRAY = new ArrayType<>(ITEM_ENTRY, UNSIGNED_VAR_INT);
    public static final Type<CommandOriginData> COMMAND_ORIGIN_DATA = new CommandOriginDataType();
    public static final Type<BedrockChunkSection> CHUNK_SECTION = new ChunkSectionType();
    public static final Type<BlockPosition> SUB_CHUNK_OFFSET = new SubChunkOffsetType();
    public static final Type<BlockChangeEntry> BLOCK_CHANGE_ENTRY = new BlockChangeEntryType();
    public static final Type<BlockChangeEntry[]> BLOCK_CHANGE_ENTRY_ARRAY = new ArrayType<>(BLOCK_CHANGE_ENTRY, UNSIGNED_VAR_INT);
    public static final Type<BedrockDataPalette> DATA_PALETTE = new DataPaletteType(true);
    public static final Type<BedrockDataPalette> RUNTIME_DATA_PALETTE = new DataPaletteType(false);
    public static final Type<EntityData> ENTITY_DATA = new EntityDataType();
    public static final Type<EntityData[]> ENTITY_DATA_ARRAY = new ArrayType<>(ENTITY_DATA, UNSIGNED_VAR_INT);
    public static final Type<EntityProperties> ENTITY_PROPERTIES = new EntityPropertiesType();
    public static final Type<EntityLink> ENTITY_LINK = new EntityLinkType();
    public static final Type<EntityLink[]> ENTITY_LINK_ARRAY = new ArrayType<>(ENTITY_LINK, UNSIGNED_VAR_INT);
    public static final Type<SkinData> SKIN = new SkinType();
    public static final Type<PlayerAbilities> PLAYER_ABILITIES = new PlayerAbilitiesType();
    public static final Type<CommandData[]> COMMAND_DATA_ARRAY = new CommandDataArrayType();
    public static final Type<FullContainerName> FULL_CONTAINER_NAME = new FullContainerNameType();
    public static final Type<FullContainerName> OPTIONAL_FULL_CONTAINER_NAME = new OptionalType<>(FULL_CONTAINER_NAME);
    public static final Type<FullContainerName[]> FULL_CONTAINER_NAME_ARRAY = new ArrayType<>(FULL_CONTAINER_NAME, UNSIGNED_VAR_INT);
    public static final Type<PassengerOfBlocks> PASSENGER_OF_BLOCKS = new PassengerOfBlocksType();
    public static final Type<PassengerOfBlocks> OPTIONAL_PASSENGER_OF_BLOCKS = new OptionalType<>(PASSENGER_OF_BLOCKS);

    private BedrockTypes() {
    }

}
