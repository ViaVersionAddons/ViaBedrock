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
package net.raphimc.viabedrock.protocol.types.entitydata;

import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PassengerOfBlockArguments_EmoteType;
import net.raphimc.viabedrock.protocol.model.PassengerOfBlocks;
import net.raphimc.viabedrock.protocol.model.Position3f;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

public class PassengerOfBlocksType extends Type<PassengerOfBlocks> {

    public PassengerOfBlocksType() {
        super(PassengerOfBlocks.class);
    }

    @Override
    public PassengerOfBlocks read(final ByteBuf buffer) {

        final BlockPosition position = BedrockTypes.BLOCK_POSITION.read(buffer);
        final Position3f offset = BedrockTypes.POSITION_3F.read(buffer);
        final float rotation = BedrockTypes.FLOAT_LE.read(buffer);
        final float rotationLimit = BedrockTypes.FLOAT_LE.read(buffer);
        final PassengerOfBlockArguments_EmoteType emote = PassengerOfBlockArguments_EmoteType.getByValue(buffer.readByte());

        return new PassengerOfBlocks(position, offset, rotation, rotationLimit, emote);
    }

    @Override
    public void write(final ByteBuf buffer, final PassengerOfBlocks value) {
        BedrockTypes.BLOCK_POSITION.write(buffer, value.position());
        BedrockTypes.POSITION_3F.write(buffer, value.offset());
        BedrockTypes.FLOAT_LE.write(buffer, value.rotation());
        BedrockTypes.FLOAT_LE.write(buffer, value.rotationLimit());
        buffer.writeByte(value.emote().getValue());
    }

}
