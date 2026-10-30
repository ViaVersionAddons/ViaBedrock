// THIS FILE IS AUTO-GENERATED. DO NOT EDIT!
package net.raphimc.viabedrock.protocol.data.enums.bedrock.generated;

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public enum CurrentCmdVersion {

    Invalid(-1),
    Initial(1),
    TpRotationClamping(2),
    NewBedrockCmdSystem(3),
    ExecuteUsesVec3(4),
    CloneFixes(5),
    UpdateAquatic(6),
    EntitySelectorUsesVec3(7),
    ContainersDontDropItemsAnymore(8),
    FiltersObeyDimensions(9),
    ExecuteAndBlockCommandAndSelfSelectorFixes(10),
    InstantEffectsUseTicks(11),
    DontRegisterBrokenFunctionCommands(12),
    ClearSpawnPointCommand(13),
    CloneAndTeleportRotationFixes(14),
    TeleportDimensionFixes(15),
    CloneUpdateBlockAndTimeFixes(16),
    CloneIntersectFix(17),
    FunctionExecuteOrderAndChestSlotFix(18),
    NonTickingAreasNoLongerConsideredLoaded(19),
    SpreadplayersHazardAndResolvePlayerByNameFix(20),
    NewExecuteCommandSyntaxExperimentAndChestLootTableFixAndTeleportFacingVerticalUnclampedAndLocateBiomeAndFeatureMerged(21),
    WaterloggingAddedToStructureCommand(22),
    SelectorDistanceFilteredAndRelativeRotationFix(23),
    NewSummonCommandAddedRotationOptionsAndBubbleColumnCloneFixAndExecuteInDimensionTeleportFixAndNewExecuteRotationFix(24),
    NewExecuteCommandReleaseEnchantCommandLevelFixAndHasItemDataFixAndCommandDeferral(25),
    ExecuteIfScoreFixes(26),
    ReplaceItemAndLootReplaceBlockCommandsDoNotPlaceItemsIntoCauldronsFix(27),
    ChangesToCommandOriginRotation(28),
    RemoveAuxValueParameterFromBlockCommands(29),
    VolumeSelectorFixes(30),
    EnableSummonRotation(31),
    SummonCommandDefaultRotation(32),
    PositionalDimensionFiltering(33),
    CommandSelectorHasItemFilterNoLongerCallsSameItemFunction(34),
    AgentSweepingBlockTest(34),
    BlockStateEquals(35),
    CommandPositionFix(35),
    CommandSelectorHasItemFilterUsesDataAsDamageForSelectingDamageableItems(36),
    ExecuteDetectConditionSubcommandNotAllowNonLoadedBlocks(37),
    RemoveSuicideKeyword(38),
    CloneContainerBlockEntityRemovalFix(39),
    StopSoundMusicFix(40),
    SpreadPlayersStuckInGroundFixAndMaxHeightParameter(41),
    LocateStructureOutput(42),
    PostBlockFlattening(43),
    TestForBlockCommandDoesNotIgnoreBlockState(44),
    Count(56),
    Latest(55),
    ;

    private static final Int2ObjectMap<CurrentCmdVersion> BY_VALUE = new Int2ObjectOpenHashMap<>();

    static {
        for (CurrentCmdVersion value : values()) {
            if (!BY_VALUE.containsKey(value.value)) {
                BY_VALUE.put(value.value, value);
            }
        }
    }

    public static CurrentCmdVersion getByValue(final int value) {
        return BY_VALUE.get(value);
    }

    public static CurrentCmdVersion getByValue(final int value, final CurrentCmdVersion fallback) {
        return BY_VALUE.getOrDefault(value, fallback);
    }

    public static CurrentCmdVersion getByName(final String name) {
        for (CurrentCmdVersion value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static CurrentCmdVersion getByName(final String name, final CurrentCmdVersion fallback) {
        for (CurrentCmdVersion value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }

    private final int value;

    CurrentCmdVersion(final CurrentCmdVersion value) {
        this(value.value);
    }

    CurrentCmdVersion(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

}
