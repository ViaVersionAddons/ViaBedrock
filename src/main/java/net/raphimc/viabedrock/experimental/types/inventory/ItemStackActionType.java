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
package net.raphimc.viabedrock.experimental.types.inventory;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.experimental.types.ExperimentalBedrockTypes;
import net.raphimc.viabedrock.protocol.model.ItemEntry;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

public class ItemStackActionType extends Type<ItemStackRequestAction> {

    public ItemStackActionType() {
        super(ItemStackRequestAction.class);
    }

    @Override
    public ItemStackRequestAction read(ByteBuf buffer) {
        return null;
    }

    @Override
    public void write(ByteBuf buffer, ItemStackRequestAction value) {
        buffer.writeByte(value.getType().getValue());
        switch (value.getType()) {
            case Take -> {
                ItemStackRequestAction.TakeAction takeAction = (ItemStackRequestAction.TakeAction) value;

                buffer.writeByte(takeAction.count());
                ExperimentalBedrockTypes.ITEM_STACK_REQUEST_SLOT.write(buffer, takeAction.source());
                ExperimentalBedrockTypes.ITEM_STACK_REQUEST_SLOT.write(buffer, takeAction.destination());
            }
            case Place -> {
                ItemStackRequestAction.PlaceAction placeAction = (ItemStackRequestAction.PlaceAction) value;

                buffer.writeByte(placeAction.count());
                ExperimentalBedrockTypes.ITEM_STACK_REQUEST_SLOT.write(buffer, placeAction.source());
                ExperimentalBedrockTypes.ITEM_STACK_REQUEST_SLOT.write(buffer, placeAction.destination());
            }
            case Swap -> {
                ItemStackRequestAction.SwapAction swapAction = (ItemStackRequestAction.SwapAction) value;

                ExperimentalBedrockTypes.ITEM_STACK_REQUEST_SLOT.write(buffer, swapAction.source());
                ExperimentalBedrockTypes.ITEM_STACK_REQUEST_SLOT.write(buffer, swapAction.destination());
            }
            case Drop -> {
                ItemStackRequestAction.DropAction dropAction = (ItemStackRequestAction.DropAction) value;

                buffer.writeByte(dropAction.count());
                ExperimentalBedrockTypes.ITEM_STACK_REQUEST_SLOT.write(buffer, dropAction.item());
                buffer.writeBoolean(dropAction.randomly());
            }
            case Destroy -> {
                ItemStackRequestAction.DestroyAction destroyAction = (ItemStackRequestAction.DestroyAction) value;

                buffer.writeByte(destroyAction.count());
                ExperimentalBedrockTypes.ITEM_STACK_REQUEST_SLOT.write(buffer, destroyAction.item());
            }
            case Consume -> {
                ItemStackRequestAction.ConsumeAction consumeAction = (ItemStackRequestAction.ConsumeAction) value;

                buffer.writeByte(consumeAction.count());
                ExperimentalBedrockTypes.ITEM_STACK_REQUEST_SLOT.write(buffer, consumeAction.item());
            }
            case Create -> {
                ItemStackRequestAction.CreateAction createAction = (ItemStackRequestAction.CreateAction) value;

                buffer.writeByte(createAction.slot());
            }
            case ScreenBeaconPayment -> {
                ItemStackRequestAction.BeaconPaymentAction screenBeaconPaymentAction = (ItemStackRequestAction.BeaconPaymentAction) value;

                BedrockTypes.VAR_INT.write(buffer, screenBeaconPaymentAction.primaryEvent());
                BedrockTypes.VAR_INT.write(buffer, screenBeaconPaymentAction.secondaryEvent());
            }
            case CraftRecipe -> {
                ItemStackRequestAction.CraftRecipeAction craftRecipeAction = (ItemStackRequestAction.CraftRecipeAction) value;

                BedrockTypes.UNSIGNED_VAR_INT.write(buffer, craftRecipeAction.recipeNetworkId());
                buffer.writeByte(craftRecipeAction.numberOfRequestedCrafts());
            }
            case CraftRecipeAuto -> {
                ItemStackRequestAction.AutoCraftRecipeAction craftRecipeAutoAction = (ItemStackRequestAction.AutoCraftRecipeAction) value;

                BedrockTypes.UNSIGNED_VAR_INT.write(buffer, craftRecipeAutoAction.recipeNetworkId());
                buffer.writeByte(craftRecipeAutoAction.numberOfRequestedCrafts());
                buffer.writeByte(craftRecipeAutoAction.timesCrafted());
                BedrockTypes.ITEM_ENTRY_ARRAY.write(buffer, craftRecipeAutoAction.ingredients().toArray(new ItemEntry[0]));
            }
            case CraftCreative -> {
                ItemStackRequestAction.CraftCreativeAction craftCreativeAction = (ItemStackRequestAction.CraftCreativeAction) value;

                BedrockTypes.UNSIGNED_VAR_INT.write(buffer, craftCreativeAction.creativeItemNetworkId());
                buffer.writeByte(craftCreativeAction.numberOfRequestedCrafts());
            }
            case CraftRecipeOptional -> {
                ItemStackRequestAction.CraftRecipeOptionalAction craftRecipeOptionalAction = (ItemStackRequestAction.CraftRecipeOptionalAction) value;

                BedrockTypes.UNSIGNED_VAR_INT.write(buffer, craftRecipeOptionalAction.recipeNetworkId());
                BedrockTypes.INT_LE.write(buffer, craftRecipeOptionalAction.filteredStringIndex());
            }
            case ScreenHUDMineBlock -> {
                ItemStackRequestAction.MineBlockAction hudMineBlockAction = (ItemStackRequestAction.MineBlockAction) value;

                BedrockTypes.VAR_INT.write(buffer, hudMineBlockAction.hotbarSlot());
                BedrockTypes.VAR_INT.write(buffer, hudMineBlockAction.predictedDurability());
                BedrockTypes.VAR_INT.write(buffer, hudMineBlockAction.stackNetworkId());
            }
            case CraftRepairAndDisenchant -> {
                ItemStackRequestAction.CraftGrindstoneAction craftGrindstoneAction = (ItemStackRequestAction.CraftGrindstoneAction) value;

                BedrockTypes.UNSIGNED_VAR_INT.write(buffer, craftGrindstoneAction.recipeNetworkId());
                buffer.writeByte(craftGrindstoneAction.numberOfRequestedCrafts());
                BedrockTypes.VAR_INT.write(buffer, craftGrindstoneAction.repairCost());
            }
            case CraftLoom -> {
                ItemStackRequestAction.CraftLoomAction craftLoomAction = (ItemStackRequestAction.CraftLoomAction) value;

                BedrockTypes.STRING.write(buffer, craftLoomAction.patternId());
                buffer.writeByte(craftLoomAction.timesCrafted());
            }
            case CraftNonImplemented_DEPRECATEDASKTYLAING -> {}
            case CraftResults_DEPRECATEDASKTYLAING -> {
                ItemStackRequestAction.CraftResultsDeprecatedAction craftResultsAction = (ItemStackRequestAction.CraftResultsDeprecatedAction) value;

                BedrockTypes.ITEM_ENTRY_ARRAY.write(buffer, craftResultsAction.resultItems().toArray(new ItemEntry[0]));
                buffer.writeByte(craftResultsAction.timesCrafted());
            }

        }
    }
}
