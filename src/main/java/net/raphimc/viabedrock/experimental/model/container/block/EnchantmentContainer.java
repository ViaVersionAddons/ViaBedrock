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

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ShortTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.libs.mcstructs.text.TextComponent;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.data.Enchantments1_20_5;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.util.RegistryUtil;
import net.raphimc.viabedrock.experimental.ExperimentalPacketFactory;
import net.raphimc.viabedrock.experimental.model.container.ExperimentalContainer;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestAction;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestInfo;
import net.raphimc.viabedrock.experimental.model.inventory.ItemStackRequestSlotInfo;
import net.raphimc.viabedrock.experimental.model.recipe.EnchantData;
import net.raphimc.viabedrock.experimental.storage.ExperimentalInventoryTracker;
import net.raphimc.viabedrock.experimental.storage.InventoryRequestStorage;
import net.raphimc.viabedrock.experimental.storage.InventoryRequestTracker;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.*;
import net.raphimc.viabedrock.protocol.data.generated.bedrock.CustomBlockTags;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.FullContainerName;
import net.raphimc.viabedrock.protocol.storage.EntityTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class EnchantmentContainer extends ExperimentalContainer {

    List<EnchantData> data = new ArrayList<>();

    public EnchantmentContainer(UserConnection user, byte containerId, TextComponent title, BlockPosition position) {
        super(user, containerId, ContainerType.ENCHANTMENT, title, position, 2, CustomBlockTags.ENCHANTING_TABLE);
    }

    @Override
    public FullContainerName getFullContainerName(int slot) {
        return switch (slot) {
            case 14 -> new FullContainerName(ContainerEnumName.EnchantingInputContainer, null);
            case 15 -> new FullContainerName(ContainerEnumName.EnchantingMaterialContainer, null);
            default -> throw new IllegalArgumentException("Invalid slot for Enchantment Container: " + slot);
        };
    }

    @Override
    public int javaSlot(final int slot) {
        return switch (slot) {
            case 14 -> 0;
            case 15 -> 1;
            default -> super.javaSlot(slot);
        };
    }

    @Override
    public int bedrockSlot(final int slot) {
        return switch (slot) {
            case 0 -> 14;
            case 1 -> 15;
            default -> super.bedrockSlot(slot);
        };
    }

    @Override
    public BedrockItem getItem(int bedrockSlot) {
        // Fix magic offset
        bedrockSlot -= 14;
        return this.items[bedrockSlot];
    }

    @Override
    public boolean setItem(final int bedrockSlot, final BedrockItem item) {
        // Fix magic offset
        return super.setItem(bedrockSlot - 14, item);
    }

    @Override
    public boolean handleButtonClick(final int button) {
        if (button < 0 || button > 2 || button >= this.data.size()) {
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received invalid enchantment option button click: " + button);
            return false;
        }

        ExperimentalInventoryTracker inventoryTracker = user.get(ExperimentalInventoryTracker.class);
        InventoryRequestTracker inventoryRequestTracker = user.get(InventoryRequestTracker.class);
        EntityTracker entityTracker  = user.get(EntityTracker.class);

        final List<ExperimentalContainer> prevContainers = new ArrayList<>();
        final ExperimentalContainer prevCursorContainer = inventoryTracker.getHudContainer().copy();

        prevContainers.add(this.copy());

        int reqId = inventoryRequestTracker.nextRequestId();

        List<ItemStackRequestAction> actions = new ArrayList<>();

        ItemStackRequestAction craftAction = new ItemStackRequestAction.CraftRecipeAction(this.data.get(button).netId(), 1);
        ItemStackRequestAction consumeAction = new ItemStackRequestAction.ConsumeAction(1, new ItemStackRequestSlotInfo(
                this.getFullContainerName(14), (byte) 14, this.getItem(14).netId()
        ));
        ItemStackRequestAction placeAction = new ItemStackRequestAction.PlaceAction(1,
                new ItemStackRequestSlotInfo(
                        new FullContainerName(ContainerEnumName.CreatedOutputContainer, null), (byte) 50, reqId
                ),
                new ItemStackRequestSlotInfo(
                        this.getFullContainerName(14), (byte) 14, reqId
                )
        );
        actions.add(craftAction);
        actions.add(consumeAction);
        actions.add(placeAction);

        if (entityTracker.getClientPlayer().gameType() == GameType.Survival || entityTracker.getClientPlayer().gameType() == GameType.Adventure) {
            ItemStackRequestAction consumeAction2 = new ItemStackRequestAction.ConsumeAction(button + 1, new ItemStackRequestSlotInfo(
                    this.getFullContainerName(15), (byte) 15, this.getItem(15).netId()
            ));
            actions.add(consumeAction2);

            this.setItem(15, this.itemAfterRemovingAmount(this.getItem(15), button + 1));
        }

        /* FIXME
        BedrockItem item = this.getItem(14).copy();
        CompoundTag tag = item.tag() != null ? item.tag().copy() : new CompoundTag();
        CompoundTag enchant = new CompoundTag();
        enchant.put("id", new ShortTag((short) this.data.get(button).type().getValue()));
        enchant.put("lvl", new ShortTag((short) this.data.get(button).level()));
        tag.put("ench", enchant);
        item.setTag(tag);
        this.setItem(14, item);
        */

        ItemStackRequestInfo request = new ItemStackRequestInfo(
                reqId,
                actions,
                List.of(),
                TextProcessingEventOrigin.Unknown
        );

        inventoryRequestTracker.addRequest(new InventoryRequestStorage(request, 0, prevCursorContainer, prevContainers)); // Store the request to track it later
        ExperimentalPacketFactory.sendBedrockInventoryRequest(user, new ItemStackRequestInfo[] {request});

        return true;
    }

    public void setEnchantData(List<EnchantData> data) {
        this.data = data;

        // Send to java client
        for (int i = 0; i < Math.min(this.data.size(), 3); i++) {
            EnchantData d = this.data.get(i);
            ExperimentalPacketFactory.sendJavaContainerProperties(this.user, this, (short) i, (short) d.cost());

            String javaEnchant = BedrockProtocol.MAPPINGS.getBedrockToJavaEnchantments().get(d.type());
            // Update the java item with the enchantment
            if (javaEnchant != null) {
                CompoundTag enchantmentsRegistry = (CompoundTag) BedrockProtocol.MAPPINGS.getJavaRegistries().get("minecraft:enchantment");
                CompoundTag enchantmentEntry = (CompoundTag) enchantmentsRegistry.get(javaEnchant);
                if (enchantmentEntry == null) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Enchantment entry is null for enchantment " + javaEnchant);
                } else {
                    int javaId = RegistryUtil.getRegistryIndex(enchantmentsRegistry, enchantmentEntry);
                    ExperimentalPacketFactory.sendJavaContainerProperties(this.user, this, (short) (i + 4), (short) javaId);
                }
            } else {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown enchantment with id " + d.type() + " and level " + d.level());
            }

            ExperimentalPacketFactory.sendJavaContainerProperties(this.user, this, (short) (i + 7), (short) d.level());
        }

    }

}

