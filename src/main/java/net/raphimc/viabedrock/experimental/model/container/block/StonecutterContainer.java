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
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.experimental.ExperimentalPacketFactory;
import net.raphimc.viabedrock.experimental.model.container.ExperimentalContainer;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestInfo;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestSlotInfo;
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
import java.util.logging.Level;

public class StonecutterContainer extends ExperimentalContainer {

    private final List<CraftingDataStorage> currentRecipes = new ArrayList<>();
    private int selectedRecipe = 0;

    public StonecutterContainer(UserConnection user, byte containerId, TextComponent title, BlockPosition position) {
        super(user, containerId, ContainerType.STONECUTTER, title, position, 2, "stonecutter_block", "stonecutter");
    }

    @Override
    public FullContainerName getFullContainerName(int slot) {
        return switch (slot) {
            case 3 -> new FullContainerName(ContainerEnumName.StonecutterInputContainer, null);
            case 50 -> new FullContainerName(ContainerEnumName.CreatedOutputContainer, null); //TODO: CreatedOutputContainer?
            default -> throw new IllegalArgumentException("Invalid slot for Stonecutter Container: " + slot);
        };
    }

    @Override
    public int javaSlot(final int slot) {
        return switch (slot) {
            case 3 -> 0;
            case 50 -> 1;
            default -> super.javaSlot(slot);
        };
    }

    @Override
    public int bedrockSlot(final int slot) {
        return switch (slot) {
            case 0 -> 3;
            case 1 -> 50;
            default -> super.bedrockSlot(slot);
        };
    }

    @Override
    public BedrockItem getItem(int bedrockSlot) {
        if (bedrockSlot == 3) {
            return this.items[0];
        } else if (bedrockSlot == 50) {
            return this.items[1];
        } else {
            throw new IllegalArgumentException("Bedrock Slot out of bounds for stonecutter (getItem): " + bedrockSlot);
        }
    }

    @Override
    public boolean setItem(final int bedrockSlot, final BedrockItem item) {
        if (bedrockSlot == 3) {
            return super.setItem(0, item);
        } else if (bedrockSlot == 50) {
            return super.setItem(1, item);
        } else {
            throw new IllegalArgumentException("Bedrock Slot out of bounds for stonecutter (setItem): " + bedrockSlot);
        }
    }

    @Override
    public boolean handleClick(final int revision, final short javaSlot, final byte button, final ContainerInput action) {
        boolean result = false;
        if (javaSlot != 1) {
            // Handle click first so we update the crafting grid before checking for a recipe
            result = super.handleClick(revision, javaSlot, button, action);
        }
        if (!ViaBedrock.getConfig().shouldEnableExperimentalFeatures()) {
            return result;
        }
        //TODO: This is experimental code...

        if (javaSlot == 0 || javaSlot == 1) {
            this.updateRecipeData(this.getItem(3));
        } else {
            return result;
        }

        if (this.currentRecipes.isEmpty()) {
            return result;
        }

        ItemRewriter itemRewriter = user.get(ItemRewriter.class);
        final CraftingDataStorage craftingDataStorage = this.currentRecipes.get(this.selectedRecipe);
        BedrockItem resultItem = BedrockItem.empty();
        if (craftingDataStorage != null) {
            // Valid recipe found, show output
            switch (craftingDataStorage.type()) {
                case SHAPELESS -> resultItem = ((ShapelessRecipe) craftingDataStorage.recipe()).getResults().get(0);
                default -> ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown recipe type for stonecutter: " + craftingDataStorage.type());
            }
        }

        PacketWrapper containerSlot = PacketWrapper.create(ClientboundPackets26_1.CONTAINER_SET_SLOT, user);
        containerSlot.write(Types.VAR_INT, (int) this.containerId());
        containerSlot.write(Types.VAR_INT, 0); // Revision
        containerSlot.write(Types.SHORT, (short) 1); // Output slot
        containerSlot.write(VersionedTypes.V1_21_11.item, itemRewriter.javaItem(resultItem));
        containerSlot.send(BedrockProtocol.class);

        if (javaSlot != 1) {
            return result;
        }

        ExperimentalInventoryTracker inventoryTracker = user.get(ExperimentalInventoryTracker.class);
        InventoryRequestTracker inventoryRequestTracker = user.get(InventoryRequestTracker.class);

        List<ExperimentalContainer> prevContainers = new ArrayList<>();
        prevContainers.add(this.copy());
        prevContainers.add(inventoryTracker.getInventoryContainer().copy());
        ExperimentalContainer prevCursorContainer = inventoryTracker.getHudContainer().copy();

        int nextRequestId = inventoryRequestTracker.nextRequestId();
        BedrockItem sourceItem = this.getItem(3);

        int craftableAmount = 1;
        int toConsume = Math.min(sourceItem.amount(), craftableAmount);
        // TODO: shift click = max to inventory

        List<ItemStackRequestAction> actions = new ArrayList<>();
        actions.add(new ItemStackRequestAction.CraftRecipeAction(craftingDataStorage.networkId(), craftableAmount));
        actions.add(new ItemStackRequestAction.ConsumeAction(
                toConsume,
                new ItemStackRequestSlotInfo(
                        this.getFullContainerName(3),
                        (byte) 3,
                        sourceItem.netId()
                )
        ));
        actions.add(new ItemStackRequestAction.TakeAction(
                craftableAmount * resultItem.amount(), // Total amount to take
                new ItemStackRequestSlotInfo(
                        this.getFullContainerName(50),
                        (byte) 50,
                        nextRequestId // TODO
                ),
                new ItemStackRequestSlotInfo(
                        new FullContainerName(ContainerEnumName.CursorContainer, null),
                        (byte) 0,
                        0 // The stackNetworkId is not known yet
                )
        ));

        ItemStackRequestInfo request = new ItemStackRequestInfo(
                nextRequestId,
                actions,
                List.of(),
                TextProcessingEventOrigin.Unknown
        );

        inventoryRequestTracker.addRequest(new InventoryRequestStorage(request, revision, prevCursorContainer, prevContainers)); // Store the request to track it later
        ExperimentalPacketFactory.sendBedrockInventoryRequest(user, new ItemStackRequestInfo[]{request});

        inventoryTracker.getHudContainer().setItem(0, resultItem); // Update cursor to the crafted item
        BedrockItem newSrc = sourceItem.copy();
        newSrc.setAmount(sourceItem.amount() - 1);
        this.setItem(3, newSrc);

        ExperimentalPacketFactory.sendJavaContainerSetContent(user, this);

        this.updateRecipeData(this.getItem(3));
        return true;
    }

