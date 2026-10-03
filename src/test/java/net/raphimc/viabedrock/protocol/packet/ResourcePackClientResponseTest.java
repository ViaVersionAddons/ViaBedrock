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
import com.viaversion.viaversion.protocol.packet.PacketWrapperImpl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ResourcePackResponse;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourcePackClientResponseTest {

    @Test
    void downloadingUsesTheStringDiscriminatorAndVarintPackCount() throws Exception {
        final String[] packIds = {"97e3f100-930a-4b47-b7ae-b8bbd38b1c19_1.0.0"};
        final PacketWrapper wrapper = new PacketWrapperImpl(ServerboundBedrockPackets.RESOURCE_PACK_CLIENT_RESPONSE, null, null);
        ResourcePackClientResponse.writeDownloading(wrapper, packIds);

        final ByteBuf buffer = Unpooled.buffer();
        try {
            wrapper.writeToBuffer(buffer);
            assertEquals(ServerboundBedrockPackets.RESOURCE_PACK_CLIENT_RESPONSE.getId(), BedrockTypes.UNSIGNED_VAR_INT.read(buffer));
            assertEquals(ResourcePackResponse.Downloading.getValue(), BedrockTypes.UNSIGNED_VAR_INT.read(buffer));
            assertEquals("downloading", BedrockTypes.STRING.read(buffer));
            assertArrayEquals(packIds, BedrockTypes.STRING_ARRAY.read(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void finishedResponsesContainTheStatusAndStringDiscriminator() throws Exception {
        for (final ResourcePackResponse status : new ResourcePackResponse[]{
            ResourcePackResponse.DownloadingFinished, ResourcePackResponse.ResourcePackStackFinished}) {
            final PacketWrapper wrapper = new PacketWrapperImpl(ServerboundBedrockPackets.RESOURCE_PACK_CLIENT_RESPONSE, null, null);
            ResourcePackClientResponse.write(wrapper, status);

            final ByteBuf buffer = Unpooled.buffer();
            try {
                wrapper.writeToBuffer(buffer);
                assertEquals(ServerboundBedrockPackets.RESOURCE_PACK_CLIENT_RESPONSE.getId(), BedrockTypes.UNSIGNED_VAR_INT.read(buffer));
                assertEquals(status.getValue(), BedrockTypes.UNSIGNED_VAR_INT.read(buffer));
                assertEquals(status == ResourcePackResponse.DownloadingFinished
                    ? "downloadingfinished" : "resourcepackstackfinished", BedrockTypes.STRING.read(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
    }

}
