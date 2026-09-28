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
package net.raphimc.viabedrock.experimental.model.container.dynamic;

import com.viaversion.nbt.tag.IntTag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.util.Pair;
import net.raphimc.viabedrock.experimental.model.container.ExperimentalContainer;
import net.raphimc.viabedrock.experimental.storage.ExperimentalInventoryTracker;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerID;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ContainerType;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.rewriter.ItemRewriter;

public class BundleContainer extends ExperimentalContainer {

    private final FullContainerName containerName;

    public BundleContainer(final UserConnection user, final FullContainerName containerName) {
        super(user, (byte) ContainerID.CONTAINER_ID_REGISTRY.getValue(), ContainerType.NONE, null, null, 64);
        this.containerName = containerName;
    }

    @Override
    public FullContainerName getFullContainerName(int slot) {
        return this.containerName;
    }

    @Override
    public Item getJavaItem(final int slot) {
        final Pair<ExperimentalContainer, Integer> holdingContainer = this.findHoldingContainer();
        if (holdingContainer == null) {
            throw new IllegalStateException("Could not find bundle in any container");
        }

        return holdingContainer.key().getJavaItem(holdingContainer.value());
    }

    @Override
    public Item[] getJavaItems() {
        final Pair<ExperimentalContainer, Integer> holdingContainer = this.findHoldingContainer();
        if (holdingContainer == null) {
            throw new IllegalStateException("Could not find bundle in any container");
        }

        return holdingContainer.key().getJavaItems();
    }

    @Override
    public boolean setItem(final int slot, final BedrockItem item) {
        return super.setItem(slot, item) && this.findHoldingContainer() != null;
    }

    @Override
    public boolean setItems(BedrockItem[] items) {
        if (items.length != this.size()) {
            final BedrockItem[] newItems = this.getItems();
            System.arraycopy(items, 0, newItems, 0, Math.min(items.length, newItems.length));
            items = newItems;
        }
        return super.setItems(items) && this.findHoldingContainer() != null;
    }

    @Override
    public int javaSlot(final int slot) {
        final Pair<ExperimentalContainer, Integer> holdingContainer = this.findHoldingContainer();
        if (holdingContainer == null) {
            throw new IllegalStateException("Could not find bundle in any container");
        }

        return holdingContainer.key().javaSlot(holdingContainer.value());
    }

    @Override
    public int bedrockSlot(final int slot) {
        final Pair<ExperimentalContainer, Integer> holdingContainer = this.findHoldingContainer();
        if (holdingContainer == null) {
            throw new IllegalStateException("Could not find bundle in any container");
        }

        return holdingContainer.key().bedrockSlot(holdingContainer.value());
    }

    @Override
    public byte javaContainerId() {
        final Pair<ExperimentalContainer, Integer> holdingContainer = this.findHoldingContainer();
        if (holdingContainer == null) {
            throw new IllegalStateException("Could not find bundle in any container");
        }

        return holdingContainer.key().javaContainerId();
    }

    public Item[] getJavaBundleItems() {
        return super.getJavaItems();
    }

    private Pair<ExperimentalContainer, Integer> findHoldingContainer() {
        final ExperimentalInventoryTracker inventoryTracker = this.user.get(ExperimentalInventoryTracker.class);

        int slot = findBundleInContainer(inventoryTracker.getInventoryContainer());
        if (slot != -1) {
            return new Pair<>(inventoryTracker.getInventoryContainer(), slot);
        }

        slot = findBundleInContainer(inventoryTracker.getCurrentContainer());
        if (slot != -1) {
            return new Pair<>(inventoryTracker.getCurrentContainer(), slot);
        }

        slot = findBundleInContainer(inventoryTracker.getOffhandContainer());
        if (slot != -1) {
            return new Pair<>(inventoryTracker.getOffhandContainer(), slot);
        }

        slot = findBundleInContainer(inventoryTracker.getArmorContainer());
        if (slot != -1) {
            return new Pair<>(inventoryTracker.getArmorContainer(), slot);
        }

        slot = findBundleInContainer(inventoryTracker.getHudContainer());
        if (slot != -1) {
            return new Pair<>(inventoryTracker.getHudContainer(), slot);
        }

        return null;
    }

    private int findBundleInContainer(final ExperimentalContainer container) {
        if (container == null) return -1;

        final ItemRewriter itemRewriter = this.user.get(ItemRewriter.class);

        final BedrockItem[] items = container.getItems();
        for (int i = 0; i < items.length; i++) {
            final BedrockItem item = items[i];
            if (item.isEmpty() || item.tag() == null) continue;

            final String itemTag = BedrockProtocol.MAPPINGS.getBedrockCustomItemTags().getOrDefault(itemRewriter.getItems().inverse().get(item.identifier()), "");
            if (!itemTag.equals("bundle")) continue;

            final IntTag bundleIdTag = item.tag().getIntTag("bundle_id");
            if (bundleIdTag == null || bundleIdTag.asInt() == 0) continue;

            if (bundleIdTag.asInt() == this.containerName.dynamicId()) {
                return i;
            }
        }

        return -1;
    }

}
