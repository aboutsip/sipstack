# sipstack.io examples

The following module contains examples of how to use sipstack.io. Each example is listed below but for details, please see the javadoc of each example for more information.

## Raw examples using Netty

sipstack.io is built ontop of Netty and the lowest layer of sipstack.io provides the raw Netty decoders and encoders. The examples found in the package ```io.sipstack.example.netty``` shows how to use the raw power of Netty.

### Netty UAS
A basic example showing how to use the raw Netty SIP decoders and encoders to implement a basic UAS. It also shows you how to setup the Netty stack to recieve both SIP over UDP and TCP.
