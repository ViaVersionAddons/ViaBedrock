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
package net.raphimc.viabedrock.experimental.rewriter;

import com.viaversion.nbt.tag.*;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.Enchantments;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.util.RegistryUtil;
import net.raphimc.viabedrock.experimental.model.map.MapObject;
import net.raphimc.viabedrock.experimental.storage.MapTracker;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.Enchant_Type;
import net.raphimc.viabedrock.protocol.model.BedrockItem;

import java.util.logging.Level;

public class ExperimentalItemRewriter {

    // BedrockTag can be null
    public static void handleItem(final UserConnection user, final BedrockItem bedrockItem, final CompoundTag bedrockTag, final Item javaItem) {

        if (bedrockTag != null) {

            if (bedrockTag.get("Damage") instanceof NumberTag durability)  {
                javaItem.dataContainer().set(StructuredDataKey.DAMAGE, durability.asInt());
            }

            if (bedrockTag.get("map_uuid") instanceof NumberTag uuidTag) {
                MapTracker mapTracker = user.get(MapTracker.class);
                final long uuid = uuidTag.asLong();

                MapObject map = mapTracker.getMapObjects().get(uuid);
                if (map == null) {
                    final int mapId = mapTracker.getNextMapId();
                    map = new MapObject(uuid, mapId);
                    mapTracker.getMapObjects().put(uuid, map);
                    //ViaBedrock.getPlatform().getLogger().log(Level.INFO, "Registered new map with id " + mapId + " and uuid " + uuid);
                }

                javaItem.dataContainer().set(StructuredDataKey.MAP_ID, map.getJavaId());
            }

            if (bedrockTag.get("display") instanceof CompoundTag displayTag) {
                if (displayTag.get("Lore") instanceof ListTag<?> loreTag) {
                    // TODO: Bedrock lore might be able to contain translatable components, but for now we just ignore that
                    Tag[] tags = loreTag.getValue().toArray(new Tag[0]);
                    javaItem.dataContainer().set(StructuredDataKey.LORE, tags);
                }
            }

            if (bedrockTag.get("ench") instanceof ListTag<?> enchantments) {

                StructuredData<Enchantments> enchantmentsData = javaItem.dataContainer().getData(StructuredDataKey.ENCHANTMENTS1_21_5);
                Enchantments javaEnchantments;
                if (enchantmentsData == null || enchantmentsData.isEmpty()) {
                    javaEnchantments = new Enchantments(true);
                } else {
                    javaEnchantments = enchantmentsData.value();
                }

                //TODO: Empty list gives an enchantment glint, but no entries in lore
                for (Tag enchantment : enchantments) {
                    if (enchantment instanceof CompoundTag compoundTag) {
                        //id and lvl must be a short. Else bedrock defaults to protection (id 0) and lvl 0 (TODO: implement the fallback)
                        if (compoundTag.get("id") instanceof ShortTag idTag && compoundTag.get("lvl") instanceof ShortTag levelTag) {
                            Enchant_Type bedrockId = Enchant_Type.getByValue(idTag.asInt());
                            int level = levelTag.asInt();

                            if (bedrockId == null) {
                                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown enchantment with id " + idTag.asInt() + " and level " + level);
                                continue;
                            }

                            String javaEnchantmentId = BedrockProtocol.MAPPINGS.getBedrockToJavaEnchantments().get(bedrockId);

                            //Update the java item with the enchantment
                            if (javaEnchantmentId != null) {
                                CompoundTag enchantmentsRegistry = (CompoundTag) BedrockProtocol.MAPPINGS.getJavaRegistries().get("minecraft:enchantment");
                                CompoundTag enchantmentEntry = (CompoundTag) enchantmentsRegistry.get(javaEnchantmentId);
                                if (enchantmentEntry == null) {
                                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Enchantment entry is null for enchantment " + javaEnchantmentId);
                                } else {
                                    int javaId = RegistryUtil.getRegistryIndex(enchantmentsRegistry, enchantmentEntry);
                                    javaEnchantments.add(javaId, level);
                                }
                            } else {
                                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Unknown enchantment with id " + bedrockId + " and level " + level);
                            }
                        }
                    }
                }

                javaItem.dataContainer().set(StructuredDataKey.ENCHANTMENTS1_21_5, javaEnchantments);
            }

        }
    }
}
