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
package net.raphimc.viabedrock.protocol.data;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.TextComponentCodec;
import com.viaversion.viaversion.protocols.v26_2to26_3.Protocol26_2To26_3;

public final class ProtocolConstants {

    public static final ProtocolVersion JAVA_VERSION = ProtocolVersion.v26_3;
    public static final Class<? extends Protocol<?, ?, ?, ?>> JAVA_PROTOCOL_CLASS = Protocol26_2To26_3.class;
    public static final int JAVA_PACK_VERSION = 88;
    public static final TextComponentCodec JAVA_TEXT_COMPONENT_SERIALIZER = TextComponentCodec.V26_1;
    public static final int JAVA_AIR_ID = 0;

    public static final String BEDROCK_VERSION_NAME = "1.26.60";
    public static final int BEDROCK_PROTOCOL_VERSION = 2225;
    public static final int BEDROCK_RAKNET_PROTOCOL_VERSION = 11;
    public static final int BEDROCK_DEFAULT_PORT = 19132;
    public static final int BEDROCK_NETHERNET_LAN_DEFAULT_PORT = 7551;
    public static final String BEDROCK_COMMAND_VERSION = "latest";
    public static final byte BEDROCK_REQUEST_CHUNK_RADIUS_MAX_RADIUS = 28;

    public static final int LAST_BLOCK_ITEM_ID = 255;
    public static final float PLAYER_GRAVITY = 0.08F;
    public static final float BLOCK_FRICTION = 0.6F;

    public static StructuredDataContainer createStructuredDataContainer() {
        final StructuredDataContainer data = new StructuredDataContainer();
        data.setIdLookup(Via.getManager().getProtocolManager().getProtocol(JAVA_PROTOCOL_CLASS), true);
        return data;
    }

    private ProtocolConstants() {
    }

}
