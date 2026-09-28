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
import net.raphimc.viabedrock.protocol.model.FullContainerName;

public class ArmorContainer extends InventorySubContainer {

    public ArmorContainer(final UserConnection user) {
        super(user, (byte) ContainerID.CONTAINER_ID_ARMOR.getValue(), ContainerType.ARMOR, 4);
    }

    @Override
    public FullContainerName getFullContainerName(int slot) {
        return new FullContainerName(ContainerEnumName.ArmorContainer, null);
    }

    @Override
    public int javaSlot(final int slot) {
        return 5 + slot;
    }

    @Override
    public int bedrockSlot(final int slot) {
        return slot - 5;
    }

}
