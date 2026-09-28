/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2023-2025 RK_01/RaphiMC and contributors
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
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.experimental.ExperimentalPacketFactory;
import net.raphimc.viabedrock.experimental.model.container.ExperimentalContainer;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestInfo;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestSlotInfo;
import net.raphimc.viabedrock.experimental.model.recipe.ItemDescriptor;
import net.raphimc.viabedrock.experimental.model.recipe.ShapedRecipe;
import net.raphimc.viabedrock.experimental.model.recipe.ShapelessRecipe;
import net.raphimc.viabedrock.experimental.storage.*;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerEnumName;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.TextProcessingEventOrigin;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.ContainerInput;
import net.raphimc.viabedrock.protocol.data.generated.bedrock.CustomBlockTags;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class CraftingTableContainer extends ExperimentalContainer {

    public CraftingTableContainer(UserConnection user, byte containerId, TextComponent title, BlockPosition position) {
        super(user, containerId, ContainerType.WORKBENCH, title, position, 10, CustomBlockTags.WORKBENCH);
    }

    @Override
    public FullContainerName getFullContainerName(int slot) {
        return switch (slot) {
            case 32, 33, 34, 35, 36, 37, 38, 39, 40 ->
                    new FullContainerName(ContainerEnumName.CraftingInputContainer, null);
            case 50 -> new FullContainerName(ContainerEnumName.CreatedOutputContainer, null);
            default -> throw new IllegalArgumentException("Invalid slot for Crafting Container: " + slot);
        };
    }

    @Override
    public int javaSlot(final int bedrockSlot) {
        return switch (bedrockSlot) {
            case 32, 33, 34, 35, 36, 37, 38, 39, 40 -> bedrockSlot - 31;
            case 50 -> 0;
            default -> super.javaSlot(bedrockSlot);
        };
    }

    @Override
    public int bedrockSlot(final int javaSlot) {
        return switch (javaSlot) {
            case 1, 2, 3, 4, 5, 6, 7, 8, 9 -> javaSlot + 31;
            case 0 -> 50;
            default -> super.bedrockSlot(javaSlot);
        };
    }

    @Override
    public BedrockItem getItem(int bedrockSlot) {
        return switch (bedrockSlot) {
            case 50 -> this.items[0];
            case 32, 33, 34, 35, 36, 37, 38, 39, 40 -> this.items[bedrockSlot - 31];
            default -> throw new IllegalArgumentException("Invalid slot for Crafting Container: " + bedrockSlot);
        };
    }

    @Override
    public boolean setItem(final int bedrockSlot, final BedrockItem item) {
        return switch (bedrockSlot) {
            case 50 -> super.setItem(0, item);
            case 32, 33, 34, 35, 36, 37, 38, 39, 40 -> super.setItem(bedrockSlot - 31, item);
            default -> throw new IllegalArgumentException("Invalid slot for Crafting Container: " + bedrockSlot);
        };
    }

    @Override
    public boolean handleClick(final int revision, final short javaSlot, final byte button, final ContainerInput action) {
        boolean result = false;
        if (javaSlot != 0) {
            // Handle click first so we update the crafting grid before checking for a recipe
            result = super.handleClick(revision, javaSlot, button, action);
        }
        if (!ViaBedrock.getConfig().shouldEnableExperimentalFeatures()) {
            return result;
        }
        //TODO: This is experimental code...

        ItemRewriter itemRewriter = user.get(ItemRewriter.class);
        CraftingDataTracker tracker = user.get(CraftingDataTracker.class);
        final CraftingDataStorage craftingDataStorage = tracker.getRecipeData(this, "crafting_table");
        BedrockItem resultItem = BedrockItem.empty();
        if (craftingDataStorage != null) {
            // Valid recipe found, show output
            switch (craftingDataStorage.type()) {
                case SHAPELESS -> resultItem = ((ShapelessRecipe) craftingDataStorage.recipe()).getResults().get(0);
                case SHAPED -> resultItem = ((ShapedRecipe) craftingDataStorage.recipe()).getResults().get(0);
                default -> ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown recipe type for crafting: " + craftingDataStorage.type());
            }
        } else {
            // No valid recipe found
            return result;
        }

        //this.setItem(0, resultItem);
        PacketWrapper containerSlot = PacketWrapper.create(ClientboundPackets26_1.CONTAINER_SET_SLOT, user);
        containerSlot.write(Types.VAR_INT, (int) this.containerId());
        containerSlot.write(Types.VAR_INT, revision);
        containerSlot.write(Types.SHORT, (short) 0); // Output slot
        containerSlot.write(VersionedTypes.V26_1.item, itemRewriter.javaItem(resultItem));
        containerSlot.send(BedrockProtocol.class);

        if (javaSlot != 0) {
            // Handle click first so we update the crafting grid before checking for a recipe
            return result;
        }

        ExperimentalInventoryTracker inventoryTracker = user.get(ExperimentalInventoryTracker.class);
        InventoryRequestTracker inventoryRequestTracker = user.get(InventoryRequestTracker.class);

        List<ExperimentalContainer> prevContainers = new ArrayList<>();
        prevContainers.add(this.copy());
        prevContainers.add(inventoryTracker.getInventoryContainer().copy());
        ExperimentalContainer prevCursorContainer = inventoryTracker.getHudContainer().copy();

        int craftableAmount = 1;

        int bedrockSlot = this.bedrockSlot(javaSlot);
        // TODO: shift click = max to inventory

        List<ItemStackRequestAction> actions = new ArrayList<>();
        actions.add(new ItemStackRequestAction.CraftRecipeAction(craftingDataStorage.networkId(), craftableAmount));
        //actions.add(new ItemStackRequestAction.CraftResultsDeprecatedAction(resultItems, 1)); //TODO: Deprecated action, hopefully removed in the future

        for (int i = 1; i <= 9; i++) {
            int inputSlot = i + 31; // Crafting grid slots in bedrock
            BedrockItem item = this.getItem(inputSlot);
            if (!item.isEmpty() && item.amount() > 0) {
                int toConsume = Math.min(item.amount(), craftableAmount);
                actions.add(new ItemStackRequestAction.ConsumeAction(
                        toConsume,
                        new ItemStackRequestSlotInfo(
                                this.getFullContainerName(inputSlot),
                                (byte) inputSlot,
                                this.getItem(inputSlot).netId()
                        )
                ));
            }
        }

        int nextRequestId = inventoryRequestTracker.nextRequestId();
        actions.add(
                new ItemStackRequestAction.TakeAction(
                        craftableAmount * resultItem.amount(), // Total amount to take
                        new ItemStackRequestSlotInfo(
                                this.getFullContainerName(bedrockSlot),
                                (byte) bedrockSlot,
                                nextRequestId // TODO
                        ),
                        new ItemStackRequestSlotInfo(
                                new FullContainerName(ContainerEnumName.CursorContainer, null),
                                (byte) 0,
                                0 // The stackNetworkId is not known yet
                        )
                )
        );

        ItemStackRequestInfo request = new ItemStackRequestInfo(
                nextRequestId,
                actions,
                List.of(),
                TextProcessingEventOrigin.Unknown
        );

        inventoryRequestTracker.addRequest(new InventoryRequestStorage(request, revision, prevCursorContainer, prevContainers)); // Store the request to track it later
        ExperimentalPacketFactory.sendBedrockInventoryRequest(user, new ItemStackRequestInfo[]{request});

        inventoryTracker.getHudContainer().setItem(0, resultItem); // Update cursor to the crafted item
        for (int i = 1; i <= 9; i++) {
            int inputSlot = i + 31; // Crafting grid slots in bedrock
            BedrockItem item = this.getItem(inputSlot);
            if (!item.isEmpty() && item.amount() > 0) {
                int toConsume = Math.min(item.amount(), craftableAmount);
                item = item.copy();
                item.setAmount(item.amount() - toConsume);
                if (item.amount() > 0) {
                    this.setItem(inputSlot, item);
                } else {
                    this.setItem(inputSlot, BedrockItem.empty());
                }
            } else {
                this.setItem(inputSlot, BedrockItem.empty());
            }
        }
        ExperimentalPacketFactory.sendJavaContainerSetContent(user, this);

        //TODO: Re-Update the output slot based on remaining items in the crafting grid
        return true;
    }
}
