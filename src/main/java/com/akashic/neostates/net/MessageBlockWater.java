package com.akashic.neostates.net;

import com.akashic.neostates.NeoStatesMod;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** One cell of the layer changed. */
public class MessageBlockWater implements IMessage {

    public int x;
    public int y;
    public int z;
    public boolean water;

    public MessageBlockWater() {}

    public MessageBlockWater(int x, int y, int z, boolean water) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.water = water;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.x);
        buf.writeShort(this.y);
        buf.writeInt(this.z);
        buf.writeBoolean(this.water);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readInt();
        this.y = buf.readShort();
        this.z = buf.readInt();
        this.water = buf.readBoolean();
    }

    public static class Handler implements IMessageHandler<MessageBlockWater, IMessage> {

        @Override
        public IMessage onMessage(MessageBlockWater message, MessageContext ctx) {
            NeoStatesMod.proxy.receive(message);
            return null;
        }
    }
}