    @Override
    public boolean handleButtonClick(final int button) {
        this.updateRecipeData(this.getItem(3));
        if (button >= this.currentRecipes.size()) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Invalid recipe button clicked in stonecutter: " + button);
            return false;
        }

        this.selectedRecipe = button;
        ItemRewriter itemRewriter = user.get(ItemRewriter.class);

        final CraftingDataStorage craftingDataStorage = this.currentRecipes.get(button);
        BedrockItem resultItem = BedrockItem.empty();
        if (craftingDataStorage != null) {
            // Valid recipe found, show output
            switch (craftingDataStorage.type()) {
                case SHAPELESS -> resultItem = ((ShapelessRecipe) craftingDataStorage.recipe()).getResults().get(0);
                default -> ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown recipe type for stonecutter: " + craftingDataStorage.type());
            }
        }

        PacketWrapper containerSlot = PacketWrapper.create(ClientboundPackets26_1.CONTAINER_SET_SLOT, user);
        containerSlot.write(Types.VAR_INT, (int) this.containerId());
        containerSlot.write(Types.VAR_INT, 0); // Revision
        containerSlot.write(Types.SHORT, (short) 1); // Output slot
        containerSlot.write(VersionedTypes.V26_1.item, itemRewriter.javaItem(resultItem));
        containerSlot.send(BedrockProtocol.class);

        return true;
    }

    // TODO: Refactor to CraftingDataTracker
    private void updateRecipeData(BedrockItem item) {
        CraftingDataTracker craftingDataTracker = user.get(CraftingDataTracker.class);
        this.currentRecipes.clear();

        for (CraftingDataStorage craftingData : craftingDataTracker.getCraftingDataList()) {
            if (craftingData.recipe() == null || !craftingData.recipe().getRecipeTag().equals("stonecutter")) {
                continue;
            }

            switch (craftingData.type()) {
                case SHAPELESS -> {
                    ShapelessRecipe recipe = (ShapelessRecipe) craftingData.recipe();
                    if (recipe.getIngredients().get(0).matchesItem(this.user, item)) {
                        this.currentRecipes.add(craftingData);
                    }
                }
                default -> ViaBedrock.getPlatform().getLogger().warning(
                        "Unknown recipe type for stonecutter: " + craftingData.type() + " in recipe " + craftingData.recipe().getUniqueId()
                );
            }
        }

        //TODO: update buttons
    }

}