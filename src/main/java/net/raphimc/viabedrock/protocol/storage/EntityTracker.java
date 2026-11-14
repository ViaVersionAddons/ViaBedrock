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
package net.raphimc.viabedrock.protocol.storage;

import com.viaversion.viaversion.api.connection.StoredObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.ChunkPosition;
import com.viaversion.viaversion.api.minecraft.Vector3d;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes26_3;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.fastutil.longs.Long2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.longs.Long2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntMap;
import com.viaversion.viaversion.libs.fastutil.objects.Object2IntOpenHashMap;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.BlockState;
import net.raphimc.viabedrock.api.model.entity.*;
import net.raphimc.viabedrock.protocol.BedrockProtocol;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class EntityTracker extends StoredObject {

    private final AtomicInteger idCounter = new AtomicInteger(2);

    private ClientPlayerEntity clientPlayerEntity = null;
    private final Long2ObjectMap<Entity> entities = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<Long> runtimeIdToUniqueId = new Long2ObjectOpenHashMap<>();
    private final Int2ObjectMap<Long> javaIdToUniqueId = new Int2ObjectOpenHashMap<>();
    private final Object2IntMap<BlockPosition> itemFrames = new Object2IntOpenHashMap<>();

    public EntityTracker(final UserConnection user) {
        super(user);
    }

    public Entity addEntity(final long uniqueId, final long runtimeId, final String type, final EntityTypes26_3 javaType) {
        final UUID javaUuid = UUID.randomUUID();
        if (javaType.isOrHasParent(EntityTypes26_3.ABSTRACT_HORSE)) {
            return this.addEntity(new AbstractHorseEntity(this.user(), uniqueId, runtimeId, type, this.getNextJavaEntityId(), javaUuid, javaType));
        } else if (javaType.isOrHasParent(EntityTypes26_3.MOB)) {
            return this.addEntity(new MobEntity(this.user(), uniqueId, runtimeId, type, this.getNextJavaEntityId(), javaUuid, javaType));
        } else if (javaType.isOrHasParent(EntityTypes26_3.LIVING_ENTITY)) {
            return this.addEntity(new LivingEntity(this.user(), uniqueId, runtimeId, type, this.getNextJavaEntityId(), javaUuid, javaType));
        } else {
            return this.addEntity(new Entity(this.user(), uniqueId, runtimeId, type, this.getNextJavaEntityId(), javaUuid, javaType));
        }
    }

    public <T extends Entity> T addEntity(final T entity) {
        return this.addEntity(entity, true);
    }

    public <T extends Entity> T addEntity(final T entity, final boolean updateTeam) {
        if (entity instanceof ClientPlayerEntity playerEntity) {
            this.clientPlayerEntity = playerEntity;
        }

        final Entity prevEntity = this.entities.put(entity.uniqueId(), entity);
        if (prevEntity != null) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Duplicate entity unique ID: " + entity.uniqueId());
            this.removeEntity(prevEntity);
            final PacketWrapper removeEntities = PacketWrapper.create(ClientboundPackets26_3.REMOVE_ENTITIES, this.user());
            removeEntities.write(Types.VAR_INT_ARRAY_PRIMITIVE, new int[]{prevEntity.javaId()}); // entity ids
            removeEntities.send(BedrockProtocol.class);
        }
        if (this.javaIdToUniqueId.put(entity.javaId(), (Long) entity.uniqueId()) != null) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Duplicate Java entity ID: " + entity.javaId());
        }
        if (this.runtimeIdToUniqueId.putIfAbsent(entity.runtimeId(), (Long) entity.uniqueId()) != null) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Duplicate entity runtime ID: " + entity.runtimeId());
        }

        if (updateTeam && entity instanceof PlayerEntity player) {
            player.createTeam();
        }

        return entity;
    }

    public void removeEntity(final Entity entity) {
        if (entity instanceof ClientPlayerEntity) {
            throw new IllegalArgumentException("Cannot remove client player entity");
        }

        this.entities.remove(entity.uniqueId());
        this.runtimeIdToUniqueId.remove(entity.runtimeId());
        this.javaIdToUniqueId.remove(entity.javaId());
        entity.remove();
    }

    public void spawnItemFrame(final BlockPosition position, final BlockState blockState) {
        this.removeItemFrame(position);

        if (!blockState.identifier().equals("frame") && !blockState.identifier().equals("glow_frame")) {
            throw new IllegalArgumentException("Block state must be a frame or glow_frame");
        }

        final int javaId = this.getNextJavaEntityId();
        this.itemFrames.put(position, javaId);

        final PacketWrapper spawnEntity = PacketWrapper.create(ClientboundPackets26_3.ADD_ENTITY, this.user());
        spawnEntity.write(Types.VAR_INT, javaId); // entity id
        spawnEntity.write(Types.UUID, UUID.randomUUID()); // uuid
        spawnEntity.write(Types.VAR_INT, blockState.identifier().equals("frame") ? EntityTypes26_3.ITEM_FRAME.getId() : EntityTypes26_3.GLOW_ITEM_FRAME.getId()); // type id
        spawnEntity.write(Types.DOUBLE, (double) position.x()); // x
        spawnEntity.write(Types.DOUBLE, (double) position.y()); // y
        spawnEntity.write(Types.DOUBLE, (double) position.z()); // z
        spawnEntity.write(Types.LOW_PRECISION_VECTOR, Vector3d.ZERO); // velocity
        spawnEntity.write(Types.BYTE, (byte) 0); // pitch
        spawnEntity.write(Types.BYTE, (byte) 0); // yaw
        spawnEntity.write(Types.BYTE, (byte) 0); // head yaw
        spawnEntity.write(Types.VAR_INT, Integer.valueOf(blockState.properties().get("facing_direction"))); // data
        spawnEntity.send(BedrockProtocol.class);
    }

    public void removeItemFrame(final BlockPosition position) {
        if (this.itemFrames.containsKey(position)) {
            final PacketWrapper removeEntities = PacketWrapper.create(ClientboundPackets26_3.REMOVE_ENTITIES, this.user());
            removeEntities.write(Types.VAR_INT_ARRAY_PRIMITIVE, new int[]{this.itemFrames.getInt(position)}); // entity ids
            removeEntities.send(BedrockProtocol.class);
        }
    }

    public void removeItemFrame(final ChunkPosition chunkPos) {
        final List<BlockPosition> toRemove = new ArrayList<>();
        for (BlockPosition position : this.itemFrames.keySet()) {
            if (position.x() >> 4 == chunkPos.chunkX() && position.z() >> 4 == chunkPos.chunkZ()) {
                toRemove.add(position);
            }
        }
        for (BlockPosition position : toRemove) {
            this.removeItemFrame(position);
        }
    }

    public void tick() {
        for (Entity entity : this.entities.values()) {
            if (entity != this.clientPlayerEntity) {
                entity.tick();
            }
        }
    }

    public void prepareForRespawn() {
        for (Entity entity : this.entities.values()) {
            entity.remove();
        }
    }

    public Entity getEntityByRid(final long runtimeId) {
        final Long uniqueId = this.runtimeIdToUniqueId.get(runtimeId);
        if (uniqueId == null) {
            return null;
        }
        return this.entities.get(uniqueId.longValue());
    }

    public Entity getEntityByUid(final long uniqueId) {
        return this.entities.get(uniqueId);
    }

    public Entity getEntityByJid(final int javaId) {
        final Long uniqueId = this.javaIdToUniqueId.get(javaId);
        if (uniqueId == null) {
            return null;
        }
        return this.entities.get((long) this.javaIdToUniqueId.get(javaId));
    }

    public ClientPlayerEntity getClientPlayer() {
        return this.clientPlayerEntity;
    }

    public boolean isEmpty() {
        return this.entities.isEmpty() || (this.entities.size() == 1 && this.entities.containsKey(this.clientPlayerEntity.uniqueId()));
    }

    public int getNextJavaEntityId() {
        return this.idCounter.getAndIncrement();
    }

}
