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
package net.raphimc.viabedrock.protocol.data;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.viaversion.nbt.io.NBTIO;
import com.viaversion.nbt.limiter.TagLimiter;
import com.viaversion.nbt.stringified.SNBT;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.MappingDataBase;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes26_2;
import com.viaversion.viaversion.api.minecraft.item.StructuredItem;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.libs.fastutil.ints.*;
import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.util.GsonUtil;
import com.viaversion.viaversion.util.Key;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.api.chunk.blockstate.BlockStateUpgrader;
import net.raphimc.viabedrock.api.item.ItemUpgrader;
import net.raphimc.viabedrock.api.model.BedrockBlockState;
import net.raphimc.viabedrock.api.model.BlockState;
import net.raphimc.viabedrock.api.resourcepack.ResourcePack;
import net.raphimc.viabedrock.api.resourcepack.content.ZipContent;
import net.raphimc.viabedrock.api.resourcepack.definition.SoundDefinitions;
import net.raphimc.viabedrock.api.util.EnumUtil;
import net.raphimc.viabedrock.api.util.FileSystemUtil;
import net.raphimc.viabedrock.api.util.JsonUtil;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.SoundSource;
import net.raphimc.viabedrock.protocol.data.generated.java.RegistryKeys;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class BedrockMappingData extends MappingDataBase {

    // Bedrock misc
    private Map<ResourcePack.Key, ResourcePack> bedrockResourcePacks;
    private Map<ResourcePack.Key, ResourcePack> bedrockSkinPacks;
    private List<ResourcePack> bedrockVanillaResourcePacks;
    private Map<String, Object> bedrockGameRules;

    // Java misc
    private CompoundTag javaRegistries;
    private CompoundTag javaTags;
    private BiMap<String, Integer> javaCommandArgumentTypes;
    private BiMap<String, Integer> javaMenus;
    private BiMap<String, Integer> javaSlotDisplays;

    // Block states
    private BlockStateUpgrader bedrockBlockStateUpgrader;
    private BiMap<String, Integer> javaBlocks;
    private BiMap<BlockState, Integer> javaBlockStates;
    private Set<BedrockBlockState> bedrockBlockStates;
    private Map<BlockState, BlockState> bedrockToJavaBlockStates;
    private Map<String, String> bedrockCustomBlockTags;
    private Map<String, Map<String, Map<String, Set<String>>>> bedrockBlockTraits;
    private BiMap<String, Integer> bedrockLegacyBlocks;
    private Int2ObjectMap<BedrockBlockState> bedrockLegacyBlockStates;
    private IntSet javaPreWaterloggedBlockStates;
    private IntSet javaFluidBlockStates;
    private Int2IntMap javaPottedBlockStates;
    private Map<String, IntSet> javaHeightMapBlockStates;

    // Biomes
    private CompoundTag bedrockBiomeDefinitions;
    private BiMap<String, Integer> bedrockBiomes;
    private BiMap<String, Integer> javaBiomes;
    private Map<String, Map<String, Object>> bedrockToJavaBiomeExtraData;

    // Items
    private ItemUpgrader bedrockItemUpgrader;
    private BiMap<String, Integer> javaItems;
    private BiMap<String, CompoundTag> bedrockItems;
    private Set<String> bedrockBlockItems;
    private Set<String> bedrockMetaItems;
    private Map<String, Set<String>> bedrockItemTags;
    private Map<String, String> bedrockCustomItemTags;
    private Map<String, Map<BlockState, JavaItemMapping>> bedrockToJavaBlockItems;
    private Map<String, Map<Integer, JavaItemMapping>> bedrockToJavaMetaItems;
    private Map<ContainerType, Integer> bedrockToJavaContainers;

    // Entities
    private BiMap<String, Integer> bedrockEntities;
    private Map<ActorDataIDs, DataItemType> bedrockEntityDataTypes;
    private Map<ActorFlags, String> bedrockEntityFlagMoLangQueries;
    private Map<String, EntityTypes26_2> bedrockToJavaEntities;
    private BiMap<String, Integer> javaBlockEntities;
    private BiMap<String, Integer> javaEntityAttributes;
    private Map<EntityTypes26_2, List<String>> javaEntityDataFields;

    // Entity Effects
    private BiMap<String, Integer> javaEffects;
    private BiMap<String, Integer> bedrockEffects;
    private Map<String, String> bedrockToJavaEffects;
    private Map<String, String> javaToBedrockEffects; //TODO: I dont know how bimaps work

    // World Effects
    private BiMap<String, Integer> javaSounds;
    private BiMap<String, Integer> javaParticles;
    private Map<String, String> bedrockBlockSounds;
    private Map<String, Map<String, SoundDefinitions.ConfiguredSound>> bedrockLevelSoundEvents;
    private Map<NoteBlockInstrument, String> bedrockNoteBlockInstrumentSounds;
    private Map<String, JavaSound> bedrockToJavaSounds;
    private Map<String, JavaParticle> bedrockToJavaParticles;
    private Map<LevelEvent, LevelEventMapping> bedrockToJavaLevelEvents;
    private Map<ParticleType, JavaParticle> bedrockToJavaLevelEventParticles;

    // Other stuff
    private BiMap<String, String> bedrockToJavaExperimentalFeatures;
    private BiMap<String, String> bedrockToJavaBannerPatterns;
    private BiMap<String, String> bedrockToJavaPaintings;
    private Map<SharedTypes_Legacy_ActorDamageCause, String> bedrockToJavaDamageCauses;
    private Map<Enchant_Type, String> bedrockToJavaEnchantments;

    public BedrockMappingData() {
        super(BedrockProtocolVersion.bedrockLatest.getName(), ProtocolConstants.JAVA_VERSION.getName());
    }

    @Override
    public void load() {
        if (Via.getManager().isDebug()) {
            this.getLogger().info("Loading " + this.unmappedVersion + " -> " + this.mappedVersion + " mappings...");
        }

        final JsonObject javaViaMappingJson = this.readJson("java/via_mappings.json");

        { // Bedrock misc
            this.bedrockResourcePacks = new HashMap<>();
            try {
                for (Map.Entry<Path, byte[]> entry : FileSystemUtil.getFilesInDirectory("assets/viabedrock/resource_packs").entrySet()) {
                    final ResourcePack resourcePack = new ResourcePack(new ZipContent(entry.getValue()));
                    this.bedrockResourcePacks.put(resourcePack.key(), resourcePack);
                }
            } catch (Exception e) {
                this.getLogger().log(Level.SEVERE, "Failed to load resource packs", e);
            }

            this.bedrockSkinPacks = new HashMap<>();
            try {
                for (Map.Entry<Path, byte[]> entry : FileSystemUtil.getFilesInDirectory("assets/viabedrock/skin_packs").entrySet()) {
                    final ResourcePack resourcePack = new ResourcePack(new ZipContent(entry.getValue()));
                    this.bedrockSkinPacks.put(resourcePack.key(), resourcePack);
                }
            } catch (Exception e) {
                this.getLogger().log(Level.SEVERE, "Failed to load skin packs", e);
            }

            final JsonArray bedrockVanillaResourcePacksJson = this.readJson("custom/vanilla_resource_packs.json", JsonArray.class);
            this.bedrockVanillaResourcePacks = new ArrayList<>(bedrockVanillaResourcePacksJson.size());
            for (JsonElement entry : bedrockVanillaResourcePacksJson) {
                final ResourcePack.Key key = ResourcePack.Key.fromString(entry.getAsString());
                if (!this.bedrockResourcePacks.containsKey(key)) {
                    throw new RuntimeException("Unknown bedrock vanilla resource pack: " + key);
                }
                this.bedrockVanillaResourcePacks.add(this.bedrockResourcePacks.get(key));
            }

            final JsonObject bedrockGameRulesJson = this.readJson("bedrock/game_rules.json");
            this.bedrockGameRules = new HashMap<>(bedrockGameRulesJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockGameRulesJson.entrySet()) {
                this.bedrockGameRules.put(entry.getKey().toLowerCase(Locale.ROOT), JsonUtil.getValue(entry.getValue()));
            }
        }

        { // Java misc
            this.javaRegistries = this.readNBT("java/registries.nbt");
            this.javaTags = this.readNBT("java/tags.nbt");

            final JsonArray javaCommandArgumentTypesJson = javaViaMappingJson.getAsJsonArray("argumenttypes");
            this.javaCommandArgumentTypes = HashBiMap.create(javaCommandArgumentTypesJson.size());
            for (int i = 0; i < javaCommandArgumentTypesJson.size(); i++) {
                this.javaCommandArgumentTypes.put(Key.namespaced(javaCommandArgumentTypesJson.get(i).getAsString()), i);
            }
            ArgumentTypeRegistry.init();
        }

        final Multimap<String, BedrockBlockState> bedrockBlockStatesByIdentifier;
        { // Block states
            this.bedrockBlockStateUpgrader = new BlockStateUpgrader();

            final JsonArray javaBlocksJson = javaViaMappingJson.getAsJsonArray("blocks");
            this.javaBlocks = HashBiMap.create(javaBlocksJson.size());
            for (int i = 0; i < javaBlocksJson.size(); i++) {
                this.javaBlocks.put(Key.namespaced(javaBlocksJson.get(i).getAsString()), i);
            }

            final JsonArray javaBlockStatesJson = javaViaMappingJson.getAsJsonArray("blockstates");
            this.javaBlockStates = HashBiMap.create(javaBlockStatesJson.size());
            for (int i = 0; i < javaBlockStatesJson.size(); i++) {
                final BlockState blockState = BlockState.fromString(javaBlockStatesJson.get(i).getAsString());
                this.javaBlockStates.put(blockState, i);
            }

            final ListTag<CompoundTag> bedrockBlockStatesTag = this.readNBT("bedrock/block_palette.nbt").getListTag("blocks", CompoundTag.class);
            this.bedrockBlockStates = new LinkedHashSet<>(bedrockBlockStatesTag.size());
            bedrockBlockStatesByIdentifier = HashMultimap.create(bedrockBlockStatesTag.size(), 32);
            for (CompoundTag tag : bedrockBlockStatesTag) {
                final BedrockBlockState bedrockBlockState = BedrockBlockState.fromNbt(tag);
                this.bedrockBlockStates.add(bedrockBlockState);
                bedrockBlockStatesByIdentifier.put(bedrockBlockState.namespacedIdentifier(), bedrockBlockState);
            }

            final JsonObject bedrockToJavaBlockStateMappingsJson = this.readJson("custom/blockstate_mappings.json");
            this.bedrockToJavaBlockStates = new HashMap<>(bedrockToJavaBlockStateMappingsJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaBlockStateMappingsJson.entrySet()) {
                final BlockState bedrockBlockState = BlockState.fromString(entry.getKey());
                if (!this.bedrockBlockStates.contains(bedrockBlockState)) {
                    throw new RuntimeException("Unknown bedrock block state: " + bedrockBlockState.toBlockStateString());
                }
                final BlockState javaBlockState = BlockState.fromString(entry.getValue().getAsString());
                if (!this.javaBlockStates.containsKey(javaBlockState)) {
                    throw new RuntimeException("Unknown java block state: " + javaBlockState.toBlockStateString());
                }
                if (this.bedrockToJavaBlockStates.put(bedrockBlockState, javaBlockState) != null) {
                    throw new RuntimeException("Duplicate bedrock -> java block state mapping for " + bedrockBlockState.toBlockStateString());
                }
            }

            final JsonObject bedrockCustomBlockTagsJson = this.readJson("custom/block_tags.json");
            this.bedrockCustomBlockTags = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : bedrockCustomBlockTagsJson.entrySet()) {
                final String tagName = entry.getKey();
                for (JsonElement itemIdentifierJson : entry.getValue().getAsJsonArray()) {
                    final String bedrockIdentifier = itemIdentifierJson.getAsString();
                    if (!bedrockBlockStatesByIdentifier.containsKey(bedrockIdentifier)) {
                        throw new RuntimeException("Unknown bedrock block: " + bedrockIdentifier);
                    }
                    if (this.bedrockCustomBlockTags.put(bedrockIdentifier, tagName) != null) {
                        throw new RuntimeException("Duplicate bedrock custom block tag for " + bedrockIdentifier);
                    }
                }
            }

            final JsonObject bedrockBlockTraitsJson = this.readJson("bedrock/block_traits.json");
            this.bedrockBlockTraits = new HashMap<>(bedrockBlockTraitsJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockBlockTraitsJson.entrySet()) {
                final String traitName = entry.getKey();
                final JsonObject enabledStatesJson = entry.getValue().getAsJsonObject();
                final Map<String, Map<String, Set<String>>> traitStateProperties = new HashMap<>(enabledStatesJson.size());
                for (Map.Entry<String, JsonElement> enabledStatesEntry : enabledStatesJson.entrySet()) {
                    final String enabledStateName = enabledStatesEntry.getKey();
                    final JsonObject propertiesJson = enabledStatesEntry.getValue().getAsJsonObject();
                    final Map<String, Set<String>> properties = new HashMap<>(propertiesJson.size());
                    for (Map.Entry<String, JsonElement> propertiesEntry : propertiesJson.entrySet()) {
                        final String propertyName = propertiesEntry.getKey();
                        final JsonArray valuesJson = propertiesEntry.getValue().getAsJsonArray();
                        final Set<String> values = new LinkedHashSet<>(valuesJson.size());
                        for (JsonElement valueJson : valuesJson) {
                            if (!values.add(valueJson.getAsString())) {
                                throw new RuntimeException("Duplicate value for property " + propertyName + " in enabled state " + enabledStateName + " of trait " + traitName);
                            }
                        }
                        if (properties.put(propertyName, values) != null) {
                            throw new RuntimeException("Duplicate property " + propertyName + " in enabled state " + enabledStateName + " of trait " + traitName);
                        }
                    }
                    if (traitStateProperties.put(enabledStateName, properties) != null) {
                        throw new RuntimeException("Duplicate enabled state " + enabledStateName + " for trait " + traitName);
                    }
                }
                if (this.bedrockBlockTraits.put(traitName, traitStateProperties) != null) {
                    throw new RuntimeException("Duplicate bedrock block trait " + traitName);
                }
            }

            final JsonObject bedrockLegacyBlocksJson = this.readJson("bedrock/block_legacy_id_map.json");
            this.bedrockLegacyBlocks = HashBiMap.create(bedrockLegacyBlocksJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockLegacyBlocksJson.entrySet()) {
                this.bedrockLegacyBlocks.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().getAsInt());
            }

            this.buildLegacyBlockStateMappings();

            final JsonArray javaPreWaterloggedBlockStatesJson = this.readJson("custom/pre_waterlogged_blockstates.json", JsonArray.class);
            this.javaPreWaterloggedBlockStates = new IntOpenHashSet(javaPreWaterloggedBlockStatesJson.size());
            for (JsonElement stateJson : javaPreWaterloggedBlockStatesJson) {
                final BlockState javaBlockState = BlockState.fromString(stateJson.getAsString());
                if (!this.javaBlockStates.containsKey(javaBlockState)) {
                    throw new RuntimeException("Unknown java block state: " + javaBlockState.toBlockStateString());
                }
                this.javaPreWaterloggedBlockStates.add(this.javaBlockStates.get(javaBlockState).intValue());
            }

            this.javaFluidBlockStates = new IntOpenHashSet(this.javaPreWaterloggedBlockStates);
            for (Map.Entry<BlockState, Integer> entry : this.javaBlockStates.entrySet()) {
                if (entry.getKey().hasProperty("waterlogged", "true") || entry.getKey().namespacedIdentifier().equals("minecraft:lava")) {
                    this.javaFluidBlockStates.add(entry.getValue().intValue());
                }
            }

            final JsonObject javaPottedBlockStatesJson = this.readJson("custom/potted_blockstates.json");
            this.javaPottedBlockStates = new Int2IntOpenHashMap(javaPottedBlockStatesJson.size());
            for (Map.Entry<String, JsonElement> entry : javaPottedBlockStatesJson.entrySet()) {
                final BlockState javaBlockState = BlockState.fromString(entry.getKey());
                if (!this.javaBlockStates.containsKey(javaBlockState)) {
                    throw new RuntimeException("Unknown java block state: " + javaBlockState.toBlockStateString());
                }
                final BlockState javaPottedBlockState = BlockState.fromString(entry.getValue().getAsString());
                if (!this.javaBlockStates.containsKey(javaPottedBlockState)) {
                    throw new RuntimeException("Unknown java block state: " + javaPottedBlockState.toBlockStateString());
                }
                this.javaPottedBlockStates.put(this.javaBlockStates.get(javaBlockState).intValue(), this.javaBlockStates.get(javaPottedBlockState).intValue());
            }

            final CompoundTag javaHeightMapBlockStatesTag = this.readNBT("java/heightmap_blockstates.nbt");
            this.javaHeightMapBlockStates = new HashMap<>(javaHeightMapBlockStatesTag.size());
            for (Map.Entry<String, Tag> entry : javaHeightMapBlockStatesTag.getValue().entrySet()) {
                final IntSet blockStates = new IntOpenHashSet();
                final IntArrayTag blockStatesArrayTag = (IntArrayTag) entry.getValue();
                for (int blockState : blockStatesArrayTag.getValue()) {
                    blockStates.add(blockState);
                }
                this.javaHeightMapBlockStates.put(entry.getKey(), blockStates);
            }
        }

        { // Biomes
            this.bedrockBiomeDefinitions = SNBT.deserializeCompoundTag(this.readJson("bedrock/biome_definitions.json").toString()); // TODO: Jank

            final JsonObject bedrockBiomesJson = this.readJson("bedrock/biomes.json", JsonObject.class);
            this.bedrockBiomes = HashBiMap.create(bedrockBiomesJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockBiomesJson.entrySet()) {
                final String bedrockBiomeName = entry.getKey();
                if (!this.bedrockBiomeDefinitions.contains(Key.namespaced(bedrockBiomeName))) {
                    throw new RuntimeException("Unknown bedrock biome: " + bedrockBiomeName);
                }
                this.bedrockBiomes.put(bedrockBiomeName, entry.getValue().getAsInt());
            }

            for (String bedrockBiomeName : this.bedrockBiomeDefinitions.keySet()) {
                if (!this.bedrockBiomes.containsKey(Key.stripMinecraftNamespace(bedrockBiomeName))) {
                    throw new RuntimeException("Missing bedrock biome id mapping: " + bedrockBiomeName);
                }
            }

            this.javaBiomes = HashBiMap.create(this.bedrockBiomes.size());
            this.javaBiomes.put("the_void", 0);
            for (String bedrockBiomeName : this.bedrockBiomes.keySet()) {
                this.javaBiomes.put(bedrockBiomeName, this.javaBiomes.size());
            }

            final JsonObject bedrockToJavaBiomeExtraDataJson = this.readJson("custom/biome_extra_data.json");
            this.bedrockToJavaBiomeExtraData = new HashMap<>(bedrockToJavaBiomeExtraDataJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaBiomeExtraDataJson.entrySet()) {
                final String dataName = entry.getKey();
                final JsonObject extraDataJson = entry.getValue().getAsJsonObject();
                final Map<String, Object> extraData = new HashMap<>(extraDataJson.size());
                for (Map.Entry<String, JsonElement> extraDataEntry : extraDataJson.entrySet()) {
                    final JsonPrimitive primitive = extraDataEntry.getValue().getAsJsonPrimitive();
                    if (primitive.isString()) {
                        extraData.put(extraDataEntry.getKey(), primitive.getAsString());
                    } else if (primitive.isNumber()) {
                        extraData.put(extraDataEntry.getKey(), primitive.getAsNumber().intValue());
                    } else if (primitive.isBoolean()) {
                        extraData.put(extraDataEntry.getKey(), primitive.getAsBoolean());
                    } else {
                        throw new IllegalArgumentException("Unknown extra data type: " + extraDataEntry.getValue().getClass().getName());
                    }
                }
                this.bedrockToJavaBiomeExtraData.put(dataName, extraData);
            }
        }

        { // Items
            this.bedrockItemUpgrader = new ItemUpgrader();

            final JsonArray javaItemsJson = javaViaMappingJson.get("items").getAsJsonArray();
            this.javaItems = HashBiMap.create(javaItemsJson.size());
            for (int i = 0; i < javaItemsJson.size(); i++) {
                this.javaItems.put(Key.namespaced(javaItemsJson.get(i).getAsString()), i);
            }

            final JsonObject bedrockItemsJson = this.readJson("bedrock/items.json");
            final Set<String> bedrockItems = new HashSet<>(bedrockItemsJson.size());
            this.bedrockItems = HashBiMap.create(bedrockItemsJson.size()); // TODO: this can probs be used instead of the block + meta split
            this.bedrockBlockItems = new HashSet<>();
            this.bedrockMetaItems = new HashSet<>();
            for (Map.Entry<String, JsonElement> entry : bedrockItemsJson.entrySet()) {
                final JsonObject itemEntry = entry.getValue().getAsJsonObject();
                final String identifier = entry.getKey();
                final int id = itemEntry.get("id").getAsInt();
                bedrockItems.add(identifier);
                if (id <= ProtocolConstants.LAST_BLOCK_ITEM_ID) {
                    this.bedrockBlockItems.add(identifier);
                } else {
                    this.bedrockMetaItems.add(identifier);
                }

                this.bedrockItems.put(identifier, SNBT.deserializeCompoundTag(itemEntry.toString()));
            }

            final JsonObject bedrockItemTagsJson = this.readJson("bedrock/item_tags.json");
            this.bedrockItemTags = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : bedrockItemTagsJson.entrySet()) {
                final String tagName = entry.getKey();
                for (JsonElement itemIdentifierJson : entry.getValue().getAsJsonArray()) {
                    final String bedrockIdentifier = itemIdentifierJson.getAsString();
                    if (!bedrockItems.contains(bedrockIdentifier)) {
                        throw new RuntimeException("Unknown bedrock item: " + bedrockIdentifier);
                    }
                    if (!this.bedrockItemTags.containsKey(bedrockIdentifier)) {
                        this.bedrockItemTags.put(bedrockIdentifier, new HashSet<>());
                    }
                    if (!this.bedrockItemTags.get(bedrockIdentifier).add(tagName)) {
                        throw new RuntimeException("Duplicate bedrock item tag " + tagName + " for " + bedrockIdentifier);
                    }
                }
            }

            final JsonObject bedrockCustomItemTagsJson = this.readJson("custom/item_tags.json");
            this.bedrockCustomItemTags = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : bedrockCustomItemTagsJson.entrySet()) {
                final String tagName = entry.getKey();
                for (JsonElement itemIdentifierJson : entry.getValue().getAsJsonArray()) {
                    final String bedrockIdentifier = itemIdentifierJson.getAsString();
                    if (!bedrockItems.contains(bedrockIdentifier)) {
                        throw new RuntimeException("Unknown bedrock item: " + bedrockIdentifier);
                    }
                    if (this.bedrockCustomItemTags.put(bedrockIdentifier, tagName) != null) {
                        throw new RuntimeException("Duplicate bedrock custom item tag for " + bedrockIdentifier);
                    }
                }
            }

            final JsonObject bedrockToJavaItemMappingsJson = this.readJson("custom/item_mappings.json");
            this.bedrockToJavaBlockItems = new HashMap<>(bedrockToJavaItemMappingsJson.size());
            this.bedrockToJavaMetaItems = new HashMap<>(bedrockToJavaItemMappingsJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaItemMappingsJson.entrySet()) {
                final String bedrockIdentifier = entry.getKey();
                if (!bedrockItems.contains(bedrockIdentifier)) {
                    throw new RuntimeException("Unknown bedrock item: " + bedrockIdentifier);
                }
                final JsonObject definition = entry.getValue().getAsJsonObject();
                if (definition.has("block")) {
                    if (!this.bedrockBlockItems.contains(bedrockIdentifier)) {
                        throw new RuntimeException("Tried to register meta item as block item: " + bedrockIdentifier);
                    }
                    final JsonObject blockDefinition = definition.get("block").getAsJsonObject();
                    final Map<BlockState, JavaItemMapping> blockItems = new HashMap<>(blockDefinition.size());
                    this.bedrockToJavaBlockItems.put(bedrockIdentifier, blockItems);
                    final List<BlockState> allPossibleStates = new ArrayList<>();
                    for (Map.Entry<String, JsonElement> blockMapping : blockDefinition.entrySet()) {
                        final BlockState blockState = BlockState.fromString(blockMapping.getKey());
                        final String blockStateIdentifier = blockState.namespacedIdentifier();
                        final List<BlockState> blockStates = new ArrayList<>();
                        for (BedrockBlockState bedrockBlockState : bedrockBlockStatesByIdentifier.get(blockStateIdentifier)) {
                            if (!bedrockBlockState.properties().keySet().containsAll(blockState.properties().keySet())) {
                                throw new RuntimeException("Unknown bedrock block state property: " + blockState.properties().keySet() + " for " + blockStateIdentifier);
                            }
                            if (bedrockBlockState.properties().entrySet().containsAll(blockState.properties().entrySet())) {
                                blockStates.add(bedrockBlockState);
                            }
                            allPossibleStates.add(bedrockBlockState);
                        }
                        if (blockStates.isEmpty()) {
                            throw new RuntimeException("Unknown bedrock block state: " + blockState.toBlockStateString());
                        }
                        for (BlockState state : blockStates) {
                            if (blockItems.put(state, this.parseJavaItemData(blockMapping.getValue().getAsJsonObject())) != null) {
                                throw new RuntimeException("Duplicate bedrock -> java item mapping for " + bedrockIdentifier);
                            }
                        }
                    }

                    /*for (BlockState state : allPossibleStates) {
                        if (!blockItems.containsKey(state)) {
                            throw new RuntimeException("Missing bedrock -> java item mapping for " + state.toBlockStateString());
                        }
                    }*/
                } else if (definition.has("meta")) {
                    if (!this.bedrockMetaItems.contains(bedrockIdentifier)) {
                        throw new RuntimeException("Tried to register block item as meta item: " + bedrockIdentifier);
                    }
                    final JsonObject metaDefinition = definition.get("meta").getAsJsonObject();
                    final Map<Integer, JavaItemMapping> metaItems = new HashMap<>(metaDefinition.size());
                    this.bedrockToJavaMetaItems.put(bedrockIdentifier, metaItems);
                    for (Map.Entry<String, JsonElement> metaMapping : metaDefinition.entrySet()) {
                        Integer meta;
                        try {
                            meta = Integer.parseInt(metaMapping.getKey());
                        } catch (NumberFormatException e) {
                            meta = null;
                        }
                        if (metaItems.put(meta, this.parseJavaItemData(metaMapping.getValue().getAsJsonObject())) != null) {
                            throw new RuntimeException("Duplicate bedrock -> java item mapping for " + bedrockIdentifier + ":" + meta);
                        }
                    }
                    if (!metaItems.containsKey(null)) {
                        throw new RuntimeException("Missing bedrock -> java item mapping for " + bedrockIdentifier + ":null");
                    }
                } else {
                    throw new RuntimeException("Unknown item mapping definition: " + definition);
                }
            }

            for (Map.Entry<String, Map<Integer, JavaItemMapping>> entry : this.bedrockToJavaMetaItems.entrySet()) {
                final String bedrockIdentifier = entry.getKey();
                for (Map.Entry<Integer, JavaItemMapping> metaEntry : entry.getValue().entrySet()) {
                    final Integer meta = metaEntry.getKey();
                    if (meta != null) {
                        final String newBedrockIdentifier = this.bedrockItemUpgrader.upgradeMetaItem(bedrockIdentifier, meta);
                        if (newBedrockIdentifier != null) {
                            if (newBedrockIdentifier.equals(metaEntry.getValue().identifier())) {
                                throw new RuntimeException("Redundant bedrock -> java item mapping for " + bedrockIdentifier + ":" + meta);
                            } else {
                                throw new RuntimeException("Upgraded " + bedrockIdentifier + ":" + meta + " to " + newBedrockIdentifier + " but it was mapped to " + metaEntry.getValue().identifier());
                            }
                        }
                    }
                }
            }

            for (String bedrockIdentifier : bedrockItems) {
                if (!this.bedrockToJavaBlockItems.containsKey(bedrockIdentifier) && !this.bedrockToJavaMetaItems.containsKey(bedrockIdentifier)) {
                    throw new RuntimeException("Missing bedrock -> java item mapping for " + bedrockIdentifier);
                }
            }

            final JsonArray javaMenusJson = javaViaMappingJson.get("menus").getAsJsonArray();
            this.javaMenus = HashBiMap.create(javaMenusJson.size());
            for (int i = 0; i < javaMenusJson.size(); i++) {
                this.javaMenus.put(Key.namespaced(javaMenusJson.get(i).getAsString()), i);
            }

            final JsonArray javaSlotDisplaysJson = javaViaMappingJson.getAsJsonArray("slot_displays");
            this.javaSlotDisplays = HashBiMap.create(javaSlotDisplaysJson.size());
            for (int i = 0; i < javaSlotDisplaysJson.size(); i++) {
                this.javaSlotDisplays.put(Key.namespaced(javaSlotDisplaysJson.get(i).getAsString()), i);
            }

            final JsonObject bedrockToJavaContainersJson = this.readJson("custom/container_mappings.json");
            this.bedrockToJavaContainers = new EnumMap<>(ContainerType.class);
            final Set<ContainerType> unmappedContainerTypes = new HashSet<>();
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaContainersJson.entrySet()) {
                final ContainerType bedrockContainerType = ContainerType.valueOf(entry.getKey());
                if (entry.getValue().isJsonNull()) {
                    unmappedContainerTypes.add(bedrockContainerType);
                    continue;
                }
                final String javaIdentifier = entry.getValue().getAsString();
                this.bedrockToJavaContainers.put(bedrockContainerType, this.getJavaMenuId(javaIdentifier));
            }
            for (ContainerType containerType : ContainerType.values()) {
                if (!this.bedrockToJavaContainers.containsKey(containerType) && !unmappedContainerTypes.contains(containerType)) {
                    throw new RuntimeException("Missing bedrock -> java container mapping for " + containerType.name());
                }
            }
        }

        { // Entities
            final CompoundTag entityIdentifiersTag = this.readNBT("bedrock/entity_identifiers.nbt");
            final ListTag<CompoundTag> entityIdentifiersListTag = entityIdentifiersTag.getListTag("idlist", CompoundTag.class);
            this.bedrockEntities = HashBiMap.create(entityIdentifiersListTag.size());
            for (CompoundTag entry : entityIdentifiersListTag) {
                this.bedrockEntities.put(entry.getStringTag("id").getValue(), entry.getIntTag("rid").asInt());
            }

            final JsonObject entityDataTypesJson = this.readJson("bedrock/entity_data_types.json");
            this.bedrockEntityDataTypes = new EnumMap<>(ActorDataIDs.class);
            final Set<ActorDataIDs> unmappedEntityDataIds = EnumSet.noneOf(ActorDataIDs.class);
            for (Map.Entry<String, JsonElement> entry : entityDataTypesJson.entrySet()) {
                final ActorDataIDs entityDataId = ActorDataIDs.valueOf(entry.getKey());
                if (entry.getValue().isJsonNull()) {
                    unmappedEntityDataIds.add(entityDataId);
                    continue;
                }
                this.bedrockEntityDataTypes.put(entityDataId, DataItemType.valueOf(entry.getValue().getAsString()));
            }
            for (ActorDataIDs entityDataId : ActorDataIDs.values()) {
                if (!this.bedrockEntityDataTypes.containsKey(entityDataId) && !unmappedEntityDataIds.contains(entityDataId)) {
                    throw new RuntimeException("Missing bedrock entity data type mapping for " + entityDataId.name());
                }
            }

            {
                final JsonObject entityFlagMoLangQueryMappingsJson = this.readJson("bedrock/entity_flag_molang_query_mappings.json");
                this.bedrockEntityFlagMoLangQueries = new EnumMap<>(ActorFlags.class);
                final Set<ActorFlags> unmappedEntityFlags = EnumSet.noneOf(ActorFlags.class);
                for (Map.Entry<String, JsonElement> entry : entityFlagMoLangQueryMappingsJson.entrySet()) {
                    final ActorFlags entityFlag = ActorFlags.valueOf(entry.getKey());
                    if (entry.getValue().isJsonNull()) {
                        unmappedEntityFlags.add(entityFlag);
                        continue;
                    }
                    this.bedrockEntityFlagMoLangQueries.put(entityFlag, entry.getValue().getAsString());
                }
                for (ActorFlags entityFlag : ActorFlags.values()) {
                    if (!this.bedrockEntityFlagMoLangQueries.containsKey(entityFlag) && !unmappedEntityFlags.contains(entityFlag)) {
                        throw new RuntimeException("Missing bedrock MoLang query mapping for " + entityFlag.name());
                    }
                }
            }

            final JsonObject bedrockToJavaEntityMappingsJson = this.readJson("custom/entity_mappings.json");
            this.bedrockToJavaEntities = new HashMap<>(bedrockToJavaEntityMappingsJson.size());
            final Set<String> unmappedEntities = new HashSet<>();
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaEntityMappingsJson.entrySet()) {
                final String bedrockIdentifier = entry.getKey();
                if (!this.bedrockEntities.containsKey(bedrockIdentifier)) {
                    throw new RuntimeException("Unknown bedrock entity identifier: " + bedrockIdentifier);
                }
                if (entry.getValue().isJsonNull()) {
                    unmappedEntities.add(bedrockIdentifier);
                    continue;
                }
                final String javaIdentifier = entry.getValue().getAsString();
                EntityTypes26_2 javaEntityType = null;
                for (EntityTypes26_2 type : EntityTypes26_2.values()) {
                    if (!type.isAbstractType() && type.identifier().equals(javaIdentifier)) {
                        javaEntityType = type;
                        break;
                    }
                }
                if (javaEntityType == null) {
                    throw new RuntimeException("Unknown java entity identifier: " + javaIdentifier);
                }
                this.bedrockToJavaEntities.put(bedrockIdentifier, javaEntityType);
            }
            for (String bedrockIdentifier : this.bedrockEntities.keySet()) {
                if (!this.bedrockToJavaEntities.containsKey(bedrockIdentifier) && !unmappedEntities.contains(bedrockIdentifier)) {
                    throw new RuntimeException("Missing bedrock -> java entity mapping for " + bedrockIdentifier);
                }
            }

            final JsonArray javaBlockEntitiesJson = javaViaMappingJson.get("blockentities").getAsJsonArray();
            this.javaBlockEntities = HashBiMap.create(javaBlockEntitiesJson.size());
            for (int i = 0; i < javaBlockEntitiesJson.size(); i++) {
                this.javaBlockEntities.put(javaBlockEntitiesJson.get(i).getAsString(), i);
            }

            final JsonArray javaEntityAttributesJson = javaViaMappingJson.get("attributes").getAsJsonArray();
            this.javaEntityAttributes = HashBiMap.create(javaEntityAttributesJson.size());
            for (int i = 0; i < javaEntityAttributesJson.size(); i++) {
                this.javaEntityAttributes.put(Key.namespaced(javaEntityAttributesJson.get(i).getAsString()), i);
            }

            final JsonObject javaEntityDataFieldsJson = this.readJson("java/entity_data_fields.json");
            this.javaEntityDataFields = new EnumMap<>(EntityTypes26_2.class);
            for (Map.Entry<String, JsonElement> entry : javaEntityDataFieldsJson.entrySet()) {
                if (EnumUtil.getEnumConstantOrNull(EntityTypes26_2.class, entry.getKey()) == null) {
                    throw new RuntimeException("Unknown java entity type: " + entry.getKey());
                }
            }
            for (EntityTypes26_2 type : EntityTypes26_2.values()) {
                if (type.isAbstractType()) continue;
                final EntityTypes26_2 realType = type;
                final List<String> allEntityTypeFields = new ArrayList<>();
                do {
                    final JsonArray entityTypeFieldsJson = javaEntityDataFieldsJson.getAsJsonArray(type.name());
                    if (entityTypeFieldsJson != null) {
                        final List<String> entityTypeFields = new ArrayList<>(entityTypeFieldsJson.size());
                        for (JsonElement entry : entityTypeFieldsJson) {
                            if (allEntityTypeFields.contains(entry.getAsString()) || entityTypeFields.contains(entry.getAsString())) {
                                throw new IllegalStateException("Duplicate entity data field for " + realType.name() + ": " + entry.getAsString());
                            } else {
                                entityTypeFields.add(entry.getAsString());
                            }
                        }
                        allEntityTypeFields.addAll(0, entityTypeFields);
                    }
                } while ((type = (EntityTypes26_2) type.getParent()) != null);
                this.javaEntityDataFields.put(realType, allEntityTypeFields);
            }
        }

        { // Entity Effects
            final JsonArray javaEffectsJson = this.readJson("java/effects.json", JsonArray.class);
            this.javaEffects = HashBiMap.create(javaEffectsJson.size());
            for (int i = 0; i < javaEffectsJson.size(); i++) {
                this.javaEffects.put(javaEffectsJson.get(i).getAsString(), i);
            }

            final JsonArray bedrockEffectsJson = this.readJson("bedrock/effects.json", JsonArray.class);
            this.bedrockEffects = HashBiMap.create(bedrockEffectsJson.size());
            for (int i = 0; i < bedrockEffectsJson.size(); i++) {
                this.bedrockEffects.put(bedrockEffectsJson.get(i).getAsString(), i + 1);
            }

            final JsonObject bedrockToJavaEffectMappingsJson = this.readJson("custom/effect_mappings.json");
            this.bedrockToJavaEffects = new HashMap<>(bedrockToJavaEffectMappingsJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaEffectMappingsJson.entrySet()) {
                final String bedrockIdentifier = entry.getKey();
                if (!this.bedrockEffects.containsKey(bedrockIdentifier)) {
                    throw new IllegalStateException("Unknown bedrock effect: " + bedrockIdentifier);
                }
                final String javaIdentifier = entry.getValue().getAsString();
                if (!this.javaEffects.containsKey(javaIdentifier)) {
                    throw new IllegalStateException("Unknown java effect: " + javaIdentifier);
                }
                this.bedrockToJavaEffects.put(bedrockIdentifier, javaIdentifier);
            }
            for (String bedrockIdentifier : this.bedrockEffects.keySet()) {
                if (!this.bedrockToJavaEffects.containsKey(bedrockIdentifier)) {
                    throw new IllegalStateException("Missing bedrock -> java effect mapping for " + bedrockIdentifier);
                }
            }

            Map<String, String> inverse = new java.util.HashMap<>(bedrockToJavaEffects.size());
            bedrockToJavaEffects.forEach((k, v) -> inverse.put(v, k));
            this.javaToBedrockEffects = inverse;

        }

        { // World Effects
            final JsonArray javaSoundsJson = javaViaMappingJson.get("sounds").getAsJsonArray();
            this.javaSounds = HashBiMap.create(javaSoundsJson.size());
            for (int i = 0; i < javaSoundsJson.size(); i++) {
                this.javaSounds.put(Key.namespaced(javaSoundsJson.get(i).getAsString()), i);
            }

            final JsonArray javaParticlesJson = javaViaMappingJson.get("particles").getAsJsonArray();
            this.javaParticles = HashBiMap.create(javaParticlesJson.size());
            for (int i = 0; i < javaParticlesJson.size(); i++) {
                this.javaParticles.put(Key.namespaced(javaParticlesJson.get(i).getAsString()), i);
            }

            final JsonObject bedrockSoundsJson = this.readJson("bedrock/sounds.json");
            final Map<String, String> bedrockSounds = new HashMap<>(bedrockSoundsJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockSoundsJson.entrySet()) {
                bedrockSounds.put(entry.getKey(), entry.getValue().getAsString());
            }

            final JsonObject bedrockBlockSoundsJson = this.readJson("bedrock/block_sounds.json");
            this.bedrockBlockSounds = new HashMap<>(bedrockBlockSoundsJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockBlockSoundsJson.entrySet()) {
                this.bedrockBlockSounds.put(entry.getKey(), entry.getValue().getAsString());
            }

            final JsonObject bedrockLevelSoundEventMappingsJson = this.readJson("bedrock/level_sound_event_mappings.json");
            this.bedrockLevelSoundEvents = new HashMap<>();
            final Set<String> unmappedLevelSoundEvents = new HashSet<>();
            for (Map.Entry<String, JsonElement> entry : bedrockLevelSoundEventMappingsJson.entrySet()) {
                if (entry.getValue().isJsonNull()) {
                    unmappedLevelSoundEvents.add(entry.getKey());
                    continue;
                }
                final JsonObject soundData = entry.getValue().getAsJsonObject();
                final Map<String, SoundDefinitions.ConfiguredSound> soundEvents = new HashMap<>(soundData.size());
                for (Map.Entry<String, JsonElement> soundEventEntry : soundData.entrySet()) {
                    final String[] keySplit = soundEventEntry.getKey().split(":", 2);
                    if (keySplit[0].equals("entity")) {
                        if (!this.bedrockEntities.containsKey(keySplit[1])) {
                            throw new RuntimeException("Unknown bedrock entity: " + keySplit[1]);
                        }
                    } else if (keySplit[0].equals("block")) {
                        if (!this.bedrockBlockSounds.containsValue(keySplit[1])) {
                            throw new RuntimeException("Unknown bedrock block sound: " + keySplit[1]);
                        }
                    } else if (keySplit[0].isEmpty()) {
                        // No validation
                    } else {
                        throw new RuntimeException("Unknown bedrock level sound event definition: " + soundEventEntry.getKey());
                    }
                    final SoundDefinitions.ConfiguredSound configuredSound = SoundDefinitions.ConfiguredSound.fromJson(soundEventEntry.getValue().getAsJsonObject());
                    if (!bedrockSounds.containsKey(configuredSound.sound())) {
                        throw new RuntimeException("Unknown bedrock sound: " + configuredSound.sound());
                    }
                    if (soundEventEntry.getKey().isEmpty()) {
                        soundEvents.put(null, configuredSound);
                    } else {
                        soundEvents.put(keySplit[1], configuredSound);
                    }
                }
                this.bedrockLevelSoundEvents.put(entry.getKey(), soundEvents);
            }
            /*for (SharedTypes_Legacy_LevelSoundEvent levelSoundEvent : SharedTypes_Legacy_LevelSoundEvent.values()) {
                if (!this.bedrockLevelSoundEvents.containsKey(levelSoundEvent) && !unmappedLevelSoundEvents.contains(levelSoundEvent)) {
                    throw new RuntimeException("Missing bedrock -> java level sound event mapping for " + levelSoundEvent.name());
                }
            }*/

            final JsonObject bedrockNoteBlockInstrumentMappingsJson = this.readJson("custom/note_block_instrument_mappings.json");
            this.bedrockNoteBlockInstrumentSounds = new EnumMap<>(NoteBlockInstrument.class);
            for (Map.Entry<String, JsonElement> entry : bedrockNoteBlockInstrumentMappingsJson.entrySet()) {
                final NoteBlockInstrument instrument = NoteBlockInstrument.valueOf(entry.getKey());
                final String sound = entry.getValue().getAsString();
                if (!bedrockSounds.containsKey(sound)) {
                    throw new RuntimeException("Unknown bedrock sound: " + sound);
                }
                this.bedrockNoteBlockInstrumentSounds.put(instrument, sound);
            }
            for (NoteBlockInstrument noteBlockInstrument : NoteBlockInstrument.values()) {
                if (!this.bedrockNoteBlockInstrumentSounds.containsKey(noteBlockInstrument)) {
                    throw new RuntimeException("Missing bedrock -> java note block instrument mapping for " + noteBlockInstrument.name());
                }
            }

            final JsonObject bedrockToJavaSoundCategoryMappingsJson = this.readJson("custom/sound_category_mappings.json");
            final Map<String, SoundSource> bedrockToJavaSoundCategories = new HashMap<>(bedrockToJavaSoundCategoryMappingsJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaSoundCategoryMappingsJson.entrySet()) {
                final String bedrockName = entry.getKey();
                if (!bedrockSounds.containsValue(bedrockName)) {
                    throw new IllegalStateException("Unknown bedrock sound category: " + bedrockName);
                }
                final SoundSource javaCategory = SoundSource.valueOf(entry.getValue().getAsString());
                bedrockToJavaSoundCategories.put(bedrockName, javaCategory);
            }
            for (String categoryName : bedrockSounds.values()) {
                if (!bedrockToJavaSoundCategories.containsKey(categoryName)) {
                    throw new IllegalStateException("Missing bedrock -> java sound category mapping for " + categoryName);
                }
            }

            final JsonObject bedrockToJavaSoundMappingsJson = this.readJson("custom/sound_mappings.json");
            this.bedrockToJavaSounds = new HashMap<>(bedrockToJavaSoundMappingsJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaSoundMappingsJson.entrySet()) {
                final String bedrockIdentifier = entry.getKey();
                if (!bedrockSounds.containsKey(bedrockIdentifier)) {
                    throw new IllegalStateException("Unknown bedrock sound: " + bedrockIdentifier);
                }
                final String javaIdentifier = entry.getValue().getAsString();
                if (!this.javaSounds.containsKey(javaIdentifier)) {
                    throw new IllegalStateException("Unknown java sound: " + javaIdentifier);
                }
                final JavaSound javaSoundMapping = new JavaSound(this.javaSounds.get(javaIdentifier), javaIdentifier, bedrockToJavaSoundCategories.get(bedrockSounds.get(bedrockIdentifier)));
                this.bedrockToJavaSounds.put(bedrockIdentifier, javaSoundMapping);
            }
            for (String bedrockIdentifier : bedrockSounds.keySet()) {
                if (!this.bedrockToJavaSounds.containsKey(bedrockIdentifier)) {
                    throw new IllegalStateException("Missing bedrock -> java sound mapping for " + bedrockIdentifier);
                }
            }

            final JsonArray bedrockParticlesJson = this.readJson("bedrock/particles.json", JsonArray.class);
            final List<String> bedrockParticles = new ArrayList<>(bedrockParticlesJson.size());
            for (JsonElement particleJson : bedrockParticlesJson) {
                bedrockParticles.add(particleJson.getAsString());
            }

            final JsonObject bedrockToJavaParticleMappingsJson = this.readJson("custom/particle_mappings.json");
            this.bedrockToJavaParticles = new HashMap<>(bedrockToJavaParticleMappingsJson.size());
            final Set<String> unmappedParticles = new HashSet<>();
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaParticleMappingsJson.entrySet()) {
                final String bedrockIdentifier = entry.getKey();
                if (!bedrockParticles.contains(bedrockIdentifier)) {
                    throw new IllegalStateException("Unknown bedrock particle: " + bedrockIdentifier);
                }
                if (entry.getValue().isJsonNull()) {
                    unmappedParticles.add(bedrockIdentifier);
                } else if (entry.getValue().isJsonObject()) {
                    this.bedrockToJavaParticles.put(bedrockIdentifier, this.parseJavaParticle(entry.getValue().getAsJsonObject()));
                } else {
                    final String javaIdentifier = entry.getValue().getAsString();
                    if (!this.javaParticles.containsKey(javaIdentifier)) {
                        throw new IllegalStateException("Unknown java particle: " + javaIdentifier);
                    }
                    final JavaParticle javaParticleMapping = new JavaParticle(new Particle(this.javaParticles.get(javaIdentifier)), 0F, 0F, 0F, 0F, 0);
                    this.bedrockToJavaParticles.put(bedrockIdentifier, javaParticleMapping);
                }
            }
            for (String bedrockIdentifier : bedrockParticles) {
                if (!this.bedrockToJavaParticles.containsKey(bedrockIdentifier) && !unmappedParticles.contains(bedrockIdentifier)) {
                    throw new IllegalStateException("Missing bedrock -> java particle mapping for " + bedrockIdentifier);
                }
            }

            final JsonObject bedrockToJavaLevelEventMappingsJson = this.readJson("custom/level_event_mappings.json");
            this.bedrockToJavaLevelEvents = new EnumMap<>(LevelEvent.class);
            final Set<LevelEvent> unmappedLevelEvents = EnumSet.noneOf(LevelEvent.class);
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaLevelEventMappingsJson.entrySet()) {
                final LevelEvent levelEvent = LevelEvent.valueOf(entry.getKey());
                if (entry.getValue().isJsonNull()) {
                    unmappedLevelEvents.add(levelEvent);
                } else if (entry.getValue().isJsonObject()) {
                    final JsonObject mapping = entry.getValue().getAsJsonObject();
                    if (mapping.has("event")) {
                        final Integer data = mapping.has("data") ? mapping.get("data").getAsInt() : null;
                        final JavaLevelEvent javaLevelEvent = new JavaLevelEvent(net.raphimc.viabedrock.protocol.data.enums.java.LevelEvent.valueOf(mapping.get("event").getAsString()), data);
                        this.bedrockToJavaLevelEvents.put(levelEvent, javaLevelEvent);
                    } else if (mapping.has("sound")) {
                        final String bedrockSound = mapping.get("sound").getAsString();
                        if (!this.bedrockToJavaSounds.containsKey(bedrockSound)) {
                            throw new IllegalStateException("Unknown bedrock sound: " + bedrockSound);
                        }
                        if (mapping.has("event")) {
                            final Integer data = mapping.has("data") ? mapping.get("data").getAsInt() : null;
                            final JavaLevelEvent javaLevelEvent = new JavaLevelEvent(net.raphimc.viabedrock.protocol.data.enums.java.LevelEvent.valueOf(mapping.get("event").getAsString()), data);
                            this.bedrockToJavaLevelEvents.put(levelEvent, new JavaSoundLevelEvent(this.bedrockToJavaSounds.get(bedrockSound), javaLevelEvent));
                        } else {
                            this.bedrockToJavaLevelEvents.put(levelEvent, this.bedrockToJavaSounds.get(bedrockSound));
                        }
                    } else if (mapping.has("particle")) {
                        this.bedrockToJavaLevelEvents.put(levelEvent, this.parseJavaParticle(mapping));
                    } else {
                        throw new IllegalStateException("Unknown level event mapping: " + mapping);
                    }
                } else {
                    this.bedrockToJavaLevelEvents.put(levelEvent, new JavaLevelEvent(net.raphimc.viabedrock.protocol.data.enums.java.LevelEvent.valueOf(entry.getValue().getAsString()), null));
                }
            }
            for (LevelEvent levelEvent : LevelEvent.values()) {
                if (!this.bedrockToJavaLevelEvents.containsKey(levelEvent) && !unmappedLevelEvents.contains(levelEvent)) {
                    throw new RuntimeException("Missing bedrock -> java level event mapping for " + levelEvent.name());
                }
            }

            final JsonObject bedrockToJavaLevelEventParticleMappingsJson = this.readJson("custom/level_event_particle_mappings.json");
            this.bedrockToJavaLevelEventParticles = new EnumMap<>(ParticleType.class);
            final Set<ParticleType> unmappedParticleTypes = EnumSet.noneOf(ParticleType.class);
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaLevelEventParticleMappingsJson.entrySet()) {
                final ParticleType particleType = ParticleType.valueOf(entry.getKey());
                if (entry.getValue().isJsonNull()) {
                    unmappedParticleTypes.add(particleType);
                } else if (entry.getValue().isJsonObject()) {
                    this.bedrockToJavaLevelEventParticles.put(particleType, this.parseJavaParticle(entry.getValue().getAsJsonObject()));
                } else {
                    final String javaIdentifier = entry.getValue().getAsString();
                    if (!this.javaParticles.containsKey(javaIdentifier)) {
                        throw new IllegalStateException("Unknown java particle: " + javaIdentifier);
                    }
                    final JavaParticle javaParticleMapping = new JavaParticle(new Particle(this.javaParticles.get(javaIdentifier)), 0F, 0F, 0F, 0F, 0);
                    this.bedrockToJavaLevelEventParticles.put(particleType, javaParticleMapping);
                }
            }
            for (ParticleType particleType : ParticleType.values()) {
                if (!this.bedrockToJavaLevelEventParticles.containsKey(particleType) && !unmappedParticleTypes.contains(particleType)) {
                    throw new RuntimeException("Missing bedrock -> java level event particle mapping for " + particleType.name());
                }
            }
        }

        { // Other stuff
            final JsonObject bedrockToJavaExperimentalFeatureMappingsJson = this.readJson("custom/experimental_feature_mappings.json");
            this.bedrockToJavaExperimentalFeatures = HashBiMap.create(bedrockToJavaExperimentalFeatureMappingsJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaExperimentalFeatureMappingsJson.entrySet()) {
                this.bedrockToJavaExperimentalFeatures.put(entry.getKey(), entry.getValue().getAsString());
            }

            final CompoundTag javaBannerPatternRegistry = this.javaRegistries.getCompoundTag(RegistryKeys.BANNER_PATTERN);
            final JsonObject bedrockToJavaBannerPatternMappingsJson = this.readJson("custom/banner_pattern_mappings.json");
            this.bedrockToJavaBannerPatterns = HashBiMap.create(bedrockToJavaBannerPatternMappingsJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaBannerPatternMappingsJson.entrySet()) {
                final String javaIdentifier = entry.getValue().getAsString();
                if (!javaBannerPatternRegistry.contains(javaIdentifier)) {
                    throw new RuntimeException("Unknown java banner pattern: " + javaIdentifier);
                }
                this.bedrockToJavaBannerPatterns.put(entry.getKey(), javaIdentifier);
            }

            final CompoundTag javaPaintingVariantRegistry = this.javaRegistries.getCompoundTag(RegistryKeys.PAINTING_VARIANT);
            final JsonObject bedrockToJavaPaintingMappingsJson = this.readJson("custom/painting_mappings.json");
            this.bedrockToJavaPaintings = HashBiMap.create(bedrockToJavaPaintingMappingsJson.size());
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaPaintingMappingsJson.entrySet()) {
                final String javaIdentifier = entry.getValue().getAsString();
                if (!javaPaintingVariantRegistry.contains(javaIdentifier)) {
                    throw new RuntimeException("Unknown java painting: " + javaIdentifier);
                }
                this.bedrockToJavaPaintings.put(entry.getKey(), javaIdentifier);
            }

            final CompoundTag javaDamageTypeRegistry = this.javaRegistries.getCompoundTag(RegistryKeys.DAMAGE_TYPE);
            final JsonObject bedrockToJavaDamageCauseMappingsJson = this.readJson("custom/damage_cause_mappings.json");
            this.bedrockToJavaDamageCauses = new EnumMap<>(SharedTypes_Legacy_ActorDamageCause.class);
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaDamageCauseMappingsJson.entrySet()) {
                final SharedTypes_Legacy_ActorDamageCause damageCause = SharedTypes_Legacy_ActorDamageCause.valueOf(entry.getKey());
                final String javaIdentifier = entry.getValue().getAsString();
                if (!javaDamageTypeRegistry.contains(javaIdentifier)) {
                    throw new RuntimeException("Unknown java damage type: " + javaIdentifier);
                }
                this.bedrockToJavaDamageCauses.put(damageCause, javaIdentifier);
            }
            for (SharedTypes_Legacy_ActorDamageCause actorDamageCause : SharedTypes_Legacy_ActorDamageCause.values()) {
                if (!this.bedrockToJavaDamageCauses.containsKey(actorDamageCause)) {
                    throw new RuntimeException("Missing bedrock -> java damage cause mapping for " + actorDamageCause.name());
                }
            }

            final CompoundTag javaEnchantmentRegistry = this.javaRegistries.getCompoundTag(RegistryKeys.ENCHANTMENT);
            final JsonObject bedrockToJavaEnchantmentMappingsJson = this.readJson("custom/enchantment_mappings.json");
            this.bedrockToJavaEnchantments = new EnumMap<>(Enchant_Type.class);
            for (Map.Entry<String, JsonElement> entry : bedrockToJavaEnchantmentMappingsJson.entrySet()) {
                final Enchant_Type enchantType = Enchant_Type.valueOf(entry.getKey());
                final String javaIdentifier = entry.getValue().getAsString();
                if (!javaEnchantmentRegistry.contains(javaIdentifier)) {
                    throw new RuntimeException("Unknown java enchantment: " + javaIdentifier);
                }
                this.bedrockToJavaEnchantments.put(enchantType, javaIdentifier);
            }
            for (Enchant_Type enchantType : Enchant_Type.values()) {
                if (!this.bedrockToJavaEnchantments.containsKey(enchantType)) {
                    throw new RuntimeException("Missing bedrock -> java enchantment mapping for " + enchantType.name());
                }
            }
        }

        DataValues.validate();
    }

    public Map<ResourcePack.Key, ResourcePack> getBedrockResourcePacks() {
        return this.bedrockResourcePacks;
    }

    public Map<ResourcePack.Key, ResourcePack> getBedrockSkinPacks() {
        return this.bedrockSkinPacks;
    }

    public List<ResourcePack> getBedrockVanillaResourcePacks() {
        return this.bedrockVanillaResourcePacks;
    }

    public Map<String, Object> getBedrockGameRules() {
        return this.bedrockGameRules;
    }

    public CompoundTag getJavaRegistries() {
        return this.javaRegistries;
    }

    public CompoundTag getJavaTags() {
        return this.javaTags;
    }

    public BiMap<String, Integer> getJavaCommandArgumentTypes() {
        return this.javaCommandArgumentTypes;
    }

    public BlockStateUpgrader getBedrockBlockStateUpgrader() {
        return this.bedrockBlockStateUpgrader;
    }

    public BiMap<String, Integer> getJavaBlocks() {
        return this.javaBlocks;
    }

    public BiMap<BlockState, Integer> getJavaBlockStates() {
        return this.javaBlockStates;
    }

    public Set<BedrockBlockState> getBedrockBlockStates() {
        return this.bedrockBlockStates;
    }

    public Map<BlockState, BlockState> getBedrockToJavaBlockStates() {
        return this.bedrockToJavaBlockStates;
    }

    public Map<String, String> getBedrockCustomBlockTags() {
        return this.bedrockCustomBlockTags;
    }

    public Map<String, Map<String, Map<String, Set<String>>>> getBedrockBlockTraits() {
        return this.bedrockBlockTraits;
    }

    public BiMap<String, Integer> getBedrockLegacyBlocks() {
        return this.bedrockLegacyBlocks;
    }

    public Int2ObjectMap<BedrockBlockState> getBedrockLegacyBlockStates() {
        return this.bedrockLegacyBlockStates;
    }

    public IntSet getJavaPreWaterloggedBlockStates() {
        return this.javaPreWaterloggedBlockStates;
    }

    public IntSet getJavaFluidBlockStates() {
        return this.javaFluidBlockStates;
    }

    public Int2IntMap getJavaPottedBlockStates() {
        return this.javaPottedBlockStates;
    }

    public Map<String, IntSet> getJavaHeightMapBlockStates() {
        return this.javaHeightMapBlockStates;
    }

    public CompoundTag getBedrockBiomeDefinitions() {
        return this.bedrockBiomeDefinitions;
    }

    public BiMap<String, Integer> getBedrockBiomes() {
        return this.bedrockBiomes;
    }

    public BiMap<String, Integer> getJavaBiomes() {
        return this.javaBiomes;
    }

    public Map<String, Map<String, Object>> getBedrockToJavaBiomeExtraData() {
        return this.bedrockToJavaBiomeExtraData;
    }

    public ItemUpgrader getBedrockItemUpgrader() {
        return this.bedrockItemUpgrader;
    }

    public BiMap<String, Integer> getJavaItems() {
        return this.javaItems;
    }

    public BiMap<String, CompoundTag> getBedrockItems() {
        return this.bedrockItems;
    }

    public Set<String> getBedrockBlockItems() {
        return this.bedrockBlockItems;
    }

    public Set<String> getBedrockMetaItems() {
        return this.bedrockMetaItems;
    }

    public Map<String, Set<String>> getBedrockItemTags() {
        return this.bedrockItemTags;
    }

    public Map<String, String> getBedrockCustomItemTags() {
        return this.bedrockCustomItemTags;
    }

    public Map<String, Map<BlockState, JavaItemMapping>> getBedrockToJavaBlockItems() {
        return this.bedrockToJavaBlockItems;
    }

    public Map<String, Map<Integer, JavaItemMapping>> getBedrockToJavaMetaItems() {
        return this.bedrockToJavaMetaItems;
    }

    public Map<ContainerType, Integer> getBedrockToJavaContainers() {
        return this.bedrockToJavaContainers;
    }

    public int getJavaMenuId(final String javaIdentifier) {
        final String namespacedIdentifier = Key.namespaced(javaIdentifier);
        final Integer javaMenuId = this.javaMenus.get(namespacedIdentifier);
        if (javaMenuId == null) {
            throw new IllegalStateException("Unknown java menu: " + namespacedIdentifier);
        }
        return javaMenuId;
    }

    public int getJavaSlotDisplayId(final String javaIdentifier) {
        final String namespacedIdentifier = Key.namespaced(javaIdentifier);
        final Integer javaSlotDisplayId = this.javaSlotDisplays.get(namespacedIdentifier);
        if (javaSlotDisplayId == null) {
            throw new IllegalStateException("Unknown java slot display: " + namespacedIdentifier);
        }
        return javaSlotDisplayId;
    }

    public BiMap<String, Integer> getBedrockEntities() {
        return this.bedrockEntities;
    }

    public Map<ActorDataIDs, DataItemType> getBedrockEntityDataTypes() {
        return this.bedrockEntityDataTypes;
    }

    public Map<ActorFlags, String> getBedrockEntityFlagMoLangQueries() {
        return this.bedrockEntityFlagMoLangQueries;
    }

    public Map<String, EntityTypes26_2> getBedrockToJavaEntities() {
        return this.bedrockToJavaEntities;
    }

    public BiMap<String, Integer> getJavaBlockEntities() {
        return this.javaBlockEntities;
    }

    public BiMap<String, Integer> getJavaEntityAttributes() {
        return this.javaEntityAttributes;
    }

    public Map<EntityTypes26_2, List<String>> getJavaEntityDataFields() {
        return this.javaEntityDataFields;
    }

    public BiMap<String, Integer> getJavaEffects() {
        return this.javaEffects;
    }

    public BiMap<String, Integer> getBedrockEffects() {
        return this.bedrockEffects;
    }

    public Map<String, String> getBedrockToJavaEffects() {
        return this.bedrockToJavaEffects;
    }

    public Map<String, String> getJavaToBedrockEffects() {
        return this.javaToBedrockEffects;
    }

    public BiMap<String, Integer> getJavaSounds() {
        return this.javaSounds;
    }

    public BiMap<String, Integer> getJavaParticles() {
        return this.javaParticles;
    }

    public Map<String, String> getBedrockBlockSounds() {
        return this.bedrockBlockSounds;
    }

    public Map<String, Map<String, SoundDefinitions.ConfiguredSound>> getBedrockLevelSoundEvents() {
        return this.bedrockLevelSoundEvents;
    }

    public Map<NoteBlockInstrument, String> getBedrockNoteBlockInstrumentSounds() {
        return this.bedrockNoteBlockInstrumentSounds;
    }

    public Map<String, JavaSound> getBedrockToJavaSounds() {
        return this.bedrockToJavaSounds;
    }

    public Map<String, JavaParticle> getBedrockToJavaParticles() {
        return this.bedrockToJavaParticles;
    }

    public Map<LevelEvent, LevelEventMapping> getBedrockToJavaLevelEvents() {
        return this.bedrockToJavaLevelEvents;
    }

    public Map<ParticleType, JavaParticle> getBedrockToJavaLevelEventParticles() {
        return this.bedrockToJavaLevelEventParticles;
    }

    public BiMap<String, String> getBedrockToJavaExperimentalFeatures() {
        return this.bedrockToJavaExperimentalFeatures;
    }

    public BiMap<String, String> getBedrockToJavaBannerPatterns() {
        return this.bedrockToJavaBannerPatterns;
    }

    public BiMap<String, String> getBedrockToJavaPaintings() {
        return this.bedrockToJavaPaintings;
    }

    public Map<SharedTypes_Legacy_ActorDamageCause, String> getBedrockToJavaDamageCauses() {
        return this.bedrockToJavaDamageCauses;
    }

    public Map<Enchant_Type, String> getBedrockToJavaEnchantments() {
        return this.bedrockToJavaEnchantments;
    }

    @Override
    protected Logger getLogger() {
        return ViaBedrock.getPlatform().getLogger();
    }

    private CompoundTag readNBT(String file) {
        file = "assets/viabedrock/data/" + file;
        try (final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(file)) {
            if (inputStream == null) {
                this.getLogger().severe("Failed to open " + file);
                return null;
            }

            return NBTIO.readTag(new DataInputStream(new GZIPInputStream(inputStream)), TagLimiter.noop(), true, CompoundTag.class);
        } catch (IOException e) {
            this.getLogger().log(Level.SEVERE, "Failed to read " + file, e);
            return null;
        }
    }

    private JsonObject readJson(final String file) {
        return this.readJson(file, JsonObject.class);
    }

    private <T> T readJson(String file, final Class<T> classOfT) {
        file = "assets/viabedrock/data/" + file;
        try (final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(file)) {
            if (inputStream == null) {
                this.getLogger().severe("Failed to open " + file);
                return null;
            }

            return GsonUtil.getGson().fromJson(new InputStreamReader(inputStream), classOfT);
        } catch (IOException e) {
            this.getLogger().log(Level.SEVERE, "Failed to read " + file, e);
            return null;
        }
    }

    private JavaParticle parseJavaParticle(final JsonObject obj) {
        final String javaIdentifier = obj.get("particle").getAsString();
        if (!this.javaParticles.containsKey(javaIdentifier)) {
            throw new IllegalStateException("Unknown java particle: " + javaIdentifier);
        }
        final float offsetX = obj.has("offset_x") ? obj.get("offset_x").getAsFloat() : 0F;
        final float offsetY = obj.has("offset_y") ? obj.get("offset_y").getAsFloat() : 0F;
        final float offsetZ = obj.has("offset_z") ? obj.get("offset_z").getAsFloat() : 0F;
        final float speed = obj.has("speed") ? obj.get("speed").getAsFloat() : 0F;
        final int count = obj.has("count") ? obj.get("count").getAsInt() : 0;
        final Particle particle = new Particle(this.javaParticles.get(javaIdentifier));
        if (obj.has("arguments")) {
            for (JsonElement argument : obj.get("arguments").getAsJsonArray()) {
                final JsonObject argumentObject = argument.getAsJsonObject();
                final String type = argumentObject.get("type").getAsString();
                switch (type) {
                    case "var_int" -> particle.add(Types.VAR_INT, argumentObject.get("value").getAsInt());
                    case "float" -> particle.add(Types.FLOAT, argumentObject.get("value").getAsFloat());
                    case "double" -> particle.add(Types.DOUBLE, argumentObject.get("value").getAsDouble());
                    case "int" -> particle.add(Types.INT, argumentObject.get("value").getAsInt());
                    case "block_state" -> {
                        final BlockState javaBlockState = BlockState.fromString(argumentObject.get("value").getAsString());
                        if (!this.javaBlockStates.containsKey(javaBlockState)) {
                            throw new IllegalStateException("Unknown java block state: " + javaBlockState.toBlockStateString());
                        }
                        particle.add(Types.VAR_INT, this.javaBlockStates.get(javaBlockState));
                    }
                    case "item_stack" -> {
                        final String identifier = argumentObject.get("value").getAsString();
                        if (!this.javaItems.containsKey(identifier)) {
                            throw new IllegalStateException("Unknown java item: " + identifier);
                        }
                        particle.add(VersionedTypes.V26_2.item, new StructuredItem(this.javaItems.get(identifier), 1, ProtocolConstants.createStructuredDataContainer()));
                    }
                    default -> throw new IllegalStateException("Unknown particle argument type: " + type);
                }
            }
        }
        return new JavaParticle(particle, offsetX, offsetY, offsetZ, speed, count);
    }

    private void buildLegacyBlockStateMappings() {
        try (final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("assets/viabedrock/data/bedrock/block_id_meta_to_1_12_0_nbt.bin")) {
            if (inputStream == null) {
                this.getLogger().severe("Failed to open block_id_meta_to_1_12_0_nbt.bin");
                return;
            }
            final byte[] bytes = inputStream.readAllBytes();
            final ByteBuf buf = Unpooled.wrappedBuffer(bytes);

            this.bedrockLegacyBlockStates = new Int2ObjectOpenHashMap<>();
            final int blockCount = BedrockTypes.UNSIGNED_VAR_INT.read(buf);
            for (int i = 0; i < blockCount; i++) {
                final String identifier = BedrockTypes.STRING.read(buf).toLowerCase(Locale.ROOT);
                if (!this.bedrockLegacyBlocks.containsKey(identifier)) {
                    throw new RuntimeException("Unknown block identifier in block_id_meta_to_1_12_0_nbt.bin: " + identifier);
                }
                final int id = this.bedrockLegacyBlocks.get(identifier);

                final int metaCount = BedrockTypes.UNSIGNED_VAR_INT.read(buf);
                for (int i1 = 0; i1 < metaCount; i1++) {
                    final int metadata = BedrockTypes.UNSIGNED_VAR_INT.read(buf);
                    final CompoundTag tag = (CompoundTag) BedrockTypes.TAG_LE.read(buf);
                    this.bedrockBlockStateUpgrader.upgradeToLatest(tag);
                    final BedrockBlockState bedrockBlockState = BedrockBlockState.fromNbt(tag);
                    if (!this.bedrockBlockStates.contains(bedrockBlockState)) {
                        throw new RuntimeException("Legacy block state " + bedrockBlockState.toBlockStateString() + " is not mapped to a modern block state");
                    }

                    this.bedrockLegacyBlockStates.put(id << 6 | metadata & 63, bedrockBlockState);
                }
            }
        } catch (Exception e) {
            this.getLogger().log(Level.SEVERE, "Failed to read block_id_meta_to_1_12_0_nbt.bin", e);
            this.bedrockLegacyBlockStates = null;
        }
    }

    private JavaItemMapping parseJavaItemData(final JsonObject obj) {
        final String javaIdentifier = obj.get("java_id").getAsString();
        if (!this.javaItems.containsKey(javaIdentifier)) {
            throw new RuntimeException("Unknown java item: " + javaIdentifier);
        }

        final String javaName = obj.has("java_name") ? obj.get("java_name").getAsString() : null;
        CompoundTag javaTag = null;
        try {
            if (obj.has("java_tag")) {
                javaTag = SNBT.deserializeCompoundTag(obj.get("java_tag").getAsString());
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to parse java tag for " + javaIdentifier, e);
        }
        return new JavaItemMapping(this.javaItems.get(javaIdentifier), javaIdentifier, javaName, javaTag);
    }

    public sealed interface LevelEventMapping permits JavaSound, JavaParticle, JavaLevelEvent, JavaSoundLevelEvent {
    }

    public record JavaSound(int id, String identifier, SoundSource category) implements LevelEventMapping {
    }

    public record JavaParticle(Particle particle, float offsetX, float offsetY, float offsetZ, float speed, int count) implements LevelEventMapping {

        public JavaParticle withParticle(final Particle particle) {
            return new JavaParticle(particle, this.offsetX, this.offsetY, this.offsetZ, this.speed, this.count);
        }

        public JavaParticle withCount(final int count) {
            return new JavaParticle(this.particle, this.offsetX, this.offsetY, this.offsetZ, this.speed, count);
        }

    }

    public record JavaLevelEvent(net.raphimc.viabedrock.protocol.data.enums.java.LevelEvent levelEvent, Integer data) implements LevelEventMapping {
    }

    public record JavaSoundLevelEvent(JavaSound sound, JavaLevelEvent levelEvent) implements LevelEventMapping {
    }

    public record JavaItemMapping(int id, String identifier, String name, CompoundTag overrideTag) {
    }

}
