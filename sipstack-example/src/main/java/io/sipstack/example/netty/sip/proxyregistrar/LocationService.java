/**
 * 
 */
package io.sipstack.example.netty.sip.proxyregistrar;

import io.pkts.packet.sip.address.SipURI;
import io.sipstack.example.netty.sip.registrar.Binding;
import io.sipstack.example.netty.sip.registrar.Registrar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * If you did the {@link Registrar} example you saw how we created an association with a AOR
 * (address-of-record) and a particular Contact address. We simply stored this in the same scope as
 * the registrar handler itself but overall there wasn't much use to that example since you could
 * only register and nothing else. Typically, you have a location store, where the registrar manages
 * the bindings and someone else, such as a location aware proxy, reads those bindings and does
 * something useful with it.
 * 
 * This is the location service where we will store the associations between an AOR and the contact
 * addresses where we can reach the AOR. This is a very simple in-memory location store and normally
 * you would either use some kind of distributed storage, such as redis, hazelcast etc or perhaps a
 * plain DB (you should ask yourself why you really need a DB though)
 * 
 * @author jonas@jonasborjesson.com
 */
public class LocationService {

    private final Map<SipURI, List<Binding>> locationStore = new HashMap<SipURI, List<Binding>>();

    private static LocationService me = new LocationService();

    private LocationService() {
        // the location service is a singleton so this will prevent
        // anyone else from creating an instance of this class.
    }

    public static LocationService getInstance() {
        return me;
    }

    public List<Binding> getBindings(final SipURI aor) {
        synchronized (this.locationStore) {
            return this.locationStore.get(aor);
        }
    }

    public List<Binding> updateBindings(final Binding binding) {
        synchronized (this.locationStore) {
            final List<Binding> bindings = ensureLocationStore(binding.getAor());
            final Iterator<Binding> it = bindings.iterator();
            boolean add = true;
            while (it.hasNext()) {
                final Binding bind = it.next();
                if (!bind.getContact().equals(binding.getContact())) {
                    continue;
                }

                if (binding.getExpires() == 0) {
                    it.remove();
                    add = false;
                } else if (!bind.getCallId().equals(binding.getCallId())) {
                    it.remove();
                } else if (binding.getCseq().getSeqNumber() > bind.getCseq().getSeqNumber()) {
                    it.remove();
                } else {
                    // cseq is less or equal so either a re-transmission or
                    // an out-of-order request
                    add = false;
                }
            }

            if (binding.getExpires() > 0 && add) {
                bindings.add(binding);
            }
            return bindings;
        }
    }

    private List<Binding> ensureLocationStore(final SipURI aor) {
        List<Binding> bindings = this.locationStore.get(aor);
        if (bindings == null) {
            bindings = new ArrayList<>();
            this.locationStore.put(aor, bindings);
        }
        return bindings;
    }

}