//[13:32:53:879] [CLIENT BOUND] - PlayerEnchantOptionsPacket(options=[EnchantOptionData(cost=2, primarySlot=16, enchants0=[], enchants1=[EnchantData(type=9, level=1)], enchants2=[], enchantName=dry physical enchant , enchantNetId=3407), EnchantOptionData(cost=5, primarySlot=16, enchants0=[], enchants1=[EnchantData(type=10, level=1)], enchants2=[], enchantName=destroy beast enchant spirit , enchantNetId=3408), EnchantOptionData(cost=7, primarySlot=16, enchants0=[], enchants1=[], enchants2=[EnchantData(type=12, level=1)], enchantName=enchant towards snuff cube , enchantNetId=3409)])

//[13:33:59:981] [SERVER BOUND] - ItemStackRequestPacket(requests=[ItemStackRequest(requestId=-109, actions=[CraftRecipeAction(recipeNetworkId=3409, numberOfRequestedCrafts=1), CraftResultsDeprecatedAction(resultItems=[BaseItemData(definition=SimpleItemDefinition(identifier=minecraft:diamond_sword, runtimeId=347, version=LEGACY, componentBased=false, componentData=null), damage=0, count=1, tag={
//        "Damage": 0i,
//        "ench": [
//        {
//        "id": 12s,
//        "lvl": 1s
//    }
//            ]
//            }, canPlace=[], canBreak=[], blockingTicks=0, blockDefinition=UnknownDefinition[runtimeId=0], usingNetId=false, netId=0)], timesCrafted=1), ConsumeAction(count=1, source=ItemStackRequestSlotData(container=ENCHANTING_INPUT, slot=14, stackNetworkId=37, containerName=FullContainerName(container=ENCHANTING_INPUT, dynamicId=null))), PlaceAction(count=1, source=ItemStackRequestSlotData(container=CREATED_OUTPUT, slot=50, stackNetworkId=-109, containerName=FullContainerName(container=CREATED_OUTPUT, dynamicId=null)), destination=ItemStackRequestSlotData(container=ENCHANTING_INPUT, slot=14, stackNetworkId=-109, containerName=FullContainerName(container=ENCHANTING_INPUT, dynamicId=null)))], filterStrings=[], textProcessingEventOrigin=null)])
