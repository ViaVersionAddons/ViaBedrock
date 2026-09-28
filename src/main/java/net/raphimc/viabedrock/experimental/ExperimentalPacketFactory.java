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
package net.raphimc.viabedrock.experimental;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import net.raphimc.viabedrock.api.model.entity.Entity;
import net.raphimc.viabedrock.experimental.model.container.ExperimentalContainer;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestInfo;
import net.raphimc.viabedrock.experimental.storage.ExperimentalInventoryTracker;
import net.raphimc.viabedrock.experimental.types.ExperimentalBedrockTypes;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.InteractPacket_Action;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PlayerActionType;
import net.raphimc.viabedrock.protocol.storage.EntityTracker;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

public class ExperimentalPacketFactory {

    public static void sendJavaContainerSetContent(final UserConnection user, final ExperimentalContainer container) {
        final PacketWrapper containerSetContent = PacketWrapper.create(ClientboundPackets26_1.CONTAINER_SET_CONTENT, user);
        writeJavaContainerSetContent(containerSetContent, container);
        containerSetContent.send(BedrockProtocol.class);
    }

    public static void sendJavaContainerProperties(final UserConnection user, final ExperimentalContainer container, final short property, final short value) {
        final PacketWrapper containerSetProperty = PacketWrapper.create(ClientboundPackets26_1.CONTAINER_SET_DATA, user);
        containerSetProperty.write(Types.VAR_INT, (int) container.javaContainerId()); // container id
        containerSetProperty.write(Types.SHORT, property); // property id
        containerSetProperty.write(Types.SHORT, value); // property value
        containerSetProperty.send(BedrockProtocol.class);
    }

    public static void sendBedrockPlayerAction(final UserConnection user, long entityId, PlayerActionType actionType, BlockPosition position, BlockPosition resultPosition, int face) {
        final PacketWrapper startItemUseOn = PacketWrapper.create(ServerboundBedrockPackets.PLAYER_ACTION, user);
        startItemUseOn.write(BedrockTypes.UNSIGNED_VAR_LONG, entityId); // entity runtime id
        startItemUseOn.write(BedrockTypes.VAR_INT, actionType.getValue()); // action type
        startItemUseOn.write(BedrockTypes.BLOCK_POSITION, position); // block position
        startItemUseOn.write(BedrockTypes.BLOCK_POSITION, resultPosition); // result position
        startItemUseOn.write(BedrockTypes.VAR_INT, face); // face
        startItemUseOn.sendToServer(BedrockProtocol.class);
    }

    public static void sendBedrockInventoryRequest(final UserConnection user, ItemStackRequestInfo[] info) {
        final PacketWrapper inventoryRequest = PacketWrapper.create(ServerboundBedrockPackets.ITEM_STACK_REQUEST, user);
        inventoryRequest.write(ExperimentalBedrockTypes.ITEM_STACK_REQUESTS, info);
        inventoryRequest.sendToServer(BedrockProtocol.class);
    }

    public static void writeJavaContainerSetContent(final PacketWrapper wrapper, final ExperimentalContainer container) {
        wrapper.write(Types.VAR_INT, (int) container.javaContainerId()); // container id
        wrapper.write(Types.VAR_INT, 0); // revision
        wrapper.write(VersionedTypes.V26_1.itemArray, container.getJavaItems()); // items
        wrapper.write(VersionedTypes.V26_1.item, wrapper.user().get(ExperimentalInventoryTracker.class).getHudContainer().getJavaItem(0)); // cursor item
    }

    public static void sendBedrockDismount(final UserConnection user, long entityRId) {
        final PacketWrapper dismountPacket = PacketWrapper.create(ServerboundBedrockPackets.INTERACT, user);
        dismountPacket.write(Types.UNSIGNED_BYTE, (short) InteractPacket_Action.StopRiding.getValue()); // action
        dismountPacket.write(BedrockTypes.UNSIGNED_VAR_LONG, entityRId); // target entity runtime id
        dismountPacket.write(BedrockTypes.OPTIONAL_POSITION_3F, null); // position
        dismountPacket.sendToServer(BedrockProtocol.class);
    }

    public static void sendJavaSetPassengers(final UserConnection user, Entity vehicle) {
        final EntityTracker entityTracker = user.get(EntityTracker.class);
        final PacketWrapper setPassengersPacket = PacketWrapper.create(ClientboundPackets26_1.SET_PASSENGERS, user);
        setPassengersPacket.write(Types.VAR_INT, vehicle.javaId()); // vehicle
        setPassengersPacket.write(Types.VAR_INT, vehicle.passengers().size()); // number of passengers
        for (long passengerUid : vehicle.passengers()) {
            setPassengersPacket.write(Types.VAR_INT, entityTracker.getEntityByUid(passengerUid).javaId()); // passenger id
        }
        setPassengersPacket.send(BedrockProtocol.class);
    }

}
