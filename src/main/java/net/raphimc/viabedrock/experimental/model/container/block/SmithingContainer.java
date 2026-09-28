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
import net.raphimc.viabedrock.experimental.model.recipe.RecipeType;
import net.raphimc.viabedrock.experimental.model.recipe.SmithingRecipe;
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

public class SmithingContainer extends ExperimentalContainer {

    public static final int RESULT_SLOT = 50;
    public static final int INPUT_SLOT = 51;
    public static final int MATERIAL_SLOT = 52;
    public static final int TEMPLATE_SLOT = 53;

    public SmithingContainer(UserConnection user, byte containerId, TextComponent title, BlockPosition position) {
        super(user, containerId, ContainerType.SMITHING_TABLE, title, position, 4, CustomBlockTags.SMITHING_TABLE);
    }

    @Override
    public FullContainerName getFullContainerName(int slot) {
        return switch (slot) {
            case TEMPLATE_SLOT -> new FullContainerName(ContainerEnumName.SmithingTableTemplateContainer, null);
            case INPUT_SLOT -> new FullContainerName(ContainerEnumName.SmithingTableInputContainer, null);
            case MATERIAL_SLOT -> new FullContainerName(ContainerEnumName.SmithingTableMaterialContainer, null);
            case RESULT_SLOT -> new FullContainerName(ContainerEnumName.SmithingTableResultPreviewContainer, null); //TODO: CreatedOutputContainer?
            default -> throw new IllegalArgumentException("Invalid slot for Smithing Container: " + slot);
        };
    }

    @Override
    public int javaSlot(final int slot) {
        return switch (slot) {
            case TEMPLATE_SLOT -> 0;
            case INPUT_SLOT -> 1;
            case MATERIAL_SLOT -> 2;
            case RESULT_SLOT -> 3;
            default -> super.javaSlot(slot);
        };
    }

    @Override
    public int bedrockSlot(final int slot) {
        return switch (slot) {
            case 0 -> TEMPLATE_SLOT;
            case 1 -> INPUT_SLOT;
            case 2 -> MATERIAL_SLOT;
            case 3 -> RESULT_SLOT;
            default -> super.bedrockSlot(slot);
        };
    }

    @Override
    public BedrockItem getItem(int bedrockSlot) {
        return switch (bedrockSlot) {
            case TEMPLATE_SLOT -> this.items[0];
            case INPUT_SLOT -> this.items[1];
            case MATERIAL_SLOT -> this.items[2];
            case RESULT_SLOT -> this.items[3];
            default -> throw new IllegalArgumentException("Invalid slot for Smithing Container: " + bedrockSlot);
        };
    }

    @Override
    public boolean setItem(final int bedrockSlot, final BedrockItem item) {
        return switch (bedrockSlot) {
            case TEMPLATE_SLOT -> super.setItem(0, item);
            case INPUT_SLOT -> super.setItem(1, item);
            case MATERIAL_SLOT -> super.setItem(2, item);
            case RESULT_SLOT -> super.setItem(3, item);
            default -> throw new IllegalArgumentException("Invalid slot for Smithing Container: " + bedrockSlot);
        };
    }

    @Override
    public boolean handleClick(int revision, short javaSlot, byte button, ContainerInput action) {
        boolean result = false;
        if (javaSlot != 3) {
            // Handle click first so we update the crafting grid before checking for a recipe
            result = super.handleClick(revision, javaSlot, button, action);
        }
        if (!ViaBedrock.getConfig().shouldEnableExperimentalFeatures()) {
            return result;
        }
        //TODO: This is experimental code...

        ItemRewriter itemRewriter = user.get(ItemRewriter.class);
        CraftingDataTracker tracker = user.get(CraftingDataTracker.class);
        final CraftingDataStorage craftingDataStorage = tracker.getRecipeData(this, "smithing_table");
        BedrockItem resultItem = BedrockItem.empty();
        if (craftingDataStorage != null) {

        }

        //this.setItem(0, resultItem);
        PacketWrapper containerSlot = PacketWrapper.create(ClientboundPackets26_1.CONTAINER_SET_SLOT, user);
        containerSlot.write(Types.VAR_INT, (int) this.containerId());
        containerSlot.write(Types.VAR_INT, revision);
        containerSlot.write(Types.SHORT, (short) 3); // Output slot
        containerSlot.write(VersionedTypes.V26_1.item, itemRewriter.javaItem(resultItem));
        containerSlot.send(BedrockProtocol.class);

        if (craftingDataStorage == null) {
            // No valid recipe found
            return result;
        }

        if (javaSlot != 3) {
            // Handle click first so we update the crafting grid before checking for a recipe
            return result;
        }

        ExperimentalInventoryTracker inventoryTracker = user.get(ExperimentalInventoryTracker.class);
        InventoryRequestTracker inventoryRequestTracker = user.get(InventoryRequestTracker.class);

        List<ExperimentalContainer> prevContainers = new ArrayList<>();
        prevContainers.add(this.copy());
        prevContainers.add(inventoryTracker.getInventoryContainer().copy());
        ExperimentalContainer prevCursorContainer = inventoryTracker.getHudContainer().copy();

        int bedrockSlot = this.bedrockSlot(javaSlot);

        List<ItemStackRequestAction> actions = new ArrayList<>();
        actions.add(new ItemStackRequestAction.CraftRecipeAction(craftingDataStorage.networkId(), 1));
        actions.add(new ItemStackRequestAction.ConsumeAction(
                1,
                new ItemStackRequestSlotInfo(
                        this.getFullContainerName(TEMPLATE_SLOT),
                        (byte) TEMPLATE_SLOT,
                        this.getItem(TEMPLATE_SLOT).netId()
                )
        ));
        actions.add(new ItemStackRequestAction.ConsumeAction(
                1,
                new ItemStackRequestSlotInfo(
                        this.getFullContainerName(INPUT_SLOT),
                        (byte) INPUT_SLOT,
                        this.getItem(INPUT_SLOT).netId()
                )
        ));
        actions.add(new ItemStackRequestAction.ConsumeAction(
                1,
                new ItemStackRequestSlotInfo(
                        this.getFullContainerName(MATERIAL_SLOT),
                        (byte) MATERIAL_SLOT,
                        this.getItem(MATERIAL_SLOT).netId()
                )
        ));

        int nextRequestId = inventoryRequestTracker.nextRequestId();

        actions.add(
                new ItemStackRequestAction.TakeAction(
                        1, // Total amount to take
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

        BedrockItem templateItem = this.getItem(TEMPLATE_SLOT).copy();
        if (templateItem.amount() - 1 != 0) {
            templateItem.setAmount(templateItem.amount() - 1);
        } else {
            templateItem = BedrockItem.empty();
        }
        this.setItem(TEMPLATE_SLOT, templateItem);

        BedrockItem inputItem = this.getItem(INPUT_SLOT).copy();
        if (inputItem.amount() - 1 != 0) {
            inputItem.setAmount(inputItem.amount() - 1);
        } else {
            inputItem = BedrockItem.empty();
        }
        this.setItem(INPUT_SLOT, inputItem);

        BedrockItem materialItem = this.getItem(MATERIAL_SLOT).copy();
        if (materialItem.amount() - 1 != 0) {
            materialItem.setAmount(materialItem.amount() - 1);
        } else {
            materialItem = BedrockItem.empty();
        }
        this.setItem(MATERIAL_SLOT, materialItem);

        ExperimentalPacketFactory.sendJavaContainerSetContent(user, this);

        //TODO: Re-Update the output slot
        return true;
    }

}