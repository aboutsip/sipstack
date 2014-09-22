/**
 * 
 */
package io.sipstack.example.netty.sip.registrar;

import io.sipstack.example.netty.sip.SimpleSipStack;

/**
 * @author jonas
 */
public final class Registrar {


    public static void main(final String[] args) throws Exception {
        final RegistrarHandler handler = new RegistrarHandler();
        final SimpleSipStack stack = new SimpleSipStack(handler, "127.0.0.1", 5060);
        stack.run();
    }

}
