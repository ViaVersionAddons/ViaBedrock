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
package net.raphimc.viabedrock.experimental.model.recipe;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.util.Key;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;

import java.util.Set;

public interface ItemDescriptor {

    ItemDescriptorType getType();

    boolean matchesItem(final UserConnection user, BedrockItem item);

    default int amount() {
        return 1;
    }

    ItemDescriptor withAmount(int amount);

    static boolean matchesAuxValue(final int auxValue, final BedrockItem item) {
        return auxValue == -1 || auxValue == Short.MAX_VALUE || auxValue == item.data() || auxValue == item.auxValue();
    }

    default void writeJavaIngredientData(final PacketWrapper packet, final UserConnection user) {
        throw new UnsupportedOperationException("Not implemented for " + getType());
    }

    record ComplexAliasDescriptor(String name, int amount) implements ItemDescriptor {

        public ComplexAliasDescriptor(final String name) {
            this(name, 1);
        }

        @Override
        public ItemDescriptorType getType() {
            return ItemDescriptorType.COMPLEX_ALIAS;
        }

        @Override
        public boolean matchesItem(UserConnection user, BedrockItem item) {
            // TODO
            return false;
        }

        @Override
        public void writeJavaIngredientData(final PacketWrapper packet, final UserConnection user) {
            //TODO
            packet.write(Types.VAR_INT, BedrockProtocol.MAPPINGS.getJavaSlotDisplayId("minecraft:empty")); // Slot Display Type
        }

        @Override
        public ItemDescriptor withAmount(final int amount) {
            return new ComplexAliasDescriptor(this.name, amount);
        }
    }

    record DefaultDescriptor(int itemId, int auxValue, int amount) implements ItemDescriptor {

        public DefaultDescriptor(final int itemId, final int auxValue) {
            this(itemId, auxValue, 1);
        }

        @Override
        public ItemDescriptorType getType() {
            return ItemDescriptorType.DEFAULT;
        }

        @Override
        public boolean matchesItem(UserConnection user, BedrockItem item) {
            return ((itemId == -1 || itemId == 0) && item.isEmpty())
                    || (!item.isEmpty() && item.identifier() == itemId && ItemDescriptor.matchesAuxValue(auxValue, item));
        }

        @Override
        public void writeJavaIngredientData(final PacketWrapper packet, final UserConnection user) {
            ItemRewriter itemRewriter = user.get(ItemRewriter.class);
            Item javaItem = itemRewriter.javaItem(new BedrockItem(itemId));
            if (javaItem == null) {
                throw new IllegalStateException("Could not find Java item for Bedrock ID: " + itemId);
            }

            packet.write(Types.VAR_INT, BedrockProtocol.MAPPINGS.getJavaSlotDisplayId("minecraft:item")); // Slot Display Type
            packet.write(Types.VAR_INT, javaItem.identifier()); // Item ID
        }

        @Override
        public ItemDescriptor withAmount(final int amount) {
            return new DefaultDescriptor(this.itemId, this.auxValue, amount);
        }

    }

    record DeferredDescriptor(String fullName, int auxValue, int amount) implements ItemDescriptor {

        public DeferredDescriptor(final String fullName, final int auxValue) {
            this(fullName, auxValue, 1);
        }

        @Override
        public ItemDescriptorType getType() {
            return ItemDescriptorType.DEFERRED;
        }

        @Override
        public boolean matchesItem(UserConnection user, BedrockItem item) {
            // TODO
            return false;
        }

        @Override
        public void writeJavaIngredientData(final PacketWrapper packet, final UserConnection user) {
            ItemRewriter itemRewriter = user.get(ItemRewriter.class);
            int itemId = itemRewriter.getItems().get(fullName); //TODO: Check if this is correct
            Item javaItem = itemRewriter.javaItem(new BedrockItem(itemId));
            if (javaItem == null) {
                throw new IllegalStateException("Could not find Java item for Bedrock ID: " + itemId);
            }

            packet.write(Types.VAR_INT, BedrockProtocol.MAPPINGS.getJavaSlotDisplayId("minecraft:item")); // Slot Display Type
            packet.write(Types.VAR_INT, javaItem.identifier()); // Item ID
        }

        @Override
        public ItemDescriptor withAmount(final int amount) {
            return new DeferredDescriptor(this.fullName, this.auxValue, amount);
        }

    }

    record InvalidDescriptor() implements ItemDescriptor {
        @Override
        public ItemDescriptorType getType() {
            return ItemDescriptorType.INVALID;
        }

        @Override
        public boolean matchesItem(UserConnection user, BedrockItem item) {
            return item.isEmpty();
        }

        @Override
        public void writeJavaIngredientData(final PacketWrapper packet, final UserConnection user) {
            packet.write(Types.VAR_INT, BedrockProtocol.MAPPINGS.getJavaSlotDisplayId("minecraft:empty")); // Slot Display Type
        }

        @Override
        public ItemDescriptor withAmount(final int amount) {
            return this;
        }

    }

    record ItemTagDescriptor(String itemTag, int amount) implements ItemDescriptor {

        public ItemTagDescriptor(final String itemTag) {
            this(itemTag, 1);
        }

        @Override
        public ItemDescriptorType getType() {
            return ItemDescriptorType.ITEM_TAG;
        }

        @Override
        public boolean matchesItem(UserConnection user, BedrockItem item) {
            if (item.isEmpty()) return false;
            ItemRewriter itemRewriter = user.get(ItemRewriter.class);
            String itemName = itemRewriter.getItems().inverse().get(item.identifier());
            Set<String> tags = BedrockProtocol.MAPPINGS.getBedrockItemTags().get(itemName);

            if (tags == null || tags.isEmpty()) return false;

            return tags.contains(itemTag);
        }

        @Override
        public void writeJavaIngredientData(final PacketWrapper packet, final UserConnection user) {
            packet.write(Types.VAR_INT, BedrockProtocol.MAPPINGS.getJavaSlotDisplayId("minecraft:tag")); // Slot Display Type
            //TODO: Convert to Java Tag properly
            packet.write(Types.IDENTIFIER, Key.of(itemTag)); // Item Tag
        }

        @Override
        public ItemDescriptor withAmount(final int amount) {
            return new ItemTagDescriptor(this.itemTag, amount);
        }

    }

    record MolangDescriptor(String tagExpression, int molangVersion, int amount) implements ItemDescriptor {

        public MolangDescriptor(final String tagExpression, final int molangVersion) {
            this(tagExpression, molangVersion, 1);
        }

        @Override
        public ItemDescriptorType getType() {
            return ItemDescriptorType.MOLANG;
        }

        @Override
        public boolean matchesItem(UserConnection user, BedrockItem item) {
            // TODO
            return false;
        }

        @Override
        public ItemDescriptor withAmount(final int amount) {
            return new MolangDescriptor(this.tagExpression, this.molangVersion, amount);
        }

    }

}