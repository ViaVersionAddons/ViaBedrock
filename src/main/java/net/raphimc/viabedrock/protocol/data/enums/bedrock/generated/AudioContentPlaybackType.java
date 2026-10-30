// THIS FILE IS AUTO-GENERATED. DO NOT EDIT!
package net.raphimc.viabedrock.protocol.data.enums.bedrock.generated;

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public enum AudioContentPlaybackType {

    Music(0),
    Sound(1),
    ;

    private static final Int2ObjectMap<AudioContentPlaybackType> BY_VALUE = new Int2ObjectOpenHashMap<>();

    static {
        for (AudioContentPlaybackType value : values()) {
            if (!BY_VALUE.containsKey(value.value)) {
                BY_VALUE.put(value.value, value);
            }
        }
    }

    public static AudioContentPlaybackType getByValue(final int value) {
        return BY_VALUE.get(value);
    }

    public static AudioContentPlaybackType getByValue(final int value, final AudioContentPlaybackType fallback) {
        return BY_VALUE.getOrDefault(value, fallback);
    }

    public static AudioContentPlaybackType getByName(final String name) {
        for (AudioContentPlaybackType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static AudioContentPlaybackType getByName(final String name, final AudioContentPlaybackType fallback) {
        for (AudioContentPlaybackType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }

    private final int value;

    AudioContentPlaybackType(final AudioContentPlaybackType value) {
        this(value.value);
    }

    AudioContentPlaybackType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

}
