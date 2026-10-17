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
package net.raphimc.viabedrock.api.model.entity;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Vector3d;
import com.viaversion.viaversion.api.minecraft.Vector3f;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes26_3;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.item.StructuredItem;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.modinterface.ViaBedrockUtilityInterface;
import net.raphimc.viabedrock.api.resourcepack.definition.EntityDefinitions;
import net.raphimc.viabedrock.api.util.MathUtil;
import net.raphimc.viabedrock.api.util.MoLangEngine;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ActorDataIds;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ActorFlags;
import net.raphimc.viabedrock.protocol.data.generated.java.EntityDataFields;
import net.raphimc.viabedrock.protocol.model.Position3f;
import net.raphimc.viabedrock.protocol.rewriter.resourcepack.CustomEntityResourceRewriter;
import net.raphimc.viabedrock.protocol.storage.ChannelStorage;
import net.raphimc.viabedrock.protocol.storage.EntityTracker;
import net.raphimc.viabedrock.protocol.storage.ResourcePackStorage;
import org.cube.converter.data.bedrock.BedrockEntityData;
import org.cube.converter.data.bedrock.controller.BedrockRenderController;
import team.unnamed.mocha.runtime.Scope;
import team.unnamed.mocha.runtime.binding.JavaObjectBinding;
import team.unnamed.mocha.runtime.standard.MochaMath;
import team.unnamed.mocha.runtime.value.MutableObjectBinding;
import team.unnamed.mocha.runtime.value.Value;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class CustomEntity extends Entity {

    private static final Scope BASE_SCOPE = Scope.create();

    static {
        //noinspection UnstableApiUsage
        BASE_SCOPE.set("math", JavaObjectBinding.of(MochaMath.class, null, new MochaMath()));
        BASE_SCOPE.readOnly(true);
    }

    private final EntityDefinitions.EntityDefinition entityDefinition;
    private final Map<String, String> inverseGeometryMap = new HashMap<>();
    private final Map<String, String> inverseTextureMap = new HashMap<>();
    private final Scope entityScope = BASE_SCOPE.copy();
    private final List<EvaluatedModel> models = new ArrayList<>();
    private final List<ItemDisplayEntity> partEntities = new ArrayList<>();
    private boolean spawned;

    public CustomEntity(final UserConnection user, final long uniqueId, final long runtimeId, final String type, final int javaId, final EntityDefinitions.EntityDefinition entityDefinition) {
        super(user, uniqueId, runtimeId, type, javaId, UUID.randomUUID(), EntityTypes26_3.INTERACTION);
        this.entityDefinition = entityDefinition;

        final MutableObjectBinding variableBinding = new MutableObjectBinding();
        this.entityScope.set("variable", variableBinding);
        this.entityScope.set("v", variableBinding);
        try {
            for (String initExpression : this.entityDefinition.entityData().getScripts().initialize()) {
                MoLangEngine.eval(this.entityScope, initExpression);
            }
        } catch (final Throwable e) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to initialize custom entity variables", e);
        }

        final MutableObjectBinding geometryBinding = new MutableObjectBinding();
        for (Map.Entry<String, String> entry : entityDefinition.entityData().getGeometries().entrySet()) {
            geometryBinding.set(entry.getKey(), Value.of(entry.getValue()));
            this.inverseGeometryMap.putIfAbsent(entry.getValue(), entry.getKey());
        }
        geometryBinding.block();
        this.entityScope.set("geometry", geometryBinding);

        final MutableObjectBinding textureBinding = new MutableObjectBinding();
        for (Map.Entry<String, String> entry : entityDefinition.entityData().getTextures().entrySet()) {
            textureBinding.set(entry.getKey(), Value.of(entry.getValue()));
            this.inverseTextureMap.putIfAbsent(entry.getValue(), entry.getKey());
        }
        textureBinding.block();
        this.entityScope.set("texture", textureBinding);

        final MutableObjectBinding materialBinding = new MutableObjectBinding();
        materialBinding.block();
        this.entityScope.set("material", materialBinding);

        this.entityScope.readOnly(true);
    }

    @Override
    public void setPosition(final Position3f position) {
        super.setPosition(position);

        if (!this.spawned) {
            this.evaluateRenderControllerChange();
            this.spawn();
        } else {
            this.partEntities.forEach(ItemDisplayEntity::updatePositionAndRotation);
        }
    }

    @Override
    public void setRotation(final Position3f rotation) {
        super.setRotation(rotation);

        if (this.spawned) {
            this.partEntities.forEach(ItemDisplayEntity::updatePositionAndRotation);
        }
    }

    @Override
    public void remove() {
        super.remove();
        this.despawn();
    }

    @Override
    protected void onEntityDataChanged() {
        super.onEntityDataChanged();

        if (this.evaluateRenderControllerChange()) {
            this.despawn();
            this.spawn();
        }
    }

    private void spawn() {
        this.spawned = true;
        if (this.models.isEmpty()) {
            return;
        }

        final EntityTracker entityTracker = this.user.get(EntityTracker.class);
        final ResourcePackStorage resourcePackStorage = this.user.get(ResourcePackStorage.class);
        final ChannelStorage channelStorage = this.user.get(ChannelStorage.class);
        if (channelStorage.hasChannel(ViaBedrockUtilityInterface.CONFIRM_CHANNEL)) {
            ViaBedrockUtilityInterface.spawnCustomEntity(this.user, this.javaUuid(), this.entityDefinition.identifier(), this.entityData());
            return;
        }

        for (EvaluatedModel model : this.models) {
            final String converterKey = this.entityDefinition.identifier() + "_" + model.key();
            if (!resourcePackStorage.getConverterData().containsKey("ce_" + converterKey + "_scale")) {
                continue;
            }

            final ItemDisplayEntity partEntity = new ItemDisplayEntity(entityTracker.getNextJavaEntityId());
            this.partEntities.add(partEntity);
            final List<EntityData> javaEntityData = new ArrayList<>();

            final StructuredDataContainer data = ProtocolConstants.createStructuredDataContainer();
            data.set(StructuredDataKey.ITEM_MODEL, CustomEntityResourceRewriter.getItemModel(this.entityDefinition.identifier()));
            data.set(StructuredDataKey.CUSTOM_MODEL_DATA1_21_4, CustomEntityResourceRewriter.getCustomModelData(model.key()));
            final StructuredItem item = new StructuredItem(BedrockProtocol.MAPPINGS.getJavaItems().get("minecraft:paper"), 1, data);
            javaEntityData.add(new EntityData(partEntity.getJavaEntityDataIndex(EntityDataFields.ITEM_STACK), VersionedTypes.V26_3.entityDataTypes.itemType, item));

            final float scale = ((Number) resourcePackStorage.getConverterData().get("ce_" + converterKey + "_scale")).floatValue();
            javaEntityData.add(new EntityData(partEntity.getJavaEntityDataIndex(EntityDataFields.SCALE), VersionedTypes.V26_3.entityDataTypes.vector3FType, new Vector3f(scale, scale, scale)));
            javaEntityData.add(new EntityData(partEntity.getJavaEntityDataIndex(EntityDataFields.TRANSLATION), VersionedTypes.V26_3.entityDataTypes.vector3FType, new Vector3f(0F, scale * 0.5F, 0F)));

            final PacketWrapper addEntity = PacketWrapper.create(ClientboundPackets26_3.ADD_ENTITY, this.user);
            addEntity.write(Types.VAR_INT, partEntity.javaId()); // entity id
            addEntity.write(Types.UUID, partEntity.javaUuid()); // uuid
            addEntity.write(Types.VAR_INT, partEntity.javaType().getId()); // type id
            addEntity.write(Types.DOUBLE, (double) this.position.x()); // x
            addEntity.write(Types.DOUBLE, (double) this.position.y()); // y
            addEntity.write(Types.DOUBLE, (double) this.position.z()); // z
            addEntity.write(Types.LOW_PRECISION_VECTOR, Vector3d.ZERO); // velocity
            addEntity.write(Types.BYTE, MathUtil.float2Byte(this.rotation.x())); // pitch
            addEntity.write(Types.BYTE, MathUtil.float2Byte(this.rotation.y())); // yaw
            addEntity.write(Types.BYTE, MathUtil.float2Byte(this.rotation.z())); // head yaw
            addEntity.write(Types.VAR_INT, 0); // data
            addEntity.send(BedrockProtocol.class);

            final PacketWrapper setEntityData = PacketWrapper.create(ClientboundPackets26_3.SET_ENTITY_DATA, this.user);
            setEntityData.write(Types.VAR_INT, partEntity.javaId()); // entity id
            setEntityData.write(VersionedTypes.V26_3.entityDataList, javaEntityData); // entity data
            setEntityData.send(BedrockProtocol.class);
        }
    }

    private void despawn() {
        this.spawned = false;
        final int[] entityIds = new int[this.partEntities.size()];
        for (int i = 0; i < this.partEntities.size(); i++) {
            entityIds[i] = this.partEntities.get(i).javaId();
        }
        this.partEntities.clear();
        final PacketWrapper removeEntities = PacketWrapper.create(ClientboundPackets26_3.REMOVE_ENTITIES, this.user);
        removeEntities.write(Types.VAR_INT_ARRAY_PRIMITIVE, entityIds); // entity ids
        removeEntities.send(BedrockProtocol.class);
    }

    private boolean evaluateRenderControllerChange() {
        final Scope executionScope = this.entityScope.copy();
        final MutableObjectBinding queryBinding = new MutableObjectBinding();
        if (this.entityData.containsKey(ActorDataIds.VARIANT)) {
            queryBinding.set("variant", Value.of(this.entityData.get(ActorDataIds.VARIANT).<Integer>value()));
        }
        if (this.entityData.containsKey(ActorDataIds.MARK_VARIANT)) {
            queryBinding.set("mark_variant", Value.of(this.entityData.get(ActorDataIds.MARK_VARIANT).<Integer>value()));
        }

        final Set<ActorFlags> entityFlags = this.entityFlags();
        for (Map.Entry<ActorFlags, String> entry : BedrockProtocol.MAPPINGS.getBedrockEntityFlagMoLangQueries().entrySet()) {
            if (entityFlags.contains(entry.getKey())) {
                queryBinding.set(entry.getValue(), Value.of(true));
            }
        }
        if (entityFlags.contains(ActorFlags.ONFIRE)) { // "on fire" flag has two names in MoLang
            queryBinding.set("is_onfire", Value.of(true));
        }

        queryBinding.block();
        executionScope.set("query", queryBinding);
        executionScope.set("q", queryBinding);

        final List<EvaluatedModel> newModels = new ArrayList<>();
        final ResourcePackStorage resourcePackStorage = user.get(ResourcePackStorage.class);
        for (BedrockEntityData.RenderController entityRenderController : this.entityDefinition.entityData().getControllers()) {
            final BedrockRenderController renderController = resourcePackStorage.getRenderControllers().get(entityRenderController.identifier());
            if (renderController == null) {
                continue;
            }
            if (!entityRenderController.condition().isBlank()) {
                try {
                    final Value conditionResult = MoLangEngine.eval(executionScope, entityRenderController.condition());
                    if (!conditionResult.getAsBoolean()) {
                        continue;
                    }
                } catch (final Throwable e) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to evaluate render controller condition", e);
                    continue;
                }
            }

            try {
                final Scope renderControllerGeometryScope = executionScope.copy();
                renderControllerGeometryScope.set("array", this.getArrayBinding(executionScope, renderController.geometries()));
                final Scope renderControllerTextureScope = executionScope.copy();
                renderControllerTextureScope.set("array", this.getArrayBinding(executionScope, renderController.textures()));

                final String geometryValue = MoLangEngine.eval(renderControllerGeometryScope, renderController.geometryExpression()).getAsString();
                final String geometryName = this.inverseGeometryMap.get(geometryValue);
                for (String textureExpression : renderController.textureExpressions()) {
                    final String textureValue = MoLangEngine.eval(renderControllerTextureScope, textureExpression).getAsString();
                    final String textureName = this.inverseTextureMap.get(textureValue);
                    if (geometryName != null && textureName != null) {
                        newModels.add(new EvaluatedModel(geometryName + "_" + textureName, geometryValue, textureValue));
                    }
                }
            } catch (final Throwable e) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to evaluate render controller", e);
                this.models.clear();
                return true;
            }
        }

        if (!newModels.isEmpty() && !this.models.equals(newModels)) {
            this.models.clear();
            this.models.addAll(newModels);
            return true;
        } else {
            return false;
        }
    }

    private MutableObjectBinding getArrayBinding(final Scope executionScope, final List<BedrockRenderController.Array> arrays) throws IOException {
        final MutableObjectBinding arrayBinding = new MutableObjectBinding();
        for (BedrockRenderController.Array array : arrays) {
            if (array.name().toLowerCase(Locale.ROOT).startsWith("array.")) {
                final String[] resolvedExpressions = new String[array.values().size()];
                for (int i = 0; i < array.values().size(); i++) {
                    resolvedExpressions[i] = MoLangEngine.eval(executionScope, array.values().get(i)).getAsString();
                }
                arrayBinding.set(array.name().substring(6), Value.of(resolvedExpressions));
            }
        }
        arrayBinding.block();
        return arrayBinding;
    }

    public record EvaluatedModel(String key, String geometryValue, String textureValue) {
    }

    private class ItemDisplayEntity extends Entity {

        ItemDisplayEntity(final int javaId) {
            super(CustomEntity.this.user, 0L, 0L, null, javaId, UUID.randomUUID(), EntityTypes26_3.ITEM_DISPLAY);
        }

        public void updatePositionAndRotation() {
            final PacketWrapper entityPositionSync = PacketWrapper.create(ClientboundPackets26_3.ENTITY_POSITION_SYNC, this.user);
            entityPositionSync.write(Types.VAR_INT, this.javaId()); // entity id
            entityPositionSync.write(Types.VAR_INT, 1); // Stepped
            entityPositionSync.write(Types.VAR_INT, 1); // 1 step
            entityPositionSync.write(Types.DOUBLE, (double) CustomEntity.this.position.x()); // x
            entityPositionSync.write(Types.DOUBLE, (double) CustomEntity.this.position.y()); // y
            entityPositionSync.write(Types.DOUBLE, (double) CustomEntity.this.position.z()); // z
            entityPositionSync.write(Types.VAR_INT, 3); // INTERPOLATION_STEP_TICKS
            entityPositionSync.write(Types.FLOAT, CustomEntity.this.rotation.y()); // yaw
            entityPositionSync.write(Types.FLOAT, CustomEntity.this.rotation.x()); // pitch
            entityPositionSync.write(Types.BOOLEAN, CustomEntity.this.isOnGround()); // on ground
            entityPositionSync.send(BedrockProtocol.class);
        }

    }

}
