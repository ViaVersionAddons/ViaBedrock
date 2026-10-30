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

import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.type.types.entitydata.EntityDataTypeTemplate;
import io.netty.buffer.ByteBuf;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.DataItemType;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

public class EntityDataType extends EntityDataTypeTemplate {

    @Override
    public EntityData read(final ByteBuf buffer) {
        final int index = BedrockTypes.UNSIGNED_VAR_INT.read(buffer); // id
        final int rawDataItemType = BedrockTypes.UNSIGNED_VAR_INT.read(buffer); // oneOf
        final int typev2 = buffer.readByte(); // type
        if (rawDataItemType != typev2) { // #blameMojang
            throw new IllegalStateException("DataItemType mismatch: " + rawDataItemType + " != " + typev2);
        }
        final DataItemType dataItemType = DataItemType.getByValue(rawDataItemType);
        if (dataItemType == null) { // Bedrock client disconnects if the data item type is not valid
            throw new IllegalStateException("Unknown DataItemType: " + rawDataItemType);
        }
        final EntityDataTypesBedrock type = EntityDataTypesBedrock.byDataItemType(dataItemType);
        return new EntityData(index, type, type.type().read(buffer));
    }

    @Override
    public void write(final ByteBuf buffer, final EntityData value) {
        BedrockTypes.UNSIGNED_VAR_INT.write(buffer, value.id()); // id
        BedrockTypes.UNSIGNED_VAR_INT.write(buffer, value.dataType().typeId()); // oneOf
        buffer.writeByte(value.dataType().typeId()); // type
        value.dataType().type().write(buffer, value.value()); // value
    }

}
