package io.sipstack.netty.codec.sip;

import io.pkts.buffer.Buffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class RawMessageTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"OPTIONS sip:pfry@127.0.0.1;transport=tcp SIP/2.0\r\n" +
                        "Via: SIP/2.0/TCP 127.0.0.1:1337;rport;branch=z9hG4k;alias\r\n" +
                        "Max-Forwards: 70\r\n" +
                        "From: <sip:tleela@127.0.01>;tag=1018302\r\n" +
                        "To: <sip:pfry@127.0.0.1>\r\n" +
                        "Call-ID: a-long-call-id\r\n" +
                        "CSeq: 1 OPTIONS\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length:    10\r\n" +
                        "\r\n" +
                        "I love you",
                        "I love you"
                },
                {"OPTIONS sip:pfry@127.0.0.1;transport=tcp SIP/2.0\r\n" +
                        "Via: SIP/2.0/TCP 127.0.0.1:1337;rport;branch=z9hG4k;alias\r\n" +
                        "Max-Forwards: 70\r\n" +
                        "From: <sip:tleela@127.0.01>;tag=1018302\r\n" +
                        "To: <sip:pfry@127.0.0.1>\r\n" +
                        "Call-ID: a-long-call-id\r\n" +
                        "CSeq: 1 OPTIONS\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length:    0\r\n" +
                        "\r\n",
                        null
                }
        });
    }

    private final String input;
    private final String expected;

    public RawMessageTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void testPayloadParsing() throws IOException, MaxMessageSizeExceededException {
        RawMessage rawMessage = convertToRawMessage(input);
        assertEquals(expected, extractPayload(rawMessage));
    }

    private RawMessage convertToRawMessage(String msg) throws IOException, MaxMessageSizeExceededException {
        ByteArrayInputStream bytes = new ByteArrayInputStream(msg.getBytes());
        RawMessage rawMessage = new RawMessage(SipMessageStreamDecoder.MAX_ALLOWED_INITIAL_LINE_SIZE,
                SipMessageStreamDecoder.MAX_ALLOWED_HEADERS_SIZE, SipMessageStreamDecoder.MAX_ALLOWED_CONTENT_LENGTH);
        int b;
        while (!rawMessage.isComplete() && (b = bytes.read()) != -1) {
            rawMessage.write((byte) b);
        }
        return rawMessage;
    }

    private String extractPayload(RawMessage msg) {
        Buffer payload = msg.getPayload();
        if (payload != null) {
            return payload.toString();
        } else {
            return null;
        }
    }

}
