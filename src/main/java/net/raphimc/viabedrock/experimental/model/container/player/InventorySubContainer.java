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
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.model.BedrockItem;

public abstract class InventorySubContainer extends InventoryRedirectContainer {

    public InventorySubContainer(final UserConnection user, final byte containerId, final ContainerType type, final int size) {
        super(user, containerId, type, size);
    }

    @Override
    public boolean setItems(BedrockItem[] items) {
        if (items.length != this.size()) {
            final BedrockItem[] newItems = this.getItems();
            System.arraycopy(items, 0, newItems, 0, Math.min(items.length, newItems.length));
            items = newItems;
        }
        return super.setItems(items);
    }

}
