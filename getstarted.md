---
title: The Protocol
layout: info
menu: getstarted.html
active_element: getstarted
sipstackio_version: 0.1.0
---

<h1 id="getting-started">Getting Started</h1>

Getting Started will guide you through the process of creating a simple UAS application and will take you through some of the more important concepts in the stack. We will also look at some of the underlying libraries that sipstack.io is using and touch upon some of the reasons why sipstack.io exists to begin with.

<h2 id="getting-started-overview">Overview</h2>

Sipstack.io is framework whose primary goal is to provide you with a light-weight, ready-to-go sipstack with the all the necessary functionality needed by a production-ready SIP application, but without the fuzz that larger application containers typically come with.

<h2 id="getting-started-netty">Netty for network access</h2>

Any SIP stack needs a blazingly fast network stack and netty.io is just that. Netty also has a large install base, an active community and over 10 years of development behind it, making it an excellent base for sipstack.io. If you are familiar with Netty, then integrating raw SIP support to your own SIP enabled application should be a breeze.

<h2 id="getting-started-pkts">Pkts.io for parsing</h2>

Equally important to a fast and reliable network stack, is the layer responsible for parsing and framing the data coming off of the network. A poor parsing/framing implementation will cripple the performance of any stack so it is important that this library is written with CPU and memory consumption in mind. Do not copy data if it is avoidable and do everything lazily since many SIP applications rarely need to parse every part of a message. Pkts.io is a library designed with these requirements in mind and is what sipstack.io is using encode/decode SIP messages and will be the library your application will use the most.

<h2 id="getting-started-maven">Maven</h2>
