// THIS FILE IS AUTO-GENERATED. DO NOT EDIT!
package net.raphimc.viabedrock.protocol.data.enums.bedrock.generated;

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public enum EditorLevelMigrationVersion {

    Legacy(0),
    EditorAchievements(1),
    ;

    private static final Int2ObjectMap<EditorLevelMigrationVersion> BY_VALUE = new Int2ObjectOpenHashMap<>();

    static {
        for (EditorLevelMigrationVersion value : values()) {
            if (!BY_VALUE.containsKey(value.value)) {
                BY_VALUE.put(value.value, value);
            }
        }
    }

    public static EditorLevelMigrationVersion getByValue(final int value) {
        return BY_VALUE.get(value);
    }

    public static EditorLevelMigrationVersion getByValue(final int value, final EditorLevelMigrationVersion fallback) {
        return BY_VALUE.getOrDefault(value, fallback);
    }

    public static EditorLevelMigrationVersion getByName(final String name) {
        for (EditorLevelMigrationVersion value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static EditorLevelMigrationVersion getByName(final String name, final EditorLevelMigrationVersion fallback) {
        for (EditorLevelMigrationVersion value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }

    private final int value;

    EditorLevelMigrationVersion(final EditorLevelMigrationVersion value) {
        this(value.value);
    }

    EditorLevelMigrationVersion(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

}
