package com.akashic.neostates.net;

import com.akashic.neostates.NeoConfig;
import com.akashic.neostates.NeoStatesMod;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * The options both sides must agree on, as the server has them: the client moves its own player and computes its own
 * light, so it follows the server while connected.
 */
public class MessageConfig implements IMessage {

    public boolean physics;
    public boolean light;
    public boolean waterlogging;

    public MessageConfig() {}

    public static MessageConfig ofServer() {
        MessageConfig message = new MessageConfig();
        message.physics = NeoConfig.modernWaterPhysics;
        message.light = NeoConfig.modernWaterLight;
        message.waterlogging = NeoConfig.waterlogging;
        return message;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte((this.physics ? 1 : 0) | (this.light ? 2 : 0) | (this.waterlogging ? 4 : 0));
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int bits = buf.readUnsignedByte();
        this.physics = (bits & 1) != 0;
        this.light = (bits & 2) != 0;
        this.waterlogging = (bits & 4) != 0;
    }

    public static class Handler implements IMessageHandler<MessageConfig, IMessage> {

        @Override
        public IMessage onMessage(MessageConfig message, MessageContext ctx) {
            NeoStatesMod.proxy.receive(message);
            return null;
        }
    }
}
