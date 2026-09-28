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
package net.raphimc.viabedrock.experimental.types.recipe;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;
import net.raphimc.viabedrock.experimental.model.recipe.ItemDescriptor;
import net.raphimc.viabedrock.experimental.model.recipe.ItemDescriptorType;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

public class NetworkItemDescriptorType extends Type<ItemDescriptor> {

    public NetworkItemDescriptorType() {
        super(ItemDescriptor.class);
    }

    @Override
    public ItemDescriptor read(ByteBuf buffer) {
        ItemDescriptorType type = ItemDescriptorType.getByValue(buffer.readByte());
        ItemDescriptor result = switch (type) {
            case COMPLEX_ALIAS -> {
                String name = BedrockTypes.STRING.read(buffer);
                yield new ItemDescriptor.ComplexAliasDescriptor(name);
            }
            case DEFAULT -> {
                int itemId = buffer.readShortLE();
                int auxValue = itemId != 0 ? buffer.readShortLE() : 0;
                yield new ItemDescriptor.DefaultDescriptor(itemId, auxValue);
            }
            case DEFERRED -> {
                String fullName = BedrockTypes.STRING.read(buffer);
                int auxValue = buffer.readIntLE();
                yield new ItemDescriptor.DeferredDescriptor(fullName, auxValue);
            }
            case INVALID -> new ItemDescriptor.InvalidDescriptor();
            case ITEM_TAG -> {
                String itemTag = BedrockTypes.STRING.read(buffer);
                yield new ItemDescriptor.ItemTagDescriptor(itemTag);
            }
            case MOLANG -> {
                String tagExpression = BedrockTypes.STRING.read(buffer);
                int molangVersion = buffer.readUnsignedByte();
                yield new ItemDescriptor.MolangDescriptor(tagExpression, molangVersion);
            }
        };

        int amount = BedrockTypes.VAR_INT.read(buffer);

        return result.withAmount(amount);
    }

    @Override
    public void write(ByteBuf buffer, ItemDescriptor value) {
        buffer.writeByte(value.getType().getValue());
        switch (value.getType()) {
            case COMPLEX_ALIAS -> {
                ItemDescriptor.ComplexAliasDescriptor descriptor = (ItemDescriptor.ComplexAliasDescriptor) value;
                BedrockTypes.STRING.write(buffer, descriptor.name());
            }
            case DEFAULT -> {
                ItemDescriptor.DefaultDescriptor descriptor = (ItemDescriptor.DefaultDescriptor) value;
                buffer.writeShortLE(descriptor.itemId());
                if (descriptor.itemId() != 0) {
                    buffer.writeShortLE(descriptor.auxValue());
                }
            }
            case DEFERRED -> {
                ItemDescriptor.DeferredDescriptor descriptor = (ItemDescriptor.DeferredDescriptor) value;
                BedrockTypes.STRING.write(buffer, descriptor.fullName());
                buffer.writeIntLE(descriptor.auxValue());
            }
            case INVALID -> {
                // Nothing to write
            }
            case ITEM_TAG -> {
                ItemDescriptor.ItemTagDescriptor descriptor = (ItemDescriptor.ItemTagDescriptor) value;
                BedrockTypes.STRING.write(buffer, descriptor.itemTag());
            }
            case MOLANG -> {
                ItemDescriptor.MolangDescriptor descriptor = (ItemDescriptor.MolangDescriptor) value;
                BedrockTypes.STRING.write(buffer, descriptor.tagExpression());
                buffer.writeByte(descriptor.molangVersion());
            }
        }
        BedrockTypes.VAR_INT.write(buffer, value.amount());
    }
}
