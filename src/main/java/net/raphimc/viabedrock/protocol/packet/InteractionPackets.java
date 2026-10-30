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
package net.raphimc.viabedrock.protocol.packet;

import com.viaversion.viaversion.api.minecraft.BlockFace;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ServerboundPackets26_3;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.model.container.player.InventoryContainer;
import net.raphimc.viabedrock.api.model.entity.ClientPlayerEntity;
import net.raphimc.viabedrock.api.model.entity.Entity;
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ClientboundBedrockPackets;
import net.raphimc.viabedrock.protocol.PlayerActionPacketFactory;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.Direction;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ComplexInventoryTransaction_Type;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.GameMode;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.InteractionHand;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.PlayerActionAction;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.EntityLink;
import net.raphimc.viabedrock.protocol.model.Position3f;
import net.raphimc.viabedrock.protocol.model.inventory.BedrockInventoryTransaction;
import net.raphimc.viabedrock.protocol.model.inventory.InventoryActionData;
import net.raphimc.viabedrock.protocol.model.inventory.InventorySource;
import net.raphimc.viabedrock.protocol.model.inventory.InventoryTransactionData;
import net.raphimc.viabedrock.protocol.rewriter.InventoryTransactionRewriter;
import net.raphimc.viabedrock.protocol.storage.ChunkTracker;
import net.raphimc.viabedrock.protocol.storage.EntityTracker;
import net.raphimc.viabedrock.protocol.storage.InventoryTracker;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.util.List;
import java.util.logging.Level;

public final class InteractionPackets {

    public static boolean handlePlayerAction(final PacketWrapper wrapper, final PlayerActionAction action) {
        final InventoryTransactionRewriter transactionRewriter = wrapper.user().get(InventoryTransactionRewriter.class);
        final InventoryContainer inventory = wrapper.user().get(InventoryTracker.class).getInventoryContainer();

        if (action == PlayerActionAction.RELEASE_USE_ITEM) {
            final BedrockInventoryTransaction transaction = new BedrockInventoryTransaction(
                0,
                null,
                null,
                ComplexInventoryTransaction_Type.ItemReleaseTransaction,
                new InventoryTransactionData.ReleaseItemTransactionData(
                    ItemReleaseActionType.Release,
                    inventory.getSelectedHotbarSlot(),
                    inventory.getSelectedHotbarItem(),
                    wrapper.user().get(EntityTracker.class).getClientPlayer().position(),
                    HandSlot.Mainhand
                )
            );
            final PacketWrapper transactionPacket = PacketWrapper.create(ServerboundBedrockPackets.INVENTORY_TRANSACTION, wrapper.user());
            transactionPacket.write(transactionRewriter.getInventoryTransactionType(), transaction);
            transactionPacket.sendToServer(BedrockProtocol.class);
            return true;
        }

        if (action != PlayerActionAction.DROP_ITEM && action != PlayerActionAction.DROP_ALL_ITEMS) {
            return false;
        }

        final BedrockItem currentItem = inventory.getSelectedHotbarItem();
        if (currentItem.isEmpty()) {
            return true;
        }

        final BedrockItem droppedItem = currentItem.copy();
        if (action == PlayerActionAction.DROP_ITEM) {
            droppedItem.setAmount(1);
        }

        BedrockItem remainingItem = currentItem.copy();
        if (action == PlayerActionAction.DROP_ITEM && currentItem.amount() > 1) {
            remainingItem.setAmount(currentItem.amount() - 1);
        } else {
            remainingItem = BedrockItem.empty();
        }

        final BedrockInventoryTransaction transaction = new BedrockInventoryTransaction(
            0,
            null,
            List.of(
                new InventoryActionData(
                    new InventorySource(InventorySourceType.World_Interaction, ContainerID.CONTAINER_ID_NONE.getValue(), InventorySourceFlags.No_Flag),
                    0,
                    BedrockItem.empty(),
                    droppedItem
                ),
                new InventoryActionData(
                    new InventorySource(InventorySourceType.Container_Inventory, ContainerID.CONTAINER_ID_INVENTORY.getValue(), InventorySourceFlags.No_Flag),
                    inventory.getSelectedHotbarSlot(),
                    currentItem,
                    remainingItem
                )
            ),
            ComplexInventoryTransaction_Type.NormalTransaction,
            new InventoryTransactionData.NormalTransactionData()
        );
        final PacketWrapper transactionPacket = PacketWrapper.create(ServerboundBedrockPackets.INVENTORY_TRANSACTION, wrapper.user());
        transactionPacket.write(transactionRewriter.getInventoryTransactionType(), transaction);
        transactionPacket.sendToServer(BedrockProtocol.class);
        return true;
    }

