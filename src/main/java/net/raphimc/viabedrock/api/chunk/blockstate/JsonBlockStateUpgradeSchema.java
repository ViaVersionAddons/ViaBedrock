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
package net.raphimc.viabedrock.api.chunk.blockstate;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.util.Pair;
import net.raphimc.viabedrock.api.util.NbtUtil;

import java.util.*;
import java.util.function.Function;

public class JsonBlockStateUpgradeSchema extends BlockStateUpgradeSchema {

    private static final String NEW_NAME_KEY = "viabedrock:newname_" + UUID.randomUUID();

    public JsonBlockStateUpgradeSchema(final JsonObject jsonObject) {
        super(jsonObject.get("maxVersionMajor").getAsInt(), jsonObject.get("maxVersionMinor").getAsInt(), jsonObject.get("maxVersionPatch").getAsInt(), jsonObject.get("maxVersionRevision").getAsInt());

        final Map<String, List<Pair<?, ?>>> remappedPropertyValuesLookup = new HashMap<>();
        if (jsonObject.has("remappedPropertyValuesIndex")) {
            final JsonObject remappedPropertyValuesIndex = jsonObject.get("remappedPropertyValuesIndex").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : remappedPropertyValuesIndex.entrySet()) {
                final List<Pair<?, ?>> mappings = new ArrayList<>();
                for (JsonElement element : entry.getValue().getAsJsonArray()) {
                    final JsonObject mapping = element.getAsJsonObject();
                    mappings.add(new Pair<>(this.getValue(mapping.get("old").getAsJsonObject()), this.getValue(mapping.get("new").getAsJsonObject())));
                }
                remappedPropertyValuesLookup.put(entry.getKey(), mappings);
            }
        }
        if (jsonObject.has("remappedStates")) {
            final JsonObject remappedStates = jsonObject.get("remappedStates").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : remappedStates.entrySet()) {
                final String identifier = entry.getKey().toLowerCase(Locale.ROOT);
                final List<RemappedStatesEntry> mappings = new ArrayList<>();
                for (JsonElement mappingEntry : entry.getValue().getAsJsonArray()) {
                    final JsonObject mappingObject = mappingEntry.getAsJsonObject();

                    final CompoundTag oldStateTag = new CompoundTag();
                    if (mappingObject.get("oldState").isJsonObject()) {
                        for (Map.Entry<String, JsonElement> property : mappingObject.get("oldState").getAsJsonObject().entrySet()) {
                            oldStateTag.put(property.getKey(), NbtUtil.createTag(this.getValue(property.getValue().getAsJsonObject())));
                        }
                    }

                    final CompoundTag newStateTag = new CompoundTag();
                    if (mappingObject.get("newState").isJsonObject()) {
                        for (Map.Entry<String, JsonElement> property : mappingObject.get("newState").getAsJsonObject().entrySet()) {
                            newStateTag.put(property.getKey(), NbtUtil.createTag(this.getValue(property.getValue().getAsJsonObject())));
                        }
                    }

                    final List<String> copiedStates = new ArrayList<>();
                    if (mappingObject.has("copiedState")) {
                        for (JsonElement property : mappingObject.get("copiedState").getAsJsonArray()) {
                            copiedStates.add(property.getAsString());
                        }
                    }

                    if (mappingObject.has("newName")) {
                        final String newName = mappingObject.get("newName").getAsString();
                        mappings.add(new RemappedStatesEntry(oldStateTag, newStateTag, copiedStates, states -> newName.toLowerCase(Locale.ROOT)));
                    } else if (mappingObject.has("newFlattenedName")) {
                        final JsonObject newFlattenedName = mappingObject.get("newFlattenedName").getAsJsonObject();
                        final String prefix = newFlattenedName.get("prefix").getAsString();
                        final String flattenedProperty = newFlattenedName.get("flattenedProperty").getAsString();
                        final String suffix = newFlattenedName.get("suffix").getAsString();
                        final JsonObject flattenedValueRemapsObject = newFlattenedName.getAsJsonObject("flattenedValueRemaps");
                        final Map<String, String> flattenedValueRemaps = new HashMap<>();
                        if (flattenedValueRemapsObject != null) {
                            for (Map.Entry<String, JsonElement> remap : flattenedValueRemapsObject.entrySet()) {
                                flattenedValueRemaps.put(remap.getKey(), remap.getValue().getAsString());
                            }
                        }

                        mappings.add(new RemappedStatesEntry(oldStateTag, newStateTag, copiedStates, states -> {
                            if (!states.contains(flattenedProperty)) {
                                return null;
                            }

                            final String flattenedValue = states.get(flattenedProperty).getValue().toString();
                            final String flattenedName = prefix + flattenedValueRemaps.getOrDefault(flattenedValue, flattenedValue) + suffix;
                            return flattenedName.toLowerCase(Locale.ROOT);
                        }));
                    } else {
                        throw new IllegalArgumentException("No new name or flattened name specified for " + identifier);
                    }
                }

                this.actions.add(tag -> {
                    final String name = tag.getStringTag("name").getValue();
                    if (!name.equals(identifier)) {
                        return;
                    }

                    if (tag.get("states") instanceof CompoundTag states) {
                        for (RemappedStatesEntry mapping : mappings) {
                            boolean matches = true;
                            if (mapping.oldStateTag != null) {
                                for (Map.Entry<String, Tag> stateTag : mapping.oldStateTag.entrySet()) {
                                    if (!stateTag.getValue().equals(states.get(stateTag.getKey()))) {
                                        matches = false;
                                        break;
                                    }
                                }
                            }

                            if (matches) {
                                final CompoundTag newStates = mapping.newStateTag.copy();
                                for (String property : mapping.copiedStates) {
                                    if (states.contains(property)) {
                                        newStates.put(property, states.get(property));
                                    }
                                }

                                final String newName = mapping.nameFunction.apply(states);
                                if (newName != null) {
                                    tag.putString("name", newName);
                                }
                                tag.put("states", newStates);

                                throw StopUpgrade.INSTANCE;
                            }
                        }
                    }
                });
            }
        }
        if (jsonObject.has("renamedIds")) {
            final JsonObject renamedIds = jsonObject.get("renamedIds").getAsJsonObject();
            final Map<String, String> mappings = new HashMap<>();
            for (Map.Entry<String, JsonElement> mappingEntry : renamedIds.entrySet()) {
                mappings.put(mappingEntry.getKey().toLowerCase(Locale.ROOT), mappingEntry.getValue().getAsString().toLowerCase(Locale.ROOT));
            }

            this.actions.add(tag -> {
                final String name = tag.getStringTag("name").getValue();
                if (!mappings.containsKey(name)) {
                    return;
                }

                tag.putString(NEW_NAME_KEY, mappings.get(name));
            });
        }
        if (jsonObject.has("flattenedProperties")) {
            final JsonObject flattenedProperties = jsonObject.get("flattenedProperties").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : flattenedProperties.entrySet()) {
                final String identifier = entry.getKey().toLowerCase(Locale.ROOT);
                final JsonObject mappingObject = entry.getValue().getAsJsonObject();
                final String prefix = mappingObject.get("prefix").getAsString();
                final String flattenedProperty = mappingObject.get("flattenedProperty").getAsString();
                final String suffix = mappingObject.get("suffix").getAsString();
                final JsonObject flattenedValueRemapsObject = mappingObject.getAsJsonObject("flattenedValueRemaps");
                final Map<String, String> flattenedValueRemaps = new HashMap<>();
                if (flattenedValueRemapsObject != null) {
                    for (Map.Entry<String, JsonElement> remap : flattenedValueRemapsObject.entrySet()) {
                        flattenedValueRemaps.put(remap.getKey(), remap.getValue().getAsString());
                    }
                }

                this.actions.add(tag -> {
                    final String name = tag.getStringTag("name").getValue();
                    if (!name.equals(identifier)) {
                        return;
                    }

                    if (tag.get("states") instanceof CompoundTag states) {
                        if (!states.contains(flattenedProperty)) {
                            return;
                        }

                        final String flattenedValue = states.get(flattenedProperty).getValue().toString();
                        final String flattenedName = prefix + flattenedValueRemaps.getOrDefault(flattenedValue, flattenedValue) + suffix;
                        tag.putString(NEW_NAME_KEY, flattenedName.toLowerCase(Locale.ROOT));
                        states.remove(flattenedProperty);
                    }
                });
            }
        }
        if (jsonObject.has("addedProperties")) {
            final JsonObject addedProperties = jsonObject.get("addedProperties").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : addedProperties.entrySet()) {
                final String identifier = entry.getKey().toLowerCase(Locale.ROOT);
                final List<Pair<String, ?>> toAdd = new ArrayList<>();
                for (Map.Entry<String, JsonElement> toAddEntry : entry.getValue().getAsJsonObject().entrySet()) {
                    toAdd.add(new Pair<>(toAddEntry.getKey(), this.getValue(toAddEntry.getValue().getAsJsonObject())));
                }

                this.actions.add(tag -> {
                    final String name = tag.getStringTag("name").getValue();
                    if (!name.equals(identifier)) {
                        return;
                    }

                    if (tag.get("states") instanceof CompoundTag states) {
                        for (Pair<String, ?> property : toAdd) {
                            if (!states.contains(property.key())) {
                                states.put(property.key(), NbtUtil.createTag(property.value()));
                            }
                        }
                    }
                });
            }
        }
        if (jsonObject.has("removedProperties")) {
            final JsonObject removedProperties = jsonObject.get("removedProperties").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : removedProperties.entrySet()) {
                final String identifier = entry.getKey().toLowerCase(Locale.ROOT);
                final List<String> toRemove = new ArrayList<>();
                for (JsonElement toRemoveEntry : entry.getValue().getAsJsonArray()) {
                    toRemove.add(toRemoveEntry.getAsString());
                }

                this.actions.add(tag -> {
                    final String name = tag.getStringTag("name").getValue();
                    if (!name.equals(identifier)) {
                        return;
                    }

                    if (tag.get("states") instanceof CompoundTag states) {
                        for (String property : toRemove) {
                            states.remove(property);
                        }
                    }
                });
            }
        }
        if (jsonObject.has("remappedPropertyValues")) {
            final JsonObject remappedPropertyValues = jsonObject.get("remappedPropertyValues").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : remappedPropertyValues.entrySet()) {
                final String identifier = entry.getKey().toLowerCase(Locale.ROOT);
                final Map<String, List<Pair<?, ?>>> mappings = new HashMap<>();
                for (Map.Entry<String, JsonElement> mappingEntry : entry.getValue().getAsJsonObject().entrySet()) {
                    mappings.put(mappingEntry.getKey(), remappedPropertyValuesLookup.get(mappingEntry.getValue().getAsString()));
                }

                this.actions.add(tag -> {
                    final String name = tag.getStringTag("name").getValue();
                    if (!name.equals(identifier)) {
                        return;
                    }

                    if (tag.get("states") instanceof CompoundTag states) {
                        for (Map.Entry<String, List<Pair<?, ?>>> mapping : mappings.entrySet()) {
                            final Tag property = states.get(mapping.getKey());
                            if (property == null) {
                                continue;
                            }

                            final Object value = property.getValue();
                            for (Pair<?, ?> valueMapping : mapping.getValue()) {
                                if (valueMapping.key().equals(value)) {
                                    states.put(mapping.getKey(), NbtUtil.createTag(valueMapping.value()));
                                }
                            }
                        }
                    }
                });
            }
        }
        if (jsonObject.has("renamedProperties")) {
            final JsonObject renamedProperties = jsonObject.get("renamedProperties").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : renamedProperties.entrySet()) {
                final String identifier = entry.getKey().toLowerCase(Locale.ROOT);
                final Map<String, String> mappings = new HashMap<>();
                for (Map.Entry<String, JsonElement> mappingEntry : entry.getValue().getAsJsonObject().entrySet()) {
                    mappings.put(mappingEntry.getKey(), mappingEntry.getValue().getAsString());
                }

                this.actions.add(tag -> {
                    final String name = tag.getStringTag("name").getValue();
                    if (!name.equals(identifier)) {
                        return;
                    }

                    if (tag.get("states") instanceof CompoundTag states) {
                        for (Map.Entry<String, String> mapping : mappings.entrySet()) {
                            final Tag property = states.remove(mapping.getKey());
                            if (property != null) {
                                states.put(mapping.getValue(), property);
                            }
                        }
                    }
                });
            }
        }

        // Apply the new name last, because the remappers are expecting the old name
        this.actions.add(tag -> {
            final String newName = tag.getString(NEW_NAME_KEY, null);
            if (newName != null) {
                tag.putString("name", newName);
                tag.remove(NEW_NAME_KEY);
            }
        });
    }

    private Object getValue(final JsonObject obj) {
        if (obj.has("int")) {
            return obj.get("int").getAsInt();
        } else if (obj.has("byte")) {
            return obj.get("byte").getAsByte();
        } else if (obj.has("string")) {
            return obj.get("string").getAsString();
        } else {
            throw new IllegalArgumentException("Unknown json value type");
        }
    }

    private record RemappedStatesEntry(CompoundTag oldStateTag, CompoundTag newStateTag, List<String> copiedStates, Function<CompoundTag, String> nameFunction) {
    }

}
