/**
 * 
 */
package io.sipstack.example.netty.sip.registrar;

import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.header.CSeqHeader;


/**
 * @author jonas@jonasborjesson.com
 */
public class Binding {

    private final SipURI aor;

    private final int expires;

    private final CSeqHeader cseq;

    private final SipURI contact;

    /**
     * 
     */
    private Binding(final SipURI aor, final int expires, final CSeqHeader cseq, final SipURI contact) {
        this.aor = aor;
        this.expires = expires;
        this.cseq = cseq;
        this.contact = contact;
    }

    public SipURI getAor() {
        return this.aor;
    }

    public int getExpires() {
        return this.expires;
    }

    public CSeqHeader getCseq() {
        return this.cseq;
    }

    public SipURI getContact() {
        return this.contact;
    }

    public static Builder with() {
        return new Builder();
    }

    public static class Builder {

        private SipURI aor;

        private int expires;

        private CSeqHeader cseq;

        private SipURI contact;

        private Builder() {
            // just to prevent instantiation
        }

        public Builder aor(final SipURI aor) {
            this.aor = aor;
            return this;
        }

        public Builder expires(final int expires) {
            this.expires = expires;
            return this;
        }

        public Builder cseq(final CSeqHeader cseq) {
            this.cseq = cseq;
            return this;
        }

        public Builder contact(final SipURI contact) {
            this.contact = contact;
            return this;
        }

        public Binding build() {
            // of course, we really should validate things here
            // but since this is a basic exmaple, we will ignore
            // this for now
            return new Binding(this.aor, this.expires, this.cseq, this.contact);
        }

    }

}
