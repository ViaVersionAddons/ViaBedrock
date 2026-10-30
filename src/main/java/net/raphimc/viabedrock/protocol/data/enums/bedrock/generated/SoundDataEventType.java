// THIS FILE IS AUTO-GENERATED. DO NOT EDIT!
package net.raphimc.viabedrock.protocol.data.enums.bedrock.generated;

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public enum SoundDataEventType {

    Stop(0),
    SetVolume(1),
    SetPitch(2),
    Fade(3),
    SeekTo(4),
    Pause(5),
    Resume(6),
    ;

    private static final Int2ObjectMap<SoundDataEventType> BY_VALUE = new Int2ObjectOpenHashMap<>();

    static {
        for (SoundDataEventType value : values()) {
            if (!BY_VALUE.containsKey(value.value)) {
                BY_VALUE.put(value.value, value);
            }
        }
    }

    public static SoundDataEventType getByValue(final int value) {
        return BY_VALUE.get(value);
    }

    public static SoundDataEventType getByValue(final int value, final SoundDataEventType fallback) {
        return BY_VALUE.getOrDefault(value, fallback);
    }

    public static SoundDataEventType getByName(final String name) {
        for (SoundDataEventType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static SoundDataEventType getByName(final String name, final SoundDataEventType fallback) {
        for (SoundDataEventType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }

    private final int value;

    SoundDataEventType(final SoundDataEventType value) {
        this(value.value);
    }

    SoundDataEventType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

}
