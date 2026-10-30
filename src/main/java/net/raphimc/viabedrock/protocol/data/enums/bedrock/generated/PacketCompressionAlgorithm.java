// THIS FILE IS AUTO-GENERATED. DO NOT EDIT!
package net.raphimc.viabedrock.protocol.data.enums.bedrock.generated;

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public enum PacketCompressionAlgorithm {

    ZLib(0),
    Snappy(1),
    None(65535),
    ;

    private static final Int2ObjectMap<PacketCompressionAlgorithm> BY_VALUE = new Int2ObjectOpenHashMap<>();

    static {
        for (PacketCompressionAlgorithm value : values()) {
            if (!BY_VALUE.containsKey(value.value)) {
                BY_VALUE.put(value.value, value);
            }
        }
    }

    public static PacketCompressionAlgorithm getByValue(final int value) {
        return BY_VALUE.get(value);
    }

    public static PacketCompressionAlgorithm getByValue(final int value, final PacketCompressionAlgorithm fallback) {
        return BY_VALUE.getOrDefault(value, fallback);
    }

    public static PacketCompressionAlgorithm getByName(final String name) {
        for (PacketCompressionAlgorithm value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static PacketCompressionAlgorithm getByName(final String name, final PacketCompressionAlgorithm fallback) {
        for (PacketCompressionAlgorithm value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }

    private final int value;

    PacketCompressionAlgorithm(final PacketCompressionAlgorithm value) {
        this(value.value);
    }

    PacketCompressionAlgorithm(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

}