    public static void register(final BedrockProtocol protocol) {
        // TODO: Track when the player start using item and send the StartUsingItem input data to the server.
        protocol.registerServerbound(ServerboundPackets26_3.USE_ITEM, ServerboundBedrockPackets.INVENTORY_TRANSACTION, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);
            final InventoryContainer inventoryContainer = wrapper.user().get(InventoryTracker.class).getInventoryContainer();
            final InventoryTransactionRewriter inventoryTransactionRewriter = wrapper.user().get(InventoryTransactionRewriter.class);

            final int hand = wrapper.read(Types.VAR_INT); // hand
            wrapper.read(Types.VAR_INT); // sequence
            wrapper.read(Types.FLOAT); // yaw
            wrapper.read(Types.FLOAT); // pitch

            // Bedrock can't hold the majority of item in offhand and can't use any either.
            // TODO: We need to handle cases where the item changes, or it affect player movement (eg: eating/blocking/etc)
            if (hand != InteractionHand.MAIN_HAND.ordinal()) {
                wrapper.cancel();
                return;
            }

            final BedrockInventoryTransaction inventoryTransaction = new BedrockInventoryTransaction(
                0, // legacy request id
                null,
                null,
                ComplexInventoryTransaction_Type.ItemUseTransaction,
                new InventoryTransactionData.UseItemTransactionData(
                    ItemUseActionType.Use,
                    ItemUseTriggerType.Unknown,
                    new BlockPosition(0, 0, 0), // block position
                    255, // block face
                    inventoryContainer.getSelectedHotbarSlot(),
                    HandSlot.Mainhand,
                    inventoryContainer.getSelectedHotbarItem(),
                    entityTracker.getClientPlayer().position(),
                    Position3f.ZERO, // click position
                    0, // block runtime id
                    ItemUsePredictedResult.Failure,
                    ItemUseClientCooldownState.Off
                )
            );
            wrapper.write(inventoryTransactionRewriter.getInventoryTransactionType(), inventoryTransaction);
        });

        protocol.registerServerbound(ServerboundPackets26_3.USE_ITEM_ON, null, wrapper -> {
            wrapper.cancel();

            final ClientPlayerEntity clientPlayer = wrapper.user().get(EntityTracker.class).getClientPlayer();
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);
            final ChunkTracker chunkTracker = wrapper.user().get(ChunkTracker.class);
            final InventoryTransactionRewriter inventoryTransactionRewriter = wrapper.user().get(InventoryTransactionRewriter.class);

            final InteractionHand hand = InteractionHand.values()[wrapper.read(Types.VAR_INT)]; // hand

            final BlockPosition position = wrapper.read(Types.BLOCK_POSITION1_14); // block position
            final int faceInt = wrapper.read(Types.UNSIGNED_BYTE); // face
            final Direction direction = Direction.getFromVerticalId(faceInt);
            if (direction == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown block face id: " + faceInt);
                return;
            }
            final BlockFace face = direction.blockFace();
            final Position3f clickPosition = new Position3f(
                wrapper.read(Types.FLOAT), // x
                wrapper.read(Types.FLOAT), // y
                wrapper.read(Types.FLOAT)  // z
            );
            final boolean insideBlock = wrapper.read(Types.BOOLEAN); // inside block
            wrapper.read(Types.BOOLEAN); // world border, this doesn't exist on Bedrock.

            // Send back block changed ack with the sequence, this will help with ghost blocks.
            PacketFactory.sendJavaBlockChangedAck(wrapper.user(), wrapper.read(Types.VAR_INT));

            // The player can only interact using the main hand on Bedrock!
            if (hand != InteractionHand.MAIN_HAND) {
                return;
            }

            // The bedrock client will send a start item use on action to the server first.
            PlayerActionPacketFactory.sendBedrockPlayerAction(
                wrapper.user(),
                clientPlayer.runtimeId(),
                PlayerActionType.StartItemUseOn,
                position,
                insideBlock ? position : position.getRelative(face),
                faceInt
            );

            // This is the main packet that the bedrock client use to interact with block.The rest of the
            final PacketWrapper transactionPacket = PacketWrapper.create(ServerboundBedrockPackets.INVENTORY_TRANSACTION, wrapper.user());

            BedrockItem predictedToItem = inventoryTracker.getInventoryContainer().getSelectedHotbarItem().copy();
            // This is not entirely correct, but at least it's more accurate than not sending actions or sending the original item data.
            if (predictedToItem.blockRuntimeId() != 0 && clientPlayer.javaGameMode() != GameMode.CREATIVE) {
                predictedToItem.setAmount(predictedToItem.amount() - 1);
            }
            if (predictedToItem.amount() <= 0) {
                predictedToItem = BedrockItem.empty();
            }

            final BedrockInventoryTransaction inventoryTransaction = new BedrockInventoryTransaction(
                0, // legacy request id
                null,
                List.of(
                    new InventoryActionData(
                        new InventorySource(InventorySourceType.Container_Inventory, ContainerID.CONTAINER_ID_INVENTORY.getValue(), InventorySourceFlags.No_Flag),
                        inventoryTracker.getInventoryContainer().getSelectedHotbarSlot(),
                        inventoryTracker.getInventoryContainer().getSelectedHotbarItem(),
                        predictedToItem
                    )
                ),
                ComplexInventoryTransaction_Type.ItemUseTransaction,
                new InventoryTransactionData.UseItemTransactionData(
                    ItemUseActionType.Place,
                    ItemUseTriggerType.Player_Input,
                    position,
                    faceInt,
                    inventoryTracker.getInventoryContainer().getSelectedHotbarSlot(),
                    HandSlot.Mainhand,
                    inventoryTracker.getInventoryContainer().getSelectedHotbarItem(),
                    clientPlayer.position(),
                    clickPosition,
                    chunkTracker.getBlockState(position),
                    ItemUsePredictedResult.Success,
                    ItemUseClientCooldownState.Off
                )
            );
            transactionPacket.write(inventoryTransactionRewriter.getInventoryTransactionType(), inventoryTransaction);

            transactionPacket.sendToServer(BedrockProtocol.class);

            // Bedrock sends a stop item use on after the transaction packet
            PlayerActionPacketFactory.sendBedrockPlayerAction(
                wrapper.user(),
                clientPlayer.runtimeId(),
                PlayerActionType.StopItemUseOn,
                position,
                new BlockPosition(0, 0, 0),
                0
            );
        });
        protocol.registerClientbound(ClientboundBedrockPackets.INVENTORY_TRANSACTION, null, wrapper -> {
            final InventoryTransactionRewriter inventoryTransactionRewriter = wrapper.user().get(InventoryTransactionRewriter.class);
            final InventoryTracker inventoryTracker = wrapper.user().get(InventoryTracker.class);

            wrapper.cancel();
            final BedrockInventoryTransaction inventoryTransaction = wrapper.read(inventoryTransactionRewriter.getInventoryTransactionType());

            if (inventoryTransaction.legacyRequestId() != 0) {
                // Ignore legacy inventory transactions for now
                return;
            }

            if (inventoryTransaction.actions() != null && !inventoryTransaction.actions().isEmpty()) {
                for (InventoryActionData action : inventoryTransaction.actions()) {
                    if (action.source().type() == InventorySourceType.Container_Inventory) {
                        final Container container = inventoryTracker.getContainerClientbound((byte) action.source().containerId(), null, null);

                        if (container != null) {
                            container.setItem(action.slot(), action.toItem());
                            PacketFactory.sendJavaContainerSetContent(wrapper.user(), container);
                        } else {
                            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received inventory action for unknown container ID: " + action.source().containerId());
                        }
                    }
                }
            }

            switch (inventoryTransaction.transactionType()) {
                case NormalTransaction -> {
                    break; // Nothing to do here for now
                }
                default -> {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received unsupported inventory transaction type: " + inventoryTransaction.transactionType());
                }
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.SET_ENTITY_LINK, ClientboundPackets26_3.SET_PASSENGERS, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);

            final EntityLink linkType = wrapper.read(BedrockTypes.ENTITY_LINK);
            final Entity vehicle = entityTracker.getEntityByUid(linkType.fromEntityUniqueId());
            if (vehicle == null) {
                wrapper.cancel();
                return;
            }

            final Entity passenger = entityTracker.getEntityByUid(linkType.toEntityUniqueId());

            // TODO: Handle Passenger type if needed
            switch (linkType.type()) {
                case Riding, Passenger -> { // TODO: This needs to be ordered properly based on the link types (rider first, then passengers)
                    vehicle.addPassenger(passenger.uniqueId());

                    wrapper.write(Types.VAR_INT, entityTracker.getEntityByUid(linkType.fromEntityUniqueId()).javaId()); // vehicle
                    wrapper.write(Types.VAR_INT, vehicle.passengers().size()); // number of passengers
                    for (long passengerUid : vehicle.passengers()) {
                        wrapper.write(Types.VAR_INT, entityTracker.getEntityByUid(passengerUid).javaId()); // passenger id
                    }

                    if (passenger.uniqueId() == entityTracker.getClientPlayer().uniqueId()) { // TODO: This could be applied to all passengers not just players
                        // The player is now riding an entity, update the state
                        entityTracker.getClientPlayer().setMountEntityRuntimeId(entityTracker.getEntityByUid(linkType.fromEntityUniqueId()).runtimeId());
                    }
                }
                case None -> { // Remove
                    vehicle.removePassenger(passenger.uniqueId());

                    wrapper.write(Types.VAR_INT, vehicle.javaId()); // vehicle
                    wrapper.write(Types.VAR_INT, vehicle.passengers().size()); // number of passengers
                    for (long passengerUid : vehicle.passengers()) {
                        wrapper.write(Types.VAR_INT, entityTracker.getEntityByUid(passengerUid).javaId()); // passenger id
                    }

                    if (passenger.uniqueId() == entityTracker.getClientPlayer().uniqueId()) { // TODO: This could be applied to all passengers not just players
                        // The player is no longer riding an entity, update the state
                        entityTracker.getClientPlayer().setMountEntityRuntimeId(-1);
                        entityTracker.getClientPlayer().setRequestedDismount(false);
                    }
                }
            }
        });

    }

    private InteractionPackets() {
    }

}
