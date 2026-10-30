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
package net.raphimc.viabedrock.protocol.packet;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundConfigurationPackets26_3;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.resourcepack.ResourcePack;
import net.raphimc.viabedrock.api.resourcepack.http.ConvertedResourcePackCache;
import net.raphimc.viabedrock.api.util.TextUtil;
import net.raphimc.viabedrock.protocol.BedrockProtocol;
import net.raphimc.viabedrock.protocol.ClientboundBedrockPackets;
import net.raphimc.viabedrock.protocol.ServerboundBedrockPackets;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PackType;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.ResourcePackResponse;
import net.raphimc.viabedrock.protocol.data.enums.java.generated.ResourcePackAction;
import net.raphimc.viabedrock.protocol.model.Experiment;
import net.raphimc.viabedrock.protocol.provider.ResourcePackProvider;
import net.raphimc.viabedrock.protocol.storage.ResourcePackDownloadTracker;
import net.raphimc.viabedrock.protocol.storage.ResourcePackLoadStateTracker;
import net.raphimc.viabedrock.protocol.storage.ResourcePackStorage;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ResourcePackPackets {

    private ResourcePackPackets() {
    }

    public static void register(final BedrockProtocol protocol) {
        protocol.registerClientbound(ClientboundBedrockPackets.RESOURCE_PACKS_INFO, null, wrapper -> {
            wrapper.cancel();
            if (wrapper.user().has(ResourcePackLoadStateTracker.class) || wrapper.user().has(ResourcePackStorage.class)) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received RESOURCE_PACKS_INFO after resource pack negotiation was already started/finished");
                return;
            }
            wrapper.read(Types.BOOLEAN); // resource pack required
            wrapper.read(Types.BOOLEAN); // has addon packs
            wrapper.read(Types.BOOLEAN); // has scripts
            wrapper.read(Types.BOOLEAN); // force disable vibrant visuals
            wrapper.read(BedrockTypes.UUID); // world template uuid
            wrapper.read(BedrockTypes.STRING); // world template version
            final ResourcePackLoadStateTracker.Info[] infos = new ResourcePackLoadStateTracker.Info[wrapper.read(BedrockTypes.UNSIGNED_VAR_INT)]; // resource packs size
            for (int i = 0; i < infos.length; i++) {
                final UUID id = wrapper.read(BedrockTypes.UUID); // pack id
                final String version = wrapper.read(BedrockTypes.STRING); // pack version
                wrapper.read(BedrockTypes.UNSIGNED_LONG_LE); // pack size
                final byte[] contentKey = wrapper.read(BedrockTypes.BYTE_ARRAY); // content key
                wrapper.read(BedrockTypes.STRING); // subpack names
                final String contentId = wrapper.read(BedrockTypes.STRING); // content identity
                wrapper.read(Types.BOOLEAN); // has scripts
                wrapper.read(Types.BOOLEAN); // is addon pack
                wrapper.read(Types.BOOLEAN); // is ray tracing capable
                URL cdnUrl = null;
                try {
                    final String cdnUrlString = wrapper.read(BedrockTypes.STRING); // cdn url
                    if (!cdnUrlString.isEmpty()) {
                        cdnUrl = new URL(cdnUrlString);
                    }
                } catch (final MalformedURLException ignored) {
                }
                infos[i] = new ResourcePackLoadStateTracker.Info(new ResourcePack.Key(id, version), contentKey, contentId, cdnUrl);
            }
            final UserConnection user = wrapper.user();
            final ResourcePackLoadStateTracker loadStateTracker = new ResourcePackLoadStateTracker(user, infos);
            user.put(loadStateTracker);

            if (shouldTranslate(user)) {
                loadStateTracker.loadRequestedResourcePacks().thenRun(() -> {
                    user.getChannel().eventLoop().execute(() -> {
                        if (!user.getChannel().isActive() || user.get(ResourcePackLoadStateTracker.class) != loadStateTracker || loadStateTracker.hasReceivedStack()) {
                            return;
                        }
                        final PacketWrapper response = PacketWrapper.create(ServerboundBedrockPackets.RESOURCE_PACK_CLIENT_RESPONSE, user);
                        ResourcePackClientResponse.write(response, ResourcePackResponse.DownloadingFinished);
                        response.sendToServer(BedrockProtocol.class);
                    });
                }).exceptionally(e -> {
                    user.getChannel().eventLoop().execute(() -> {
                        if (user.getChannel().isActive() && user.get(ResourcePackLoadStateTracker.class) == loadStateTracker) {
                            BedrockProtocol.kickForIllegalState(user, "One of the server resource packs failed to load. Try again later.", e);
                        }
                    });
                    return null;
                });
            } else {
                final PacketWrapper response = PacketWrapper.create(ServerboundBedrockPackets.RESOURCE_PACK_CLIENT_RESPONSE, wrapper.user());
                ResourcePackClientResponse.write(response, ResourcePackResponse.DownloadingFinished);
                response.sendToServer(BedrockProtocol.class);
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.RESOURCE_PACK_STACK, null, wrapper -> {
            wrapper.cancel();
            final UserConnection user = wrapper.user();
            final ResourcePackLoadStateTracker loadStateTracker = user.get(ResourcePackLoadStateTracker.class);
            if (loadStateTracker == null) {
                sendStackFinished(user);
                return;
            }

            wrapper.read(Types.BOOLEAN); // resource pack required
            final ResourcePack.Key[] keys = new ResourcePack.Key[wrapper.read(BedrockTypes.UNSIGNED_VAR_INT)]; // resource packs size
            for (int i = 0; i < keys.length; i++) {
                final UUID id = UUID.fromString(wrapper.read(BedrockTypes.STRING)); // id
                final String version = wrapper.read(BedrockTypes.STRING); // version
                wrapper.read(BedrockTypes.STRING); // subpack name
                keys[i] = new ResourcePack.Key(id, version);
            }
            wrapper.read(BedrockTypes.STRING); // base game version
            final Experiment[] experiments = wrapper.read(BedrockTypes.EXPERIMENT_ARRAY); // experiments
            wrapper.read(Types.BOOLEAN); // experiments previously toggled
            wrapper.read(Types.BOOLEAN); // include editor packs
            for (Experiment experiment : experiments) {
                if (experiment.enabled()) {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "This server uses an experimental resource pack: " + experiment.name());
                }
            }

            if (loadStateTracker.hasReceivedStack()) {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received duplicate RESOURCE_PACK_STACK");
                return;
            }
            loadStateTracker.markStackReceived();
            if (shouldTranslate(user)) {
                loadStateTracker.loadedFuture().whenCompleteAsync((ignored, error) -> {
                    if (!user.getChannel().isActive() || user.get(ResourcePackLoadStateTracker.class) != loadStateTracker) {
                        return;
                    }
                    if (error != null) {
                        BedrockProtocol.kickForIllegalState(user, "One of the server resource packs failed to load. Try again later.", error);
                    } else {
                        try {
                            finishStack(user, loadStateTracker, keys);
                        } catch (final Throwable e) {
                            BedrockProtocol.kickForIllegalState(user, "Failed to prepare the server resource packs.", e);
                        }
                    }
                }, user.getChannel().eventLoop());
            } else {
                finishStack(user, loadStateTracker, keys);
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.RESOURCE_PACK_DATA_INFO, null, wrapper -> {
            wrapper.cancel();
            final String key = wrapper.read(BedrockTypes.STRING); // resource name
            final long chunkSize = wrapper.read(BedrockTypes.UNSIGNED_INT_LE); // chunk size
            wrapper.read(BedrockTypes.UNSIGNED_INT_LE); // number of chunks (Ignored by the Bedrock client)
            final long size = wrapper.read(BedrockTypes.UNSIGNED_LONG_LE); // file size
            final byte[] hash = wrapper.read(BedrockTypes.BYTE_ARRAY); // file hash
            final boolean premium = wrapper.read(Types.BOOLEAN); // is premium pack
            final PackType type = PackType.getByValue(wrapper.read(Types.UNSIGNED_BYTE), PackType.Invalid); // pack type

            final ResourcePackDownloadTracker.Download download = wrapper.user().get(ResourcePackDownloadTracker.class).add(key, size, chunkSize, hash, premium, type);
            for (long chunk = 0; chunk < download.receivedChunks().length; chunk++) {
                final PacketWrapper resourcePackChunkRequest = wrapper.create(ServerboundBedrockPackets.RESOURCE_PACK_CHUNK_REQUEST);
                resourcePackChunkRequest.write(BedrockTypes.STRING, key); // resource name
                resourcePackChunkRequest.write(BedrockTypes.UNSIGNED_INT_LE, chunk); // chunk
                resourcePackChunkRequest.sendToServer(BedrockProtocol.class);
            }
        });
        protocol.registerClientbound(ClientboundBedrockPackets.RESOURCE_PACK_CHUNK_DATA, null, wrapper -> {
            wrapper.cancel();
            final String key = wrapper.read(BedrockTypes.STRING); // resource name
            final long chunk = wrapper.read(BedrockTypes.UNSIGNED_INT_LE); // chunk id
            wrapper.read(BedrockTypes.UNSIGNED_LONG_LE); // byte offset
            final byte[] data = wrapper.read(BedrockTypes.BYTE_ARRAY); // chunk data

            final ResourcePackDownloadTracker downloadTracker = wrapper.user().get(ResourcePackDownloadTracker.class);
            final ResourcePackDownloadTracker.Download download = downloadTracker.get(key);
            if (download != null && !download.isComplete()) {
                ViaBedrock.getPlatform().getLogger().log(Level.INFO, "Downloading resource pack chunk " + chunk + "/" + download.chunkSize() + " for " + key);
                if (download.acceptDataChunk(chunk, data)) {
                    finishDownload(wrapper.user(), downloadTracker, key, download);
                }
            } else {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Received RESOURCE_PACK_CHUNK_DATA for unknown pack: " + key);
            }

        });

        protocol.registerServerboundTransition(ServerboundConfigurationPackets1_21_9.RESOURCE_PACK, ServerboundBedrockPackets.RESOURCE_PACK_CLIENT_RESPONSE, wrapper -> {
            wrapper.read(Types.UUID); // id
            final ResourcePackAction action = ResourcePackAction.values()[wrapper.read(Types.VAR_INT)]; // action
            switch (action) {
                case SUCCESSFULLY_LOADED -> {
                    final ResourcePackStorage resourcePackStorage = wrapper.user().get(ResourcePackStorage.class);
                    if (resourcePackStorage != null) {
                        resourcePackStorage.setLoadedOnJavaClient();
                    }
                    ResourcePackClientResponse.write(wrapper, ResourcePackResponse.ResourcePackStackFinished);
                }
                case FAILED_DOWNLOAD, FAILED_RELOAD, DISCARDED -> {
                    ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Client resource pack download/load failed");
                    ResourcePackClientResponse.write(wrapper, ResourcePackResponse.ResourcePackStackFinished);
                }
                case DECLINED, INVALID_URL -> {
                    ResourcePackClientResponse.write(wrapper, ResourcePackResponse.ResourcePackStackFinished);
                }
                case ACCEPTED, DOWNLOADED -> wrapper.cancel();
                default -> throw new IllegalStateException("Unhandled ResourcePackAction: " + action);
            }
        });
    }

    private static void finishDownload(final UserConnection user, final ResourcePackDownloadTracker downloadTracker,
                                       final String key, final ResourcePackDownloadTracker.Download download) {
        final ResourcePackLoadStateTracker loadStateTracker = user.get(ResourcePackLoadStateTracker.class);
        final Function<Supplier<ResourcePack>, CompletableFuture<ResourcePack>> preparation = ViaBedrock.getResourcePackServer() != null
                ? ViaBedrock.getResourcePackServer()::prepareResources : loadStateTracker != null ? loadStateTracker::prepareStack : null;
        if (preparation == null) {
            throw new IllegalStateException("Received resource pack data without a load tracker");
        }
        ResourcePackDownloadCompletion.finish(preparation, user.getChannel().eventLoop(),
                () -> user.getChannel().isActive() && user.get(ResourcePackDownloadTracker.class) == downloadTracker
                        && user.get(ResourcePackLoadStateTracker.class) == loadStateTracker,
                downloadTracker, key, download, resourcePack -> {
                    if (download.type() == PackType.Resources && loadStateTracker != null) {
                        loadStateTracker.addRemoteResourcePack(resourcePack);
                    }
                }, error -> failDownload(user, loadStateTracker, error));
    }

    private static void failDownload(final UserConnection user, final ResourcePackLoadStateTracker loadStateTracker, final Throwable error) {
        if (loadStateTracker != null) {
            loadStateTracker.failLoading(error);
        }
        BedrockProtocol.kickForIllegalState(user, "One of the server resource packs failed to load. Try again later.", error);
    }

    private static void finishStack(final UserConnection user, final ResourcePackLoadStateTracker loadStateTracker, final ResourcePack.Key[] keys) {
        final Function<Supplier<ResourcePackStorage>, CompletableFuture<ResourcePackStorage>> preparation = ViaBedrock.getResourcePackServer() != null
                ? ViaBedrock.getResourcePackServer()::prepareStorage : loadStateTracker::prepareStack;
        ResourcePackPreparation.prepare(preparation, user.getChannel().eventLoop(),
                () -> user.getChannel().isActive() && user.get(ResourcePackLoadStateTracker.class) == loadStateTracker,
                () -> prepareStack(loadStateTracker, keys), (storage, error) -> {
                    if (error != null) {
                        BedrockProtocol.kickForIllegalState(user, "Failed to prepare the server resource packs.", error);
                        return;
                    }
                    user.remove(ResourcePackLoadStateTracker.class);
                    user.put(storage);
                    if (shouldTranslate(user)) {
                        ViaBedrock.getResourcePackServer().prepare(storage).whenCompleteAsync((pack, conversionError) -> {
                            if (!user.getChannel().isActive() || user.get(ResourcePackStorage.class) != storage) {
                                return;
                            }
                            if (conversionError != null) {
                                BedrockProtocol.kickForIllegalState(user, "Failed to convert the server resource packs.", conversionError);
                                return;
                            }
                            try {
                                final UUID httpToken = UUID.randomUUID();
                                ViaBedrock.getResourcePackServer().addConnection(httpToken, user, pack);
                                sendJavaResourcePack(user, pack, httpToken);
                            } catch (final Throwable publicationError) {
                                BedrockProtocol.kickForIllegalState(user, "Failed to advertise the server resource packs.", publicationError);
                            }
                        }, user.getChannel().eventLoop());
                    } else {
                        sendStackFinished(user);
                    }
                }, error -> BedrockProtocol.kickForIllegalState(user, "Failed to publish the server resource packs.", error));
    }

    private static ResourcePackStorage prepareStack(final ResourcePackLoadStateTracker loadStateTracker, final ResourcePack.Key[] keys) {
        loadStateTracker.saveRemoteResourcePacks();
        loadStateTracker.loadUnrequestedResourcePacks(keys);
        final List<ResourcePack> resourcePacks = new ArrayList<>();
        for (ResourcePack.Key key : keys) {
            final ResourcePack resourcePack = loadStateTracker.getResourcePack(key);
            if (resourcePack != null) {
                final ResourcePackLoadStateTracker.Info info = loadStateTracker.getRequest(key);
                final boolean decrypt = info != null && info.contentKey().length > 0 && resourcePack.isContentEncrypted();
                if (decrypt) {
                    resourcePack.decryptContent(info.contentKey(), info.contentId());
                    try {
                        Via.getManager().getProviders().get(ResourcePackProvider.class).save(resourcePack, info != null ? info.cacheIdentity() : null);
                    } catch (final Throwable e) {
                        ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Failed to save resource pack: " + resourcePack.key(), e);
                    }
                }
                resourcePacks.add(resourcePack);
            } else {
                ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Missing resource pack: " + key);
            }
        }
        return new ResourcePackStorage(resourcePacks);
    }

    private static void sendStackFinished(final UserConnection user) {
        final PacketWrapper response = PacketWrapper.create(ServerboundBedrockPackets.RESOURCE_PACK_CLIENT_RESPONSE, user);
        ResourcePackClientResponse.write(response, ResourcePackResponse.ResourcePackStackFinished);
        response.scheduleSendToServer(BedrockProtocol.class);
    }

    private static boolean shouldTranslate(final UserConnection user) {
        return ViaBedrock.getConfig().shouldTranslateResourcePacks() && user.getProtocolInfo().protocolVersion().newerThanOrEqualTo(ProtocolConstants.JAVA_VERSION);
    }

    private static void sendJavaResourcePack(final UserConnection user, final ConvertedResourcePackCache.Pack pack, final UUID httpToken) {
        final PacketWrapper resourcePack = PacketWrapper.create(ClientboundConfigurationPackets26_3.RESOURCE_PACK_PUSH, user);
        resourcePack.write(Types.UUID, pack.id()); // id
        resourcePack.write(Types.STRING, ViaBedrock.getResourcePackServer().getUrl() + "?token=" + httpToken); // url
        resourcePack.write(Types.STRING, pack.sha1()); // hash of the exact ZIP served by the HTTP server
        resourcePack.write(Types.BOOLEAN, false); // required
        resourcePack.write(Types.OPTIONAL_TAG, TextUtil.stringToNbt(
                "\n§aThis server uses Bedrock resource packs. Apply their Java Edition conversion? "
                    + "If you decline, some textures and models may be missing.")
        ); // prompt
        resourcePack.scheduleSend(BedrockProtocol.class);
    }

}
