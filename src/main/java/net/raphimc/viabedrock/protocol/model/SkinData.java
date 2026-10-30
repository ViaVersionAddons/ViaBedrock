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
package net.raphimc.viabedrock.protocol.model;

import java.awt.image.BufferedImage;
import java.util.List;

public record SkinData(String skinId, String skinResourcePatch, BufferedImage skinData, List<AnimationData> animations, BufferedImage capeData,
                       String geometryData, String geometryDataEngineVersion, String animationData, boolean premium, boolean persona, boolean capeOnClassic, boolean primaryUser,
                       String capeId, String fullSkinId, String armSize, String skinColor, List<PersonaPieceData> personaPieces, List<PersonaPieceTintData> tintColors,
                       boolean overridingPlayerAppearance) {

    public record AnimationData(BufferedImage image, int type, float frames, int expression) {
    }

    public record PersonaPieceData(String id, String type, String packId, boolean defaultPiece, String productId) {
    }

    public record PersonaPieceTintData(String type, List<String> colors) {
    }

}
