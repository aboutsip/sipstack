/**
 * 
 */
package io.sipstack.netty.codec.sip;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.impl.SipParser;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Takes a {@link SipMessage} (request or response) and encodes it to a
 * {@link ByteBuf} before sending it down the pipe.
 * 
 * @author jonas@jonasborjesson.com
 */
public final class SipMessageEncoder extends MessageToMessageEncoder<SipMessage> {

    // @Override
    protected void encode(final ChannelHandlerContext ctx, final SipMessage msg, final ByteBuf out) throws Exception {
        final Buffer b = msg.toBuffer();
        for (int i = 0; i < b.getReadableBytes(); ++i) {
            out.writeByte(b.getByte(i));
        }
        out.writeByte(SipParser.CR);
        out.writeByte(SipParser.LF);

        // if ("QOTM?".equals(packet.content().toString(CharsetUtil.UTF_8))) {
        // ctx.write(new DatagramPacket(
        // Unpooled.copiedBuffer("QOTM: " + nextQuote(), CharsetUtil.UTF_8), packet.sender()));
        // }
    }

    @Override
    protected void encode(final ChannelHandlerContext ctx, final SipMessage msg, final List<Object> out)
            throws Exception {
        final Buffer b = msg.toBuffer();
        final int capacity = b.capacity() + 2;
        final ByteBuf buffer = ctx.alloc().buffer(capacity, capacity);

        for (int i = 0; i < b.getReadableBytes(); ++i) {
            buffer.writeByte(b.getByte(i));
        }
        buffer.writeByte(SipParser.CR);
        buffer.writeByte(SipParser.LF);
        final DatagramPacket pkt = new DatagramPacket(buffer, new InetSocketAddress("127.0.0.1", 5070));
        out.add(pkt);
    }

}
