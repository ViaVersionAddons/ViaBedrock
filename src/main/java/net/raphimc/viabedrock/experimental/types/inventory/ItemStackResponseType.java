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
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackResponseInfo;
import net.raphimc.viabedrock.experimental.types.ExperimentalBedrockTypes;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ItemStackNetResult;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.List;

public class ItemStackResponseType extends Type<ItemStackResponseInfo> {

    public ItemStackResponseType() {
        super(ItemStackResponseInfo.class);
    }

    @Override
    public ItemStackResponseInfo read(ByteBuf buffer) {
        ItemStackNetResult result =  ItemStackNetResult.getByValue(buffer.readByte());
        int requestId = BedrockTypes.VAR_INT.read(buffer);

        if (result != ItemStackNetResult.Success) {
            return new ItemStackResponseInfo(result, requestId, null);
        }

        List<ItemStackResponseContainerInfo> containers = List.of(ExperimentalBedrockTypes.ITEM_STACK_RESPONSE_CONTAINERS.read(buffer));

        return new ItemStackResponseInfo(result, requestId, containers);
    }

    @Override
    public void write(ByteBuf buffer, ItemStackResponseInfo value) {
        buffer.writeByte(value.result().getValue());
        BedrockTypes.VAR_INT.write(buffer, value.requestId());

        if (value.result() != ItemStackNetResult.Success) {
            return;
        }

        ExperimentalBedrockTypes.ITEM_STACK_RESPONSE_CONTAINERS.write(buffer, value.containers().toArray(new ItemStackResponseContainerInfo[0]));
    }
}
