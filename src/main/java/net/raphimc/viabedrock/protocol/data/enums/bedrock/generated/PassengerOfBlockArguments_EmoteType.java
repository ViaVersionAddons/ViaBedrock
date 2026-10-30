// THIS FILE IS AUTO-GENERATED. DO NOT EDIT!
package net.raphimc.viabedrock.protocol.data.enums.bedrock.generated;

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public enum PassengerOfBlockArguments_EmoteType {

    Standing(0),
    Riding(1),
    Laying(2),
    ;

    private static final Int2ObjectMap<PassengerOfBlockArguments_EmoteType> BY_VALUE = new Int2ObjectOpenHashMap<>();

    static {
        for (PassengerOfBlockArguments_EmoteType value : values()) {
            if (!BY_VALUE.containsKey(value.value)) {
                BY_VALUE.put(value.value, value);
            }
        }
    }

    public static PassengerOfBlockArguments_EmoteType getByValue(final int value) {
        return BY_VALUE.get(value);
    }

    public static PassengerOfBlockArguments_EmoteType getByValue(final int value, final PassengerOfBlockArguments_EmoteType fallback) {
        return BY_VALUE.getOrDefault(value, fallback);
    }

    public static PassengerOfBlockArguments_EmoteType getByName(final String name) {
        for (PassengerOfBlockArguments_EmoteType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static PassengerOfBlockArguments_EmoteType getByName(final String name, final PassengerOfBlockArguments_EmoteType fallback) {
        for (PassengerOfBlockArguments_EmoteType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }

    private final int value;

    PassengerOfBlockArguments_EmoteType(final PassengerOfBlockArguments_EmoteType value) {
        this(value.value);
    }

    PassengerOfBlockArguments_EmoteType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

}
