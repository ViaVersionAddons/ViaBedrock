// THIS FILE IS AUTO-GENERATED. DO NOT EDIT!
package net.raphimc.viabedrock.protocol.data.enums.bedrock.generated;

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public enum MatchmakingState {

    Idle(0),
    Matchmaking(1),
    MatchFound(2),
    Canceled(3),
    PlayerLeftParty(4),
    PlayerLeftServer(5),
    ServerShutdown(6),
    TimedOut(7),
    RequeueAsParty(8),
    ;

    private static final Int2ObjectMap<MatchmakingState> BY_VALUE = new Int2ObjectOpenHashMap<>();

    static {
        for (MatchmakingState value : values()) {
            if (!BY_VALUE.containsKey(value.value)) {
                BY_VALUE.put(value.value, value);
            }
        }
    }

    public static MatchmakingState getByValue(final int value) {
        return BY_VALUE.get(value);
    }

    public static MatchmakingState getByValue(final int value, final MatchmakingState fallback) {
        return BY_VALUE.getOrDefault(value, fallback);
    }

    public static MatchmakingState getByName(final String name) {
        for (MatchmakingState value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static MatchmakingState getByName(final String name, final MatchmakingState fallback) {
        for (MatchmakingState value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }

    private final int value;

    MatchmakingState(final MatchmakingState value) {
        this(value.value);
    }

    MatchmakingState(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

}
