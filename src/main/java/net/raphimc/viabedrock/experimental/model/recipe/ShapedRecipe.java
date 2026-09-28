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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ShapedRecipe extends Recipe {

    private final ItemDescriptor[][] pattern; // TODO: Better representation?
    private final List<BedrockItem> results;
    private final boolean mirrored;

    public ShapedRecipe(String uniqueId, UUID recipeId, String recipeTag, int priority, ItemDescriptor[][] pattern, List<BedrockItem> results, boolean mirrored) {
        super(uniqueId, recipeId, recipeTag, priority);
        this.pattern = pattern;
        this.results = results;
        this.mirrored = mirrored;
    }

    public ItemDescriptor[][] getPattern() {
        return pattern;
    }

    public List<BedrockItem> getResults() {
        return results;
    }

    public boolean isMirrored() {
        return mirrored;
    }

    @Override
    public void writeJavaRecipeData(final PacketWrapper packet, final UserConnection user) {
        packet.write(Types.VAR_INT, 1); // Shaped recipe type
        packet.write(Types.VAR_INT, pattern[0].length); // Width
        packet.write(Types.VAR_INT, pattern.length); // Height
        packet.write(Types.VAR_INT, pattern[0].length * pattern.length); // Number of ingredients
        for (ItemDescriptor[] row : pattern) {
            for (ItemDescriptor descriptor : row) {
                descriptor.writeJavaIngredientData(packet, user);
            }
        }
        new ItemDescriptor.DefaultDescriptor(results.get(0).identifier(), results.get(0).auxValue()).writeJavaIngredientData(packet, user);
        new ItemDescriptor.InvalidDescriptor().writeJavaIngredientData(packet, user); //TODO: Crafting Station
    }

    @Override
    public String toString() {
        return "ShapedRecipe{" +
                "pattern=" + Arrays.deepToString(pattern) +
                ", results=" + results +
                ", mirrored=" + mirrored +
                "} " + super.toString();
    }

}
