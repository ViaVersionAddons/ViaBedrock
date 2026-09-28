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

public enum RecipeType {
    SHAPELESS(0),
    SHAPED(1),
    UNKNOWN_1(2),
    UNKNOWN_2(3),
    MULTI(4),
    USER_DATA_SHAPELESS(5),
    SHAPELESS_CHEMISTRY(6),
    SHAPED_CHEMISTRY(7),
    SMITHING_TRANSFORM(8),
    SMITHING_TRIM(9);

    private static final Int2ObjectMap<RecipeType> BY_VALUE = new Int2ObjectOpenHashMap<>();

    static {
        for (RecipeType value : values()) {
            if (!BY_VALUE.containsKey(value.value)) {
                BY_VALUE.put(value.value, value);
            }
        }
    }

    public static RecipeType getByValue(final int value) {
        return BY_VALUE.get(value);
    }

    public static RecipeType getByValue(final int value, final RecipeType fallback) {
        return BY_VALUE.getOrDefault(value, fallback);
    }

    public static RecipeType getByName(final String name) {
        for (RecipeType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static RecipeType getByName(final String name, final RecipeType fallback) {
        for (RecipeType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }

    private final int value;

    RecipeType(final RecipeType value) {
        this(value.value);
    }

    RecipeType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

}
