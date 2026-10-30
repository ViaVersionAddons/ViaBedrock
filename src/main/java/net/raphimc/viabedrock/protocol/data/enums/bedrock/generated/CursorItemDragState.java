// THIS FILE IS AUTO-GENERATED. DO NOT EDIT!
package net.raphimc.viabedrock.protocol.data.enums.bedrock.generated;

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public enum CursorItemDragState {

    Start(0),
    Stop(1),
    ;

    private static final Int2ObjectMap<CursorItemDragState> BY_VALUE = new Int2ObjectOpenHashMap<>();

    static {
        for (CursorItemDragState value : values()) {
            if (!BY_VALUE.containsKey(value.value)) {
                BY_VALUE.put(value.value, value);
            }
        }
    }

    public static CursorItemDragState getByValue(final int value) {
        return BY_VALUE.get(value);
    }

    public static CursorItemDragState getByValue(final int value, final CursorItemDragState fallback) {
        return BY_VALUE.getOrDefault(value, fallback);
    }

    public static CursorItemDragState getByName(final String name) {
        for (CursorItemDragState value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static CursorItemDragState getByName(final String name, final CursorItemDragState fallback) {
        for (CursorItemDragState value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }

    private final int value;

    CursorItemDragState(final CursorItemDragState value) {
        this(value.value);
    }

    CursorItemDragState(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

}
