/*
 * This file is part of ViaBedrock - https://github.com/ViaVersionAddons/ViaBedrock
 * Copyright (C) 2023-2026 RK_01
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
package net.raphimc.viabedrock.api.model.entity;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes26_3;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.protocol.data.enums.java.EntityEvent;
import net.raphimc.viabedrock.protocol.data.generated.java.EntityDataFields;

import java.util.List;
import java.util.UUID;

public class ArmadilloEntity extends MobEntity {

    private static final int IDLE = 0;
    private static final int ROLLING = 1;
    private static final int SCARED = 2;
    private static final int UNROLLING = 3;
    private static final int ROLL_DURATION_TICKS = 10;

    private String bedrockState;
    private int javaState = -1;
    private int rollingTicks;
    private boolean pendingPeek;

    public ArmadilloEntity(final UserConnection user, final long uniqueId, final long runtimeId, final String type, final int javaId, final UUID javaUuid) {
        super(user, uniqueId, runtimeId, type, javaId, javaUuid, EntityTypes26_3.ARMADILLO);
    }

    public void translateState(final String state, final List<EntityData> javaEntityData) {
        if (state.equals(this.bedrockState)) {
            return;
        }

        final int javaState = switch (state) {
            case "unrolled" -> IDLE;
            case "rolled_up" -> "unrolled".equals(this.bedrockState) ? ROLLING : SCARED;
            case "rolled_up_peeking", "rolled_up_relaxing" -> SCARED;
            case "rolled_up_unrolling" -> UNROLLING;
            default -> -1;
        };
        if (javaState == -1) {
            ViaBedrock.getPlatform().getLogger().warning("Received unknown armadillo state " + state);
            return;
        }

        this.bedrockState = state;
        this.pendingPeek = state.equals("rolled_up_peeking");
        this.rollingTicks = javaState == ROLLING ? ROLL_DURATION_TICKS : 0;
        if (this.javaState != javaState) {
            this.javaState = javaState;
            javaEntityData.add(this.stateData());
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.rollingTicks > 0 && --this.rollingTicks == 0) {
            this.javaState = SCARED;
            this.sendEntityData(List.of(this.stateData()));
        }
        // The client needs the SCARED metadata before it can play the peek animation.
        if (this.pendingPeek) {
            this.pendingPeek = false;
            PacketFactory.sendJavaEntityEvent(this.user, this, EntityEvent.ARMADILLO_PEEK);
        }
    }

    private EntityData stateData() {
        return new EntityData(this.getJavaEntityDataIndex(EntityDataFields.ARMADILLO_STATE), VersionedTypes.V26_3.entityDataTypes().armadilloState, this.javaState);
    }

}
