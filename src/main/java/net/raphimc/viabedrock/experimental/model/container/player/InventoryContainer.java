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
package net.raphimc.viabedrock.experimental.model.container.player;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.StructuredItem;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import net.raphimc.viabedrock.experimental.model.container.ExperimentalContainer;
import net.raphimc.viabedrock.experimental.storage.ExperimentalInventoryTracker;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerID;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.InteractPacket_Action;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;
import net.raphimc.viabedrock.protocol.storage.EntityTracker;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

public class InventoryContainer extends ExperimentalContainer {

    private byte selectedHotbarSlot = 0;

    public InventoryContainer(final UserConnection user) {
        super(user, (byte) ContainerID.CONTAINER_ID_INVENTORY.getValue(), ContainerType.INVENTORY, null, null, 36);
    }

    public InventoryContainer(final UserConnection user, final byte containerId, final BlockPosition position, final InventoryContainer inventoryContainer) {
        super(user, containerId, inventoryContainer.type, inventoryContainer.title, position, inventoryContainer.items, inventoryContainer.validBlockTags);
        this.selectedHotbarSlot = inventoryContainer.selectedHotbarSlot;
    }

    @Override
    public FullContainerName getFullContainerName(int slot) {
        if (slot < 9) {
            return new FullContainerName(ContainerEnumName.HotbarContainer, null);
        }

        return new FullContainerName(ContainerEnumName.InventoryContainer, null);
    }

    @Override
    public Item[] getJavaItems() {
        final ExperimentalInventoryTracker inventoryTracker = this.user.get(ExperimentalInventoryTracker.class);
        final Item[] inventoryItems = super.getJavaItems();
        final Item[] armorItems = inventoryTracker.getArmorContainer().getActualJavaItems();
        final Item[] offhandItems = inventoryTracker.getOffhandContainer().getActualJavaItems();
        final ExperimentalContainer hudContainer = inventoryTracker.getHudContainer();

        final Item[] combinedItems = StructuredItem.emptyArray(46);
        System.arraycopy(armorItems, 0, combinedItems, 5, armorItems.length);
        System.arraycopy(inventoryItems, 9, combinedItems, 9, 27);
        System.arraycopy(inventoryItems, 0, combinedItems, 36, 9);
        System.arraycopy(offhandItems, 0, combinedItems, 45, offhandItems.length);
        for (int i = 0; i < 4; i++) {
            combinedItems[1 + i] = hudContainer.getJavaItem(28 + i);
        }
        return combinedItems;
    }

    @Override
    public boolean setItems(BedrockItem[] items) {
        if (items.length != this.size()) {
            final BedrockItem[] newItems = this.getItems();
            System.arraycopy(items, 0, newItems, 0, Math.min(items.length, newItems.length));
            items = newItems;
        }
        return super.setItems(items);
    }

    @Override
    public int javaSlot(final int slot) {
        if (slot < 9) {
            return 36 + slot;
        } else {
            return super.javaSlot(slot);
        }
    }

    @Override
    public int bedrockSlot(final int slot) {
        if (slot >= 36 && slot < 45) {
            return slot - 36;
        } else {
            return super.bedrockSlot(slot);
        }
    }

    @Override
    public byte javaContainerId() {
        return (byte) ContainerID.CONTAINER_ID_INVENTORY.getValue();
    }

    public byte getSelectedHotbarSlot() {
        return this.selectedHotbarSlot;
    }

    public BedrockItem getSelectedHotbarItem() {
        return this.getItem(this.selectedHotbarSlot);
    }

    public void sendSelectedHotbarSlotToClient() {
        final PacketWrapper setHeldSlot = PacketWrapper.create(ClientboundPackets26_1.SET_HELD_SLOT, this.user);
        setHeldSlot.write(Types.VAR_INT, (int) this.selectedHotbarSlot);
        setHeldSlot.send(BedrockProtocol.class);
    }

    public void setSelectedHotbarSlot(final byte slot, final PacketWrapper mobEquipment) {
        final BedrockItem oldItem = this.getItem(this.selectedHotbarSlot);
        final BedrockItem newItem = this.getItem(slot);
        this.selectedHotbarSlot = slot;
        this.onSelectedHotbarSlotChanged(oldItem, newItem, mobEquipment);
    }

    @Override
    protected void onSlotChanged(final int slot, final BedrockItem oldItem, final BedrockItem newItem) {
        super.onSlotChanged(slot, oldItem, newItem);
        if (slot == this.selectedHotbarSlot) {
            final PacketWrapper mobEquipment = PacketWrapper.create(ServerboundBedrockPackets.MOB_EQUIPMENT, this.user);
            this.onSelectedHotbarSlotChanged(oldItem, newItem, mobEquipment);
            mobEquipment.sendToServer(BedrockProtocol.class);
        }
    }

    private void onSelectedHotbarSlotChanged(final BedrockItem oldItem, final BedrockItem newItem, final PacketWrapper mobEquipment) {
        if (oldItem.isDifferent(newItem)) {
            final PacketWrapper interact = PacketWrapper.create(ServerboundBedrockPackets.INTERACT, this.user);
            interact.write(Types.UNSIGNED_BYTE, (short) InteractPacket_Action.InteractUpdate.getValue()); // action
            interact.write(BedrockTypes.UNSIGNED_VAR_LONG, 0L); // target entity runtime id
            interact.write(BedrockTypes.OPTIONAL_POSITION_3F, null); // position
            interact.sendToServer(BedrockProtocol.class);
        }

        mobEquipment.write(BedrockTypes.UNSIGNED_VAR_LONG, this.user.get(EntityTracker.class).getClientPlayer().runtimeId()); // entity runtime id
        mobEquipment.write(this.user.get(ItemRewriter.class).newItemType(), newItem); // item
        mobEquipment.write(Types.BYTE, this.selectedHotbarSlot); // slot
        mobEquipment.write(Types.BYTE, this.selectedHotbarSlot); // selected slot
        mobEquipment.write(Types.BYTE, this.containerId); // container id
    }

}
