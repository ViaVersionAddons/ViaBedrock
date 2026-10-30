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
package net.raphimc.viabedrock.protocol.types.inventory;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import io.netty.buffer.ByteBuf;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ComplexInventoryTransaction_Type;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.model.inventory.BedrockInventoryTransaction;
import net.raphimc.viabedrock.protocol.model.inventory.InventoryActionData;
import net.raphimc.viabedrock.protocol.model.inventory.InventoryTransactionData;
import net.raphimc.viabedrock.protocol.model.inventory.LegacySetItemSlotData;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;
import net.raphimc.viabedrock.protocol.types.InventoryTypes;

import java.util.List;

public class InventoryTransactionPacketType extends Type<BedrockInventoryTransaction> {

    private final UserConnection user;
    private final Type<InventoryActionData[]> inventoryActionDataType;

    public InventoryTransactionPacketType(final UserConnection user, final Type<InventoryActionData[]> inventoryActionDataType) {
        super(BedrockInventoryTransaction.class);
        this.user = user;
        this.inventoryActionDataType = inventoryActionDataType;
    }

    @Override
    public BedrockInventoryTransaction read(final ByteBuf buffer) {
        final ItemRewriter itemRewriter = this.user.get(ItemRewriter.class);
        if (itemRewriter == null) {
            throw new IllegalStateException("ItemRewriter not found for user " + this.user);
        }

        final int legacyRequestId = BedrockTypes.VAR_INT.read(buffer);
        LegacySetItemSlotData[] legacySlots = new LegacySetItemSlotData[0];
        if (buffer.readBoolean()) {
            if (legacyRequestId < -1 && (legacyRequestId & 1) == 0) {
                legacySlots = InventoryTypes.LEGACY_SET_ITEM_SLOT_DATA.read(buffer);
            }
        }

        final ComplexInventoryTransaction_Type type = ComplexInventoryTransaction_Type.getByValue(BedrockTypes.UNSIGNED_VAR_INT.read(buffer));
        final InventoryActionData[] actions = this.inventoryActionDataType.read(buffer);
        final InventoryTransactionData transactionData = switch (type) {
            case NormalTransaction -> new InventoryTransactionData.NormalTransactionData();
            case InventoryMismatch -> new InventoryTransactionData.MismatchTransactionData();
            case ItemUseTransaction -> new InventoryTransactionData.UseItemTransactionData(
                ItemUseActionType.getByValue(BedrockTypes.VAR_INT.read(buffer)),
                ItemUseTriggerType.getByValue(buffer.readByte()),
                BedrockTypes.BLOCK_POSITION.read(buffer),
                buffer.readByte(),
                BedrockTypes.VAR_INT.read(buffer),
                HandSlot.getByValue(buffer.readByte()),
                itemRewriter.newItemType().read(buffer),
                BedrockTypes.POSITION_3F.read(buffer),
                BedrockTypes.POSITION_3F.read(buffer),
                BedrockTypes.UNSIGNED_VAR_INT.read(buffer),
                ItemUsePredictedResult.getByValue(buffer.readByte()),
                ItemUseClientCooldownState.getByValue(buffer.readByte())
            );
            case ItemUseOnEntityTransaction -> new InventoryTransactionData.UseItemOnEntityTransactionData(
                BedrockTypes.UNSIGNED_VAR_LONG.read(buffer),
                ItemUseOnActorActionType.getByValue(BedrockTypes.VAR_INT.read(buffer)),
                BedrockTypes.VAR_INT.read(buffer),
                HandSlot.getByValue(buffer.readByte()),
                itemRewriter.newItemType().read(buffer),
                BedrockTypes.POSITION_3F.read(buffer),
                BedrockTypes.POSITION_3F.read(buffer)
            );
            case ItemReleaseTransaction -> new InventoryTransactionData.ReleaseItemTransactionData(
                ItemReleaseActionType.getByValue(BedrockTypes.VAR_INT.read(buffer)),
                BedrockTypes.VAR_INT.read(buffer),
                itemRewriter.newItemType().read(buffer),
                BedrockTypes.POSITION_3F.read(buffer),
                HandSlot.getByValue(buffer.readByte())
            );
        };

        return new BedrockInventoryTransaction(legacyRequestId, List.of(legacySlots), List.of(actions), type, transactionData);
    }

