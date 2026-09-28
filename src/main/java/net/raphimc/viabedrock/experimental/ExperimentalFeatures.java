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

import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockFace;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.TranslationComponent;
import com.viaversion.viaversion.libs.fastutil.longs.LongArrayList;
import com.viaversion.viaversion.libs.fastutil.longs.LongList;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPackets26_1;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.chunk.BedrockBlockEntity;
import net.raphimc.viabedrock.api.model.container.Container;
import net.raphimc.viabedrock.api.model.entity.ClientPlayerEntity;
import net.raphimc.viabedrock.api.model.entity.Entity;
import net.raphimc.viabedrock.api.util.PacketFactory;
import net.raphimc.viabedrock.experimental.model.inventory.BedrockInventoryTransaction;
import net.raphimc.viabedrock.experimental.model.inventory.InventoryActionData;
import net.raphimc.viabedrock.experimental.model.inventory.InventorySource;
import net.raphimc.viabedrock.experimental.model.inventory.InventoryTransactionData;
import net.raphimc.viabedrock.experimental.model.map.MapDecoration;
import net.raphimc.viabedrock.experimental.model.map.MapObject;
import net.raphimc.viabedrock.experimental.model.map.MapTrackedObject;
import net.raphimc.viabedrock.api.util.TextUtil;
import net.raphimc.viabedrock.experimental.model.container.ExperimentalContainer;
import net.raphimc.viabedrock.experimental.model.container.block.*;
import net.raphimc.viabedrock.experimental.model.container.player.InventoryContainer;
import net.raphimc.viabedrock.experimental.model.inventory.*;
import net.raphimc.viabedrock.experimental.model.recipe.*;
import net.raphimc.viabedrock.experimental.rewriter.InventoryTransactionRewriter;
import net.raphimc.viabedrock.experimental.storage.MapTracker;
import net.raphimc.viabedrock.experimental.util.JavaMapPaletteUtil;
import net.raphimc.viabedrock.experimental.storage.*;
import net.raphimc.viabedrock.experimental.tasks.ExperimentalInventoryTrackerTickTask;
import net.raphimc.viabedrock.experimental.types.ExperimentalBedrockTypes;
import net.raphimc.viabedrock.experimental.util.ProtocolUtil;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ClientboundBedrockPackets;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.enums.Direction;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.ItemUseInventoryTransaction_TriggerType;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.ContainerInput;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.GameMode;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.InteractionHand;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.PlayerActionAction;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.model.EntityLink;
import net.raphimc.viabedrock.protocol.model.Position3f;
import net.raphimc.viabedrock.protocol.rewriter.BlockStateRewriter;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;
import net.raphimc.viabedrock.protocol.storage.*;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;
import net.raphimc.viabedrock.protocol.storage.ChunkTracker;
import net.raphimc.viabedrock.protocol.storage.EntityTracker;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;
import net.raphimc.viabedrock.protocol.types.model.EntityLinkType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * This class is used to register experimental features that are not yet stable/tested enough to be included in the main protocol.
 * These features may be subject to change or removal in future versions.
 */
public class ExperimentalFeatures {

    private static final int MAP_FLAGS_ALL = ClientboundMapItemDataPacket_Type.Creation.getValue() | ClientboundMapItemDataPacket_Type.DecorationUpdate.getValue() | ClientboundMapItemDataPacket_Type.TextureUpdate.getValue();

    public static void registerPacketTranslators(final BedrockProtocol protocol) {
        ProtocolUtil.prependServerbound(protocol, ServerboundPackets26_1.PLAYER_ACTION, wrapper -> {
            final InventoryTransactionRewriter inventoryTransactionRewriter = wrapper.user().get(InventoryTransactionRewriter.class);
            final ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);

            final PlayerActionAction action = PlayerActionAction.values()[wrapper.passthrough(Types.VAR_INT)]; // action
            wrapper.passthrough(Types.BLOCK_POSITION1_14); // block position
            wrapper.passthrough(Types.UNSIGNED_BYTE); // face
            wrapper.passthrough(Types.VAR_INT); // sequence number

            if (action == PlayerActionAction.RELEASE_USE_ITEM) {
                final InventoryContainer inventoryContainer = inventoryTracker.getInventoryContainer();

                wrapper.clearPacket();
                wrapper.setPacketType(ServerboundBedrockPackets.INVENTORY_TRANSACTION);

                BedrockInventoryTransaction inventoryTransaction = new BedrockInventoryTransaction(
                        0, // legacy request id
                        null,
                        null,
                        ComplexInventoryTransaction_Type.ItemReleaseTransaction,
                        new InventoryTransactionData.ReleaseItemTransactionData(
                                ItemReleaseInventoryTransaction_ActionType.Release,
                                inventoryContainer.getSelectedHotbarSlot(),
                                inventoryContainer.getSelectedHotbarItem(),
                                wrapper.user().get(EntityTracker.class).getClientPlayer().position()
                        )
                );
                wrapper.write(inventoryTransactionRewriter.getInventoryTransactionType(), inventoryTransaction);

                wrapper.sendToServer(BedrockProtocol.class);
                wrapper.cancel();
            } else if (action == PlayerActionAction.DROP_ITEM || action == PlayerActionAction.DROP_ALL_ITEMS) {
                final BedrockItem currentItem = inventoryTracker.getInventoryContainer().getSelectedHotbarItem();

                wrapper.cancel();
                if (currentItem.isEmpty()) {
                    return;
                }

                BedrockItem predictedAmount = currentItem.copy();
                if (action == PlayerActionAction.DROP_ITEM) {
                    predictedAmount.setAmount(1); // Drop a single item
                }

                BedrockItem predictedToItem = currentItem.copy();
                if (action == PlayerActionAction.DROP_ITEM) {
                    if (predictedToItem.amount() > 1) {
                        predictedToItem.setAmount(currentItem.amount() - 1);
                    } else {
                        predictedToItem = BedrockItem.empty();
                    }
                } else {
                    predictedToItem = BedrockItem.empty();
                }

                final PacketWrapper transactionPacket = PacketWrapper.create(ServerboundBedrockPackets.INVENTORY_TRANSACTION, wrapper.user());

                BedrockInventoryTransaction inventoryTransaction = new BedrockInventoryTransaction(
                        0,
                        null,
                        List.of(
                                new InventoryActionData(
                                        new InventorySource(InventorySourceType.WorldInteraction, ContainerID.CONTAINER_ID_NONE.getValue(), InventorySource_InventorySourceFlags.NoFlag),
                                        0,
                                        BedrockItem.empty(),
                                        predictedAmount
                                ),
                                new InventoryActionData(
                                        new InventorySource(InventorySourceType.ContainerInventory, ContainerID.CONTAINER_ID_INVENTORY.getValue(), InventorySource_InventorySourceFlags.NoFlag),
                                        inventoryTracker.getInventoryContainer().getSelectedHotbarSlot(),
                                        currentItem,
                                        predictedToItem
                                )
                        ),

                        ComplexInventoryTransaction_Type.NormalTransaction,
                        new InventoryTransactionData.NormalTransactionData()
                );

                transactionPacket.write(inventoryTransactionRewriter.getInventoryTransactionType(), inventoryTransaction);

                transactionPacket.sendToServer(BedrockProtocol.class);

                //TODO: I think vanilla client also sends these and im not sure what their purposes are but it works without them
                    /*final PacketWrapper interactPacket = PacketWrapper.create(ServerboundBedrockPackets.INTERACT, wrapper.user());

                    interactPacket.write(Types.BYTE, (byte) InteractPacket_Action.InteractUpdate.getValue());
                    interactPacket.write(BedrockTypes.UNSIGNED_VAR_LONG, clientPlayer.runtimeId());
                    interactPacket.write(BedrockTypes.POSITION_3F, new Position3f(0, 0, 0));

                    interactPacket.sendToServer(BedrockProtocol.class);

                    final PacketWrapper mobEquipPacket = PacketWrapper.create(ServerboundBedrockPackets.MOB_EQUIPMENT, wrapper.user());

                    mobEquipPacket.write(BedrockTypes.UNSIGNED_VAR_LONG, clientPlayer.runtimeId());
                    mobEquipPacket.write(itemRewriter.newItemType(), predictedToItem);
                    mobEquipPacket.write(Types.BYTE, inventoryTracker.getInventoryContainer().getSelectedHotbarSlot());
                    mobEquipPacket.write(Types.BYTE, inventoryTracker.getInventoryContainer().getSelectedHotbarSlot());
                    mobEquipPacket.write(Types.BYTE, (byte) ContainerID.CONTAINER_ID_INVENTORY.getValue());

                    mobEquipPacket.sendToServer(BedrockProtocol.class);*/
            }


        });

