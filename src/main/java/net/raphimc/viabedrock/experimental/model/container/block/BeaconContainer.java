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
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.chunk.BedrockBlockEntity;
import net.raphimc.viabedrock.experimental.ExperimentalPacketFactory;
import net.raphimc.viabedrock.experimental.model.container.ExperimentalContainer;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestInfo;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestSlotInfo;
import net.raphimc.viabedrock.experimental.storage.ExperimentalInventoryTracker;
import net.raphimc.viabedrock.experimental.storage.InventoryRequestStorage;
import net.raphimc.viabedrock.experimental.storage.InventoryRequestTracker;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.TextProcessingEventOrigin;
import net.raphimc.viabedrock.protocol.data.generated.bedrock.CustomBlockTags;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.storage.ChunkTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class BeaconContainer extends ExperimentalContainer {

    public BeaconContainer(UserConnection user, byte containerId, TextComponent title, BlockPosition position) {
        super(user, containerId, ContainerType.BEACON, title, position, 1, CustomBlockTags.BEACON);

        PacketWrapper propertiesPacket = PacketWrapper.create(ClientboundPackets26_1.CONTAINER_SET_DATA, user);
        propertiesPacket.write(Types.VAR_INT, (int) containerId);
        propertiesPacket.write(Types.SHORT, (short) 0); // Property ID (Power level)
        // TODO: Dynamically set this based on the beacon's current level
        propertiesPacket.write(Types.SHORT, (short) 4); // Property Value (0-4)
        propertiesPacket.scheduleSend(BedrockProtocol.class);

        ChunkTracker chunkTracker = user.get(ChunkTracker.class);
        BedrockBlockEntity bedrockBlockEntity = chunkTracker.getBlockEntity(position);
        if (bedrockBlockEntity != null) {
            int primaryEffect = bedrockBlockEntity.tag().getInt("primary", 0);
            int secondaryEffect = bedrockBlockEntity.tag().getInt("secondary", 0);

            //TODO: This is kinda cooked, refactor later
            final String bedrockIdentifierPrimary = BedrockProtocol.MAPPINGS.getBedrockEffects().inverse().get(primaryEffect);
            final String bedrockIdentifierSecondary = BedrockProtocol.MAPPINGS.getBedrockEffects().inverse().get(secondaryEffect);

            final String javaIdentifierPrimary = BedrockProtocol.MAPPINGS.getBedrockToJavaEffects().get(bedrockIdentifierPrimary);
            final String javaIdentifierSecondary = BedrockProtocol.MAPPINGS.getBedrockToJavaEffects().get(bedrockIdentifierSecondary);

            final int javaIdPrimary = javaIdentifierPrimary == null ? -1 : BedrockProtocol.MAPPINGS.getJavaEffects().get(javaIdentifierPrimary);
            final int javaIdSecondary = javaIdentifierSecondary == null ? -1 : BedrockProtocol.MAPPINGS.getJavaEffects().get(javaIdentifierSecondary);

            ViaBedrock.getPlatform().getLogger().info("Beacon effects - Primary: " + javaIdPrimary + " Secondary: " + javaIdSecondary);

            PacketWrapper propertiesPacket2 = PacketWrapper.create(ClientboundPackets26_1.CONTAINER_SET_DATA, user);
            propertiesPacket2.write(Types.VAR_INT, (int) containerId);
            propertiesPacket2.write(Types.SHORT, (short) 1); // Property ID (First potion effect )
            propertiesPacket2.write(Types.SHORT, (short) javaIdPrimary); // Property Value
            propertiesPacket2.scheduleSend(BedrockProtocol.class);

            PacketWrapper propertiesPacket3 = PacketWrapper.create(ClientboundPackets26_1.CONTAINER_SET_DATA, user);
            propertiesPacket3.write(Types.VAR_INT, (int) containerId);
            propertiesPacket3.write(Types.SHORT, (short) 2); // Property ID (Second potion effect )
            propertiesPacket3.write(Types.SHORT, (short) javaIdSecondary); // Property Value
            propertiesPacket3.scheduleSend(BedrockProtocol.class);
        }
    }

    @Override
    public FullContainerName getFullContainerName(int slot) {
        if (slot != 27) {
            throw new IllegalArgumentException("Invalid slot for Beacon Container: " + slot);
        }
        return new FullContainerName(ContainerEnumName.BeaconPaymentContainer, null);
    }

    @Override
    public int javaSlot(final int slot) {
        if (slot == 27) {
            return 0;
        } else {
            return super.javaSlot(slot);
        }
    }

    @Override
    public int bedrockSlot(final int slot) {
        if (slot == 0) {
            return 27;
        } else {
            return super.bedrockSlot(slot);
        }
    }

    @Override
    public BedrockItem getItem(int bedrockSlot) {
        // Fix magic offset
        bedrockSlot -= 27;
        return this.items[bedrockSlot];
    }

    @Override
    public boolean setItem(final int bedrockSlot, final BedrockItem item) {
        // Fix magic offset
        return super.setItem(bedrockSlot - 27, item);
    }

    public void updateEffects(int primaryEffect, int secondaryEffect) {
        InventoryRequestTracker inventoryRequestTracker = this.user.get(InventoryRequestTracker.class);
        ExperimentalInventoryTracker inventoryTracker = this.user.get(ExperimentalInventoryTracker.class);

        //TODO: This is kinda cooked, refactor later
        final String javaIdentifierPrimary = BedrockProtocol.MAPPINGS.getJavaEffects().inverse().get(primaryEffect);
        final String javaIdentifierSecondary = BedrockProtocol.MAPPINGS.getJavaEffects().inverse().get(secondaryEffect);

        final String bedrockIdentifierPrimary = BedrockProtocol.MAPPINGS.getJavaToBedrockEffects().get(javaIdentifierPrimary);
        final String bedrockIdentifierSecondary = BedrockProtocol.MAPPINGS.getJavaToBedrockEffects().get(javaIdentifierSecondary);

        final int bedrockIdPrimary = bedrockIdentifierPrimary == null ? 0 : BedrockProtocol.MAPPINGS.getBedrockEffects().get(bedrockIdentifierPrimary);
        final int bedrockIdSecondary = bedrockIdentifierSecondary == null ? 0 : BedrockProtocol.MAPPINGS.getBedrockEffects().get(bedrockIdentifierSecondary);

        final BedrockItem paymentItem = this.getItem(27);

        ItemStackRequestInfo requestInfo = new ItemStackRequestInfo(
                inventoryRequestTracker.nextRequestId(),
                List.of(
                        new ItemStackRequestAction.BeaconPaymentAction(
                                bedrockIdPrimary,
                                bedrockIdSecondary
                        ),
                        new ItemStackRequestAction.DestroyAction(
                                1,
                                new ItemStackRequestSlotInfo(
                                        this.getFullContainerName(27),
                                        (byte) 27,
                                        paymentItem.netId()
                                )
                        )
                ),
                List.of(),
                TextProcessingEventOrigin.Unknown
        );

        List<ExperimentalContainer> prevContainers = new ArrayList<>();
        prevContainers.add(this.copy());
        ExperimentalContainer prevCursorContainer = inventoryTracker.getHudContainer().copy();

        this.setItem(27, this.itemAfterRemovingAmount(paymentItem, 1)); // Clear the payment slot

        inventoryRequestTracker.addRequest(new InventoryRequestStorage(requestInfo, 0, prevCursorContainer, prevContainers));
        ExperimentalPacketFactory.sendBedrockInventoryRequest(user, new ItemStackRequestInfo[] {requestInfo});
    }

}
