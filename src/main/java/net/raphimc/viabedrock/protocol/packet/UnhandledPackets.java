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
package net.raphimc.viabedrock.protocol.packet;

import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ServerboundPackets26_3;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ClientboundBedrockPackets;

public final class UnhandledPackets {

    public static void register(final BedrockProtocol protocol) {
        protocol.cancelClientbound(ClientboundBedrockPackets.SET_HEALTH); // Seems to do nothing meaningful
        protocol.cancelClientbound(ClientboundBedrockPackets.CAMERA); // Not relevant (Education Edition)
        protocol.cancelClientbound(ClientboundBedrockPackets.PHOTO_TRANSFER); // Not relevant (Education Edition)
        protocol.cancelClientbound(ClientboundBedrockPackets.SHOW_PROFILE);
        protocol.cancelClientbound(ClientboundBedrockPackets.LAB_TABLE); // Not relevant (Education Edition)
        protocol.cancelClientbound(ClientboundBedrockPackets.EDUCATION_SETTINGS); // Not relevant (Education Edition)
        protocol.cancelClientbound(ClientboundBedrockPackets.EMOTE); // Not possible in Java Edition
        protocol.cancelClientbound(ClientboundBedrockPackets.CODE_BUILDER); // Not relevant (Education Edition)
        protocol.cancelClientbound(ClientboundBedrockPackets.EMOTE_LIST); // Not possible in Java Edition
        protocol.cancelClientbound(ClientboundBedrockPackets.CAMERA_SHAKE); // Not possible in Java Edition
        protocol.cancelClientbound(ClientboundBedrockPackets.PLAYER_FOG); // Not possible in Java Edition
        protocol.cancelClientbound(ClientboundBedrockPackets.EDU_URI_RESOURCE); // Not relevant (Education Edition)
        protocol.cancelClientbound(ClientboundBedrockPackets.SCRIPT_MESSAGE); // Not relevant (Education Edition)
        protocol.cancelClientbound(ClientboundBedrockPackets.LESSON_PROGRESS); // Not relevant (Education Edition)
        protocol.cancelClientbound(ClientboundBedrockPackets.CAMERA_PRESETS); // Not possible in Java Edition
        protocol.cancelClientbound(ClientboundBedrockPackets.CAMERA_INSTRUCTION); // Not possible in Java Edition
        protocol.cancelClientbound(ClientboundBedrockPackets.SET_HUD); // Not possible in Java Edition
        protocol.cancelClientbound(ClientboundBedrockPackets.CURRENT_STRUCTURE_FEATURE); // Useless
        protocol.cancelClientbound(ClientboundBedrockPackets.CAMERA_AIM_ASSIST); // Not possible in Java Edition
        protocol.cancelClientbound(ClientboundBedrockPackets.CAMERA_AIM_ASSIST_PRESETS); // Not possible in Java Edition
        protocol.cancelClientbound(ClientboundBedrockPackets.PLAYER_VIDEO_CAPTURE); // Not possible in Java Edition

        protocol.registerServerboundTransition(ServerboundConfigurationPackets1_21_9.KEEP_ALIVE, null, PacketWrapper::cancel);
        protocol.cancelServerbound(ServerboundPackets26_3.CHAT_ACK);
        protocol.cancelServerbound(ServerboundPackets26_3.CHAT_SESSION_UPDATE);
        protocol.cancelServerbound(ServerboundPackets26_3.CHUNK_BATCH_RECEIVED);
        protocol.cancelServerbound(ServerboundPackets26_3.COOKIE_RESPONSE);
        protocol.cancelServerbound(ServerboundPackets26_3.DEBUG_SUBSCRIPTION_REQUEST);
        protocol.cancelServerbound(ServerboundPackets26_3.KEEP_ALIVE);
        protocol.cancelServerbound(ServerboundPackets26_3.PLAYER_LOADED);
        protocol.cancelServerbound(ServerboundPackets26_3.SET_TEST_BLOCK);
        protocol.cancelServerbound(ServerboundPackets26_3.TEST_INSTANCE_BLOCK_ACTION);
    }

    private UnhandledPackets() {
    }

}