        // TODO: Track when the player start using item and send the StartUsingItem input data to the server.
        protocol.registerServerbound(ServerboundPackets26_1.USE_ITEM, ServerboundBedrockPackets.INVENTORY_TRANSACTION, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);
            final InventoryContainer inventoryContainer = wrapper.user().get(ExperimentalInventoryTracker.class).getInventoryContainer();
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

            BedrockInventoryTransaction inventoryTransaction = new BedrockInventoryTransaction(
                    0, // legacy request id
                    null,
                    null,
                    ComplexInventoryTransaction_Type.ItemUseTransaction,
                    new InventoryTransactionData.UseItemTransactionData(
                            ItemUseInventoryTransaction_ActionType.Use,
                            ItemUseInventoryTransaction_TriggerType.Unknown,
                            new BlockPosition(0, 0, 0), // block position
                            255, // block face
                            inventoryContainer.getSelectedHotbarSlot(),
                            inventoryContainer.getSelectedHotbarItem(),
                            entityTracker.getClientPlayer().position(),
                            Position3f.ZERO, // click position
                            0, // block runtime id
                            ItemUseInventoryTransaction_PredictedResult.Failure,
                            ItemUseInventoryTransaction_ClientCooldownState.Off
                    )
            );
            wrapper.write(inventoryTransactionRewriter.getInventoryTransactionType(), inventoryTransaction);
        });

        protocol.registerServerbound(ServerboundPackets26_1.USE_ITEM_ON, null, wrapper -> {
            wrapper.cancel();

            final ClientPlayerEntity clientPlayer = wrapper.user().get(EntityTracker.class).getClientPlayer();
            final ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
            final ChunkTracker chunkTracker = wrapper.user().get(ChunkTracker.class);
            final InventoryTransactionRewriter inventoryTransactionRewriter = wrapper.user().get(InventoryTransactionRewriter.class);

            final InteractionHand hand = InteractionHand.values()[wrapper.read(Types.VAR_INT)]; // hand

            BlockPosition position = wrapper.read(Types.BLOCK_POSITION1_14); // block position
            int faceInt = wrapper.read(Types.UNSIGNED_BYTE); // face
            Direction direction = Direction.getFromVerticalId(faceInt);
            if (direction == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown block face id: " + faceInt);
                return;
            }
            BlockFace face = direction.blockFace();
            Position3f clickPosition = new Position3f(
                    wrapper.read(Types.FLOAT), // x
                    wrapper.read(Types.FLOAT), // y
                    wrapper.read(Types.FLOAT)  // z
            );
            boolean insideBlock = wrapper.read(Types.BOOLEAN); // inside block
            wrapper.read(Types.BOOLEAN); // world border, this doesn't exist on Bedrock.

            // Send back block changed ack with the sequence, this will help with ghost blocks.
            PacketFactory.sendJavaBlockChangedAck(wrapper.user(), wrapper.read(Types.VAR_INT));

            // The player can only interact using the main hand on Bedrock!
            if (hand != InteractionHand.MAIN_HAND) {
                return;
            }

            // The bedrock client will send a start item use on action to the server first.
            ExperimentalPacketFactory.sendBedrockPlayerAction(
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

            BedrockInventoryTransaction inventoryTransaction = new BedrockInventoryTransaction(
                    0, // legacy request id
                    null,
                    List.of(
                            new InventoryActionData(
                                    new InventorySource(InventorySourceType.ContainerInventory, ContainerID.CONTAINER_ID_INVENTORY.getValue(), InventorySource_InventorySourceFlags.NoFlag),
                                    inventoryTracker.getInventoryContainer().getSelectedHotbarSlot(),
                                    inventoryTracker.getInventoryContainer().getSelectedHotbarItem(),
                                    predictedToItem
                            )
                    ),
                    ComplexInventoryTransaction_Type.ItemUseTransaction,
                    new InventoryTransactionData.UseItemTransactionData(
                            ItemUseInventoryTransaction_ActionType.Place,
                            ItemUseInventoryTransaction_TriggerType.PlayerInput,
                            position,
                            faceInt,
                            inventoryTracker.getInventoryContainer().getSelectedHotbarSlot(),
                            inventoryTracker.getInventoryContainer().getSelectedHotbarItem(),
                            clientPlayer.position(),
                            clickPosition,
                            chunkTracker.getBlockState(position),
                            ItemUseInventoryTransaction_PredictedResult.Success,
                            ItemUseInventoryTransaction_ClientCooldownState.Off
                    )
            );
            transactionPacket.write(inventoryTransactionRewriter.getInventoryTransactionType(), inventoryTransaction);

            transactionPacket.sendToServer(BedrockProtocol.class);

            // Bedrock sends a stop item use on after the transaction packet
            ExperimentalPacketFactory.sendBedrockPlayerAction(
                    wrapper.user(),
                    clientPlayer.runtimeId(),
                    PlayerActionType.StopItemUseOn,
                    position,
                    new BlockPosition(0, 0, 0),
                    0
            );
        });
        protocol.registerServerbound(ServerboundPackets26_1.CONTAINER_CLICK, null, wrapper -> {
            wrapper.cancel();
            final int containerId = wrapper.read(Types.VAR_INT); // container id
            final int revision = wrapper.read(Types.VAR_INT); // revision
            final short slot = wrapper.read(Types.SHORT); // slot
            final byte button = wrapper.read(Types.BYTE); // button
            final ContainerInput action = ContainerInput.values()[wrapper.read(Types.VAR_INT)]; // action

            final ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
            if (inventoryTracker.getPendingCloseContainer() != null) {
                wrapper.cancel();
                return;
            }
            final ExperimentalContainer container = inventoryTracker.getContainerServerbound((byte) containerId);
            if (container == null) {
                if (containerId == ContainerID.CONTAINER_ID_INVENTORY.getValue()) {
                    // Bedrock client can send multiple OpenInventory requests if the server doesn't respond, so this is fine here
                    final PacketWrapper interact = PacketWrapper.create(ServerboundBedrockPackets.INTERACT, wrapper.user());
                    interact.write(Types.UNSIGNED_BYTE, (short) InteractPacket_Action.OpenInventory.getValue()); // action
                    interact.write(BedrockTypes.UNSIGNED_VAR_LONG, wrapper.user().get(EntityTracker.class).getClientPlayer().runtimeId()); // target entity runtime id
                    interact.write(BedrockTypes.OPTIONAL_POSITION_3F, null); // position
                    interact.sendToServer(BedrockProtocol.class);
                    ExperimentalPacketFactory.sendJavaContainerSetContent(wrapper.user(), inventoryTracker.getInventoryContainer());
                }

                wrapper.cancel();
                return;
            }
            if (!container.handleClick(revision, slot, button, action)) {
                if (container.type() != ContainerType.INVENTORY) {
                    ExperimentalPacketFactory.sendJavaContainerSetContent(wrapper.user(), inventoryTracker.getInventoryContainer());
                }
                ExperimentalPacketFactory.sendJavaContainerSetContent(wrapper.user(), container);
            }
        }, true);
        protocol.registerServerbound(ServerboundPackets26_1.CONTAINER_BUTTON_CLICK, null, wrapper -> {
            wrapper.cancel();
            final int containerId = wrapper.read(Types.VAR_INT); // container id
            final int button = wrapper.read(Types.VAR_INT); // button

            final ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
            if (inventoryTracker.getPendingCloseContainer() != null) {
                wrapper.cancel();
                return;
            }
            final ExperimentalContainer container = inventoryTracker.getContainerServerbound((byte) containerId);
            if (container == null) {
                wrapper.cancel();
                return;
            }
            if (!container.handleButtonClick(button)) {
                if (container.type() != ContainerType.INVENTORY) {
                    ExperimentalPacketFactory.sendJavaContainerSetContent(wrapper.user(), inventoryTracker.getInventoryContainer());
                }
                ExperimentalPacketFactory.sendJavaContainerSetContent(wrapper.user(), container);
            }
        }, true);
        protocol.registerServerbound(ServerboundPackets26_1.SET_CREATIVE_MODE_SLOT, null, wrapper -> {
            wrapper.cancel();
            final short slot = wrapper.read(Types.SHORT); // slot
            final Item item = wrapper.read(VersionedTypes.V26_1.lengthPrefixedItem); // item

            final ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
            if (inventoryTracker.getPendingCloseContainer() != null) {
                wrapper.cancel();
                return;
            }
            ExperimentalPacketFactory.sendJavaContainerSetContent(wrapper.user(), inventoryTracker.getInventoryContainer());
        }, true);
        protocol.registerServerbound(ServerboundPackets26_1.CONTAINER_CLOSE, ServerboundBedrockPackets.CONTAINER_CLOSE, new PacketHandlers() {
            @Override
            protected void register() {
                map(Types.VAR_INT, Types.BYTE); // container id
                create(Types.BYTE, (byte) ContainerType.NONE.getValue()); // type
                create(Types.BOOLEAN, false); // server initiated
                handler(wrapper -> {
                    final ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
                    final byte containerId = wrapper.get(Types.BYTE, 0);
                    final ExperimentalContainer container = inventoryTracker.getContainerServerbound(containerId);
                    if (container == null) {
                        wrapper.cancel();
                        return;
                    }

                    if (container.javaContainerId() != container.containerId()) {
                        wrapper.set(Types.BYTE, 0, container.containerId());
                    }
                    inventoryTracker.markPendingClose(container);
                });
            }
        }, true);
        protocol.registerServerbound(ServerboundPackets26_1.SET_CARRIED_ITEM, ServerboundBedrockPackets.MOB_EQUIPMENT, wrapper -> {
            final short slot = wrapper.read(Types.SHORT); // slot
            wrapper.user().get(ExperimentalInventoryTracker.class).getInventoryContainer().setSelectedHotbarSlot((byte) slot, wrapper); // slot
        }, true);
        protocol.appendServerbound(ServerboundPackets26_1.INTERACT, wrapper -> {
            if (wrapper.isCancelled()) return;
            final InventoryContainer inventoryContainer = wrapper.user().get(ExperimentalInventoryTracker.class).getInventoryContainer();
            wrapper.set(BedrockTypes.VAR_INT, 1, (int) inventoryContainer.getSelectedHotbarSlot()); // hotbar slot
            wrapper.set(wrapper.user().get(ItemRewriter.class).newItemType(), 0, inventoryContainer.getSelectedHotbarItem()); // held item
        });
        protocol.registerServerbound(ServerboundPackets26_1.SET_BEACON, null, wrapper -> {
            wrapper.cancel();
            if (!ViaBedrock.getConfig().shouldEnableExperimentalFeatures()) return;

            int primaryPower = -1;
            int secondaryPower = -1;

            final boolean hasPrimary = wrapper.read(Types.BOOLEAN);
            if (hasPrimary) {
                primaryPower = wrapper.read(Types.VAR_INT);
            }
            final boolean hasSecondary = wrapper.read(Types.BOOLEAN);
            if (hasSecondary) {
                secondaryPower = wrapper.read(Types.VAR_INT);
            }

            final ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
            if (inventoryTracker.isContainerOpen() && inventoryTracker.getCurrentContainer() instanceof BeaconContainer beaconContainer) {
                beaconContainer.updateEffects(primaryPower, secondaryPower);
            }
        });
        protocol.registerServerbound(ServerboundPackets26_1.RENAME_ITEM, null, wrapper -> {
            wrapper.cancel();
            ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
            final String newName = wrapper.read(Types.STRING);

            if (inventoryTracker.isContainerOpen() && inventoryTracker.getCurrentContainer() instanceof AnvilContainer anvilContainer) {
                anvilContainer.setRenameText(newName);
            }
        });
        protocol.registerServerbound(ServerboundPackets26_1.CONTAINER_SLOT_STATE_CHANGED, ServerboundBedrockPackets.TOGGLE_CRAFTER_SLOT_REQUEST, wrapper -> {
            int slotId = wrapper.read(Types.VAR_INT);
            int windowId = wrapper.read(Types.VAR_INT);
            boolean state = wrapper.read(Types.BOOLEAN);

            ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
            ExperimentalContainer container = inventoryTracker.getContainerServerbound((byte) windowId);
            if (!(container instanceof CrafterContainer)) {
                wrapper.cancel();
                return;
            }

            wrapper.write(BedrockTypes.INT_LE, container.position().x());
            wrapper.write(BedrockTypes.INT_LE, container.position().y());
            wrapper.write(BedrockTypes.INT_LE, container.position().z());
            wrapper.write(Types.UNSIGNED_BYTE, (short) slotId);
            wrapper.write(Types.BOOLEAN, !state);
        });

        protocol.registerClientbound(ClientboundBedrockPackets.CONTAINER_OPEN, ClientboundPackets26_1.OPEN_SCREEN, wrapper -> {
            final ChunkTracker chunkTracker = wrapper.user().get(ChunkTracker.class);
            final BlockStateRewriter blockStateRewriter = wrapper.user().get(BlockStateRewriter.class);
            final ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
            final byte containerId = wrapper.read(Types.BYTE); // container id
            final byte rawType = wrapper.read(Types.BYTE); // type
            final ContainerType type = ContainerType.getByValue(rawType);
            if (type == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown ContainerType: " + rawType);
                wrapper.cancel();
                return;
            }
            final BlockPosition position = wrapper.read(BedrockTypes.BLOCK_POSITION); // position
            wrapper.read(BedrockTypes.VAR_LONG); // entity unique id

            if (inventoryTracker.isContainerOpen() || wrapper.user().get(InventoryTracker.class).isAnyScreenOpen()) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Server tried to open container while another container is open");
                PacketFactory.sendBedrockContainerClose(wrapper.user(), (byte) -1, ContainerType.NONE);
                wrapper.cancel();
                return;
            }
            final BedrockBlockEntity blockEntity = chunkTracker.getBlockEntity(position);
            TextComponent title = new TranslationComponent("container." + blockStateRewriter.tag(chunkTracker.getBlockState(position)));
            if (blockEntity != null && blockEntity.tag().get("CustomName") instanceof StringTag customNameTag) {
                title = TextUtil.stringToTextComponent(wrapper.user().get(ResourcePackStorage.class).getTexts().translate(customNameTag.getValue()));
            }

            int size = 27;
            if (blockEntity != null && blockEntity.tag() != null
                    && blockEntity.tag().contains("id") && blockEntity.tag().getString("id").equals("Chest")
                    && blockEntity.tag().contains("pairlead")) {
                // Double chest
                size = 54;
            }

            final ExperimentalContainer container;
            switch (type) {
                case INVENTORY -> {
                    inventoryTracker.setCurrentContainer(new InventoryContainer(wrapper.user(), containerId, position, inventoryTracker.getInventoryContainer()));
                    wrapper.cancel();
                    return;
                }
                case CONTAINER -> container = new ChestContainer(wrapper.user(), containerId, title, position, size);
                case HOPPER -> container = new HopperContainer(wrapper.user(), containerId, title, position);
                case FURNACE -> container = new FurnaceContainer(wrapper.user(), containerId, title, position);
                case BLAST_FURNACE -> container = new BlastFurnaceContainer(wrapper.user(), containerId, title, position);
                case SMOKER -> container = new SmokerContainer(wrapper.user(), containerId, title, position);
                case BREWING_STAND -> container = new BrewingStandContainer(wrapper.user(), containerId, title, position);
                case BEACON -> container = new BeaconContainer(wrapper.user(), containerId, title, position);
                case ENCHANTMENT -> container = new EnchantmentContainer(wrapper.user(),  containerId, title, position);
                case ANVIL -> container = new AnvilContainer(wrapper.user(), containerId, title, position);
                case SMITHING_TABLE -> container = new SmithingContainer(wrapper.user(), containerId, title, position);
                case STONECUTTER -> container = new StonecutterContainer(wrapper.user(), containerId, title, position);
                case DISPENSER, DROPPER -> container = new Generic3x3Container(wrapper.user(), containerId, type, title, position);
                case WORKBENCH -> container = new CraftingTableContainer(wrapper.user(), containerId, title, position);
                case GRINDSTONE -> container = new GrindstoneContainer(wrapper.user(), containerId, title, position);
                case CRAFTER -> container = new CrafterContainer(wrapper.user(), containerId, title, position);
                case COMPOUND_CREATOR, ELEMENT_CONSTRUCTOR, MATERIAL_REDUCER, LAB_TABLE -> { // Education Edition features, no java equivalent
                    wrapper.cancel();
                    return;
                }
                case NONE, CAULDRON, JUKEBOX, ARMOR, HAND, HUD, DECORATED_POT -> { // Bedrock client can't open these containers
                    wrapper.cancel();
                    return;
                }
                default -> {
                    // throw new IllegalStateException("Unhandled ContainerType: " + type);
                    wrapper.cancel();
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Tried to open unimplemented container: " + type);
                    PacketFactory.sendBedrockContainerClose(wrapper.user(), containerId, ContainerType.NONE);
                    return;
                }
            }
            inventoryTracker.setCurrentContainer(container);

            wrapper.write(Types.VAR_INT, (int) containerId); // container id
            if (blockEntity != null && blockEntity.tag() != null
                    && blockEntity.tag().contains("id") && blockEntity.tag().getString("id").equals("Chest")
                    && blockEntity.tag().contains("pairlead")) {
                //TODO: Temporary fix
                wrapper.write(Types.VAR_INT, BedrockProtocol.MAPPINGS.getJavaMenuId("minecraft:generic_9x6"));
            } else {
                wrapper.write(Types.VAR_INT, BedrockProtocol.MAPPINGS.getBedrockToJavaContainers().get(type)); // type
            }
            wrapper.write(Types.TAG, TextUtil.textComponentToNbt(title)); // title
        }, true);
        protocol.registerClientbound(ClientboundBedrockPackets.CONTAINER_CLOSE, ClientboundPackets26_1.CONTAINER_CLOSE, new PacketHandlers() {
            @Override
            protected void register() {
                map(Types.BYTE, Types.VAR_INT); // container id
                handler(wrapper -> {
                    final ContainerType containerType = ContainerType.getByValue(wrapper.read(Types.BYTE)); // type
                    final boolean serverInitiated = wrapper.read(Types.BOOLEAN); // server initiated

                    final ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
                    final ExperimentalContainer container = serverInitiated ? inventoryTracker.getCurrentContainer() : inventoryTracker.getPendingCloseContainer();
                    if (container == null) {
                        wrapper.cancel();
                        return;
                    }

                    if (serverInitiated && containerType != container.type()) {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Server tried to close container, but container type was not correct");
                        wrapper.cancel();
                        return;
                    }
                    inventoryTracker.setCurrentContainerClosed(serverInitiated);
                });
            }
        }, true);
        protocol.registerClientbound(ClientboundBedrockPackets.INVENTORY_CONTENT, ClientboundPackets26_1.CONTAINER_SET_CONTENT, wrapper -> {
            final ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);
            final int containerId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // container id
            final BedrockItem[] items = wrapper.read(itemRewriter.newItemArrayType()); // items
            final FullContainerName containerName = wrapper.read(BedrockTypes.FULL_CONTAINER_NAME); // container name
            final BedrockItem storageItem = wrapper.read(itemRewriter.newItemType()); // storage item

            final ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
            final ExperimentalContainer container = inventoryTracker.getContainerClientbound((byte) containerId, containerName, storageItem);
            if (container != null && container.setItems(items)) {
                ExperimentalPacketFactory.writeJavaContainerSetContent(wrapper, container);
            } else {
                wrapper.cancel();
            }
        }, true);
        protocol.registerClientbound(ClientboundBedrockPackets.INVENTORY_SLOT, ClientboundPackets26_1.CONTAINER_SET_SLOT, wrapper -> {
            final ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);
            final int containerId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // container id
            final int slot = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // slot
            final FullContainerName containerName = wrapper.read(BedrockTypes.OPTIONAL_FULL_CONTAINER_NAME); // container name
            final BedrockItem storageItem = wrapper.read(itemRewriter.optionalNewItemType()); // storage item
            final BedrockItem item = wrapper.read(itemRewriter.newItemType()); // item

            final ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
            final ExperimentalContainer container = inventoryTracker.getContainerClientbound((byte) containerId, containerName, storageItem);
            if (container != null && container.setItem(slot, item)) {
                if (container.type() == ContainerType.HUD && slot == 0) { // cursor item
                    wrapper.setPacketType(ClientboundPackets26_1.SET_CURSOR_ITEM);
                } else {
                    wrapper.write(Types.VAR_INT, (int) container.javaContainerId()); // container id
                    wrapper.write(Types.VAR_INT, 0); // revision
                    wrapper.write(Types.SHORT, (short) container.javaSlot(slot)); // slot
                }
                wrapper.write(VersionedTypes.V26_1.item, container.getJavaItem(slot)); // item
            } else {
                wrapper.cancel();
            }
        }, true);
        protocol.registerClientbound(ClientboundBedrockPackets.CONTAINER_REGISTRY_CLEANUP, null, wrapper -> {
            wrapper.cancel();
            final ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
            final FullContainerName[] removedContainers = wrapper.read(BedrockTypes.FULL_CONTAINER_NAME_ARRAY); // removed containers
            for (FullContainerName containerName : removedContainers) {
                inventoryTracker.removeDynamicContainer(containerName);
            }
        }, true);
        protocol.registerClientbound(ClientboundBedrockPackets.CRAFTING_DATA, null, wrapper -> {
            wrapper.cancel();
            CraftingDataTracker craftingDataTracker = wrapper.user().get(CraftingDataTracker.class);
            ItemRewriter itemRewriter = wrapper.user().get(ItemRewriter.class);

            List<CraftingDataStorage> recipes = new ArrayList<>();
            final int craftingDataSize = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);
            for (int i = 0; i < craftingDataSize; i++) {
                final RecipeType recipeType = RecipeType.getByValue(wrapper.read(BedrockTypes.VAR_INT));
                switch (recipeType) {
                    case SHAPELESS, USER_DATA_SHAPELESS, SHAPELESS_CHEMISTRY -> {
                        final String recipeId = wrapper.read(BedrockTypes.STRING);
                        final List<ItemDescriptor> ingredients = List.of(wrapper.read(ExperimentalBedrockTypes.ITEM_DESCRIPTORS));
                        final List<BedrockItem> results = List.of(wrapper.read(itemRewriter.itemInstanceArrayType()));
                        final UUID recipeUuid = wrapper.read(BedrockTypes.UUID);
                        final String recipeTag = wrapper.read(BedrockTypes.STRING);
                        final int priority = wrapper.read(BedrockTypes.VAR_INT);

                        // TODO: Sync unlocking recipes
                        if (recipeType == RecipeType.SHAPELESS || recipeType == RecipeType.USER_DATA_SHAPELESS) {
                            final byte unlock = wrapper.read(Types.BYTE);
                            if (unlock == 0) {
                                wrapper.read(ExperimentalBedrockTypes.ITEM_DESCRIPTORS);
                            }
                        }

                        final int netId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);

                        CraftingDataStorage recipe = new CraftingDataStorage(
                                recipeType,
                                netId,
                                new ShapelessRecipe(recipeId, recipeUuid, recipeTag, priority, ingredients, results)
                        );
                        recipes.add(recipe);
                    }
                    case SHAPED, SHAPED_CHEMISTRY -> {
                        final String recipeId = wrapper.read(BedrockTypes.STRING);
                        final int width = wrapper.read(BedrockTypes.VAR_INT);
                        final int height = wrapper.read(BedrockTypes.VAR_INT);
                        final ItemDescriptor[][] ingredients = new ItemDescriptor[height][width];
                        for (int row = 0; row < height; row++) {
                            for (int col = 0; col < width; col++) {
                                ingredients[row][col] = wrapper.read(ExperimentalBedrockTypes.ITEM_DESCRIPTOR_TYPE);
                            }
                        }

                        final List<BedrockItem> results = List.of(wrapper.read(itemRewriter.itemInstanceArrayType()));
                        final UUID recipeUuid = wrapper.read(BedrockTypes.UUID);
                        final String recipeTag = wrapper.read(BedrockTypes.STRING);
                        final int priority = wrapper.read(BedrockTypes.VAR_INT);
                        final boolean assumeSymmetric = wrapper.read(Types.BOOLEAN);

                        // TODO: Sync unlocking recipes
                        if (recipeType == RecipeType.SHAPED) {
                            final byte unlock = wrapper.read(Types.BYTE);
                            if (unlock == 0) {
                                wrapper.read(ExperimentalBedrockTypes.ITEM_DESCRIPTORS);
                            }
                        }

                        final int netId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);

                        CraftingDataStorage recipe = new CraftingDataStorage(
                                recipeType,
                                netId,
                                new ShapedRecipe(recipeId, recipeUuid, recipeTag, priority, ingredients, results, assumeSymmetric)
                        );
                        recipes.add(recipe);
                    }
                    case UNKNOWN_1, UNKNOWN_2 -> { // TODO: What is this for
                        final int itemData = wrapper.read(BedrockTypes.VAR_INT);

                        if (recipeType == RecipeType.UNKNOWN_2) {
                            final int itemAuxData = wrapper.read(BedrockTypes.VAR_INT);
                        }

                        BedrockItem result = wrapper.read(itemRewriter.itemInstanceType());

                        final String recipeTag = wrapper.read(BedrockTypes.STRING);
                    }
                    case MULTI -> { // TODO: What is this for
                        final UUID recipeUuid = wrapper.read(BedrockTypes.UUID);
                        final int netId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);
                    }
                    case SMITHING_TRANSFORM, SMITHING_TRIM -> {
                        final String recipeId = wrapper.read(BedrockTypes.STRING);
                        final ItemDescriptor template = wrapper.read(ExperimentalBedrockTypes.ITEM_DESCRIPTOR_TYPE);
                        final ItemDescriptor base = wrapper.read(ExperimentalBedrockTypes.ITEM_DESCRIPTOR_TYPE);
                        final ItemDescriptor addition = wrapper.read(ExperimentalBedrockTypes.ITEM_DESCRIPTOR_TYPE);
                        BedrockItem result = BedrockItem.empty();
                        if (recipeType == RecipeType.SMITHING_TRANSFORM) {
                            result = wrapper.read(itemRewriter.itemInstanceType());
                        }
                        final String recipeTag = wrapper.read(BedrockTypes.STRING);
                        final int netId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);

                        CraftingDataStorage recipe = new CraftingDataStorage(
                                recipeType,
                                netId,
                                new SmithingRecipe(recipeId, UUID.randomUUID() /*TODO: Could potentially be clashes*/, recipeTag, 0, template, base, addition, result)
                        );
                        recipes.add(recipe);
                    }
                    default -> {
                        ViaBedrock.getPlatform().getLogger().warning("Received unsupported recipe type: " + recipeType);
                    }
                }
            }
            craftingDataTracker.updateCraftingDataList(recipes);

            craftingDataTracker.sendJavaUpdateRecipes(wrapper.user());
            //craftingDataTracker.sendJavaRecipeBook(wrapper.user()); //TODO: Broken: 26.1

            wrapper.clearPacket();

            // TODO: Potion Mixes

            // TODO: Container Mixes

            // TODO: Material Reducers

            // TODO: Clear recipes
        });
        protocol.registerClientbound(ClientboundBedrockPackets.ITEM_STACK_RESPONSE, null, wrapper -> {
            wrapper.cancel();
            ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
            InventoryRequestTracker inventoryRequestTracker = wrapper.user().get(InventoryRequestTracker.class);
            ItemStackResponseInfo[] infoList = wrapper.read(ExperimentalBedrockTypes.ITEM_STACK_RESPONSES);

            // Resync the inventory content based on the response
            for (ItemStackResponseInfo info : infoList) {

                InventoryRequestStorage requestInfo = inventoryRequestTracker.getRequest(info.requestId());
                if (requestInfo == null) {
                    ViaBedrock.getPlatform().getLogger().warning("Received item stack response for unknown request ID: " + info.requestId());
                    continue;
                }
                inventoryRequestTracker.removeRequest(info.requestId());

                if (info.result() != ItemStackNetResult.Success) {
                    ViaBedrock.getPlatform().getLogger().warning("Received unsuccessful item stack response: " + info.result());
                    inventoryTracker.getHudContainer().setItems(requestInfo.prevCursorContainer().getItems().clone());
                    for (ExperimentalContainer container : requestInfo.prevContainers()) {
                        ExperimentalContainer newContainer = inventoryTracker.getContainerClientbound(container.containerId(), null, null);
                        if (newContainer == null) continue;
                        newContainer.setItems(container.getItems().clone());
                        ExperimentalPacketFactory.sendJavaContainerSetContent(wrapper.user(), newContainer);  // Resync the container content on Java side
                    }
                    continue;
                }

                //TODO: This is required for crafting so that the cursor item net id is updated properly
                //TODO: Check that the items match the request, if not resync the container (We probably should do this anyway to be safe)
                List<ExperimentalContainer> mismatchedContainers = new ArrayList<>();
                for (ItemStackResponseContainerInfo containerInfo : info.containers()) {
                    for (ItemStackResponseSlotInfo slotInfo : containerInfo.slots()) {
                        ExperimentalContainer container = inventoryTracker.getContainerFromName(containerInfo.containerName(), slotInfo.slot());
                        if (container == null) {
                            ViaBedrock.getPlatform().getLogger().warning("Received item stack response for unknown container: " + containerInfo.containerName());
                            continue;
                        }

                        // Check if the item matches the expected item
                        BedrockItem expectedItem = container.getItem(slotInfo.slot());
                        if (expectedItem.isEmpty()) continue; //TODO
                        if (expectedItem.netId() == null || expectedItem.netId() != slotInfo.itemNetId() || expectedItem.amount() != slotInfo.amount()) {
                            BedrockItem newItem = expectedItem.copy();
                            newItem.setNetId(slotInfo.itemNetId());
                            newItem.setAmount(slotInfo.amount());
                            container.setItem(slotInfo.slot(), newItem);
                            if (container.getFullContainerName(slotInfo.slot()).name() != ContainerEnumName.CursorContainer) {
                                mismatchedContainers.add(container);
                            }
                        }
                        // TODO:  Handle custom name and durability
                    }
                }

                for (ExperimentalContainer container : mismatchedContainers) {
                    ExperimentalPacketFactory.sendJavaContainerSetContent(wrapper.user(), container);  // Resync the container content on Java side
                }
                // Force resync cursor
                ExperimentalPacketFactory.sendJavaContainerSetContent(wrapper.user(), inventoryTracker.getInventoryContainer());
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.CONTAINER_SET_DATA, ClientboundPackets26_1.CONTAINER_SET_DATA, wrapper -> {
            byte containerId = wrapper.read(Types.BYTE);
            int id = wrapper.read(BedrockTypes.VAR_INT);
            int value = wrapper.read(BedrockTypes.VAR_INT);

            ExperimentalContainer container = wrapper.user().get(ExperimentalInventoryTracker.class).getContainerClientbound(containerId, null, null);
            if (container == null) {
                // TODO: This throws every time we open a container
                // Unknown container, ignore
                wrapper.cancel();
                ViaBedrock.getPlatform().getLogger().warning("Received ContainerSetData packet for unknown container: containerId=" + containerId + ", id=" + id + ", value=" + value);
                return;
            }
            int windowId = container.javaContainerId();
            if (windowId == -1) {
                // Unknown container, ignore
                wrapper.cancel();
                ViaBedrock.getPlatform().getLogger().warning("Received ContainerSetData packet for unknown container: containerId=" + containerId + ", id=" + id + ", value=" + value);
                return;
            }

            short javaId = container.translateContainerData(id);
            if (javaId == -1) {
                ViaBedrock.getPlatform().getLogger().warning("Received ContainerSetData packet with unknown id: containerId=" + containerId + ", id=" + id + ", value=" + value);
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.VAR_INT, windowId);
            wrapper.write(Types.SHORT, javaId);
            wrapper.write(Types.SHORT, (short) value);
        });
        protocol.registerClientbound(ClientboundBedrockPackets.PLAYER_ENCHANT_OPTIONS, null, wrapper -> {
            wrapper.cancel();
            ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
            if (!(inventoryTracker.getCurrentContainer() instanceof EnchantmentContainer)){
                return;
            }
            EnchantmentContainer enchantmentContainer = (EnchantmentContainer) inventoryTracker.getCurrentContainer();

            final int size = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // size

            List<EnchantData> data = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                final byte cost = wrapper.read(Types.BYTE); // cost
                wrapper.read(BedrockTypes.INT_LE); // slot

                // TODO: How does bedrock decide on what to show
                Enchant_Type selected = null;
                int level = -1;

                final int l1 = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // size
                for (int j = 0; j < l1; j++) {
                    final int id = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // enchant id
                    final byte lvl = wrapper.read(Types.BYTE); // enchant level

                    if (selected == null) {
                        selected = Enchant_Type.getByValue(id);
                        level = lvl;
                    }
                }

                final int l2 = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // size
                for (int j = 0; j < l2; j++) {
                    final int id = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // enchant id
                    final byte lvl = wrapper.read(Types.BYTE); // enchant level

                    if (selected == null) {
                        selected = Enchant_Type.getByValue(id);
                        level = lvl;
                    }
                }

                final int l3 = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // size
                for (int j = 0; j < l3; j++) {
                    final int id = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // enchant id
                    final byte lvl = wrapper.read(Types.BYTE); // enchant level

                    if (selected == null) {
                        selected = Enchant_Type.getByValue(id);
                        level = lvl;
                    }
                }

                wrapper.read(BedrockTypes.STRING); // Name
                final int netId = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // Enchant Net Id

                data.add(new EnchantData(cost, selected, level, netId));
            }

            enchantmentContainer.setEnchantData(data);
        });
        ProtocolUtil.appendClientbound(protocol, ClientboundBedrockPackets.CHANGE_DIMENSION, wrapper -> {
            final ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);
            if (inventoryTracker.isContainerOpen()) {
                inventoryTracker.setCurrentContainerClosed(true);
            }
            ExperimentalPacketFactory.sendJavaContainerSetContent(wrapper.user(), inventoryTracker.getInventoryContainer()); // Java client always resets inventory on respawn. Resend it
            inventoryTracker.getInventoryContainer().sendSelectedHotbarSlotToClient(); // Java client always resets selected hotbar slot on respawn. Resend it
        });
        protocol.registerClientbound(ClientboundBedrockPackets.INVENTORY_TRANSACTION, null, wrapper -> {
            final InventoryTransactionRewriter inventoryTransactionRewriter = wrapper.user().get(InventoryTransactionRewriter.class);
            ExperimentalInventoryTracker inventoryTracker = wrapper.user().get(ExperimentalInventoryTracker.class);

            wrapper.cancel();
            BedrockInventoryTransaction inventoryTransaction = wrapper.read(inventoryTransactionRewriter.getInventoryTransactionType());

            if (inventoryTransaction.legacyRequestId() != 0) {
                // Ignore legacy inventory transactions for now
                return;
            }

            if (inventoryTransaction.actions() != null && !inventoryTransaction.actions().isEmpty()) {
                for (InventoryActionData action : inventoryTransaction.actions()) {
                    if (action.source().type() == InventorySourceType.ContainerInventory) {
                        ExperimentalContainer container = inventoryTracker.getContainerClientbound((byte) action.source().containerId(), null, null);

                        if (container != null) {
                            container.setItem(action.slot(), action.toItem());
                            ExperimentalPacketFactory.sendJavaContainerSetContent(wrapper.user(),  container);
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
                case ItemReleaseTransaction -> {

                }
                default -> {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received unsupported inventory transaction type: " + inventoryTransaction.transactionType());
                }
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.SET_ENTITY_LINK, ClientboundPackets26_1.SET_PASSENGERS, wrapper -> {
            final EntityTracker entityTracker = wrapper.user().get(EntityTracker.class);

            final EntityLink linkType = wrapper.read(BedrockTypes.ENTITY_LINK);
            final Entity vehicle = entityTracker.getEntityByUid(linkType.fromEntityUniqueId());
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
                        entityTracker.getClientPlayer().setMountEntityRId(entityTracker.getEntityByUid(linkType.fromEntityUniqueId()).runtimeId());
                    }
                }
                case None -> { // Remove
                    vehicle.removePassenger(passenger.uniqueId());

                    wrapper.write(Types.VAR_INT, vehicle.javaId()); // vehicle
                    wrapper.write(Types.VAR_INT, vehicle.passengers().size()); // number of passengers
                    for (long passengerUid : vehicle.passengers()) {
                        wrapper.write(Types.VAR_INT, entityTracker.getEntityByUid(passengerUid).javaId()); // passenger id
                    }

                    if (passenger.uniqueId() == entityTracker.getClientPlayer().uniqueId()) {// TODO: This could be applied to all passengers not just players
                        // The player is no longer riding an entity, update the state
                        entityTracker.getClientPlayer().setMountEntityRId(-1);
                        entityTracker.getClientPlayer().setRequestedDismount(false);
                    }
                }
            }
        });

        protocol.registerClientbound(ClientboundBedrockPackets.MAP_ITEM_DATA, ClientboundPackets26_1.MAP_ITEM_DATA, wrapper -> {
            MapTracker mapTracker = wrapper.user().get(MapTracker.class);

            final long mapId = wrapper.read(BedrockTypes.VAR_LONG); // map id
            final int typeFlags = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // type flags
            final byte dimension = wrapper.read(Types.BYTE); // dimension
            final boolean locked = wrapper.read(Types.BOOLEAN); // locked
            final BlockPosition origin = wrapper.read(BedrockTypes.BLOCK_POSITION); // origin

            final LongList trackedEntities = new LongArrayList();
            if ((typeFlags & ClientboundMapItemDataPacket_Type.Creation.getValue()) != 0) {
                final int length = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // length
                for (int i = 0; i < length; i++) {
                    trackedEntities.add(wrapper.read(BedrockTypes.VAR_LONG).longValue());
                }
            }

            byte scale = 0;
            if ((typeFlags & MAP_FLAGS_ALL) != 0) {
                scale = wrapper.read(Types.BYTE); // scale
            }

            final List<MapDecoration> decorations = new ArrayList<>();
            final List<MapTrackedObject> trackedObjects = new ArrayList<>();
            if ((typeFlags & ClientboundMapItemDataPacket_Type.DecorationUpdate.getValue()) != 0) {
                final int length = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // length
                for (int i = 0; i < length; i++) {
                    MapTrackedObject.Type objectType = MapTrackedObject.Type.values()[wrapper.read(BedrockTypes.INT_LE)]; //TODO: Error logging
                    switch (objectType) {
                        case BLOCK:
                            trackedObjects.add(new MapTrackedObject(wrapper.read(BedrockTypes.BLOCK_POSITION)));
                            break;
                        case ENTITY:
                            trackedObjects.add(new MapTrackedObject(wrapper.read(BedrockTypes.VAR_LONG)));
                            break;
                    }
                }

                final int decorLength = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // length
                for (int i = 0; i < decorLength; i++) {
                    final byte iconType = wrapper.read(Types.BYTE);
                    final byte rotation = wrapper.read(Types.BYTE);
                    final byte x = wrapper.read(Types.BYTE);
                    final byte y = wrapper.read(Types.BYTE);
                    final String name = wrapper.read(BedrockTypes.STRING); // name
                    final int color = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // color

                    decorations.add(new MapDecoration(iconType, rotation, x, y, name, color));
                }
            }

            int width = 0;
            int height = 0;
            int xOffset = 0;
            int yOffset = 0;
            int[] colors = new int[0];
            if ((typeFlags & ClientboundMapItemDataPacket_Type.TextureUpdate.getValue()) != 0) {
                width = wrapper.read(BedrockTypes.VAR_INT); // width
                height = wrapper.read(BedrockTypes.VAR_INT); // height
                xOffset = wrapper.read(BedrockTypes.VAR_INT); // x offset
                yOffset = wrapper.read(BedrockTypes.VAR_INT); // y offset

                final int colorsLength = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT); // colors length
                colors = new int[colorsLength];
                for (int i = 0; i < colorsLength; i++) {
                    colors[i] = wrapper.read(BedrockTypes.UNSIGNED_VAR_INT);
                }
            }

            //TODO: Clean this up
            int nextJavaId = mapTracker.getNextMapId();
            if ((typeFlags & ClientboundMapItemDataPacket_Type.Creation.getValue()) != 0) {
                MapObject existingMap = mapTracker.getMapObjects().get(mapId);
                if (existingMap != null) {
                    existingMap.getTrackedEntities().clear();
                    existingMap.getTrackedEntities().addAll(trackedEntities);
                } else {
                    MapObject mapObject = new MapObject(
                            mapId,
                            dimension,
                            locked,
                            origin,
                            trackedEntities,
                            scale,
                            trackedObjects,
                            decorations,
                            width,
                            height,
                            xOffset,
                            yOffset,
                            colors,
                            nextJavaId
                    );
                    mapTracker.getMapObjects().put(mapId, mapObject);
                }
            }
            if ((typeFlags & ClientboundMapItemDataPacket_Type.DecorationUpdate.getValue()) != 0) {
                MapObject existingMap = mapTracker.getMapObjects().get(mapId);
                if (existingMap != null) {
                    existingMap.getTrackedObjects().clear();
                    existingMap.getTrackedObjects().addAll(trackedObjects);
                    existingMap.getDecorations().clear();
                    existingMap.getDecorations().addAll(decorations);
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received map decoration update for unknown map id: " + mapId);
                    MapObject mapObject = new MapObject(
                            mapId,
                            dimension,
                            locked,
                            origin,
                            trackedEntities,
                            scale,
                            trackedObjects,
                            decorations,
                            0,
                            0,
                            0,
                            0,
                            new int[0],
                            nextJavaId
                    );
                    mapTracker.getMapObjects().put(mapId, mapObject);
                }
            }
            if ((typeFlags & ClientboundMapItemDataPacket_Type.TextureUpdate.getValue()) != 0) {
                MapObject existingMap = mapTracker.getMapObjects().get(mapId);
                if (existingMap != null) {
                    existingMap.setWidth(width);
                    existingMap.setHeight(height);
                    existingMap.setXOffset(xOffset);
                    existingMap.setYOffset(yOffset);
                    existingMap.setColors(colors);
                } else {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received map texture update for unknown map id: " + mapId);
                    MapObject mapObject = new MapObject(
                            mapId,
                            dimension,
                            locked,
                            origin,
                            trackedEntities,
                            scale,
                            new ArrayList<>(),
                            new ArrayList<>(),
                            width,
                            height,
                            xOffset,
                            yOffset,
                            colors,
                            nextJavaId
                    );
                    mapTracker.getMapObjects().put(mapId, mapObject);
                }
            }

            MapObject mapObject = mapTracker.getMapObjects().get(mapId);
            if (mapObject == null) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received map item data for unknown map id: " + mapId);
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.VAR_INT, mapObject.getJavaId()); // map id
            wrapper.write(Types.BYTE, mapObject.getScale()); // scale
            wrapper.write(Types.BOOLEAN, mapObject.isLocked()); // locked

            wrapper.write(Types.BOOLEAN, false); // Icons (Prefixed Optional, TODO: Implement)
            wrapper.write(Types.UNSIGNED_BYTE, (short) mapObject.getWidth()); // width
            if (mapObject.getWidth() > 0) {
                wrapper.write(Types.UNSIGNED_BYTE, (short) mapObject.getHeight()); // height
                wrapper.write(Types.BYTE, (byte) mapObject.getXOffset()); // xOffset
                wrapper.write(Types.BYTE, (byte) mapObject.getYOffset()); // yOffset

                wrapper.write(Types.VAR_INT, mapObject.getColors().length);
                for (short color : JavaMapPaletteUtil.convertToJavaPalette(mapObject.getColors())) {
                    wrapper.write(Types.UNSIGNED_BYTE, color);
                }

            } else {
                //ViaBedrock.getPlatform().getLogger().warning("Sent empty map data for map id: " + mapId);
                //TODO: Bedrock requests map data if it doesnt have it, so we need to send something
            }
        });
    }

    public static void registerTasks() {
        Via.getPlatform().runRepeatingSync(new ExperimentalInventoryTrackerTickTask(), 1L);
    }

    public static void registerStorages(final UserConnection user) {
        user.put(new ExperimentalInventoryTracker(user));
        user.put(new InventoryTransactionRewriter(user));
        user.put(new InventoryRequestTracker(user));
        user.put(new CraftingDataTracker(user));
        user.put(new MapTracker(user));
    }

    public static void handlePlayerRespawn(final UserConnection user) {
        final ClientPlayerEntity clientPlayer = user.get(EntityTracker.class).getClientPlayer();
        final GameRulesStorage gameRulesStorage = user.get(GameRulesStorage.class);
        final ExperimentalInventoryTracker inventoryTracker = user.get(ExperimentalInventoryTracker.class);

        if (clientPlayer.isDead() && !gameRulesStorage.<Boolean>getGameRule("keepInventory")) {
            inventoryTracker.getInventoryContainer().clearItems();
            inventoryTracker.getOffhandContainer().clearItems();
            inventoryTracker.getArmorContainer().clearItems();
            inventoryTracker.getHudContainer().clearItems();
        }

        if (gameRulesStorage.getGameRule("keepInventory")) {
            ExperimentalPacketFactory.sendJavaContainerSetContent(user, inventoryTracker.getInventoryContainer()); // Java client always resets inventory on respawn. Resend it
        }
        inventoryTracker.getInventoryContainer().sendSelectedHotbarSlotToClient(); // Java client always resets selected hotbar slot on respawn. Resend it
    }

}
