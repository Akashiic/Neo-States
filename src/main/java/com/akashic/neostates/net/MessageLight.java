package com.akashic.neostates.net;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import com.akashic.neostates.NeoStatesMod;
import com.akashic.neostates.water.Sections;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * One kind of light (sky or block) of some sections of a chunk, after the server relit them. The game has no packet
 * for light alone, and resending the chunk would resend its blocks and tile entities too.
 */
public class MessageLight implements IMessage {

    /** Bytes of a section's light: a nibble per cell. */
    public static final int NIBBLES = 2048;
    private static final int MAX_SECTIONS = 48;

    public int chunkX;
    public int chunkZ;
    public boolean sky;
    public byte[] sectionYs = new byte[0];
    public byte[][] light = new byte[0][];

    public MessageLight() {}

    /**
     * @param sections a bit per section, {@code section Y + 32}
     * @return null when none of those sections has that light
     */
    public static MessageLight of(Chunk chunk, boolean sky, long sections) {
        byte[] ys = new byte[MAX_SECTIONS];
        byte[][] data = new byte[MAX_SECTIONS][];
        int n = 0;
        for (int bit = 0; bit < MAX_SECTIONS; bit++) {
            if ((sections & 1L << bit) == 0L) {
                continue;
            }
            ExtendedBlockStorage section = Sections.section(chunk, bit - 32);
            NibbleArray array = section == null ? null
                : sky ? section.getSkylightArray() : section.getBlocklightArray();
            if (array == null || array.data.length != NIBBLES) {
                continue;
            }
            ys[n] = (byte) (bit - 32);
            data[n++] = array.data.clone();
        }
        if (n == 0) {
            return null;
        }
        MessageLight message = new MessageLight();
        message.chunkX = chunk.xPosition;
        message.chunkZ = chunk.zPosition;
        message.sky = sky;
        message.sectionYs = java.util.Arrays.copyOf(ys, n);
        message.light = java.util.Arrays.copyOf(data, n);
        return message;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.chunkX);
        buf.writeInt(this.chunkZ);
        buf.writeBoolean(this.sky);
        buf.writeByte(this.sectionYs.length);
        buf.writeBytes(this.sectionYs);
        byte[] raw = new byte[this.sectionYs.length * NIBBLES];
        for (int i = 0; i < this.light.length; i++) {
            System.arraycopy(this.light[i], 0, raw, i * NIBBLES, NIBBLES);
        }
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        deflater.setInput(raw);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length / 4 + 64);
        byte[] chunk = new byte[4096];
        while (!deflater.finished()) {
            out.write(chunk, 0, deflater.deflate(chunk));
        }
        deflater.end();
        buf.writeInt(out.size());
        buf.writeBytes(out.toByteArray());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.chunkX = buf.readInt();
        this.chunkZ = buf.readInt();
        this.sky = buf.readBoolean();
        int n = buf.readUnsignedByte();
        if (n > MAX_SECTIONS) {
            throw new IllegalArgumentException("light sections: " + n);
        }
        this.sectionYs = new byte[n];
        buf.readBytes(this.sectionYs);
        int length = buf.readInt();
        if (length < 0 || length > buf.readableBytes()) {
            throw new IllegalArgumentException("light payload: " + length);
        }
        byte[] packed = new byte[length];
        buf.readBytes(packed);
        byte[] raw = new byte[n * NIBBLES];
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(packed);
            int read = 0;
            while (read < raw.length && !inflater.finished()) {
                int got = inflater.inflate(raw, read, raw.length - read);
                if (got == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    break;
                }
                read += got;
            }
            if (read != raw.length) {
                throw new IllegalArgumentException("light payload is short: " + read);
            }
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("light payload", e);
        } finally {
            inflater.end();
        }
        this.light = new byte[n][];
        for (int i = 0; i < n; i++) {
            this.light[i] = java.util.Arrays.copyOfRange(raw, i * NIBBLES, (i + 1) * NIBBLES);
        }
    }

    public static class Handler implements IMessageHandler<MessageLight, IMessage> {

        @Override
        public IMessage onMessage(MessageLight message, MessageContext ctx) {
            NeoStatesMod.proxy.receive(message);
            return null;
        }
    }
}
