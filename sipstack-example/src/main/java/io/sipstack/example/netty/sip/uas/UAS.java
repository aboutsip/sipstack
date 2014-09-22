/**
 * 
 */
package io.sipstack.example.netty.sip.uas;

import io.sipstack.example.netty.sip.SimpleSipStack;

/**
 * Basic mini sip stack for acting as an UAS. Run it and then use e.g. SIPp
 * (http://sipp.sourceforge.net) to test it out like so:
 * 
 * <pre>
 * sipp -sn uac 127.0.0.1:5060
 * </pre>
 * 
 * This will start SIPp using the 'uac' scenario, i.e., SIPp will act as a client and execute a call
 * setup/teardown flow, which is SIPs way of making a "phone call". By default SIPp will use UDP so
 * the above example will use UDP as the transport.
 * 
 * If you want to use TCP instead, run SIPp with the transport switch like so:
 * 
 * <pre>
 * sipp -sn uac -t t1 127.0.0.1:5060
 * </pre>
 * 
 * which will use a single TCP socket for all calls. If you want a new socket for each call then:
 * 
 * <pre>
 * sipp -sn uac -t tn 127.0.0.1:5060
 * </pre>
 * 
 * and finally, if you want to limit the max number of concurrent connections, use the -max_socket
 * option:
 * 
 * <pre>
 * sipp -sn uac -t tn -max_socket 100 127.0.0.1:5060
 * </pre>
 * 
 * which will use a maximum of 100 concurrent sockets. Once that maximum is reached, any new calls
 * will reuse any already open connections. For more options, please refer to the SIPp manual.
 * 
 * 
 * @author jonas@jonasborjesson.com
 *
 */
public final class UAS {

    public static void main(final String[] args) throws Exception {
        final UASHandler handler = new UASHandler();
        new SimpleSipStack(handler, "127.0.0.1", 5060).run();
    }

}