    @Override
    public void write(final ByteBuf buffer, final BedrockInventoryTransaction bedrockInventoryTransaction) {
        final ItemRewriter itemRewriter = this.user.get(ItemRewriter.class);
        if (itemRewriter == null) {
            throw new IllegalStateException("ItemRewriter not found for user " + this.user);
        }

        BedrockTypes.VAR_INT.write(buffer, bedrockInventoryTransaction.legacyRequestId());
        Types.BOOLEAN.write(buffer, bedrockInventoryTransaction.legacyRequestId() != 0);
        if (bedrockInventoryTransaction.legacyRequestId() != 0) {
            InventoryTypes.LEGACY_SET_ITEM_SLOT_DATA.write(buffer, bedrockInventoryTransaction.legacySlots().toArray(new LegacySetItemSlotData[0]));
        }

        BedrockTypes.UNSIGNED_VAR_INT.write(buffer, bedrockInventoryTransaction.transactionType().getValue());
        if (bedrockInventoryTransaction.actions() != null) { //TODO: Make actions list Optional
            this.inventoryActionDataType.write(buffer, bedrockInventoryTransaction.actions().toArray(new InventoryActionData[0]));
        } else {
            this.inventoryActionDataType.write(buffer, new InventoryActionData[0]);
        }
        switch (bedrockInventoryTransaction.transactionType()) {
            case NormalTransaction, InventoryMismatch -> {
                // No additional data to write
            }
            case ItemUseTransaction -> {
                final InventoryTransactionData.UseItemTransactionData data = (InventoryTransactionData.UseItemTransactionData) bedrockInventoryTransaction.transactionData();
                BedrockTypes.VAR_INT.write(buffer, data.actionType().getValue());
                buffer.writeByte(data.triggerType().getValue());
                BedrockTypes.BLOCK_POSITION.write(buffer, data.blockPosition());
                buffer.writeByte(data.face());
                BedrockTypes.VAR_INT.write(buffer, data.hotbarSlot());
                buffer.writeByte(data.handSlot().getValue());
                itemRewriter.newItemType().write(buffer, data.itemInHand());
                BedrockTypes.POSITION_3F.write(buffer, data.playerPosition());
                BedrockTypes.POSITION_3F.write(buffer, data.clickPosition());
                BedrockTypes.UNSIGNED_VAR_INT.write(buffer, data.blockRuntimeId());
                buffer.writeByte(data.predictedResult().getValue());
                buffer.writeByte(data.clientCooldownState().getValue());
            }
            case ItemUseOnEntityTransaction -> {
                final InventoryTransactionData.UseItemOnEntityTransactionData data = (InventoryTransactionData.UseItemOnEntityTransactionData) bedrockInventoryTransaction.transactionData();
                BedrockTypes.UNSIGNED_VAR_LONG.write(buffer, data.entityRuntimeId());
                BedrockTypes.VAR_INT.write(buffer, data.actionType().getValue());
                BedrockTypes.VAR_INT.write(buffer, data.hotbarSlot());
                buffer.writeByte(data.handSlot().getValue());
                itemRewriter.newItemType().write(buffer, data.itemInHand());
                BedrockTypes.POSITION_3F.write(buffer, data.playerPosition());
                BedrockTypes.POSITION_3F.write(buffer, data.clickPosition());
            }
            case ItemReleaseTransaction -> {
                final InventoryTransactionData.ReleaseItemTransactionData data = (InventoryTransactionData.ReleaseItemTransactionData) bedrockInventoryTransaction.transactionData();
                BedrockTypes.VAR_INT.write(buffer, data.actionType().getValue());
                BedrockTypes.VAR_INT.write(buffer, data.hotbarSlot());
                itemRewriter.newItemType().write(buffer, data.itemInHand());
                BedrockTypes.POSITION_3F.write(buffer, data.headPosition());
                buffer.writeByte(data.handSlot().getValue());
            }
        }
    }

}
