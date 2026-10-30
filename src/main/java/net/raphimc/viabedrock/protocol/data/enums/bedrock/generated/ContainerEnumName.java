// THIS FILE IS AUTO-GENERATED. DO NOT EDIT!
package net.raphimc.viabedrock.protocol.data.enums.bedrock.generated;

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public enum ContainerEnumName {

    AnvilInputContainer(0),
    AnvilMaterialContainer(1),
    AnvilResultPreviewContainer(2),
    SmithingTableInputContainer(3),
    SmithingTableMaterialContainer(4),
    SmithingTableResultPreviewContainer(5),
    ArmorContainer(6),
    LevelEntityContainer(7),
    BeaconPaymentContainer(8),
    BrewingStandInputContainer(9),
    BrewingStandResultContainer(10),
    BrewingStandFuelContainer(11),
    CombinedHotbarAndInventoryContainer(12),
    CraftingInputContainer(13),
    CraftingOutputPreviewContainer(14),
    RecipeConstructionContainer(15),
    RecipeNatureContainer(16),
    RecipeItemsContainer(17),
    RecipeFoodContainer(64),
    RecipeBlocksContainer(65),
    RecipeFurnaceItemsContainer(66),
    RecipeSearchContainer(18),
    RecipeSearchBarContainer(19),
    RecipeEquipmentContainer(20),
    RecipeBookContainer(21),
    EnchantingInputContainer(22),
    EnchantingMaterialContainer(23),
    FurnaceFuelContainer(24),
    FurnaceIngredientContainer(25),
    FurnaceResultContainer(26),
    HorseEquipContainer(27),
    HotbarContainer(28),
    InventoryContainer(29),
    ShulkerBoxContainer(30),
    TradeIngredient1Container(31),
    TradeIngredient2Container(32),
    TradeResultPreviewContainer(33),
    OffhandContainer(34),
    CompoundCreatorInput(35),
    CompoundCreatorOutputPreview(36),
    ElementConstructorOutputPreview(37),
    MaterialReducerInput(38),
    MaterialReducerOutput(39),
    LabTableInput(40),
    LoomInputContainer(41),
    LoomDyeContainer(42),
    LoomMaterialContainer(43),
    LoomResultPreviewContainer(44),
    BlastFurnaceIngredientContainer(45),
    SmokerIngredientContainer(46),
    Trade2Ingredient1Container(47),
    Trade2Ingredient2Container(48),
    Trade2ResultPreviewContainer(49),
    GrindstoneInputContainer(50),
    GrindstoneAdditionalContainer(51),
    GrindstoneResultPreviewContainer(52),
    StonecutterInputContainer(53),
    StonecutterResultPreviewContainer(54),
    CartographyInputContainer(55),
    CartographyAdditionalContainer(56),
    CartographyResultPreviewContainer(57),
    BarrelContainer(58),
    CursorContainer(59),
    CreatedOutputContainer(60),
    SmithingTableTemplateContainer(61),
    CrafterLevelEntityContainer(62),
    DynamicContainer(63),
    ReservedContainerA(67),
    ReservedContainerB(68),
    ReservedContainerC(69),
    ReservedContainerD(70),
    ;

    private static final Int2ObjectMap<ContainerEnumName> BY_VALUE = new Int2ObjectOpenHashMap<>();

    static {
        for (ContainerEnumName value : values()) {
            if (!BY_VALUE.containsKey(value.value)) {
                BY_VALUE.put(value.value, value);
            }
        }
    }

    public static ContainerEnumName getByValue(final int value) {
        return BY_VALUE.get(value);
    }

    public static ContainerEnumName getByValue(final int value, final ContainerEnumName fallback) {
        return BY_VALUE.getOrDefault(value, fallback);
    }

    public static ContainerEnumName getByName(final String name) {
        for (ContainerEnumName value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static ContainerEnumName getByName(final String name, final ContainerEnumName fallback) {
        for (ContainerEnumName value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }

    private final int value;

    ContainerEnumName(final ContainerEnumName value) {
        this(value.value);
    }

    ContainerEnumName(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

}
