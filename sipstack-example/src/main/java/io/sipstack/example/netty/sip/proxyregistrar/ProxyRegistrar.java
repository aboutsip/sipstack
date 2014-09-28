/**
 * 
 */
package io.sipstack.example.netty.sip.proxyregistrar;

import io.sipstack.example.netty.sip.SimpleSipStack;

/**
 * 
 * @author jonas@jonasborjesson.com
 */
public final class ProxyRegistrar {

    public static void main(final String[] args) throws Exception {
        final ProxyRegistrarHandler handler = new ProxyRegistrarHandler();
        final SimpleSipStack stack = new SimpleSipStack(handler, "10.0.1.28", 5060);
        handler.setStack(stack);
        stack.run();
    }

}
