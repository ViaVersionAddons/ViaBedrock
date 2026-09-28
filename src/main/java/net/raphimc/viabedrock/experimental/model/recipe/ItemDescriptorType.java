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

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public enum ItemDescriptorType {
    INVALID(0),
    DEFAULT(1),
    MOLANG(2),
    ITEM_TAG(3),
    DEFERRED(4),
    COMPLEX_ALIAS(5);

    private static final Int2ObjectMap<ItemDescriptorType> BY_VALUE = new Int2ObjectOpenHashMap<>();

    static {
        for (ItemDescriptorType value : values()) {
            if (!BY_VALUE.containsKey(value.value)) {
                BY_VALUE.put(value.value, value);
            }
        }
    }

    public static ItemDescriptorType getByValue(final int value) {
        return BY_VALUE.get(value);
    }

    public static ItemDescriptorType getByValue(final int value, final ItemDescriptorType fallback) {
        return BY_VALUE.getOrDefault(value, fallback);
    }

    public static ItemDescriptorType getByName(final String name) {
        for (ItemDescriptorType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static ItemDescriptorType getByName(final String name, final ItemDescriptorType fallback) {
        for (ItemDescriptorType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }

    private final int value;

    ItemDescriptorType(final ItemDescriptorType value) {
        this(value.value);
    }

    ItemDescriptorType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
