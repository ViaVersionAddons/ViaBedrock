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
package net.raphimc.viabedrock.experimental.storage;

import com.viaversion.viaversion.api.connection.StoredObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public class InventoryRequestTracker extends StoredObject {

    Int2ObjectOpenHashMap<InventoryRequestStorage> requests = new Int2ObjectOpenHashMap<>();
    int requestIdCounter = -1;

    public InventoryRequestTracker(UserConnection user) {
        super(user);
    }

    public int nextRequestId() {
        return this.requestIdCounter -= 2; // Bedrock seems to use negative odd numbers for request IDs
    }

    public void addRequest(InventoryRequestStorage info) {
        this.requests.put(info.requestInfo().requestId(), info);
    }

    public InventoryRequestStorage getRequest(int requestId) {
        return this.requests.get(requestId);
    }

    public void removeRequest(int requestId) {
        this.requests.remove(requestId);
    }

}
