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
import com.viaversion.viaversion.api.minecraft.item.Item;
import net.raphimc.viabedrock.experimental.model.container.ExperimentalContainer;
import net.raphimc.viabedrock.experimental.storage.ExperimentalInventoryTracker;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;

public abstract class InventoryRedirectContainer extends ExperimentalContainer {

    public InventoryRedirectContainer(final UserConnection user, final byte containerId, final ContainerType type, final int size) {
        super(user, containerId, type, null, null, size);
    }

    @Override
    public Item[] getJavaItems() {
        return this.user.get(ExperimentalInventoryTracker.class).getInventoryContainer().getJavaItems();
    }

    @Override
    public byte javaContainerId() {
        return this.user.get(ExperimentalInventoryTracker.class).getInventoryContainer().javaContainerId();
    }

    protected Item[] getActualJavaItems() {
        return super.getJavaItems();
    }

}
