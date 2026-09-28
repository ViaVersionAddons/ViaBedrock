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
package net.raphimc.viabedrock.experimental.model.container;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.experimental.ExperimentalPacketFactory;
import net.raphimc.viabedrock.experimental.model.container.player.InventoryContainer;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestInfo;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestSlotInfo;
import net.raphimc.viabedrock.experimental.storage.ExperimentalInventoryTracker;
import net.raphimc.viabedrock.experimental.storage.InventoryRequestStorage;
import net.raphimc.viabedrock.experimental.storage.InventoryRequestTracker;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.TextProcessingEventOrigin;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.ContainerInput;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public abstract class ExperimentalContainer {

    protected final UserConnection user;
    protected final byte containerId;
    protected final ContainerType type;
    protected final TextComponent title;
    protected final BlockPosition position;
    protected final BedrockItem[] items;
    protected final Set<String> validBlockTags;

    public ExperimentalContainer(final UserConnection user, final byte containerId, final ContainerType type, final TextComponent title, final BlockPosition position, final int size, final String... validBlockTags) {
        this.user = user;
        this.containerId = containerId;
        this.type = type;
        this.title = title;
        this.position = position;
        this.items = BedrockItem.emptyArray(size);
        this.validBlockTags = Set.of(validBlockTags);
    }

    protected ExperimentalContainer(final UserConnection user, final byte containerId, final ContainerType type, final TextComponent title, final BlockPosition position, final BedrockItem[] items, final Set<String> validBlockTags) {
        this.user = user;
        this.containerId = containerId;
        this.type = type;
        this.title = title;
        this.position = position;
        this.items = items;
        this.validBlockTags = validBlockTags;
    }

    public abstract FullContainerName getFullContainerName(int slot);

    public boolean handleClick(final int revision, final short javaSlot, final byte button, final ContainerInput action) {
        if (javaSlot == -1) return false; // TODO: Safeguard

        ExperimentalInventoryTracker inventoryTracker = user.get(ExperimentalInventoryTracker.class);
        InventoryRequestTracker inventoryRequestTracker = user.get(InventoryRequestTracker.class);
        ClickContext clickContext = new ClickContext(this, this.bedrockSlot(javaSlot), inventoryTracker, inventoryRequestTracker);

        /* TODO: Could potentially lead to a race condition if we receive a inventory update before the response for the request,
         *  a better solution would be to store the specific changes made in the request. From my testing this doesnt seem to happen though
         */
        clickContext.prevContainers.add(this.copy()); // Store previous state of the container

        List<ItemStackRequestAction> itemActions = switch (action) {
            case PICKUP -> this.singletonAction(this.handlePickupClick(clickContext, javaSlot, button));
            case SWAP -> this.singletonAction(this.handleSwapClick(clickContext, javaSlot, button));
            case QUICK_MOVE -> this.handleQuickMoveClick(clickContext, javaSlot);
            case THROW -> this.singletonAction(this.handleThrowClick(clickContext, javaSlot, button));
            default -> List.of();
        };

        if (itemActions.isEmpty()) {
            return false;
        }

        ItemStackRequestInfo request = new ItemStackRequestInfo(
                clickContext.inventoryRequestTracker.nextRequestId(),
                itemActions,
                List.of(),
                TextProcessingEventOrigin.Unknown
        );

        clickContext.inventoryRequestTracker.addRequest(new InventoryRequestStorage(request, revision, clickContext.prevCursorContainer, clickContext.prevContainers)); // Store the request to track it later
        ExperimentalPacketFactory.sendBedrockInventoryRequest(user, new ItemStackRequestInfo[] {request});

        return true;
    }

    private ItemStackRequestAction handlePickupClick(final ClickContext clickContext, final short javaSlot, final byte button) {
        ExperimentalContainer container = clickContext.container;
        int bedrockSlot = clickContext.bedrockSlot;
        BedrockItem cursorItem = clickContext.inventoryTracker.getHudContainer().getItem(0);

        if (javaSlot == -999) {
            return this.dropCursorItem(clickContext.inventoryTracker, button);
        }

        if (!(container instanceof InventoryContainer) && (javaSlot < 0 || javaSlot >= container.getItems().length)) {
            ExperimentalContainer inventoryContainer = clickContext.inventoryTracker.getInventoryContainer();
            int invSlot = inventoryContainer.bedrockSlot(javaSlot - container.getItems().length + 9); // Map to inventory slot
            if (invSlot < 0 || invSlot >= inventoryContainer.getItems().length) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Tried to handle click for " + container.type() + ", but slot was out of bounds (" + javaSlot + ")");
                return null;
            }

            bedrockSlot = invSlot;
            container = inventoryContainer;
            clickContext.container = container;
            clickContext.bedrockSlot = bedrockSlot;
            clickContext.prevContainers.add(container.copy()); // Store previous state of the inventory container
        }

        if (container instanceof InventoryContainer) {
            if (javaSlot >= 0 && javaSlot < 5) {
                ExperimentalContainer hudContainer = clickContext.inventoryTracker.getHudContainer();
                int hudSlot  = hudContainer.bedrockSlot(javaSlot);

                bedrockSlot = hudSlot;
                container = hudContainer;
                clickContext.container = container;
                clickContext.bedrockSlot = hudSlot;
                clickContext.prevContainers.add(container.copy());

                // TODO: Crafting
            } else if (javaSlot >= 5 && javaSlot < 9) {
                // Armor slots
                ExperimentalContainer armorContainer = clickContext.inventoryTracker.getArmorContainer();
                int armorSlot = armorContainer.bedrockSlot(javaSlot);

                bedrockSlot = armorSlot;
                container = armorContainer;
                clickContext.container = armorContainer;
                clickContext.bedrockSlot = armorSlot;
                clickContext.prevContainers.add(armorContainer.copy());
            } else if (javaSlot == 45) {
                // Offhand
                ExperimentalContainer offhandContainer = clickContext.inventoryTracker.getOffhandContainer();
                int offhandSlot = offhandContainer.bedrockSlot(javaSlot);

                bedrockSlot  = offhandSlot;
                container = offhandContainer;
                clickContext.container = offhandContainer;
                clickContext.bedrockSlot = offhandSlot;
                clickContext.prevContainers.add(offhandContainer.copy());
            }
        }

        // TODO: Container Limited Slots (e.g. Furnace Fuel/Input/Output) Note: this might not be needed as the server will reject invalid moves anyway

        BedrockItem item = container.getItem(bedrockSlot);
        if (item.isEmpty() && cursorItem.isEmpty()) {
            return null;
        }

        if (cursorItem.isEmpty()) {
            return this.handlePickupTake(clickContext, container, bedrockSlot, button, item);
        }

        final ItemRewriter itemRewriter = this.user.get(ItemRewriter.class);

        if (item.isEmpty() || (!item.isDifferent(cursorItem) && item.amount() < itemRewriter.maxStackSize(item))) {
            return this.handlePickupPlace(clickContext, container, bedrockSlot, button, cursorItem, item);
        }

        return this.handlePickupSwap(clickContext, container, bedrockSlot, cursorItem, item);
    }

    private ItemStackRequestAction handlePickupTake(final ClickContext clickContext, final ExperimentalContainer container, final int bedrockSlot, final byte button, final BedrockItem item) {
        int amountToTake = button == 0 ? item.amount() : (item.amount() + 1) / 2;

        BedrockItem finalCursorItem = this.copyStackWithAmount(item, amountToTake);
        clickContext.inventoryTracker.getHudContainer().setItem(0, finalCursorItem);

        BedrockItem finalContainerItem = this.itemAfterRemovingAmount(item, amountToTake);
        container.setItem(bedrockSlot, finalContainerItem);

        return new ItemStackRequestAction.TakeAction(
                amountToTake,
                new ItemStackRequestSlotInfo(container.getFullContainerName(bedrockSlot), (byte) bedrockSlot, finalCursorItem.netId()),
                new ItemStackRequestSlotInfo(clickContext.inventoryTracker.getHudContainer().getFullContainerName(0), (byte) 0, 0)
        );
    }

    private ItemStackRequestAction handlePickupPlace(final ClickContext clickContext, final ExperimentalContainer container, final int bedrockSlot, final byte button, final BedrockItem cursorItem, final BedrockItem item) {
        int amt = button == 0 ? cursorItem.amount() : 1;
        int amountToPlace = item.isDifferent(cursorItem) ? amt : Math.min(64 - item.amount(), cursorItem.amount());

        final int containerNetId = item.netId() != null ? item.netId() : 0;
        BedrockItem finalContainerItem = item.copy();
        if (item.isDifferent(cursorItem)) {
            finalContainerItem = cursorItem.copy();
            finalContainerItem.setAmount(amountToPlace);
        } else {
            finalContainerItem.setAmount(item.amount() + amountToPlace);
        }
        container.setItem(bedrockSlot, finalContainerItem);

        final int cursorNetId = cursorItem.netId();
        BedrockItem finalCursorItem = this.itemAfterRemovingAmount(cursorItem, amountToPlace);
        clickContext.inventoryTracker.getHudContainer().setItem(0, finalCursorItem);

        return new ItemStackRequestAction.PlaceAction(
                amountToPlace,
                new ItemStackRequestSlotInfo(clickContext.inventoryTracker.getHudContainer().getFullContainerName(0), (byte) 0, cursorNetId),
                new ItemStackRequestSlotInfo(container.getFullContainerName(bedrockSlot), (byte) bedrockSlot, containerNetId)
        );
    }

    private ItemStackRequestAction handlePickupSwap(final ClickContext clickContext, final ExperimentalContainer container, final int bedrockSlot, final BedrockItem cursorItem, final BedrockItem item) {
        BedrockItem cursorCopy = cursorItem.copy();
        BedrockItem itemCopy = item.copy();

        container.setItem(bedrockSlot, cursorCopy);
        clickContext.inventoryTracker.getHudContainer().setItem(0, itemCopy);

        return new ItemStackRequestAction.SwapAction(
                new ItemStackRequestSlotInfo(clickContext.inventoryTracker.getHudContainer().getFullContainerName(0), (byte) 0, cursorItem.netId()),
                new ItemStackRequestSlotInfo(container.getFullContainerName(bedrockSlot), (byte) bedrockSlot, item.netId())
        );
    }

    private ItemStackRequestAction handleSwapClick(final ClickContext clickContext, final short javaSlot, final byte button) {
        if (button < 0 || button > 8) {
            // TODO: Handle offhand
            return null;
        }

        ExperimentalContainer container = clickContext.container;
        if (javaSlot < 0 || javaSlot >= container.getItems().length) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Tried to handle swap for " + container.type() + ", but slot was out of bounds (" + javaSlot + ")");
            return null;
        }

        ExperimentalContainer hotbarContainer = clickContext.inventoryTracker.getInventoryContainer();

        BedrockItem item = container.getItem(clickContext.bedrockSlot).copy();
        BedrockItem hotbarItem = hotbarContainer.getItem(button).copy();

        if (item.isEmpty() && hotbarItem.isEmpty()) {
            return null;
        }

        container.setItem(clickContext.bedrockSlot, hotbarItem);
        hotbarContainer.setItem(button, item);

        if (hotbarItem.isEmpty()) {
            return new ItemStackRequestAction.PlaceAction(
                    item.amount(),
                    new ItemStackRequestSlotInfo(container.getFullContainerName(clickContext.bedrockSlot), (byte) clickContext.bedrockSlot, item.netId()),
                    new ItemStackRequestSlotInfo(hotbarContainer.getFullContainerName(button), button, 0)
            );
        } else if (item.isEmpty()) {
            return new ItemStackRequestAction.PlaceAction(
                    hotbarItem.amount(),
                    new ItemStackRequestSlotInfo(hotbarContainer.getFullContainerName(button), button, hotbarItem.netId()),
                    new ItemStackRequestSlotInfo(container.getFullContainerName(clickContext.bedrockSlot), (byte) clickContext.bedrockSlot, 0)
            );
        }

        return new ItemStackRequestAction.SwapAction(
                new ItemStackRequestSlotInfo(hotbarContainer.getFullContainerName(button), button, hotbarItem.netId()),
                new ItemStackRequestSlotInfo(container.getFullContainerName(clickContext.bedrockSlot), (byte) clickContext.bedrockSlot, item.netId())
        );
    }

    private List<ItemStackRequestAction> handleQuickMoveClick(final ClickContext clickContext, final short javaSlot) {
        final SlotRef source = this.resolveJavaSlot(clickContext, javaSlot);
        if (source == null) {
            return List.of();
        }

        BedrockItem sourceItem = source.container().getItem(source.bedrockSlot());
        if (sourceItem.isEmpty()) {
            return List.of();
        }

        final ItemRewriter itemRewriter = this.user.get(ItemRewriter.class);

        final List<ItemStackRequestAction> actions = new ArrayList<>();
        final List<QuickMoveRange> ranges = this.quickMoveRanges(javaSlot, source);
        for (boolean mergePass : new boolean[]{true, false}) {
            for (QuickMoveRange range : ranges) {
                final int start = range.backwards() ? range.endJavaSlot() - 1 : range.startJavaSlot();
                final int end = range.backwards() ? range.startJavaSlot() - 1 : range.endJavaSlot();
                final int step = range.backwards() ? -1 : 1;
                for (int javaDestSlot = start; javaDestSlot != end && !sourceItem.isEmpty(); javaDestSlot += step) {
                    final int bedrockDestSlot = range.container().bedrockSlot(javaDestSlot);
                    if (source.container() == range.container() && source.bedrockSlot() == bedrockDestSlot) {
                        continue;
                    }
                    /*if (!range.container().canQuickMoveToSlot(bedrockDestSlot, sourceItem)) {
                        continue;
                    }*/

                    final BedrockItem destinationItem = range.container().getItem(bedrockDestSlot);
                    final int slotMaxStackSize = itemRewriter.maxStackSize(destinationItem);
                    if (mergePass) {
                        if (destinationItem == null || destinationItem.isEmpty() || destinationItem.isDifferent(sourceItem) || destinationItem.amount() >= slotMaxStackSize) {
                            continue;
                        }
                    } else if (destinationItem != null && !destinationItem.isEmpty()) {
                        continue;
                    }

                    final int amountToMove = mergePass
                            ? Math.min(sourceItem.amount(), slotMaxStackSize - destinationItem.amount())
                            : Math.min(sourceItem.amount(), slotMaxStackSize);
                    if (amountToMove <= 0) {
                        continue;
                    }

                    int destNetId = destinationItem != null && !destinationItem.isEmpty() ? destinationItem.netId() : 0;

                    clickContext.prevContainers.add(source.container());
                    clickContext.prevContainers.add(range.container());
                    actions.add(new ItemStackRequestAction.PlaceAction(
                            amountToMove,
                            new ItemStackRequestSlotInfo(source.container().getFullContainerName(source.bedrockSlot()), (byte) source.bedrockSlot(), sourceItem.netId()),
                            new ItemStackRequestSlotInfo(range.container().getFullContainerName(bedrockDestSlot), (byte) bedrockDestSlot, destNetId)
                    ));

                    final BedrockItem newSourceItem = this.itemAfterRemovingAmount(sourceItem, amountToMove);
                    source.container().setItem(source.bedrockSlot(), newSourceItem);
                    if (destinationItem == null || destinationItem.isEmpty()) {
                        final BedrockItem newDestinationItem = sourceItem.copy();
                        newDestinationItem.setAmount(amountToMove);
                        range.container().setItem(bedrockDestSlot, newDestinationItem);
                    } else {
                        final BedrockItem newDestinationItem = destinationItem.copy();
                        newDestinationItem.setAmount(destinationItem.amount() + amountToMove);
                        range.container().setItem(bedrockDestSlot, newDestinationItem);
                    }
                    sourceItem = newSourceItem;
                }
            }
        }

        return actions;
    }

    private ItemStackRequestAction handleThrowClick(final ClickContext clickContext, final short javaSlot, final byte button) {
        if (javaSlot == -999) {
            return this.dropCursorItem(clickContext.inventoryTracker, button);
        }

        ExperimentalContainer container = clickContext.container;
        if (javaSlot < 0 || javaSlot >= container.getItems().length) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Tried to handle throw for " + container.type() + ", but slot was out of bounds (" + javaSlot + ")");
            return null;
        }

        BedrockItem item = container.getItem(clickContext.bedrockSlot);

        if (item.isEmpty()) {
            return null;
        }

        int amountToDrop = button == 0 ? 1 : item.amount();

        BedrockItem finalContainerItem = this.itemAfterRemovingAmount(item, amountToDrop);
        container.setItem(clickContext.bedrockSlot, finalContainerItem);

        return new ItemStackRequestAction.DropAction(
                amountToDrop,
                new ItemStackRequestSlotInfo(container.getFullContainerName(clickContext.bedrockSlot), (byte) clickContext.bedrockSlot, item.netId()),
                false
        );
    }

    private ItemStackRequestAction dropCursorItem(final ExperimentalInventoryTracker inventoryTracker, final byte button) {
        BedrockItem cursorItem = inventoryTracker.getHudContainer().getItem(0);
        if (cursorItem.isEmpty()) {
            return null;
        }

        int amountToDrop = button == 0 ? cursorItem.amount() : 1;
        inventoryTracker.getHudContainer().setItem(0, this.itemAfterRemovingAmount(cursorItem, amountToDrop));

        return new ItemStackRequestAction.DropAction(
                amountToDrop,
                new ItemStackRequestSlotInfo(inventoryTracker.getHudContainer().getFullContainerName(0), (byte) 0, cursorItem.netId()),
                false
        );
    }

    protected ItemStackRequestAction dropItem(final ExperimentalContainer container, final int bedrockSlot, final int amountToDrop) {
        BedrockItem item = container.getItem(bedrockSlot);
        if (item.isEmpty()) {
            return null;
        }

        BedrockItem finalContainerItem = this.itemAfterRemovingAmount(item, amountToDrop);
        container.setItem(bedrockSlot, finalContainerItem);

        return new ItemStackRequestAction.DropAction(
                amountToDrop,
                new ItemStackRequestSlotInfo(container.getFullContainerName(bedrockSlot), (byte) bedrockSlot, item.netId()),
                false
        );
    }

    private List<ItemStackRequestAction> singletonAction(final ItemStackRequestAction action) {
        return action == null ? List.of() : List.of(action);
    }

    // TODO: Move this to per-class override?
    private List<QuickMoveRange> quickMoveRanges(final short javaSlot, final SlotRef source) {
        final ExperimentalInventoryTracker inventoryTracker = this.user.get(ExperimentalInventoryTracker.class);
        final InventoryContainer inventory = inventoryTracker.getInventoryContainer();
        if (this instanceof InventoryContainer) {
            final List<QuickMoveRange> ranges = new ArrayList<>();
            if (javaSlot >= 9 && javaSlot < 45) {
                final QuickMoveRange equipmentRange = this.equipmentQuickMoveRange(source, inventoryTracker);
                if (equipmentRange != null) {
                    ranges.add(equipmentRange);
                }
            }

            if (javaSlot >= 9 && javaSlot < 36) {
                ranges.add(new QuickMoveRange(inventory, 36, 45, false));
            } else if (javaSlot >= 36 && javaSlot < 45) {
                ranges.add(new QuickMoveRange(inventory, 9, 36, false));
            } else {
                ranges.add(new QuickMoveRange(inventory, 9, 45, false));
            }
            return ranges;
        }

        if (source.container() != inventory && source.container() != inventoryTracker.getArmorContainer() && source.container() != inventoryTracker.getOffhandContainer()) {
            return List.of(
                    new QuickMoveRange(inventory, 0, 9, true),
                    new QuickMoveRange(inventory, 9, inventory.size(), true)
            );
        }

        final BedrockItem sourceItem = source.container().getItem(source.bedrockSlot());
        return switch (this.type) {
            case FURNACE, BLAST_FURNACE, SMOKER -> /*this.isFurnaceFuel(sourceItem)
                    ? List.of(new QuickMoveRange(this, 1, 2, false), new QuickMoveRange(this, 0, 1, false))
                    :*/ List.of(new QuickMoveRange(this, 0, 1, false), new QuickMoveRange(this, 1, 2, false));
            case BREWING_STAND -> {
                /*if (this.isItem(sourceItem, "minecraft:blaze_powder")) {
                    yield List.of(new QuickMoveRange(this, 4, 5, false));
                }
                if (this.isBrewingBottle(sourceItem)) {
                    yield List.of(new QuickMoveRange(this, 0, 3, false));
                }*/
                yield List.of(new QuickMoveRange(this, 3, 4, false));
            }
            case BEACON -> List.of(new QuickMoveRange(this, 0, 1, false));
            case ANVIL -> List.of(new QuickMoveRange(this, 0, 2, false));
            case ENCHANTMENT -> List.of(new QuickMoveRange(this, 0, 2, false));
            case SMITHING_TABLE -> List.of(new QuickMoveRange(this, 0, 3, false));
            case STONECUTTER -> List.of(new QuickMoveRange(this, 0, 1, false));
            case LOOM -> List.of(new QuickMoveRange(this, 0, 3, false));
            case CARTOGRAPHY -> List.of(new QuickMoveRange(this, 0, 2, false));
            case WORKBENCH -> List.of(new QuickMoveRange(this, 1, 10, false));
            case GRINDSTONE -> List.of(new QuickMoveRange(this, 0, 2, false));
            case CRAFTER -> List.of(new QuickMoveRange(this, 0, 9, false));
            default -> List.of(new QuickMoveRange(this, 0, this.size(), false));
        };
    }

    private QuickMoveRange equipmentQuickMoveRange(final SlotRef source, final ExperimentalInventoryTracker inventoryTracker) {
        final BedrockItem item = source.container().getItem(source.bedrockSlot());
        final int javaSlot = this.equipmentJavaSlot(item);
        if (javaSlot >= 5 && javaSlot < 9) {
            final ExperimentalContainer armorContainer = inventoryTracker.getArmorContainer();
            final int bedrockSlot = armorContainer.bedrockSlot(javaSlot);
            return armorContainer.getItem(bedrockSlot).isEmpty() ? new QuickMoveRange(armorContainer, javaSlot, javaSlot + 1, false) : null;
        }
        if (javaSlot == 45) {
            final ExperimentalContainer offhandContainer = inventoryTracker.getOffhandContainer();
            return offhandContainer.getItem(0).isEmpty() ? new QuickMoveRange(offhandContainer, 45, 46, false) : null;
        }
        return null;
    }

    // TODO: Delete this
    private int equipmentJavaSlot(final BedrockItem item) {
        if (item.isEmpty()) {
            return -1;
        }

        final String identifier = this.user.get(ItemRewriter.class).getItems().inverse().get(item.identifier());
        if (identifier == null) {
            return -1;
        }

        final String name = identifier.startsWith("minecraft:") ? identifier.substring("minecraft:".length()) : identifier;
        if (name.endsWith("_helmet") || name.endsWith("_skull") || name.endsWith("_head") || name.equals("carved_pumpkin")) {
            return 5;
        }
        if (name.endsWith("_chestplate") || name.equals("elytra")) {
            return 6;
        }
        if (name.endsWith("_leggings")) {
            return 7;
        }
        if (name.endsWith("_boots")) {
            return 8;
        }
        if (name.equals("shield")) {
            return 45;
        }
        return -1;
    }

    private SlotRef resolveJavaSlot(final ClickContext clickContext, final short javaSlot) {
        if (javaSlot < 0) {
            return null;
        }

        ExperimentalContainer container = clickContext.container;
        int bedrockSlot = clickContext.bedrockSlot;
        final ExperimentalInventoryTracker inventoryTracker = clickContext.inventoryTracker;

        if (!(container instanceof InventoryContainer) && (javaSlot < 0 || javaSlot >= container.getItems().length)) {
            final ExperimentalContainer inventoryContainer = inventoryTracker.getInventoryContainer();
            final int invSlot = inventoryContainer.bedrockSlot(javaSlot - container.getItems().length + 9);
            if (invSlot < 0 || invSlot >= inventoryContainer.getItems().length) {
                return null;
            }
            return new SlotRef(inventoryContainer, invSlot);
        }

        if (container instanceof InventoryContainer) {
            if (javaSlot >= 0 && javaSlot < 5) {
                final ExperimentalContainer hudContainer = inventoryTracker.getHudContainer();
                return new SlotRef(hudContainer, hudContainer.bedrockSlot(javaSlot));
            } else if (javaSlot >= 5 && javaSlot < 9) {
                final ExperimentalContainer armorContainer = inventoryTracker.getArmorContainer();
                return new SlotRef(armorContainer, armorContainer.bedrockSlot(javaSlot));
            } else if (javaSlot == 45) {
                final ExperimentalContainer offhandContainer = inventoryTracker.getOffhandContainer();
                return new SlotRef(offhandContainer, offhandContainer.bedrockSlot(javaSlot));
            }
        }

        return new SlotRef(container, bedrockSlot);
    }

    private BedrockItem copyStackWithAmount(final BedrockItem item, final int amount) {
        BedrockItem copy = item.copy();
        copy.setAmount(amount);
        return copy;
    }

    protected BedrockItem itemAfterRemovingAmount(final BedrockItem item, final int amountToRemove) {
        if (amountToRemove >= item.amount()) {
            return BedrockItem.empty();
        }

        BedrockItem copy = item.copy();
        copy.setAmount(item.amount() - amountToRemove);
        return copy;
    }

    private static final class ClickContext {
        private ExperimentalContainer container;
        private int bedrockSlot;
        private final ExperimentalInventoryTracker inventoryTracker;
        private final InventoryRequestTracker inventoryRequestTracker;
        private final List<ExperimentalContainer> prevContainers = new ArrayList<>();
        private final ExperimentalContainer prevCursorContainer;

        private ClickContext(final ExperimentalContainer container, final int bedrockSlot, final ExperimentalInventoryTracker inventoryTracker, final InventoryRequestTracker inventoryRequestTracker) {
            this.container = container;
            this.bedrockSlot = bedrockSlot;
            this.inventoryTracker = inventoryTracker;
            this.inventoryRequestTracker = inventoryRequestTracker;
            this.prevCursorContainer = inventoryTracker.getHudContainer().copy();
        }
    }

    private record SlotRef(ExperimentalContainer container, int bedrockSlot) {
    }

    private record QuickMoveRange(ExperimentalContainer container, int startJavaSlot, int endJavaSlot, boolean backwards) {
    }

    public boolean handleButtonClick(final int button) {
        return false;
    }

    public void clearItems() {
        for (int i = 0; i < this.items.length; i++) {
            this.items[i] = BedrockItem.empty();
        }
    }

    public Item getJavaItem(final int slot) {
        return this.user.get(ItemRewriter.class).javaItem(this.getItem(slot));
    }

    public Item[] getJavaItems() {
        return this.user.get(ItemRewriter.class).javaItems(this.items);
    }

    public BedrockItem getItem(final int bedrockSlot) {
        return this.items[bedrockSlot];
    }

    public BedrockItem[] getItems() {
        return Arrays.copyOf(this.items, this.items.length);
    }

    public boolean setItem(final int bedrockSlot, final BedrockItem item) {
        if (bedrockSlot < 0 || bedrockSlot >= this.items.length) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Tried to set item for " + this.type + ", but slot was out of bounds (" + bedrockSlot + ")");
            return false;
        }

        final BedrockItem oldItem = this.items[bedrockSlot];
        this.items[bedrockSlot] = item;
        this.onSlotChanged(bedrockSlot, oldItem, item);
        return true;
    }

    public boolean setItems(final BedrockItem[] items) {
        if (items.length != this.items.length) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Tried to set items for " + this.type + ", but items array length was not correct (" + items.length + " != " + this.items.length + ")");
            return false;
        }

        System.arraycopy(items, 0, this.items, 0, items.length);
        return true;
    }

    public int javaSlot(final int bedrockSlot) {
        return bedrockSlot;
    }

    public int bedrockSlot(final int javaSlot) {
        return javaSlot;
    }

    public byte javaContainerId() {
        return this.containerId();
    }

    public int size() {
        return this.items.length;
    }

    public byte containerId() {
        return this.containerId;
    }

    public ContainerType type() {
        return this.type;
    }

    public TextComponent title() {
        return this.title;
    }

    public BlockPosition position() {
        return this.position;
    }

    public boolean isValidBlockTag(final String tag) {
        if (tag == null) {
            return false;
        } else {
            return this.validBlockTags.contains(tag);
        }
    }

    protected void onSlotChanged(final int javaSlot, final BedrockItem oldItem, final BedrockItem newItem) {
    }

    public ExperimentalContainer copy() { // TODO: This probably isnt the best way to do this
        BedrockItem[] itemsCopy = Arrays.copyOf(this.items, this.items.length);
        return new ExperimentalContainer(this.user, this.containerId, this.type, this.title, this.position, itemsCopy, this.validBlockTags) {
            @Override
            public FullContainerName getFullContainerName(int slot) {
                return ExperimentalContainer.this.getFullContainerName(slot);
            }
        };
    }

    public short translateContainerData(int containerData) {
        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "translateContainerData not implemented for container type: " + this.type);
        return -1;
    }
}
