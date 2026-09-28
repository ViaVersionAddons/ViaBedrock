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
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestInfo;
import net.raphimc.viabedrock.experimental.types.ExperimentalBedrockTypes;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.TextProcessingEventOrigin;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.List;

public class ItemStackRequestType extends Type<ItemStackRequestInfo> {

    public ItemStackRequestType() {
        super(ItemStackRequestInfo.class);
    }

    @Override
    public ItemStackRequestInfo read(ByteBuf buffer) {
        int requestId = BedrockTypes.VAR_INT.read(buffer);
        List<ItemStackRequestAction> actions = List.of(ExperimentalBedrockTypes.ITEM_STACK_REQUEST_ACTIONS.read(buffer));
        List<String> toFilter = List.of(BedrockTypes.STRING_ARRAY.read(buffer));
        TextProcessingEventOrigin origin = TextProcessingEventOrigin.getByValue(BedrockTypes.INT_LE.read(buffer));

        return new ItemStackRequestInfo(requestId, actions, toFilter, origin);
    }

    @Override
    public void write(ByteBuf buffer, ItemStackRequestInfo value) {
        BedrockTypes.VAR_INT.write(buffer, value.requestId());
        ExperimentalBedrockTypes.ITEM_STACK_REQUEST_ACTIONS.write(buffer, value.actions().toArray(new ItemStackRequestAction[0]));
        BedrockTypes.STRING_ARRAY.write(buffer, value.toFilter().toArray(new String[0]));
        BedrockTypes.INT_LE.write(buffer, value.origin().getValue());
    }
}
