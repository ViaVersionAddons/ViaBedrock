/*
 * This file is part of ViaBedrock - https://github.com/RaphiMC/ViaBedrock
 * Copyright (C) 2026 RK_01/RaphiMC and contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package net.raphimc.viabedrock.api.resourcepack.http;

import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explicit cache schema for converted model scales, readiness, and unresolved assets. */
final class ConverterDataManifest {

    static final String PATH = "viabedrock.converter.json";

    private ConverterDataManifest() {
    }

    static JsonObject encode(final Map<String, Object> data) {
        final JsonObject manifest = new JsonObject();
        manifest.addProperty("version", 1);
        final JsonObject scales = new JsonObject();
        final JsonObject attachables = new JsonObject();
        final JsonObject missing = new JsonObject();
        data.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            if (entry.getKey().startsWith("ce_") && entry.getKey().endsWith("_scale") && entry.getValue() instanceof Number scale && Float.isFinite(scale.floatValue())) {
                scales.addProperty(entry.getKey(), scale.doubleValue());
            } else if (entry.getKey().startsWith("ca_") && entry.getValue() instanceof Boolean ready) {
                attachables.addProperty(entry.getKey(), ready);
            } else if (entry.getKey().startsWith("model_missing_") && entry.getValue() instanceof String reason) {
                missing.addProperty(entry.getKey(), reason);
            } else {
                throw new IllegalArgumentException("Unsupported converter metadata " + entry.getKey());
            }
        });
        manifest.add("entity_scales", scales);
        manifest.add("attachable_models", attachables);
        manifest.add("unresolved_models", missing);
        return manifest;
    }

    static Map<String, Object> decode(final String source) {
        final JsonObject manifest = JsonParser.parseString(source).getAsJsonObject();
        if (!manifest.has("version") || manifest.get("version").getAsInt() != 1) {
            throw new IllegalArgumentException("Unsupported converter metadata version");
        }
        final Map<String, Object> data = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : manifest.getAsJsonObject("entity_scales").entrySet()) {
            final float scale = entry.getValue().getAsFloat();
            if (!entry.getKey().startsWith("ce_") || !entry.getKey().endsWith("_scale") || !Float.isFinite(scale)) {
                throw new IllegalArgumentException("Invalid converted entity scale");
            }
            data.put(entry.getKey(), scale);
        }
        for (Map.Entry<String, JsonElement> entry : manifest.getAsJsonObject("attachable_models").entrySet()) {
            if (!entry.getKey().startsWith("ca_") || !entry.getValue().getAsJsonPrimitive().isBoolean()) {
                throw new IllegalArgumentException("Invalid converted attachable readiness");
            }
            data.put(entry.getKey(), entry.getValue().getAsBoolean());
        }
        for (Map.Entry<String, JsonElement> entry : manifest.getAsJsonObject("unresolved_models").entrySet()) {
            if (!entry.getKey().startsWith("model_missing_") || !entry.getValue().getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Invalid unresolved model asset");
            }
            data.put(entry.getKey(), entry.getValue().getAsString());
        }
        return Map.copyOf(data);
    }

}
