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
package net.raphimc.viabedrock.protocol.model.inventory;

import com.viaversion.viaversion.api.minecraft.BlockPosition;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.Position3f;

public interface InventoryTransactionData {

    // NormalTransactionData represents an inventory transaction data object for normal transactions, such as crafting. It has no content.
    record NormalTransactionData() implements InventoryTransactionData {
    }

    // MismatchTransactionData represents a mismatched inventory transaction's data object.
    record MismatchTransactionData() implements InventoryTransactionData {
    }

    // UseItemTransactionData represents an inventory transaction data object sent when the client uses an item on a block.
    record UseItemTransactionData(
        ItemUseActionType actionType,
        ItemUseTriggerType triggerType,
        BlockPosition blockPosition,
        int face,
        int hotbarSlot,
        HandSlot handSlot,
        BedrockItem itemInHand,
        Position3f playerPosition,
        Position3f clickPosition,
        int blockRuntimeId,
        ItemUsePredictedResult predictedResult,
        ItemUseClientCooldownState clientCooldownState
    ) implements InventoryTransactionData {
    }

    // UseItemOnEntityTransactionData represents an inventory transaction data object sent when the client uses an item on an entity.
    record UseItemOnEntityTransactionData(
        long entityRuntimeId,
        ItemUseOnActorActionType actionType,
        int hotbarSlot,
        HandSlot handSlot,
        BedrockItem itemInHand,
        Position3f playerPosition,
        Position3f clickPosition
    ) implements InventoryTransactionData {
    }

    // ReleaseItemTransactionData represents an inventory transaction data object sent when the client releases the item it was using,
    // for example when stopping while eating or stopping the charging of a bow.
    record ReleaseItemTransactionData(
        ItemReleaseActionType actionType,
        int hotbarSlot,
        BedrockItem itemInHand,
        Position3f headPosition,
        HandSlot handSlot
    ) implements InventoryTransactionData {
    }

}
