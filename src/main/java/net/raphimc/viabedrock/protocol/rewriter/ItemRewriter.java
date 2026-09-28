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
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.StoredObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.StructuredItem;
import com.viaversion.viaversion.api.type.OptionalType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.fastutil.ints.IntSortedSet;
import com.viaversion.viaversion.util.Key;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.BlockState;
import net.raphimc.viabedrock.api.resourcepack.definition.ItemDefinitions;
import net.raphimc.viabedrock.api.util.TextUtil;
import net.raphimc.viabedrock.experimental.rewriter.ExperimentalItemRewriter;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.data.BedrockMappingData;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ItemVersion;
import net.raphimc.viabedrock.protocol.data.generated.bedrock.CustomItemTags;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.ItemEntry;
import net.raphimc.viabedrock.protocol.rewriter.item.BundleItemRewriter;
import net.raphimc.viabedrock.protocol.rewriter.resourcepack.CustomAttachableResourceRewriter;
import net.raphimc.viabedrock.protocol.rewriter.resourcepack.CustomItemTextureResourceRewriter;
import net.raphimc.viabedrock.protocol.storage.ResourcePackStorage;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;
import net.raphimc.viabedrock.protocol.types.array.ArrayType;
import net.raphimc.viabedrock.protocol.types.item.BedrockItemType;
import net.raphimc.viabedrock.protocol.types.item.NetworkItemStackDescriptorType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ItemRewriter extends StoredObject {

    private static final Map<String, NbtRewriter> ITEM_NBT_REWRITERS = new HashMap<>();

    private final BiMap<String, Integer> items;
    private final Set<String> componentItems;
    private final Int2ObjectMap<IntSortedSet> blockItemValidBlockStates;
    private final Type<BedrockItem> itemType;
    private final Type<BedrockItem> optionalItemType;
    private final Type<BedrockItem[]> itemArrayType;
    private final Type<BedrockItem> itemInstanceType;
    private final Type<BedrockItem[]> itemInstanceArrayType;
    private final Type<BedrockItem> newItemType;
    private final Type<BedrockItem> optionalNewItemType;
    private final Type<BedrockItem[]> newItemArrayType;

    static {
        // TODO: Add missing item nbt rewriters
        ITEM_NBT_REWRITERS.put(CustomItemTags.BUNDLE, new BundleItemRewriter());
    }

    public ItemRewriter(final UserConnection user, final ItemEntry[] itemEntries) {
        super(user);

        this.items = HashBiMap.create(itemEntries.length);
        this.componentItems = new HashSet<>();
        for (ItemEntry itemEntry : itemEntries) {
            this.items.inverse().remove(itemEntry.id());
            this.items.put(itemEntry.identifier(), itemEntry.id());
            if (itemEntry.version() == ItemVersion.DataDriven || (itemEntry.version() == ItemVersion.None && itemEntry.componentBased())) {
                this.componentItems.add(itemEntry.identifier());
            }
        }
        final Set<String> blockItems = new HashSet<>(BedrockProtocol.MAPPINGS.getBedrockBlockItems());
        blockItems.removeIf(identifier -> !this.items.containsKey(identifier));

        this.blockItemValidBlockStates = new Int2ObjectOpenHashMap<>(blockItems.size());
        final BlockStateRewriter blockStateRewriter = this.user().get(BlockStateRewriter.class);
        for (String identifier : blockItems) {
            IntSortedSet validBlockStates = blockStateRewriter.validBlockStates(identifier);
            if (validBlockStates == null) {
                final String[] components = identifier.split(":", 2);
                if (components[1].startsWith("item.")) {
                    validBlockStates = blockStateRewriter.validBlockStates(components[0] + ':' + components[1].substring(5));
                }
            }
            if (validBlockStates != null) {
                this.blockItemValidBlockStates.put(this.items.get(identifier).intValue(), validBlockStates);
            } else {
                Via.getPlatform().getLogger().log(Level.WARNING, "Missing block for block item: " + identifier);
            }
        }

        this.itemType = new BedrockItemType(this.items.getOrDefault("minecraft:shield", 0), this.blockItemValidBlockStates, false, true);
        this.optionalItemType = new OptionalType<>(this.itemType);
        this.itemArrayType = new ArrayType<>(this.itemType, BedrockTypes.UNSIGNED_VAR_INT);

        this.itemInstanceType = new BedrockItemType(this.items.getOrDefault("minecraft:shield", 0), this.blockItemValidBlockStates, false, false);
        this.itemInstanceArrayType = new ArrayType<>(this.itemInstanceType, BedrockTypes.UNSIGNED_VAR_INT);
        this.newItemType = new NetworkItemStackDescriptorType(this.items.getOrDefault("minecraft:shield", 0), this.blockItemValidBlockStates, false);
        this.optionalNewItemType = new OptionalType<>(this.newItemType);
        this.newItemArrayType = new ArrayType<>(this.newItemType, BedrockTypes.UNSIGNED_VAR_INT);
    }

    public Item javaItem(final BedrockItem bedrockItem) {
        if (bedrockItem.isEmpty()) return StructuredItem.empty();

        final String identifier = this.items.inverse().get(bedrockItem.identifier());
        if (identifier == null) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing item identifier for id: " + bedrockItem.identifier());
            return StructuredItem.empty();
        }

        final BedrockMappingData.JavaItemMapping javaItemMapping;
        final Map<BlockState, BedrockMappingData.JavaItemMapping> blockItemMappings = BedrockProtocol.MAPPINGS.getBedrockToJavaBlockItems().get(identifier);
        if (blockItemMappings != null) {
            if (bedrockItem.blockRuntimeId() == 0) { // Manually constructed items might not have a valid block state set
                final IntSortedSet validBlockStates = this.blockItemValidBlockStates.get(bedrockItem.identifier());
                bedrockItem.setBlockRuntimeId(validBlockStates.firstInt());
            }
            javaItemMapping = blockItemMappings.get(this.user().get(BlockStateRewriter.class).blockState(bedrockItem.blockRuntimeId()));
        } else {
            final int meta = bedrockItem.data() & 0xFFFF;
            final String newIdentifier = BedrockProtocol.MAPPINGS.getBedrockItemUpgrader().upgradeMetaItem(identifier, meta);
            if (newIdentifier != null) {
                final Map<BlockState, BedrockMappingData.JavaItemMapping> newBlockItemMappings = BedrockProtocol.MAPPINGS.getBedrockToJavaBlockItems().get(newIdentifier);
                if (newBlockItemMappings != null) {
                    final IntSortedSet validBlockStates = this.blockItemValidBlockStates.get(bedrockItem.identifier());
                    javaItemMapping = newBlockItemMappings.get(this.user().get(BlockStateRewriter.class).blockState(validBlockStates.firstInt()));
                } else {
                    javaItemMapping = null;
                }
            } else {
                final Map<Integer, BedrockMappingData.JavaItemMapping> metaItemMappings = BedrockProtocol.MAPPINGS.getBedrockToJavaMetaItems().get(identifier);
                if (metaItemMappings != null) {
                    if (!metaItemMappings.containsKey(meta)) {
                        if (metaItemMappings.size() != 1 || meta != 0) {
                            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing meta: " + meta + " for item: " + identifier);
                        }
                        javaItemMapping = metaItemMappings.get(null);
                    } else {
                        javaItemMapping = metaItemMappings.get(meta);
                    }
                } else {
                    javaItemMapping = null;
                }
            }
        }

        final Item javaItem;
        if (javaItemMapping != null) {
            final StructuredDataContainer data = ProtocolConstants.createStructuredDataContainer();
            if (javaItemMapping.overrideTag() != null) {
                // javaTag.setValue(this.overrideTag.copy().getValue());
                // TODO: Update: Fix this
            }
            if (javaItemMapping.name() != null) {
                final ResourcePackStorage resourcePackStorage = this.user().get(ResourcePackStorage.class);
                data.set(StructuredDataKey.ITEM_NAME, TextUtil.stringToNbt(resourcePackStorage.getTexts().get(javaItemMapping.name())));
                data.set(StructuredDataKey.LORE, new Tag[]{TextUtil.stringToNbt("§7[ViaBedrock] Mapped item: " + identifier)});
            }
            javaItem = new StructuredItem(javaItemMapping.id(), bedrockItem.amount(), data);
        } else {
            final ResourcePackStorage resourcePackStorage = this.user().get(ResourcePackStorage.class);
            final ItemDefinitions.ItemDefinition itemDefinition = resourcePackStorage.getItems().get(identifier);
            final StructuredDataContainer data = ProtocolConstants.createStructuredDataContainer();

            if (itemDefinition != null) {
                if (itemDefinition.displayNameComponent() != null) {
                    data.set(StructuredDataKey.ITEM_NAME, TextUtil.stringToNbt(resourcePackStorage.getTexts().translate(itemDefinition.displayNameComponent())));
                } else if (this.componentItems.contains(identifier)) {
                    data.set(StructuredDataKey.ITEM_NAME, TextUtil.stringToNbt(resourcePackStorage.getTexts().get("item." + Key.stripMinecraftNamespace(identifier))));
                } else {
                    data.set(StructuredDataKey.ITEM_NAME, TextUtil.stringToNbt(resourcePackStorage.getTexts().get("item." + Key.stripMinecraftNamespace(identifier) + ".name")));
                }

                if (resourcePackStorage.getAttachables().attachables().containsKey(identifier) && resourcePackStorage.isLoadedOnJavaClient() && resourcePackStorage.getConverterData().containsKey("ca_" + identifier + "_default")) {
                    data.set(StructuredDataKey.ITEM_MODEL, CustomAttachableResourceRewriter.getItemModel(identifier));
                    data.set(StructuredDataKey.CUSTOM_MODEL_DATA1_21_4, CustomAttachableResourceRewriter.getCustomModelData("default"));
                } else if (itemDefinition.iconComponent() != null && resourcePackStorage.isLoadedOnJavaClient()) {
                    data.set(StructuredDataKey.ITEM_MODEL, CustomItemTextureResourceRewriter.getItemModel(itemDefinition.iconComponent()));
                    data.set(StructuredDataKey.CUSTOM_MODEL_DATA1_21_4, CustomItemTextureResourceRewriter.getCustomModelData("0"));
                } else {
                    data.set(StructuredDataKey.LORE, new Tag[]{TextUtil.stringToNbt("§7[ViaBedrock] Custom item: " + identifier)});
                }
            } else {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing bedrock -> java item mapping for " + identifier);
                data.set(StructuredDataKey.ITEM_NAME, TextUtil.stringToNbt("§cMissing item: " + identifier));
            }
            javaItem = new StructuredItem(BedrockProtocol.MAPPINGS.getJavaItems().get("minecraft:paper"), bedrockItem.amount(), data);
        }

        final CompoundTag bedrockTag = bedrockItem.tag();
        if (bedrockTag != null) {
            if (bedrockTag.get("display") instanceof CompoundTag display) {
                if (display.contains("Name")) { // Bedrock client defaults to empty string if the type is wrong
                    javaItem.dataContainer().set(StructuredDataKey.CUSTOM_NAME, TextUtil.stringToNbt(display.getString("Name", "")));
                }
            }
        }

        if (ViaBedrock.getConfig().shouldEnableExperimentalFeatures()) {
            ExperimentalItemRewriter.handleItem(this.user(), bedrockItem, bedrockTag, javaItem);
        }

        final String tag = BedrockProtocol.MAPPINGS.getBedrockCustomItemTags().get(identifier);
        if (ITEM_NBT_REWRITERS.containsKey(tag)) {
            ITEM_NBT_REWRITERS.get(tag).toJava(this.user(), bedrockItem, javaItem);
        }

        return javaItem;
    }

    public CompoundTag javaItem(final CompoundTag bedrockTag) {
        final CompoundTag javaTag = new CompoundTag();

        if (bedrockTag == null) {
            return null;
        }

        String bedrockId = bedrockTag.getString("Name");
        if (bedrockId == null || bedrockId.isEmpty()) {
            return null;
        }

        Integer id = this.items.get(bedrockId);
        if (id == null) {
            ViaBedrock.getPlatform().getLogger().warning("Could not find item " + bedrockId);
            return null;
        }

        BedrockItem item = new BedrockItem(
                id,
                (short) 0,
                bedrockTag.getByte("Count"),
                bedrockTag.getCompoundTag("tag")
        );
        if (item.isEmpty()) {
            return null;
        }

        Item javaItem = this.javaItem(item);

        String javaId = BedrockProtocol.MAPPINGS.getJavaItems().inverse().get(javaItem.identifier());
        javaTag.put("id", new StringTag(javaId));

        javaTag.put("count",new IntTag(javaItem.amount()));
        if (javaItem.tag() != null) {
            javaTag.put("components", javaItem.tag());
        }

        return javaTag;
    }

    public Item[] javaItems(final BedrockItem[] bedrockItems) {
        final Item[] javaItems = new Item[bedrockItems.length];
        for (int i = 0; i < bedrockItems.length; i++) {
            javaItems[i] = this.javaItem(bedrockItems[i]);
        }
        return javaItems;
    }

    public BedrockItem bedrockItem(final Item javaItem) {
        throw new UnsupportedOperationException("Translating Java items to Bedrock is not yet supported");
    }

    public BedrockItem[] bedrockItems(final Item[] javaItems) {
        final BedrockItem[] bedrockItems = new BedrockItem[javaItems.length];
        for (int i = 0; i < javaItems.length; i++) {
            bedrockItems[i] = this.bedrockItem(javaItems[i]);
        }
        return bedrockItems;
    }

    public int maxStackSize(final BedrockItem bedrockItem) {
        if (bedrockItem == null || bedrockItem.isEmpty()) {
            return 64;
        }

        final String identifier = this.items.inverse().get(bedrockItem.identifier());
        if (identifier == null) {
            return 64;
        }

        final ItemDefinitions.ItemDefinition itemDefinition = this.user().get(ResourcePackStorage.class).getItems().get(identifier);
        if (itemDefinition != null && itemDefinition.maxStackSize() != null) {
            return Math.max(1, itemDefinition.maxStackSize());
        }

        CompoundTag item = BedrockProtocol.MAPPINGS.getBedrockItems().get(identifier);
        return item.getInt("maxStackSize", 64);
    }

    public BiMap<String, Integer> getItems() {
        return this.items;
    }

    public Set<String> getComponentItems() {
        return this.componentItems;
    }

    public Type<BedrockItem> itemType() {
        return this.itemType;
    }

    public Type<BedrockItem> optionalItemType() {
        return this.optionalItemType;
    }

    public Type<BedrockItem[]> itemArrayType() {
        return this.itemArrayType;
    }

    public Type<BedrockItem> itemInstanceType() {
        return this.itemInstanceType;
    }

    public Type<BedrockItem[]> itemInstanceArrayType() {
        return this.itemInstanceArrayType;
    }

    public Type<BedrockItem> newItemType() {
        return this.newItemType;
    }

    public Type<BedrockItem> optionalNewItemType() {
        return this.optionalNewItemType;
    }

    public Type<BedrockItem[]> newItemArrayType() {
        return this.newItemArrayType;
    }

    public interface NbtRewriter {

        void toJava(final UserConnection user, final BedrockItem bedrockItem, final Item javaItem);

    }

}
