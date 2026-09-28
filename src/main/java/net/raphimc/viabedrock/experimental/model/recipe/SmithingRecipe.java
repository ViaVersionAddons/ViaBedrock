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
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import net.raphimc.viabedrock.protocol.model.BedrockItem;

import java.util.UUID;

public class SmithingRecipe extends Recipe {

    private final ItemDescriptor template;
    private final ItemDescriptor baseIngredient;
    private final ItemDescriptor additionIngredient;
    private final BedrockItem result;

    public SmithingRecipe(String uniqueId, UUID recipeId, String recipeTag, int priority, ItemDescriptor template, ItemDescriptor baseIngredient, ItemDescriptor additionIngredient, BedrockItem result) {
        super(uniqueId, recipeId, recipeTag, priority);
        this.template = template;
        this.baseIngredient = baseIngredient;
        this.additionIngredient = additionIngredient;
        this.result = result;
    }

    @Override
    public void writeJavaRecipeData(final PacketWrapper packet, final UserConnection user) {
        packet.write(Types.VAR_INT, 4); // Smithing recipe type
        template.writeJavaIngredientData(packet, user);
        baseIngredient.writeJavaIngredientData(packet, user);
        additionIngredient.writeJavaIngredientData(packet, user);
        new ItemDescriptor.DefaultDescriptor(result.identifier(), result.auxValue()).writeJavaIngredientData(packet, user);
        new ItemDescriptor.InvalidDescriptor().writeJavaIngredientData(packet, user); //TODO: Crafting Station
    }

    @Override
    public String toString() {
        return "SmithingRecipe{" +
                "template=" + template +
                ", baseIngredient=" + baseIngredient +
                ", additionIngredient=" + additionIngredient +
                ", result=" + result +
                "} " + super.toString();
    }

    public  ItemDescriptor getTemplate() {
        return template;
    }

    public  ItemDescriptor getBaseIngredient() {
        return baseIngredient;
    }

    public  ItemDescriptor getAdditionIngredient() {
        return additionIngredient;
    }

    public  BedrockItem getResult() {
        return result;
    }

}
