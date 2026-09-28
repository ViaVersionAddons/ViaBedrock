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
package net.raphimc.viabedrock.experimental.model.container.block;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import net.raphimc.viabedrock.experimental.model.container.ExperimentalContainer;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.data.generated.bedrock.CustomBlockTags;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.rewriter.BlockStateRewriter;
import net.raphimc.viabedrock.protocol.storage.ChunkTracker;

public class ChestContainer extends ExperimentalContainer {

    private final boolean isBarrel;
    private final boolean isShulkerBox;

    public ChestContainer(final UserConnection user, final byte containerId, final TextComponent title, final BlockPosition position, final int size) {
        super(user, containerId, ContainerType.CONTAINER, title, position, size, CustomBlockTags.CHEST, CustomBlockTags.TRAPPED_CHEST, CustomBlockTags.BARREL, CustomBlockTags.SHULKER_BOX, CustomBlockTags.ENDER_CHEST);

        // TODO: Is there a better way to do this
        ChunkTracker tracker = user.get(ChunkTracker.class);
        BlockStateRewriter blockStateRewriter = user.get(BlockStateRewriter.class);
        int blockState = tracker.getBlockState(position);
        String tag = blockStateRewriter.tag(blockState);
        this.isBarrel = CustomBlockTags.BARREL.equals(tag);
        this.isShulkerBox = CustomBlockTags.SHULKER_BOX.equals(tag);
    }

    @Override
    public FullContainerName getFullContainerName(int slot) {
        if (isShulkerBox) {
            return new FullContainerName(ContainerEnumName.ShulkerBoxContainer, null);
        } else if (isBarrel) {
            return new FullContainerName(ContainerEnumName.BarrelContainer, null);
        } else {
            return new FullContainerName(ContainerEnumName.LevelEntityContainer, null);
        }
    }

}
