// THIS FILE IS AUTO-GENERATED. DO NOT EDIT!
package net.raphimc.viabedrock.protocol.data.enums.bedrock.generated;

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public enum MinecraftEventing_AchievementIds {

    ChestFullOfCobblestone(7),
    DiamondForYou(10),
    IronBelly(20),
    IronMan(21),
    OnARail(29),
    Overkill(30),
    ReturnToSender(37),
    SniperDuel(38),
    StayinFrosty(39),
    TakeInventory(40),
    MapRoom(50),
    FreightStation(52),
    SmeltEverything(53),
    TasteOfYourOwnMedicine(54),
    WhenPigsFly(56),
    Inception(58),
    ArtificialSelection(60),
    FreeDiver(61),
    SpawnTheWither(62),
    Beaconator(63),
    GreatView(64),
    SuperSonic(65),
    TheEndAgain(66),
    TreasureHunter(67),
    ShootingStar(68),
    FashionShow(69),
    SelfPublishedAuthor(71),
    AlternativeFuel(72),
    SleepWithTheFishes(73),
    Castaway(74),
    ImAMarineBiologist(75),
    SailThe7Seas(76),
    MeGold(77),
    Ahoy(78),
    Atlantis(79),
    OnePickleTwoPickleSeaPickleFour(80),
    DoaBarrelRoll(81),
    Moskstraumen(82),
    Echolocation(83),
    WhereHaveYouBeen(84),
    TopOfTheWorld(85),
    FruitOnTheLoom(86),
    SoundTheAlarm(87),
    BuyLowSellHigh(88),
    Disenchanted(89),
    TimeForStew(90),
    BeeOurGuest(91),
    TotalBeeLocation(92),
    StickySituation(93),
    CoverMeInDebris(94),
    FloatYourGoat(95),
    Friend(96),
    WaxOnWaxOff(97),
    StriderRiddenInLavaInOverworld(98),
    GoatHornAcquired(99),
    JukeboxUsedInMeadows(100),
    TradedAtWorldHeight(101),
    SurvivedFallFromWorldHeight(102),
    SneakCloseToSculkSensor(103),
    ItSpreads(104),
    BirthdaySong(105),
    WithOurPowersCombined(106),
    PlantingThePast(107),
    CarefulRestoration(108),
    Revaulting(109),
    CraftersCraftingCrafters(110),
    WhoNeedsRockets(111),
    OverOverkill(112),
    HeartTransplanter(113),
    StayHydrated(114),
    MobKabob(115),
    AdventuringTime(116),
    UhOh(117),
    GettingWood(118),
    BenchMaking(119),
    TimeToMine(120),
    HotTopic(121),
    AcquireHardware(122),
    GettingAnUpgrade(123),
    MonsterHunter(124),
    Diamonds(125),
    PlethoraOfCats(126),
    WhatGoesAroundComesAround(127),
    ;

    private static final Int2ObjectMap<MinecraftEventing_AchievementIds> BY_VALUE = new Int2ObjectOpenHashMap<>();

    static {
        for (MinecraftEventing_AchievementIds value : values()) {
            if (!BY_VALUE.containsKey(value.value)) {
                BY_VALUE.put(value.value, value);
            }
        }
    }

    public static MinecraftEventing_AchievementIds getByValue(final int value) {
        return BY_VALUE.get(value);
    }

    public static MinecraftEventing_AchievementIds getByValue(final int value, final MinecraftEventing_AchievementIds fallback) {
        return BY_VALUE.getOrDefault(value, fallback);
    }

    public static MinecraftEventing_AchievementIds getByName(final String name) {
        for (MinecraftEventing_AchievementIds value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static MinecraftEventing_AchievementIds getByName(final String name, final MinecraftEventing_AchievementIds fallback) {
        for (MinecraftEventing_AchievementIds value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }

    private final int value;

    MinecraftEventing_AchievementIds(final MinecraftEventing_AchievementIds value) {
        this(value.value);
    }

    MinecraftEventing_AchievementIds(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

}
