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
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestSlotInfo;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

public class ItemStackSlotRequestType extends Type<ItemStackRequestSlotInfo> {

    public ItemStackSlotRequestType() {
        super(ItemStackRequestSlotInfo.class);
    }

    @Override
    public ItemStackRequestSlotInfo read(ByteBuf buffer) {
        FullContainerName container = BedrockTypes.FULL_CONTAINER_NAME.read(buffer);
        byte slot = buffer.readByte();
        int stackNetworkId = BedrockTypes.VAR_INT.read(buffer);

        return new ItemStackRequestSlotInfo(container, slot, stackNetworkId);
    }

    @Override
    public void write(ByteBuf buffer, ItemStackRequestSlotInfo value) {
        BedrockTypes.FULL_CONTAINER_NAME.write(buffer, value.container());
        buffer.writeByte(value.slot());
        BedrockTypes.VAR_INT.write(buffer, value.stackNetworkId());
    }
}
