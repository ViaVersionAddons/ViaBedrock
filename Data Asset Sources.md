# ViaBedrock Data Asset Sources

This file lists all the sources/code to obtain the data assets used in ViaBedrock.  
If a file is not listed here, it has been created manually and is updated manually.

The Java data generator and its setup steps are in [generator/README.md](generator/README.md).

### data/bedrock/biome_definitions.json

[CloudburstMC/Data](https://github.com/CloudburstMC/Data/blob/a8a4341d7763d6eb8547cff3ca46b4153d60163d/stripped_biome_definitions.json)

### data/bedrock/biomes.json

[axolotl-pm/BedrockData](https://github.com/axolotl-pm/BedrockData/blob/aeb5f24894f1873dde6f7b4f39490ad85ce59094/biome_id_map.json)

### data/bedrock/block_id_meta_to_1_12_0_nbt.bin

[PMMP/BedrockBlockUpgradeSchema](https://github.com/pmmp/BedrockBlockUpgradeSchema/blob/8b72c47109e174ac7f17c3ac546748f8e49a5fdf/id_meta_to_nbt/1.12.0.bin)

### data/bedrock/block_legacy_id_map.json

[PMMP/BedrockBlockUpgradeSchema](https://github.com/pmmp/BedrockBlockUpgradeSchema/blob/79bb3ad542ef19e828fdf1fa6adc54f1fa4b3bb5/block_legacy_id_map.json)

### data/bedrock/block_palette.nbt

[GeyserMC/Geyser](https://github.com/GeyserMC/Geyser/blob/fd35d5918ae8e8b6c05d91179315fa00413c1f65/core/src/main/resources/bedrock/block_palette.26_60.nbt)

### data/bedrock/block_sounds.json

This file has been generated using `./gradlew generateBedrockSoundLists` from the official bedrock client's assets folder.

### data/bedrock/block_traits.json

[microsoft.com](https://learn.microsoft.com/en-us/minecraft/creator/documents/intro-block-traits?view=minecraft-bedrock-stable)  
[Mojang/bedrock-samples](https://github.com/Mojang/bedrock-samples/blob/84aa19df535fcf6d044f28bf76f236029bd8407a/metadata/doc_modules/addons.json)  
[wiki.bedrock.dev](https://wiki.bedrock.dev/blocks/block-traits.html)

### data/bedrock/effects.json

[minecraft.wiki](https://minecraft.wiki/w/Effect)

### data/bedrock/entity_identifiers.nbt

[Kaooot/bedrock-network-data](https://github.com/Kaooot/bedrock-network-data/blob/6abdab1f549ffafdd627f4d4de34709a0e981345/release/1.26.40/entity_identifiers.nbt)

### data/bedrock/game_rules.json

This file has been dumped from a BDS server using [CloudburstMC/ProxyPass](https://github.com/CloudburstMC/ProxyPass).

### data/bedrock/item_tags.json

[Kaooot/bedrock-network-data](https://github.com/Kaooot/bedrock-network-data/blob/6abdab1f549ffafdd627f4d4de34709a0e981345/release/1.26.40/item_tags.json)

### data/bedrock/level_sound_event_mappings.json

This file has been generated using `./gradlew generateBedrockSoundLists` from the official bedrock client's assets folder.

### data/bedrock/particles.json

This file has been generated using `./gradlew generateBedrockParticleList` from the official bedrock client's assets folder.

### data/bedrock/runtime_item_states.json

[CloudburstMC/Data](https://github.com/CloudburstMC/Data/blob/293c848098cc636a2758937fb6610b8bed1f12c5/runtime_item_states.json)

### data/bedrock/sounds.json

This file has been generated using `./gradlew generateBedrockSoundLists` from the official bedrock client's assets folder.

### data/custom/block_tags.json and data/custom/potted_blockstates.json

Run `./gradlew syncDerivedMappings` after updating the Bedrock palette and Java registry. It adds sign and shelf tags and directly named potted blocks. Other entries in these files remain curated.

### data/java/effects.json

This file can be dumped with the Fabric mod in `generator`.

### data/java/entity_data_fields.json

This file can be dumped with the Fabric mod in `generator`.

### data/java/heightmap_blockstates.nbt

This file can be dumped with the Fabric mod in `generator`.

### data/java/registries.nbt

This file can be dumped from a vanilla server with the Fabric mod in `generator`.

### data/java/tags.nbt

This file can be dumped from a vanilla server with the Fabric mod in `generator`.

### data/java/via_mappings.json

[ViaVersion/Mappings](https://github.com/ViaVersion/Mappings/blob/e999640a3742e87080972cf13b15e329d5fada32/mappings/mapping-26.3.json)

### block_state_upgrade_schema/*

[opencollab-incubator/BedrockBlockUpgradeSchema](https://github.com/opencollab-incubator/BedrockBlockUpgradeSchema/tree/master/nbt_upgrade_schema)

### item_upgrade_schema/*

[opencollab-incubator/BedrockItemUpgradeSchema](https://github.com/opencollab-incubator/BedrockItemUpgradeSchema/tree/master)

### resource_packs/*

These files have been generated using `./gradlew generateBedrockPacks` from the official bedrock client's assets folder.

### skin_packs/*

These files have been generated using `./gradlew generateBedrockPacks` from the official bedrock client's assets folder.
