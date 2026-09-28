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
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackResponseSlotInfo;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

public class ItemStackSlotResponseType extends Type<ItemStackResponseSlotInfo> {

    public ItemStackSlotResponseType() {
        super(ItemStackResponseSlotInfo.class);
    }

    @Override
    public ItemStackResponseSlotInfo read(ByteBuf buffer) {

        byte requestedSlot = buffer.readByte();
        byte slot = buffer.readByte();
        byte amount = buffer.readByte();
        int itemNetId = BedrockTypes.VAR_INT.read(buffer);
        String customName = BedrockTypes.STRING.read(buffer);
        String filteredCustomName = BedrockTypes.STRING.read(buffer);
        int durability = BedrockTypes.VAR_INT.read(buffer);

        return new ItemStackResponseSlotInfo(requestedSlot, slot, amount, itemNetId, customName, filteredCustomName, durability);
    }

    @Override
    public void write(ByteBuf buffer, ItemStackResponseSlotInfo value) {
        buffer.writeByte(value.requestedSlot());
        buffer.writeByte(value.slot());
        buffer.writeByte(value.amount());
        BedrockTypes.VAR_INT.write(buffer, value.itemNetId());
        BedrockTypes.STRING.write(buffer, value.customName());
        BedrockTypes.STRING.write(buffer, value.filteredCustomName());
        BedrockTypes.VAR_INT.write(buffer, value.durability());
    }

}
