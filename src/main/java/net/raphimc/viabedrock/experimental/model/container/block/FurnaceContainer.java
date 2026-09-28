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
package net.raphimc.viabedrock.experimental.model.container.block;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import net.raphimc.viabedrock.experimental.model.container.ExperimentalContainer;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.data.generated.bedrock.CustomBlockTags;
import net.raphimc.viabedrock.protocol.model.FullContainerName;

public class FurnaceContainer extends ExperimentalContainer {

    public FurnaceContainer(UserConnection user, byte containerId, TextComponent title, BlockPosition position) {
        super(user, containerId, ContainerType.FURNACE, title, position, 3, CustomBlockTags.FURNACE);
    }

    public FurnaceContainer(UserConnection user, byte containerId, ContainerType type, TextComponent title, BlockPosition position, String... validBlockTags) {
        super(user, containerId, type, title, position, 3, validBlockTags);
    }

    @Override
    public FullContainerName getFullContainerName(int slot) {
        return switch (slot) {
            case 0 -> new FullContainerName(ContainerEnumName.FurnaceIngredientContainer, null);
            case 1 -> new FullContainerName(ContainerEnumName.FurnaceFuelContainer, null);
            case 2 -> new FullContainerName(ContainerEnumName.FurnaceResultContainer, null);
            default -> throw new IllegalArgumentException("Invalid slot for Furnace Container: " + slot);
        };
    }

    @Override
    public short translateContainerData(int containerData) {
        /*if (javaId == 3) { // TODO: Handle this properly
            //TODO: This doesnt seem to be sent by bedrock except once at the start of opening the furnace
            value = 200; // Java furnace progress max is always 200 ticks (Bedrock seems to always send 0 here)
        }*/

        return switch (containerData) {
            case 0 -> 2; // Progress arrow
            case 1 -> 0; // Fuel progress
            case 2 -> 1; // Max fuel progress
            case 3 -> 3; // Max progress arrow
            default -> -1; // Unknown
        };
    }

}
