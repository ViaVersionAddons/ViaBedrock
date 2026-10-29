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
package net.raphimc.viabedrock.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import net.raphimc.viabedrock.ViaBedrock;
import net.raphimc.viabedrock.api.io.compression.CompressionAlgorithm;
import net.raphimc.viabedrock.api.io.compression.NoopCompression;
import net.raphimc.viabedrock.api.io.compression.SnappyCompression;
import net.raphimc.viabedrock.api.io.compression.ZlibCompression;
import net.raphimc.viabedrock.protocol.data.enums.bedrock.generated.PacketCompressionAlgorithm;

import java.util.List;
import java.util.logging.Level;

public class CompressionCodec extends ByteToMessageCodec<ByteBuf> {

    private final CompressionAlgorithm preferredCompressionAlgorithm;
    private final int threshold;
    private ZlibCompression zLibCompression;
    private SnappyCompression snappyCompression;

    public CompressionCodec(final PacketCompressionAlgorithm preferredCompressionAlgorithm, final int threshold) {
        this.preferredCompressionAlgorithm = this.getCompressionAlgorithm(preferredCompressionAlgorithm);
        this.threshold = threshold;
    }

    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        if (this.zLibCompression != null) {
            this.zLibCompression.end();
        }
        if (this.snappyCompression != null) {
            this.snappyCompression.end();
        }
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final ByteBuf in, final ByteBuf out) throws Exception {
        if (!in.isReadable()) {
            return;
        }

        final int inputSize = in.readableBytes();
        if (inputSize < this.threshold || this.preferredCompressionAlgorithm instanceof NoopCompression) {
            out.writeByte(PacketCompressionAlgorithm.None.getValue());
            out.writeBytes(in);
        } else {
            in.markReaderIndex();
            final ByteBuf compressedData = ctx.alloc().buffer();
            this.preferredCompressionAlgorithm.compress(in, compressedData);
            if (compressedData.readableBytes() < inputSize) {
                out.writeByte(this.preferredCompressionAlgorithm.getAlgorithm().getValue());
                out.writeBytes(compressedData);
            } else {
                in.resetReaderIndex();
                out.writeByte(PacketCompressionAlgorithm.None.getValue());
                out.writeBytes(in);
            }
            compressedData.release();
        }
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) throws Exception {
        if (!in.isReadable()) {
            return;
        }

        final int rawAlgorithm = in.readUnsignedByte();
        final PacketCompressionAlgorithm algorithm = rawAlgorithm == 0xFF ? PacketCompressionAlgorithm.None : PacketCompressionAlgorithm.getByValue(rawAlgorithm);
        if (algorithm == null) { // Bedrock client drops the packet if the algorithm is unknown
            ViaBedrock.getPlatform().getLogger().log(Level.WARNING, "Dropping packet with unknown PacketCompressionAlgorithm: " + rawAlgorithm);
            in.skipBytes(in.readableBytes());
            return;
        }

        final CompressionAlgorithm compressionAlgorithm = this.getCompressionAlgorithm(algorithm);
        if (compressionAlgorithm instanceof NoopCompression) {
            out.add(in.readRetainedSlice(in.readableBytes()));
        } else {
            final ByteBuf uncompressedData = ctx.alloc().buffer();
            compressionAlgorithm.decompress(in, uncompressedData); // Bedrock client would drop packets with invalid data, but this would be too insane to do
            out.add(uncompressedData);
        }
    }

    private CompressionAlgorithm getCompressionAlgorithm(final PacketCompressionAlgorithm algorithm) {
        return switch (algorithm) {
            case None -> NoopCompression.INSTANCE;
            case ZLib -> {
                if (this.zLibCompression == null) {
                    this.zLibCompression = new ZlibCompression();
                }
                yield this.zLibCompression;
            }
            case Snappy -> {
                if (this.snappyCompression == null) {
                    this.snappyCompression = new SnappyCompression();
                }
                yield this.snappyCompression;
            }
        };
    }

}
