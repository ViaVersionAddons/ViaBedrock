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
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ResourcePackResponse;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

public final class ResourcePackClientResponse {

    private ResourcePackClientResponse() {
    }

    public static void write(final PacketWrapper wrapper, final ResourcePackResponse status) {
        if (status == ResourcePackResponse.Downloading) {
            throw new IllegalArgumentException("Downloading requires a pack list");
        }
        writeStatus(wrapper, status);
    }

    public static void writeDownloading(final PacketWrapper wrapper, final String[] packIds) {
        writeStatus(wrapper, ResourcePackResponse.Downloading);
        wrapper.write(BedrockTypes.STRING_ARRAY, packIds);
    }

    private static void writeStatus(final PacketWrapper wrapper, final ResourcePackResponse status) {
        final String name = switch (status) {
            case Cancel -> "cancel";
            case Downloading -> "downloading";
            case DownloadingFinished -> "downloadingfinished";
            case ResourcePackStackFinished -> "resourcepackstackfinished";
        };
        wrapper.write(BedrockTypes.UNSIGNED_VAR_INT, status.getValue());
        wrapper.write(BedrockTypes.STRING, name);
    }

}
