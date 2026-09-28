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
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackResponseContainerInfo;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackResponseSlotInfo;
import net.raphimc.viabedrock.experimental.types.ExperimentalBedrockTypes;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.List;

public class ItemStackContainerResponseType extends Type<ItemStackResponseContainerInfo> {

    public ItemStackContainerResponseType() {
        super(ItemStackResponseContainerInfo.class);
    }

    @Override
    public ItemStackResponseContainerInfo read(ByteBuf buffer) {
        FullContainerName containerName = BedrockTypes.FULL_CONTAINER_NAME.read(buffer);

        List<ItemStackResponseSlotInfo> slots = List.of(ExperimentalBedrockTypes.ITEM_STACK_RESPONSE_SLOTS.read(buffer));

        return new ItemStackResponseContainerInfo(containerName, slots);
    }

    @Override
    public void write(ByteBuf buffer, ItemStackResponseContainerInfo value) {
        BedrockTypes.FULL_CONTAINER_NAME.write(buffer, value.containerName());

        ExperimentalBedrockTypes.ITEM_STACK_RESPONSE_SLOTS.write(buffer, value.slots().toArray(new ItemStackResponseSlotInfo[0]));
    }
}
