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
package net.raphimc.viabedrock.experimental.types;

import com.viaversion.viaversion.api.type.Type;
import net.raphimc.viabedrock.experimental.model.inventory.*;
import net.raphimc.viabedrock.experimental.model.recipe.ItemDescriptor;
import net.raphimc.viabedrock.experimental.types.inventory.*;
import net.raphimc.viabedrock.experimental.types.recipe.NetworkItemDescriptorType;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;
import net.raphimc.viabedrock.protocol.types.array.ArrayType;

public class ExperimentalBedrockTypes {

    public static final Type<LegacySetItemSlotData[]> LEGACY_SET_ITEM_SLOT_DATA = new ArrayType<>(new LegacySetItemSlotDataType(), BedrockTypes.UNSIGNED_VAR_INT);
    public static final Type<InventorySource> INVENTORY_SOURCE = new InventorySourcePacketType();

    public static final Type<ItemStackRequestInfo[]> ITEM_STACK_REQUESTS = new ArrayType<>(new ItemStackRequestType(), BedrockTypes.UNSIGNED_VAR_INT);
    public static final Type<ItemStackRequestAction[]> ITEM_STACK_REQUEST_ACTIONS = new ArrayType<>(new ItemStackActionType(), BedrockTypes.UNSIGNED_VAR_INT);
    public static final Type<ItemStackRequestSlotInfo> ITEM_STACK_REQUEST_SLOT = new ItemStackSlotRequestType();

    public static final Type<ItemStackResponseInfo[]> ITEM_STACK_RESPONSES = new ArrayType<>(new ItemStackResponseType(), BedrockTypes.UNSIGNED_VAR_INT);
    public static final Type<ItemStackResponseContainerInfo[]> ITEM_STACK_RESPONSE_CONTAINERS = new ArrayType<>(new ItemStackContainerResponseType(), BedrockTypes.UNSIGNED_VAR_INT);
    public static final Type<ItemStackResponseSlotInfo[]> ITEM_STACK_RESPONSE_SLOTS = new ArrayType<>(new ItemStackSlotResponseType(), BedrockTypes.UNSIGNED_VAR_INT);

    public static final Type<ItemDescriptor> ITEM_DESCRIPTOR_TYPE = new NetworkItemDescriptorType();
    public static final Type<ItemDescriptor[]> ITEM_DESCRIPTORS = new ArrayType<>(ITEM_DESCRIPTOR_TYPE, BedrockTypes.UNSIGNED_VAR_INT);

}
