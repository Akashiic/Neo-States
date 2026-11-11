package com.akashic.neostates.net;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import com.akashic.neostates.NeoStatesMod;
import com.akashic.neostates.water.Sections;
import com.akashic.neostates.water.WaterBits;
import com.akashic.neostates.water.WaterSection;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** The whole layer of one chunk, sent to a player that starts watching it. Only sections with a bit travel. */
public class MessageChunkWater implements IMessage {

    public int chunkX;
    public int chunkZ;
    public byte[] sectionYs = new byte[0];
    public byte[][] bits = new byte[0][];

    public MessageChunkWater() {}

    /** @return null when the chunk holds no waterlogged cell */
    public static MessageChunkWater of(Chunk chunk) {
        int n = 0;
        byte[] ys = new byte[16 - Sections.minSectionY()];
        byte[][] data = new byte[ys.length][];
        for (int sectionY = Sections.minSectionY(); sectionY < 16; sectionY++) {
            ExtendedBlockStorage section = Sections.section(chunk, sectionY);
            if (section == null) {
                continue;
            }
            long[] layer = ((WaterSection) section).neostates$getWater();
            if (WaterBits.isEmpty(layer)) {
                continue;
            }
            ys[n] = (byte) sectionY;
            data[n++] = WaterBits.toBytes(layer);
        }
        if (n == 0) {
            return null;
        }
        MessageChunkWater message = new MessageChunkWater();
        message.chunkX = chunk.xPosition;
        message.chunkZ = chunk.zPosition;
        message.sectionYs = java.util.Arrays.copyOf(ys, n);
        message.bits = java.util.Arrays.copyOf(data, n);
        return message;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.chunkX);
        buf.writeInt(this.chunkZ);
        buf.writeByte(this.sectionYs.length);
        for (int i = 0; i < this.sectionYs.length; i++) {
            buf.writeByte(this.sectionYs[i]);
            buf.writeBytes(this.bits[i]);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.chunkX = buf.readInt();
        this.chunkZ = buf.readInt();
        int n = buf.readUnsignedByte();
        if (n > 48) {
            throw new IllegalArgumentException("water sections: " + n);
        }
        this.sectionYs = new byte[n];
        this.bits = new byte[n][];
        for (int i = 0; i < n; i++) {
            this.sectionYs[i] = buf.readByte();
            this.bits[i] = new byte[WaterBits.BYTES];
            buf.readBytes(this.bits[i]);
        }
    }

    public static class Handler implements IMessageHandler<MessageChunkWater, IMessage> {

        @Override
        public IMessage onMessage(MessageChunkWater message, MessageContext ctx) {
            NeoStatesMod.proxy.receive(message);
            return null;
        }
    }
}
