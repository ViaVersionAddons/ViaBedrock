// THIS FILE IS AUTO-GENERATED. DO NOT EDIT!
package net.raphimc.viabedrock.protocol.data.enums.bedrock.generated;

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public enum ContainerType {

    NONE(-9),
    INVENTORY(-1),
    CONTAINER(0),
    WORKBENCH(1),
    FURNACE(2),
    ENCHANTMENT(3),
    BREWING_STAND(4),
    ANVIL(5),
    DISPENSER(6),
    DROPPER(7),
    HOPPER(8),
    CAULDRON(9),
    MINECART_CHEST(10),
    MINECART_HOPPER(11),
    HORSE(12),
    BEACON(13),
    STRUCTURE_EDITOR(14),
    TRADE(15),
    COMMAND_BLOCK(16),
    JUKEBOX(17),
    ARMOR(18),
    HAND(19),
    COMPOUND_CREATOR(20),
    ELEMENT_CONSTRUCTOR(21),
    MATERIAL_REDUCER(22),
    LAB_TABLE(23),
    LOOM(24),
    LECTERN(25),
    GRINDSTONE(26),
    BLAST_FURNACE(27),
    SMOKER(28),
    STONECUTTER(29),
    CARTOGRAPHY(30),
    HUD(31),
    JIGSAW_EDITOR(32),
    SMITHING_TABLE(33),
    CHEST_BOAT(34),
    DECORATED_POT(35),
    CRAFTER(36),
    RESERVED_CONTAINER_TYPE(38),
    ;

    private static final Int2ObjectMap<ContainerType> BY_VALUE = new Int2ObjectOpenHashMap<>();

    static {
        for (ContainerType value : values()) {
            if (!BY_VALUE.containsKey(value.value)) {
                BY_VALUE.put(value.value, value);
            }
        }
    }

    public static ContainerType getByValue(final int value) {
        return BY_VALUE.get(value);
    }

    public static ContainerType getByValue(final int value, final ContainerType fallback) {
        return BY_VALUE.getOrDefault(value, fallback);
    }

    public static ContainerType getByName(final String name) {
        for (ContainerType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static ContainerType getByName(final String name, final ContainerType fallback) {
        for (ContainerType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }

    private final int value;

    ContainerType(final ContainerType value) {
        this(value.value);
    }

    ContainerType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

}
