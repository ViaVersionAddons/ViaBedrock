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
package net.raphimc.viabedrock.protocol.rewriter;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.viaversion.nbt.tag.*;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.libs.fastutil.ints.*;
import com.viaversion.viaversion.libs.fastutil.objects.Object2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2ObjectOpenHashMap;
import com.viaversion.viaversion.util.Key;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.chunk.blockstate.BlockStateSanitizer;
import net.raphimc.viabedrock.api.model.BedrockBlockState;
import net.raphimc.viabedrock.api.model.BlockState;
import net.raphimc.viabedrock.api.util.BlockStateHasher;
import net.raphimc.viabedrock.api.util.CombinationUtil;
import net.raphimc.viabedrock.api.util.HashedPaletteComparator;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.model.BlockProperties;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class BlockStateRewriter implements StorableObject {

    private final Int2IntMap blockStateIdMappings = new Int2IntOpenHashMap(); // Bedrock -> Java
    private final Int2IntMap legacyBlockStateIdMappings = new Int2IntOpenHashMap(); // Bedrock -> Bedrock
    private final BiMap<BlockState, Integer> blockStateMappings = HashBiMap.create(); // Bedrock -> Bedrock
    private final Object2ObjectMap<String, IntSortedSet> validBlockStates = new Object2ObjectOpenHashMap<>(); // Bedrock -> Bedrock
    private final Int2ObjectMap<String> blockStateTags = new Int2ObjectOpenHashMap<>(); // Bedrock
    private final BlockStateSanitizer blockStateSanitizer;

    public BlockStateRewriter(final BlockProperties[] blockProperties, final boolean hashedRuntimeBlockIds) {
        this.blockStateIdMappings.defaultReturnValue(-1);
        this.legacyBlockStateIdMappings.defaultReturnValue(-1);

        final List<BedrockBlockState> bedrockBlockStates = this.getBedrockBlockStates(blockProperties);
        final Map<BlockState, Integer> javaBlockStates = BedrockProtocol.MAPPINGS.getJavaBlockStates();
        final Map<BlockState, BlockState> bedrockToJavaBlockStates = BedrockProtocol.MAPPINGS.getBedrockToJavaBlockStates();
        final Map<String, String> blockTags = BedrockProtocol.MAPPINGS.getBedrockCustomBlockTags();
        final Set<String> bedrockBlockIdentifiers = bedrockBlockStates.stream().map(BedrockBlockState::namespacedIdentifier).collect(Collectors.toSet());
        final List<BedrockBlockState> customBlockStates = new ArrayList<>();

        final Map<String, CompoundTag> effectiveBlockProperties = new HashMap<>();
        for (BlockProperties blockProperty : blockProperties) {
            final String identifier = blockProperty.name().toLowerCase(Locale.ROOT);
            if (bedrockBlockIdentifiers.contains(identifier)) {
                continue; // Bedrock client does not allow overriding vanilla block states
            }

            if (!effectiveBlockProperties.containsKey(identifier)) {
                effectiveBlockProperties.put(identifier, blockProperty.properties());
            }
        }

        for (Map.Entry<String, CompoundTag> blockProperty : effectiveBlockProperties.entrySet()) {
            if (!(blockProperty.getValue().get("vanilla_block_data") instanceof CompoundTag)) { // Bedrock client ignores blocks without this tag
                continue;
            }
            if (!(blockProperty.getValue().get("menu_category") instanceof CompoundTag)) { // Bedrock client crashes if this tag is missing
                throw new IllegalStateException("Missing menu_category tag for " + blockProperty.getKey());
            }

            final Map<String, Set<Tag>> propertiesMap = new LinkedHashMap<>();
            final ListTag<CompoundTag> properties = blockProperty.getValue().getListTag("properties", CompoundTag.class);
            if (properties != null) { // https://wiki.bedrock.dev/blocks/block-states.html
                for (CompoundTag property : properties) {
                    if (property.get("name") instanceof StringTag nameTag) {
                        final String name = nameTag.getValue();
                        if (property.get("enum") instanceof ListTag<?> enumTag) {
                            final Set<Tag> values = new LinkedHashSet<>();
                            for (Tag tag : enumTag) {
                                values.add(tag);
                            }
                            propertiesMap.put(name, values);
                        }
                    }
                }
            }
            final ListTag<CompoundTag> traits = blockProperty.getValue().getListTag("traits", CompoundTag.class);
            if (traits != null) { // https://wiki.bedrock.dev/blocks/block-traits.html
                for (CompoundTag trait : traits) {
                    if (trait.get("name") instanceof StringTag nameTag) {
                        final String name = Key.namespaced(nameTag.getValue());
                        final Map<String, Map<String, Set<String>>> traitStateProperties = BedrockProtocol.MAPPINGS.getBedrockBlockTraits().get(name);
                        if (traitStateProperties == null) {
                            throw new RuntimeException("Missing block trait state properties for " + name);
                        }

                        if (trait.get("enabled_states") instanceof CompoundTag enabledStatesTag) {
                            for (Map.Entry<String, Tag> tag : enabledStatesTag) {
                                final String key = Key.namespaced(tag.getKey());
                                final boolean enabled = tag.getValue() instanceof ByteTag && ((ByteTag) tag.getValue()).asByte() != 0;
                                if (enabled) {
                                    if (traitStateProperties.containsKey(key)) {
                                        for (Map.Entry<String, Set<String>> propertiesEntry : traitStateProperties.get(key).entrySet()) {
                                            final Set<Tag> values = new LinkedHashSet<>();
                                            for (String value : propertiesEntry.getValue()) {
                                                values.add(new StringTag(value));
                                            }
                                            propertiesMap.put(propertiesEntry.getKey(), values);
                                        }
                                    } else {
                                        throw new RuntimeException("Missing block trait state properties for trait " + name + " and enabled state " + key);
                                    }
                                }
                            }
                        } else {
                            throw new RuntimeException("Missing enabled_states tag for trait " + name);
                        }
                    }
                }
            }

            final List<CompoundTag> combinations = CombinationUtil.generateCombinations(propertiesMap).stream()
                .map(stringTagMap -> {
                    final CompoundTag combination = new CompoundTag();
                    for (Map.Entry<String, Tag> entry : stringTagMap.entrySet()) {
                        combination.put(entry.getKey(), entry.getValue().copy());
                    }
                    return combination;
                }).collect(Collectors.toList());
            if (combinations.isEmpty()) {
                combinations.add(new CompoundTag());
            }

            for (CompoundTag combination : combinations) {
                final CompoundTag blockStateTag = new CompoundTag();
                blockStateTag.putString("name", blockProperty.getKey());
                blockStateTag.put("states", combination);
                blockStateTag.putInt("network_id", BlockStateHasher.hash(blockStateTag));

                customBlockStates.add(BedrockBlockState.fromNbt(blockStateTag));
            }
        }

        bedrockBlockStates.addAll(customBlockStates);
        bedrockBlockStates.sort((a, b) -> HashedPaletteComparator.INSTANCE.compare(a.namespacedIdentifier(), b.namespacedIdentifier()));

        for (int i = 0; i < bedrockBlockStates.size(); i++) {
            final BedrockBlockState bedrockBlockState = bedrockBlockStates.get(i);
            int bedrockId = hashedRuntimeBlockIds ? bedrockBlockState.blockStateTag().getIntTag("network_id").asInt() : i;
            if (hashedRuntimeBlockIds && this.blockStateMappings.containsValue(bedrockId)) {
                bedrockId++; // Bedrock client increments the hash in case a collision occurs
            }

            this.blockStateMappings.put(bedrockBlockState, bedrockId);
            this.validBlockStates.computeIfAbsent(bedrockBlockState.namespacedIdentifier(), k -> new IntLinkedOpenHashSet()).add(bedrockId);

            if (blockTags.containsKey(bedrockBlockState.namespacedIdentifier())) {
                this.blockStateTags.put(bedrockId, blockTags.get(bedrockBlockState.namespacedIdentifier()));
            }

            if (bedrockToJavaBlockStates.containsKey(bedrockBlockState)) {
                final int javaId = javaBlockStates.get(bedrockToJavaBlockStates.get(bedrockBlockState));
                this.blockStateIdMappings.put(bedrockId, javaId);
            } else {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing bedrock -> java block state mapping: " + bedrockBlockState.toBlockStateString());
                final int javaId = javaBlockStates.get(bedrockToJavaBlockStates.get(BedrockBlockState.INFO_UPDATE));
                this.blockStateIdMappings.put(bedrockId, javaId);
            }
        }

        for (Int2ObjectMap.Entry<BedrockBlockState> entry : BedrockProtocol.MAPPINGS.getBedrockLegacyBlockStates().int2ObjectEntrySet()) {
            final int legacyId = entry.getIntKey() >> 6;
            final int legacyData = entry.getIntKey() & 63;
            if (legacyData > 15) {
                continue; // Dirty hack Mojang did in 1.12. Can be ignored safely as those values can't be used in chunk packets.
            }

            this.legacyBlockStateIdMappings.put(legacyId << 4 | legacyData & 15, this.blockStateMappings.getOrDefault(entry.getValue(), -1).intValue());
        }

        this.blockStateSanitizer = new BlockStateSanitizer(bedrockBlockStates);
    }

    private List<BedrockBlockState> getBedrockBlockStates(final BlockProperties[] definitions) {
        final Set<String> registered = new HashSet<>();
        for (final BlockProperties definition : definitions) {
            if (definition.properties().contains("vanilla_block_data")) {
                registered.add(definition.name().toLowerCase(Locale.ROOT));
            }
        }
        final List<BedrockBlockState> states = new ArrayList<>(BedrockProtocol.MAPPINGS.getBedrockBlockStates().size());
        for (final BedrockBlockState state : BedrockProtocol.MAPPINGS.getBedrockBlockStates()) {
            if (!BedrockProtocol.MAPPINGS.getBedrockDataDrivenBlocks().contains(state.namespacedIdentifier()) || registered.contains(state.namespacedIdentifier())) {
                states.add(state);
            }
        }
        return states;
    }

    public int bedrockId(final CompoundTag bedrockBlockStateTag) {
        final CompoundTag bedrockBlockStateTagClone = bedrockBlockStateTag.copy();
        try {
            BedrockProtocol.MAPPINGS.getBedrockBlockStateUpgrader().upgradeToLatest(bedrockBlockStateTagClone);
            this.blockStateSanitizer.sanitize(bedrockBlockStateTagClone);

            return this.bedrockId(BedrockBlockState.fromNbt(bedrockBlockStateTagClone));
        } catch (final Throwable e) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Error while rewriting block state tag: " + bedrockBlockStateTag, e);
            return this.bedrockId(BedrockBlockState.AIR);
        }
    }

    public int bedrockId(final BlockState bedrockBlockState) {
        return this.blockStateMappings.getOrDefault(bedrockBlockState, -1);
    }

    public BlockState blockState(final int bedrockBlockStateId) {
        return this.blockStateMappings.inverse().get(bedrockBlockStateId);
    }

    public int bedrockId(final int legacyBlockStateId) {
        return this.legacyBlockStateIdMappings.get(legacyBlockStateId);
    }

    public int javaId(final int bedrockBlockStateId) {
        return this.blockStateIdMappings.get(bedrockBlockStateId);
    }

    public int waterlog(final int javaBlockStateId) {
        if (BedrockProtocol.MAPPINGS.getJavaPreWaterloggedBlockStates().contains(javaBlockStateId)) {
            return javaBlockStateId;
        }

        final BlockState waterlogged = BedrockProtocol.MAPPINGS.getJavaBlockStates().inverse().get(javaBlockStateId).withProperty("waterlogged", "true");
        return BedrockProtocol.MAPPINGS.getJavaBlockStates().getOrDefault(waterlogged, -1);
    }

    public IntSortedSet validBlockStates(final String bedrockBlockIdentifier) {
        return this.validBlockStates.get(bedrockBlockIdentifier);
    }

    public String tag(final int bedrockBlockStateId) {
        return this.blockStateTags.get(bedrockBlockStateId);
    }

}
