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
package net.raphimc.viabedrock.protocol.rewriter.blockentity;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import net.raphimc.viabedrock.api.chunk.BedrockBlockEntity;
import net.raphimc.viabedrock.api.chunk.BlockEntityWithBlockState;
import net.raphimc.viabedrock.api.model.BlockState;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.storage.ChunkTracker;

public final class ChestBlockEntityRewriter extends LootableContainerBlockEntityRewriter {

    @Override
    public BlockEntity toJava(final UserConnection user, final BedrockBlockEntity bedrockBlockEntity) {
        final BlockEntity javaBlockEntity = super.toJava(user, bedrockBlockEntity);
        final int originalId = user.get(ChunkTracker.class).getJavaBlockState(bedrockBlockEntity.position());
        final BlockState original = BedrockProtocol.MAPPINGS.getJavaBlockStates().inverse().get(originalId);
        final BlockState paired = withPair(original, bedrockBlockEntity);
        if (paired == original) {
            return javaBlockEntity;
        }
        final Integer javaId = BedrockProtocol.MAPPINGS.getJavaBlockStates().get(paired);
        return javaId == null ? javaBlockEntity : new BlockEntityWithBlockState(javaBlockEntity, javaId);
    }

    static BlockState withPair(final BlockState state, final BedrockBlockEntity blockEntity) {
        if (state == null || !state.properties().containsKey("type") || !state.properties().containsKey("facing")) {
            return state;
        }
        final CompoundTag tag = blockEntity.tag();
        if (!(tag.get("pairx") instanceof IntTag pairX) || !(tag.get("pairz") instanceof IntTag pairZ)) {
            return state;
        }

        final long dx = (long) pairX.asInt() - blockEntity.position().x();
        final long dz = (long) pairZ.asInt() - blockEntity.position().z();
        if (Math.abs(dx) + Math.abs(dz) != 1) {
            return state;
        }

        final boolean left = switch (state.properties().get("facing")) {
            case "north" -> dx == 1;
            case "east" -> dz == 1;
            case "south" -> dx == -1;
            case "west" -> dz == -1;
            default -> false;
        };
        final boolean right = switch (state.properties().get("facing")) {
            case "north" -> dx == -1;
            case "east" -> dz == -1;
            case "south" -> dx == 1;
            case "west" -> dz == 1;
            default -> false;
        };
        return left ? state.withProperty("type", "left") : right ? state.withProperty("type", "right") : state;
    }

}
