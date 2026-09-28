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
package net.raphimc.viabedrock.experimental.model.container.player;

import com.viaversion.viaversion.api.connection.UserConnection;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerID;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;

public class HudContainer extends InventoryRedirectContainer {

    public HudContainer(final UserConnection user) {
        super(user, (byte) ContainerID.CONTAINER_ID_PLAYER_ONLY_UI.getValue(), ContainerType.HUD, 54);
    }

    @Override
    public FullContainerName getFullContainerName(int slot) {
        // TODO: Crafting output slot
        if (slot == 0) {
            return new FullContainerName(ContainerEnumName.CursorContainer, null);
        } else if (slot >= 28 && slot <= 31) {
            return new FullContainerName(ContainerEnumName.CraftingInputContainer, null);
        } else if (slot == 50) {
            return new FullContainerName(ContainerEnumName.CraftingOutputPreviewContainer, null);
        } else {
            return new FullContainerName(ContainerEnumName.CursorContainer, null); // TODO: This should not happen
        }
    }

    @Override
    public boolean setItem(final int bedrockSlot, final BedrockItem item) {
        if (super.setItem(bedrockSlot, item)) {
            return bedrockSlot == 0 || (bedrockSlot >= 28 && bedrockSlot <= 31) || bedrockSlot == 50;
        } else {
            return false;
        }
    }

    @Override
    public int javaSlot(final int slot) {
        if (slot >= 28 && slot <= 31) {
            return slot - 27;
        } else if (slot == 50) {
            return 0;
        } else {
            return super.javaSlot(slot);
        }
    }

    @Override
    public int bedrockSlot(final int slot) {
        if (slot >= 1 && slot <= 4) {
            return slot + 27;
        } else if (slot == 0) {
            return 50;
        } else {
            return super.bedrockSlot(slot);
        }
    }

}
