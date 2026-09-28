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

public class BrewingStandContainer extends ExperimentalContainer {

    public BrewingStandContainer(UserConnection user, byte containerId, TextComponent title, BlockPosition position) {
        super(user, containerId, ContainerType.BREWING_STAND, title, position, 5, CustomBlockTags.BREWING_STAND);
    }

    @Override
    public FullContainerName getFullContainerName(int slot) {
        return switch (slot) {
            case 0 -> new FullContainerName(ContainerEnumName.BrewingStandFuelContainer, null);
            case 1, 2, 3 -> new FullContainerName(ContainerEnumName.BrewingStandResultContainer, null);
            case 4 -> new FullContainerName(ContainerEnumName.BrewingStandInputContainer, null);
            default -> throw new IllegalArgumentException("Invalid slot for Brewing Container: " + slot);
        };
    }

    @Override
    public int javaSlot(final int slot) {
        return switch (slot) {
            case 0 -> 3;
            case 1, 2, 3 -> slot - 1;
            case 4 -> 4;
            default -> super.javaSlot(slot);
        };
    }

    @Override
    public int bedrockSlot(final int slot) {
        return switch (slot) {
            case 3 -> 0;
            case 0, 1, 2 -> slot + 1;
            case 4 -> 4;
            default -> super.bedrockSlot(slot);
        };
    }

    @Override
    public short translateContainerData(int containerData) {
        return switch (containerData) {
            case 0 -> 0; // Progress arrow
            case 1 -> 1; // Fuel progress
            default -> -1; // Unknown
        };
    }
}
