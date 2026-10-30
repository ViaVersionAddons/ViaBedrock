// THIS FILE IS AUTO-GENERATED. DO NOT EDIT!
package net.raphimc.viabedrock.protocol.data.enums.bedrock.generated;

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

public enum MinecraftPacketIds {

    KeepAlive(0),
    Login(1),
    PlayStatus(2),
    ServerToClientHandshake(3),
    ClientToServerHandshake(4),
    Disconnect(5),
    ResourcePacksInfo(6),
    ResourcePackStack(7),
    ResourcePackClientResponse(8),
    Text(9),
    SetTime(10),
    StartGame(11),
    AddPlayer(12),
    AddActor(13),
    RemoveActor(14),
    AddItemActor(15),
    ServerPlayerPostMovePosition(16),
    TakeItemActor(17),
    MoveAbsoluteActor(18),
    MovePlayer(19),
    UpdateBlock(21),
    AddPainting(22),
    LevelEvent(25),
    TileEvent(26),
    ActorEvent(27),
    MobEffect(28),
    UpdateAttributes(29),
    InventoryTransaction(30),
    PlayerEquipment(31),
    MobArmorEquipment(32),
    Interact(33),
    BlockPickRequest(34),
    ActorPickRequest(35),
    PlayerAction(36),
    HurtArmor(38),
    SetActorData(39),
    SetActorMotion(40),
    SetActorLink(41),
    SetHealth(42),
    SetSpawnPosition(43),
    Animate(44),
    Respawn(45),
    ContainerOpen(46),
    ContainerClose(47),
    PlayerHotbar(48),
    InventoryContent(49),
    InventorySlot(50),
    ContainerSetData(51),
    CraftingData(52),
    GuiDataPickItem(54),
    BlockActorData(56),
    FullChunkData(58),
    SetCommandsEnabled(59),
    SetDifficulty(60),
    ChangeDimension(61),
    SetPlayerGameType(62),
    PlayerList(63),
    SimpleEvent(64),
    LegacyTelemetryEvent(65),
    SpawnExperienceOrb(66),
    MapData(67),
    MapInfoRequest(68),
    RequestChunkRadius(69),
    ChunkRadiusUpdated(70),
    GameRulesChanged(72),
    Camera(73),
    BossEvent(74),
    ShowCredits(75),
    AvailableCommands(76),
    CommandRequest(77),
    CommandBlockUpdate(78),
    CommandOutput(79),
    UpdateTrade(80),
    UpdateEquip(81),
    ResourcePackDataInfo(82),
    ResourcePackChunkData(83),
    ResourcePackChunkRequest(84),
    Transfer(85),
    PlaySound(86),
    StopSound(87),
    SetTitle(88),
    AddBehaviorTree(89),
    StructureBlockUpdate(90),
    ShowStoreOffer(91),
    PurchaseReceipt(92),
    PlayerSkin(93),
    SubclientLogin(94),
    AutomationClientConnect(95),
    SetLastHurtBy(96),
    BookEdit(97),
    NPCRequest(98),
    PhotoTransfer(99),
    ShowModalForm(100),
    ModalFormResponse(101),
    ServerSettingsRequest(102),
    ServerSettingsResponse(103),
    ShowProfile(104),
    SetDefaultGameType(105),
    RemoveObjective(106),
    SetDisplayObjective(107),
    SetScore(108),
    LabTable(109),
    UpdateBlockSynced(110),
    MoveDeltaActor(111),
    SetScoreboardIdentity(112),
    SetLocalPlayerAsInit(113),
    UpdateSoftEnum(114),
    Ping(115),
    ScriptCustomEvent(117),
    SpawnParticleEffect(118),
    AvailableActorIDList(119),
    NetworkChunkPublisherUpdate(121),
    BiomeDefinitionList(122),
    LevelSoundEvent(123),
    LevelEventGeneric(124),
    LecternUpdate(125),
    ClientCacheStatus(129),
    OnScreenTextureAnimation(130),
    MapCreateLockedCopy(131),
    StructureTemplateDataExportRequest(132),
    StructureTemplateDataExportResponse(133),
    ClientCacheBlobStatusPacket(135),
    ClientCacheMissResponsePacket(136),
    EducationSettingsPacket(137),
    Emote(138),
    MultiplayerSettingsPacket(139),
    SettingsCommandPacket(140),
    AnvilDamage(141),
    CompletedUsingItem(142),
    ServerboundCursorItemDrag(358),
    NetworkSettings(143),
    PlayerAuthInputPacket(144),
    CreativeContent(145),
    PlayerEnchantOptions(146),
    ItemStackRequest(147),
    ItemStackResponse(148),
    PlayerArmorDamage(149),
    CodeBuilderPacket(150),
    UpdatePlayerGameType(151),
    EmoteList(152),
    PositionTrackingDBServerBroadcast(153),
    PositionTrackingDBClientRequest(154),
    DebugInfoPacket(155),
    PacketViolationWarning(156),
    MotionPredictionHints(157),
    TriggerAnimation(158),
    CameraShake(159),
    PlayerFogSetting(160),
    CorrectPlayerMovePredictionPacket(161),
    ItemRegistryPacket(162),
    ClientBoundDebugRendererPacket(164),
    SyncActorProperty(165),
    AddVolumeEntityPacket(166),
    RemoveVolumeEntityPacket(167),
    SimulationTypePacket(168),
    NpcDialoguePacket(169),
    EduUriResourcePacket(170),
    CreatePhotoPacket(171),
    UpdateSubChunkBlocks(172),
    SubChunkPacket(174),
    SubChunkRequestPacket(175),
    PlayerStartItemCooldown(176),
    ScriptMessagePacket(177),
    CodeBuilderSourcePacket(178),
    TickingAreasLoadStatus(179),
    DimensionDataPacket(180),
    AgentAction(181),
    ChangeMobProperty(182),
    LessonProgressPacket(183),
    RequestAbilityPacket(184),
    RequestPermissionsPacket(185),
    ToastRequest(186),
    UpdateAbilitiesPacket(187),
    UpdateAdventureSettingsPacket(188),
    DeathInfo(189),
    EditorNetworkPacket(190),
    FeatureRegistryPacket(191),
    ServerStats(192),
    RequestNetworkSettings(193),
    GameTestRequestPacket(194),
    GameTestResultsPacket(195),
    PlayerClientInputPermissions(196),
    CameraPresets(198),
    UnlockedRecipes(199),
    TitleSpecificPacketsStart(200),
    TitleSpecificPacketsEnd(299),
    CameraInstruction(300),
    TrimData(302),
    OpenSign(303),
    AgentAnimation(304),
    RefreshEntitlementsPacket(305),
    PlayerToggleCrafterSlotRequestPacket(306),
    SetPlayerInventoryOptions(307),
    SetHudPacket(308),
    AwardAchievementPacket(309),
    ClientboundCloseScreen(310),
    ServerboundLoadingScreenPacket(312),
    JigsawStructureDataPacket(313),
    CurrentStructureFeaturePacket(314),
    ServerboundDiagnosticsPacket(315),
    CameraAimAssist(316),
    ContainerRegistryCleanup(317),
    MovementEffect(318),
    CameraAimAssistActorPriority(339),
    CameraAimAssistPresets(320),
    ClientCameraAimAssist(321),
    ClientMovementPredictionSyncPacket(322),
    UpdateClientOptions(323),
    PlayerVideoCapturePacket(324),
    PlayerUpdateEntityOverridesPacket(325),
    PlayerLocation(326),
    SyncWorldClocks(344),
    SendPartyDestinationCookie(349),
    PartyDestinationCookieResponse(350),
    SetPlayerFurnaceOptions(351),
    RecordStarted(352),
    ClientboundMatchmakingState(353),
    ServerboundMatchmakingCancel(356),
    ClientboundPlayAudioContent(359),
    ServerboundRegisterAudioContent(360),
    // V Not in docs V
    ClientboundControlSchemeSetPacket(327),
    PrimitiveShapesPacket(328),
    ServerboundPackSettingChangePacket(329),
    ClientboundDataStorePacket(330),
    GraphicsOverrideParameterPacket(331),
    ServerboundDataStorePacket(332),
    ClientboundDataDrivenUIShowScreenPacket(333),
    ClientboundDataDrivenUICloseScreenPacket(334),
    ClientboundDataDrivenUIReloadPacket(335),
    ClientboundTextureShiftPacket(336),
    VoxelShapesPacket(337),
    CameraSplinePacket(338),
    ResourcePacksReadyForValidationPacket(340),
    LocatorBarPacket(341),
    PartyChangedPacket(342),
    ServerboundDataDrivenScreenClosedPacket(343),
    ClientboundAttributeLayerSyncPacket(345),
    ServerStoreInfoPacket(346),
    ServerPresenceInfoPacket(347),
    ClientboundUpdateSoundDataPacket(348),
    ServerboundStonecutterSetRecipePacket(354),
    ClientboundStonecutterSetRecipePacket(355),
    SetPassengerOfBlockPacket(357),
    ;

    private static final Int2ObjectMap<MinecraftPacketIds> BY_VALUE = new Int2ObjectOpenHashMap<>();

    static {
        for (MinecraftPacketIds value : values()) {
            if (!BY_VALUE.containsKey(value.value)) {
                BY_VALUE.put(value.value, value);
            }
        }
    }

    public static MinecraftPacketIds getByValue(final int value) {
        return BY_VALUE.get(value);
    }

    public static MinecraftPacketIds getByValue(final int value, final MinecraftPacketIds fallback) {
        return BY_VALUE.getOrDefault(value, fallback);
    }

    public static MinecraftPacketIds getByName(final String name) {
        for (MinecraftPacketIds value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }

    public static MinecraftPacketIds getByName(final String name, final MinecraftPacketIds fallback) {
        for (MinecraftPacketIds value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return fallback;
    }

    private final int value;

    MinecraftPacketIds(final MinecraftPacketIds value) {
        this(value.value);
    }

    MinecraftPacketIds(final int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

}
