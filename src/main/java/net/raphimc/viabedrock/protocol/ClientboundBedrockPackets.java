/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2023-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viabedrock.protocol;

import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.MinecraftPacketIds;

public enum ClientboundBedrockPackets implements ClientboundPacketType {

    PLAY_STATUS(MinecraftPacketIds.PlayStatus.getValue()),
    SERVER_TO_CLIENT_HANDSHAKE(MinecraftPacketIds.ServerToClientHandshake.getValue()),
    DISCONNECT(MinecraftPacketIds.Disconnect.getValue()),
    RESOURCE_PACKS_INFO(MinecraftPacketIds.ResourcePacksInfo.getValue()),
    RESOURCE_PACK_STACK(MinecraftPacketIds.ResourcePackStack.getValue()),
    TEXT(MinecraftPacketIds.Text.getValue()),
    SET_TIME(MinecraftPacketIds.SetTime.getValue()),
    START_GAME(MinecraftPacketIds.StartGame.getValue()),
    ADD_PLAYER(MinecraftPacketIds.AddPlayer.getValue()),
    ADD_ENTITY(MinecraftPacketIds.AddActor.getValue()),
    REMOVE_ENTITY(MinecraftPacketIds.RemoveActor.getValue()),
    ADD_ITEM_ENTITY(MinecraftPacketIds.AddItemActor.getValue()),
    SERVER_PLAYER_POST_MOVE_POSITION(MinecraftPacketIds.ServerPlayerPostMovePosition.getValue()),
    TAKE_ITEM_ENTITY(MinecraftPacketIds.TakeItemActor.getValue()),
    MOVE_ENTITY_ABSOLUTE(MinecraftPacketIds.MoveAbsoluteActor.getValue()),
    MOVE_PLAYER(MinecraftPacketIds.MovePlayer.getValue()),
    UPDATE_BLOCK(MinecraftPacketIds.UpdateBlock.getValue()),
    ADD_PAINTING(MinecraftPacketIds.AddPainting.getValue()),
    LEVEL_EVENT(MinecraftPacketIds.LevelEvent.getValue()),
    BLOCK_EVENT(MinecraftPacketIds.TileEvent.getValue()),
    ENTITY_EVENT(MinecraftPacketIds.ActorEvent.getValue()),
    MOB_EFFECT(MinecraftPacketIds.MobEffect.getValue()),
    UPDATE_ATTRIBUTES(MinecraftPacketIds.UpdateAttributes.getValue()),
    INVENTORY_TRANSACTION(MinecraftPacketIds.InventoryTransaction.getValue()),
    MOB_EQUIPMENT(MinecraftPacketIds.PlayerEquipment.getValue()),
    MOB_ARMOR_EQUIPMENT(MinecraftPacketIds.MobArmorEquipment.getValue()),
    PLAYER_ACTION(MinecraftPacketIds.PlayerAction.getValue()),
    HURT_ARMOR(MinecraftPacketIds.HurtArmor.getValue()),
    SET_ENTITY_DATA(MinecraftPacketIds.SetActorData.getValue()),
    SET_ENTITY_MOTION(MinecraftPacketIds.SetActorMotion.getValue()),
    SET_ENTITY_LINK(MinecraftPacketIds.SetActorLink.getValue()),
    SET_HEALTH(MinecraftPacketIds.SetHealth.getValue()),
    SET_SPAWN_POSITION(MinecraftPacketIds.SetSpawnPosition.getValue()),
    ANIMATE(MinecraftPacketIds.Animate.getValue()),
    RESPAWN(MinecraftPacketIds.Respawn.getValue()),
    CONTAINER_OPEN(MinecraftPacketIds.ContainerOpen.getValue()),
    CONTAINER_CLOSE(MinecraftPacketIds.ContainerClose.getValue()),
    PLAYER_HOTBAR(MinecraftPacketIds.PlayerHotbar.getValue()),
    INVENTORY_CONTENT(MinecraftPacketIds.InventoryContent.getValue()),
    INVENTORY_SLOT(MinecraftPacketIds.InventorySlot.getValue()),
    CONTAINER_SET_DATA(MinecraftPacketIds.ContainerSetData.getValue()),
    CRAFTING_DATA(MinecraftPacketIds.CraftingData.getValue()),
    GUI_DATA_PICK_ITEM(MinecraftPacketIds.GuiDataPickItem.getValue()),
    BLOCK_ENTITY_DATA(MinecraftPacketIds.BlockActorData.getValue()),
    LEVEL_CHUNK(MinecraftPacketIds.FullChunkData.getValue()),
    SET_COMMANDS_ENABLED(MinecraftPacketIds.SetCommandsEnabled.getValue()),
    SET_DIFFICULTY(MinecraftPacketIds.SetDifficulty.getValue()),
    CHANGE_DIMENSION(MinecraftPacketIds.ChangeDimension.getValue()),
    SET_PLAYER_GAME_TYPE(MinecraftPacketIds.SetPlayerGameType.getValue()),
    PLAYER_LIST(MinecraftPacketIds.PlayerList.getValue()),
    SIMPLE_EVENT(MinecraftPacketIds.SimpleEvent.getValue()),
    LEGACY_TELEMETRY_EVENT(MinecraftPacketIds.LegacyTelemetryEvent.getValue()),
    SPAWN_EXPERIENCE_ORB(MinecraftPacketIds.SpawnExperienceOrb.getValue()),
    MAP_ITEM_DATA(MinecraftPacketIds.MapData.getValue()),
    CHUNK_RADIUS_UPDATED(MinecraftPacketIds.ChunkRadiusUpdated.getValue()),
    GAME_RULES_CHANGED(MinecraftPacketIds.GameRulesChanged.getValue()),
    CAMERA(MinecraftPacketIds.Camera.getValue()),
    BOSS_EVENT(MinecraftPacketIds.BossEvent.getValue()),
    SHOW_CREDITS(MinecraftPacketIds.ShowCredits.getValue()),
    AVAILABLE_COMMANDS(MinecraftPacketIds.AvailableCommands.getValue()),
    COMMAND_OUTPUT(MinecraftPacketIds.CommandOutput.getValue()),
    UPDATE_TRADE(MinecraftPacketIds.UpdateTrade.getValue()),
    UPDATE_EQUIP(MinecraftPacketIds.UpdateEquip.getValue()),
    RESOURCE_PACK_DATA_INFO(MinecraftPacketIds.ResourcePackDataInfo.getValue()),
    RESOURCE_PACK_CHUNK_DATA(MinecraftPacketIds.ResourcePackChunkData.getValue()),
    TRANSFER(MinecraftPacketIds.Transfer.getValue()),
    PLAY_SOUND(MinecraftPacketIds.PlaySound.getValue()),
    STOP_SOUND(MinecraftPacketIds.StopSound.getValue()),
    SET_TITLE(MinecraftPacketIds.SetTitle.getValue()),
    ADD_BEHAVIOR_TREE(MinecraftPacketIds.AddBehaviorTree.getValue()),
    SHOW_STORE_OFFER(MinecraftPacketIds.ShowStoreOffer.getValue()),
    PLAYER_SKIN(MinecraftPacketIds.PlayerSkin.getValue()),
    AUTOMATION_CLIENT_CONNECT(MinecraftPacketIds.AutomationClientConnect.getValue()),
    SET_LAST_HURT_BY(MinecraftPacketIds.SetLastHurtBy.getValue()),
    PHOTO_TRANSFER(MinecraftPacketIds.PhotoTransfer.getValue()),
    MODAL_FORM_REQUEST(MinecraftPacketIds.ShowModalForm.getValue()),
    SERVER_SETTINGS_RESPONSE(MinecraftPacketIds.ServerSettingsResponse.getValue()),
    SHOW_PROFILE(MinecraftPacketIds.ShowProfile.getValue()),
    SET_DEFAULT_GAME_TYPE(MinecraftPacketIds.SetDefaultGameType.getValue()),
    REMOVE_OBJECTIVE(MinecraftPacketIds.RemoveObjective.getValue()),
    SET_DISPLAY_OBJECTIVE(MinecraftPacketIds.SetDisplayObjective.getValue()),
    SET_SCORE(MinecraftPacketIds.SetScore.getValue()),
    LAB_TABLE(MinecraftPacketIds.LabTable.getValue()),
    UPDATE_BLOCK_SYNCED(MinecraftPacketIds.UpdateBlockSynced.getValue()),
    MOVE_ENTITY_DELTA(MinecraftPacketIds.MoveDeltaActor.getValue()),
    SET_SCOREBOARD_IDENTITY(MinecraftPacketIds.SetScoreboardIdentity.getValue()),
    UPDATE_SOFT_ENUM(MinecraftPacketIds.UpdateSoftEnum.getValue()),
    NETWORK_STACK_LATENCY(MinecraftPacketIds.Ping.getValue()),
    SPAWN_PARTICLE_EFFECT(MinecraftPacketIds.SpawnParticleEffect.getValue()),
    AVAILABLE_ENTITY_IDENTIFIERS(MinecraftPacketIds.AvailableActorIDList.getValue()),
    NETWORK_CHUNK_PUBLISHER_UPDATE(MinecraftPacketIds.NetworkChunkPublisherUpdate.getValue()),
    BIOME_DEFINITION_LIST(MinecraftPacketIds.BiomeDefinitionList.getValue()),
    LEVEL_SOUND_EVENT(MinecraftPacketIds.LevelSoundEvent.getValue()),
    LEVEL_EVENT_GENERIC(MinecraftPacketIds.LevelEventGeneric.getValue()),
    ON_SCREEN_TEXTURE_ANIMATION(MinecraftPacketIds.OnScreenTextureAnimation.getValue()),
    STRUCTURE_TEMPLATE_DATA_RESPONSE(MinecraftPacketIds.StructureTemplateDataExportResponse.getValue()),
    CLIENT_CACHE_MISS_RESPONSE(MinecraftPacketIds.ClientCacheMissResponsePacket.getValue()),
    EDUCATION_SETTINGS(MinecraftPacketIds.EducationSettingsPacket.getValue()),
    EMOTE(MinecraftPacketIds.Emote.getValue()),
    MULTIPLAYER_SETTINGS(MinecraftPacketIds.MultiplayerSettingsPacket.getValue()),
    COMPLETED_USING_ITEM(MinecraftPacketIds.CompletedUsingItem.getValue()),
    NETWORK_SETTINGS(MinecraftPacketIds.NetworkSettings.getValue()),
    CREATIVE_CONTENT(MinecraftPacketIds.CreativeContent.getValue()),
    PLAYER_ENCHANT_OPTIONS(MinecraftPacketIds.PlayerEnchantOptions.getValue()),
    ITEM_STACK_RESPONSE(MinecraftPacketIds.ItemStackResponse.getValue()),
    PLAYER_ARMOR_DAMAGE(MinecraftPacketIds.PlayerArmorDamage.getValue()),
    CODE_BUILDER(MinecraftPacketIds.CodeBuilderPacket.getValue()),
    UPDATE_PLAYER_GAME_TYPE(MinecraftPacketIds.UpdatePlayerGameType.getValue()),
    EMOTE_LIST(MinecraftPacketIds.EmoteList.getValue()),
    POSITION_TRACKING_DB_SERVER_BROADCAST(MinecraftPacketIds.PositionTrackingDBServerBroadcast.getValue()),
    DEBUG_INFO(MinecraftPacketIds.DebugInfoPacket.getValue()),
    PACKET_VIOLATION_WARNING(MinecraftPacketIds.PacketViolationWarning.getValue()),
    MOTION_PREDICTION_HINTS(MinecraftPacketIds.MotionPredictionHints.getValue()),
    ANIMATE_ENTITY(MinecraftPacketIds.TriggerAnimation.getValue()),
    CAMERA_SHAKE(MinecraftPacketIds.CameraShake.getValue()),
    PLAYER_FOG(MinecraftPacketIds.PlayerFogSetting.getValue()),
    CORRECT_PLAYER_MOVE_PREDICTION(MinecraftPacketIds.CorrectPlayerMovePredictionPacket.getValue()),
    ITEM_REGISTRY(MinecraftPacketIds.ItemRegistryPacket.getValue()),
    DEBUG_RENDERER(MinecraftPacketIds.ClientBoundDebugRendererPacket.getValue()),
    SYNC_ENTITY_PROPERTY(MinecraftPacketIds.SyncActorProperty.getValue()),
    ADD_VOLUME_ENTITY(MinecraftPacketIds.AddVolumeEntityPacket.getValue()),
    REMOVE_VOLUME_ENTITY(MinecraftPacketIds.RemoveVolumeEntityPacket.getValue()),
    SIMULATION_TYPE(MinecraftPacketIds.SimulationTypePacket.getValue()),
    NPC_DIALOGUE(MinecraftPacketIds.NpcDialoguePacket.getValue()),
    EDU_URI_RESOURCE(MinecraftPacketIds.EduUriResourcePacket.getValue()),
    UPDATE_SUB_CHUNK_BLOCKS(MinecraftPacketIds.UpdateSubChunkBlocks.getValue()),
    SUB_CHUNK(MinecraftPacketIds.SubChunkPacket.getValue()),
    PLAYER_START_ITEM_COOLDOWN(MinecraftPacketIds.PlayerStartItemCooldown.getValue()),
    SCRIPT_MESSAGE(MinecraftPacketIds.ScriptMessagePacket.getValue()),
    TICKING_AREAS_LOAD_STATUS(MinecraftPacketIds.TickingAreasLoadStatus.getValue()),
    DIMENSION_DATA(MinecraftPacketIds.DimensionDataPacket.getValue()),
    AGENT_ACTION_EVENT(MinecraftPacketIds.AgentAction.getValue()),
    CHANGE_MOB_PROPERTY(MinecraftPacketIds.ChangeMobProperty.getValue()),
    LESSON_PROGRESS(MinecraftPacketIds.LessonProgressPacket.getValue()),
    TOAST_REQUEST(MinecraftPacketIds.ToastRequest.getValue()),
    UPDATE_ABILITIES(MinecraftPacketIds.UpdateAbilitiesPacket.getValue()),
    UPDATE_ADVENTURE_SETTINGS(MinecraftPacketIds.UpdateAdventureSettingsPacket.getValue()),
    DEATH_INFO(MinecraftPacketIds.DeathInfo.getValue()),
    EDITOR_NETWORK(MinecraftPacketIds.EditorNetworkPacket.getValue()),
    FEATURE_REGISTRY(MinecraftPacketIds.FeatureRegistryPacket.getValue()),
    SERVER_STATS(MinecraftPacketIds.ServerStats.getValue()),
    GAME_TEST_RESULTS(MinecraftPacketIds.GameTestResultsPacket.getValue()),
    UPDATE_CLIENT_INPUT_LOCKS(MinecraftPacketIds.PlayerClientInputPermissions.getValue()),
    CAMERA_PRESETS(MinecraftPacketIds.CameraPresets.getValue()),
    UNLOCKED_RECIPES(MinecraftPacketIds.UnlockedRecipes.getValue()),
    CAMERA_INSTRUCTION(MinecraftPacketIds.CameraInstruction.getValue()),
    TRIM_DATA(MinecraftPacketIds.TrimData.getValue()),
    OPEN_SIGN(MinecraftPacketIds.OpenSign.getValue()),
    AGENT_ANIMATION(MinecraftPacketIds.AgentAnimation.getValue()),
    SET_PLAYER_INVENTORY_OPTIONS(MinecraftPacketIds.SetPlayerInventoryOptions.getValue()),
    SET_HUD(MinecraftPacketIds.SetHudPacket.getValue()),
    AWARD_ACHIEVEMENT(MinecraftPacketIds.AwardAchievementPacket.getValue()),
    CLOSE_FORM(MinecraftPacketIds.ClientboundCloseScreen.getValue()),
    JIGSAW_STRUCTURE_DATA(MinecraftPacketIds.JigsawStructureDataPacket.getValue()),
    CURRENT_STRUCTURE_FEATURE(MinecraftPacketIds.CurrentStructureFeaturePacket.getValue()),
    CAMERA_AIM_ASSIST(MinecraftPacketIds.CameraAimAssist.getValue()),
    CONTAINER_REGISTRY_CLEANUP(MinecraftPacketIds.ContainerRegistryCleanup.getValue()),
    MOVEMENT_EFFECT(MinecraftPacketIds.MovementEffect.getValue()),
    CAMERA_AIM_ASSIST_PRESETS(MinecraftPacketIds.CameraAimAssistPresets.getValue()),
    PLAYER_VIDEO_CAPTURE(MinecraftPacketIds.PlayerVideoCapturePacket.getValue()),
    PLAYER_UPDATE_ENTITY_OVERRIDES(MinecraftPacketIds.PlayerUpdateEntityOverridesPacket.getValue()),
    PLAYER_LOCATION(MinecraftPacketIds.PlayerLocation.getValue()),
    //CAMERA_SPLINE(MinecraftPacketIds.CameraSplinePacket.getValue()),
    SYNC_WORLD_CLOCKS(MinecraftPacketIds.SyncWorldClocks.getValue()),
    VOXEL_SHAPES(MinecraftPacketIds.VoxelShapesPacket.getValue()),
    SET_PLAYER_FURNACE_OPTIONS(MinecraftPacketIds.SetPlayerFurnaceOptions.getValue()),
    RECORD_STARTED(MinecraftPacketIds.RecordStarted.getValue());
    //STONECUTTER_SET_RECIPE(MinecraftPacketIds.ClientboundStonecutterSetRecipePacket.getValue());
    // UPDATE_SOUND_DATA(MinecraftPacketIds.ClientboundUpdateSoundDataPacket.getValue());
    // CONTROL_SCHEME_SET(MinecraftPacketIds.ClientboundControlSchemeSetPacket.getValue()),
    // PRIMITIVE_SHAPES(MinecraftPacketIds.PrimitiveShapesPacket.getValue()),
    // DATA_STORE(MinecraftPacketIds.ClientboundDataStore.getValue()),
    // GRAPHICS_OVERRIDE_PARAMETER(MinecraftPacketIds.GraphicsParameterOverridePacket.getValue()),
    // DATA_DRIVEN_UI_SHOW_SCREEN(MinecraftPacketIds.ClientboundDataDrivenUIShowScreen.getValue()),
    // DATA_DRIVEN_UI_CLOSE_SCREEN(MinecraftPacketIds.ClientboundDataDrivenUICloseScreen.getValue()),
    // DATA_DRIVEN_UI_RELOAD(MinecraftPacketIds.ClientboundDataDrivenUIReload.getValue()),
    // TEXTURE_SHIFT(MinecraftPacketIds.ClientboundTextureShiftPacket.getValue()),
    // CAMERA_SPLINE(MinecraftPacketIds.CameraSpline.getValue()),
    // CAMERA_AIM_ASSIST_ACTOR_PRIORITY(MinecraftPacketIds.CameraAimAssistActorPriority.getValue()),
    // LOCATOR_BAR(MinecraftPacketIds.LocatorBarPacket.getValue()),
    // ATTRIBUTE_LAYER_SYNC(MinecraftPacketIds.ClientboundAttributeLayerSyncPacket.getValue()),
    // SERVER_STORE_INFO(MinecraftPacketIds.ServerStoreInfo.getValue()),
    // SERVER_PRESENCE_INFO(MinecraftPacketIds.ServerPresenceInfo.getValue());

    private static final ClientboundBedrockPackets[] REGISTRY = new ClientboundBedrockPackets[512];

    private final int id;

    static {
        for (ClientboundBedrockPackets packet : values()) {
            REGISTRY[packet.id] = packet;
        }
    }

    public static ClientboundBedrockPackets getPacket(final int id) {
        if (id < 0 || id >= REGISTRY.length) {
            return null;
        }

        return REGISTRY[id];
    }

    ClientboundBedrockPackets(final int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return name();
    }

}
