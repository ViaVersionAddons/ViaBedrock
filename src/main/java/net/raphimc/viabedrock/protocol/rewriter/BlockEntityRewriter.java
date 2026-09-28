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

import com.viaversion.nbt.tag.ByteTag;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntityImpl;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.chunk.BedrockBlockEntity;
import net.raphimc.viabedrock.api.model.BedrockBlockState;
import net.raphimc.viabedrock.api.util.TextUtil;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.data.generated.bedrock.CustomBlockTags;
import net.raphimc.viabedrock.protocol.rewriter.blockentity.*;
import net.raphimc.viabedrock.protocol.storage.ResourcePackStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class BlockEntityRewriter {

    private static final Map<String, Rewriter> BLOCK_ENTITY_REWRITERS = new HashMap<>();

    private static final Rewriter NULL_REWRITER = (user, bedrockBlockEntity) -> null;
    private static final Rewriter NOOP_REWRITER = (user, bedrockBlockEntity) -> new BlockEntityImpl(bedrockBlockEntity.packedXZ(), bedrockBlockEntity.y(), -1, new CompoundTag());

    static {
        // TODO: Enhancement: Add missing block entities
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.CALIBRATED_SCULK_SENSOR, NOOP_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.COPPER_GOLEM_STATUE, NOOP_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.NOTE_BLOCK, NULL_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.PISTON, NOOP_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.MOVING_BLOCK, NULL_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.SCULK_SENSOR, NOOP_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.SCULK_SHRIEKER, NOOP_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.SHELF, NOOP_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.TRIAL_SPAWNER, NOOP_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.VAULT, NOOP_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.WORKBENCH, NULL_REWRITER);

        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.BANNER, new BannerBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.BARREL, new LootableContainerBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.BEACON, new BeaconBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.BED, new BedBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.BEEHIVE, new BeehiveBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.BELL, NOOP_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.BLAST_FURNACE, new FurnaceBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.BREWING_STAND, new BrewingStandBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.BRUSHABLE_BLOCK, new BrushableBlockBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.CAMPFIRE, new CampfireBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.CAULDRON, NULL_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.CHEST, new LootableContainerBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.CHISELED_BOOKSHELF, new ChiseledBookshelfBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.COMMAND_BLOCK, new CommandBlockBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.COMPARATOR, new ComparatorBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.CONDUIT, new ConduitBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.CRAFTER, new CrafterBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.CREAKING_HEART, new CreakingHeartBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.DAYLIGHT_DETECTOR, NOOP_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.DECORATED_POT, new DecoratedPotBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.DISPENSER, new LootableContainerBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.DROPPER, new LootableContainerBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.ENCHANTING_TABLE, new NamedBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.END_GATEWAY, new EndGatewayBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.END_PORTAL, NOOP_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.ENDER_CHEST, NOOP_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.FLOWER_POT, new FlowerPotBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.FURNACE, new FurnaceBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.HANGING_SIGN, new SignBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.HOPPER, new HopperBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.ITEM_FRAME, NULL_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.JIGSAW, new JigsawBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.JUKEBOX, new JukeboxBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.LECTERN, new LecternBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.LODESTONE, NULL_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.MOB_SPAWNER, new MobSpawnerBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.NETHER_REACTOR, NULL_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.SCULK_CATALYST, NOOP_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.SHELF, new ShelfBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.SHULKER_BOX, new LootableContainerBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.SIGN, new SignBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.SKULL, new SkullBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.SMOKER, new FurnaceBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.SPORE_BLOSSOM, NULL_REWRITER);
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.STRUCTURE_BLOCK, new StructureBlockBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.TRAPPED_CHEST, new LootableContainerBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.TRIAL_SPAWNER, new TrialSpawnerBlockEntityRewriter());
        BLOCK_ENTITY_REWRITERS.put(CustomBlockTags.VAULT, new VaultBlockEntityRewriter());

    }

    public static BlockEntity toJava(final UserConnection user, final int bedrockBlockStateId, final BedrockBlockEntity bedrockBlockEntity) {
        final BlockStateRewriter blockStateRewriter = user.get(BlockStateRewriter.class);
        if (bedrockBlockStateId == blockStateRewriter.bedrockId(BedrockBlockState.AIR)) {
            return null;
        }

        final String tag = blockStateRewriter.tag(bedrockBlockStateId);
        if (isBlockEntity(tag)) {
            final BlockEntity javaBlockEntity = BLOCK_ENTITY_REWRITERS.get(tag).toJava(user, bedrockBlockEntity);
            if (javaBlockEntity == null) return null;

            if (javaBlockEntity.tag() != null) {
                final int typeId = BedrockProtocol.MAPPINGS.getJavaBlockEntities().getOrDefault(tag, -1);
                if (typeId == -1) throw new IllegalStateException("Unknown java block entity type: " + tag);

                return javaBlockEntity.withTypeId(typeId);
            }

            return javaBlockEntity;
        } else {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing block entity translation for " + bedrockBlockStateId + " (" + tag + ")");
        }

        return null;
    }

    public static boolean isBlockEntity(final String tag) {
        return BLOCK_ENTITY_REWRITERS.containsKey(tag);
    }

    public interface Rewriter {

        BlockEntity toJava(final UserConnection user, final BedrockBlockEntity bedrockBlockEntity);

        default void copy(final CompoundTag fromTag, final CompoundTag toTag, final String key, final Class<?> expectedType) {
            this.copy(fromTag, toTag, key, key, expectedType);
        }

        default void copy(final CompoundTag fromTag, final CompoundTag toTag, final String fromKey, final String toKey, final Class<?> expectedType) {
            if (expectedType.isInstance(fromTag.get(fromKey))) {
                toTag.put(toKey, fromTag.get(fromKey).copy());
            }
        }

        default void copyCustomName(final UserConnection user, final CompoundTag fromTag, final CompoundTag toTag) {
            if (fromTag.get("CustomName") instanceof StringTag customNameTag) {
                toTag.put("CustomName", this.rewriteCustomName(user, customNameTag));
            }
        }

        default void copyItemList(final UserConnection user, final CompoundTag fromTag, final CompoundTag toTag) {
            if (fromTag.get("Items") instanceof ListTag<?> itemsTag && CompoundTag.class.equals(itemsTag.getElementType())) {
                toTag.put("Items", this.rewriteItemList(user, (ListTag<CompoundTag>) itemsTag));
            }
        }

        default void copyItem(final UserConnection user, final CompoundTag fromTag, final CompoundTag toTag, final String key) {
            this.copyItem(user, fromTag, toTag, key, key);
        }

        default void copyItem(final UserConnection user, final CompoundTag fromTag, final CompoundTag toTag, final String fromKey, final String toKey) {
            if (fromTag.get(fromKey) instanceof CompoundTag itemTag) {
                CompoundTag item = this.rewriteItem(user, itemTag);
                if (item != null) {
                    toTag.put(toKey, item);
                }
            }
        }

        default CompoundTag rewriteItem(final UserConnection user, final CompoundTag bedrockItemTag) {
            return user.get(ItemRewriter.class).javaItem(bedrockItemTag);
        }

        default ListTag<?> rewriteItemList(final UserConnection user, final ListTag<CompoundTag> bedrockItemList) {
            final ListTag<CompoundTag> javaItemList = new ListTag<>(CompoundTag.class);
            final ItemRewriter itemRewriter = user.get(ItemRewriter.class);
            for (CompoundTag bedrockItemTag : bedrockItemList) {
                final CompoundTag javaItemTag = itemRewriter.javaItem(bedrockItemTag);
                if (javaItemTag == null) continue;
                this.copy(bedrockItemTag, javaItemTag, "Slot", ByteTag.class);
                javaItemList.add(javaItemTag);
            }
            return javaItemList;
        }

        default StringTag rewriteCustomName(final UserConnection user, final StringTag textTag) {
            return new StringTag(TextUtil.stringToJson(user.get(ResourcePackStorage.class).getTexts().translate(textTag.getValue())));
        }

    }

}
