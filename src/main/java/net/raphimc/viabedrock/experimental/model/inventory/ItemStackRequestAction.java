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
package net.raphimc.viabedrock.experimental.model.inventory;

import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ItemStackRequestActionType;
import net.raphimc.viabedrock.protocol.model.ItemEntry;

import java.util.List;

// See https://github.com/CloudburstMC/Protocol/blob/3.0/bedrock-codec/src/main/java/org/cloudburstmc/protocol/bedrock/data/inventory/itemstack/request/action/
public interface ItemStackRequestAction {

    ItemStackRequestActionType getType();

    /**
     * AutoCraftRecipeStackRequestActionData is sent by the client similarly to the CraftRecipeStackRequestActionData. The
     * only difference is that the recipe is automatically created and crafted by shift clicking the recipe book.
     */
    record AutoCraftRecipeAction(int recipeNetworkId, byte timesCrafted, List<ItemEntry> ingredients, int numberOfRequestedCrafts) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.CraftRecipeAuto;
        }
    }

    /**
     * BeaconPaymentStackRequestActionData is sent by the client when it submits an item to enable effects from a
     * beacon. These items will have been moved into the beacon item slot in advance.
     */
    record BeaconPaymentAction(int primaryEvent, int secondaryEvent) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.ScreenBeaconPayment;
        }
    }

    /**
     * ConsumeStackRequestAction is sent by the client when it uses an item to craft another item. The original
     * item is 'consumed'.
     */
    record ConsumeAction(int count, ItemStackRequestSlotInfo item) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.Consume;
        }
    }

    /**
     * CraftCreativeStackRequestActionData is sent by the client when it takes an item out of the creative inventory.
     * The item is thus not really crafted, but instantly created.
     */
    record CraftCreativeAction(int creativeItemNetworkId, int numberOfRequestedCrafts) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.CraftCreative;
        }
    }

    record CraftGrindstoneAction(int recipeNetworkId, int numberOfRequestedCrafts, int repairCost) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.CraftRepairAndDisenchant;
        }
    }

    record CraftLoomAction(String patternId, int timesCrafted) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.CraftLoom;
        }
    }

    /**
     * CraftNonImplementedStackRequestActionData is an action sent for inventory actions that aren't yet implemented
     * in the new system. These include, for example, anvils
     */
    record CraftNonImplementedAction() implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.CraftNonImplemented_DEPRECATEDASKTYLAING;
        }
    }

    /**
     * CraftRecipeStackRequestActionData is sent by the client the moment it begins crafting an item. This is the
     * first action sent, before the Consume and Create item stack request actions.
     * This action is also sent when an item is enchanted. Enchanting should be treated mostly the same way as
     * crafting, where the old item is consumed.
     */
    record CraftRecipeAction(int recipeNetworkId, int numberOfRequestedCrafts) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.CraftRecipe;
        }
    }

    /**
     * Called when renaming an item in an anvil or cartography table. Uses the filter strings present in the request.
     */
    record CraftRecipeOptionalAction(int recipeNetworkId, int filteredStringIndex) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.CraftRecipeOptional;
        }
    }

    /**
     * CraftResultsDeprecatedStackRequestAction is an additional, deprecated packet sent by the client after
     * crafting. It holds the final results and the amount of times the recipe was crafted. It shouldn't be used.
     * This action is also sent when an item is enchanted. Enchanting should be treated mostly the same way as
     * crafting, where the old item is consumed.
     */
    record CraftResultsDeprecatedAction(List<ItemEntry> resultItems, int timesCrafted) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.CraftResults_DEPRECATEDASKTYLAING;
        }
    }

    /**
     * CreateStackRequestActionData is sent by the client when an item is created through being used as part of a
     * recipe. For example, when milk is used to craft a cake, the buckets are leftover. The buckets are moved to
     * the slot sent by the client here.
     * Note that before this is sent, an action for consuming all items in the crafting table/grid is sent. Items
     * that are not fully consumed when used for a recipe should not be destroyed there, but instead, should be
     * turned into their respective resulting items.
     */
    record CreateAction(int slot) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.Create;
        }
    }

    /**
     * DestroyStackRequestActionData is sent by the client when it destroys an item in creative mode by moving it
     * back into the creative inventory.
     */
    record DestroyAction(int count, ItemStackRequestSlotInfo item) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.Destroy;
        }
    }

    /**
     * DropStackRequestActionData is sent by the client when it drops an item out of the inventory when it has its
     * inventory opened. This action is not sent when a player drops an item out of the hotbar using the Q button
     * (or the equivalent on mobile). The InventoryTransaction packet is still used for that action, regardless of
     * whether the item stack network IDs are used or not.
     */
    record DropAction(int count, ItemStackRequestSlotInfo item, boolean randomly) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.Drop;
        }
    }

    /**
     * MineBlockStackRequestActionData is sent by the client when it breaks a block.
     */
    record MineBlockAction(int hotbarSlot, int predictedDurability, int stackNetworkId) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.ScreenHUDMineBlock;
        }
    }

    /**
     * PlaceStackRequestAction is sent by the client to the server to place x amount of items from one slot into
     * another slot, such as when shift clicking an item in the inventory to move it around or when moving an item
     * in the cursor into a slot.
     */
    record PlaceAction(int count, ItemStackRequestSlotInfo source, ItemStackRequestSlotInfo destination) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.Place;
        }
    }

    /**
     * SwapStackRequestActionData is sent by the client to swap the item in its cursor with an item present in another
     * container. The two item stacks swap places.
     */
    record SwapAction(ItemStackRequestSlotInfo source, ItemStackRequestSlotInfo destination) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.Swap;
        }
    }

    /**
     * TakeStackRequestActionData is sent by the client to the server to take x amount of items from one slot in a
     * container to the cursor.
     */
    record TakeAction(int count, ItemStackRequestSlotInfo source, ItemStackRequestSlotInfo destination) implements ItemStackRequestAction {
        @Override
        public ItemStackRequestActionType getType() {
            return ItemStackRequestActionType.Take;
        }
    }

}
