---
title: The Protocol
layout: info
menu: getstarted.html
active_element: getstarted
---

<h1 id="getting-started">Getting Started</h1>

This guide will take you through the process of creating a simple UAS application and will guide you through some of the more important concepts of sipstack.io. We will also look at some of the underlying libraries that sipstack.io is using and touch upon some of the reasons why sipstack.io exists to begin with.

<h2 id="getting-started-overview">Overview</h2>

Sipstack.io is framework whose primary goal is to provide you with a light-weight, ready-to-go sipstack with the all the necessary functionality needed by a production-ready SIP application, but without the fuzz that larger application containers typically come with.

<h2 id="getting-started-netty">Netty for network access</h2>

Any SIP stack needs a blazingly fast and reliable network stack and [netty.io](http://www.netty.io) is just that. Netty also has a large install base, an active community and over 10 years of development behind it, making it an excellent base for sipstack.io. If you are familiar with Netty, then integrating raw SIP support to your own SIP enabled application should be a breeze.

<h2 id="getting-started-pkts">Pkts.io for parsing</h2>

Equally important to a fast and reliable network stack, is the layer responsible for parsing and framing the data coming off of the network. A poor parsing/framing implementation will cripple the performance of any stack so it is important that this library is written with CPU and memory consumption in mind. Do not copy data if it is avoidable and do everything lazily since many SIP applications rarely need to parse every part of a message anyway. [Pkts.io](http://www.pkts.io) is a library designed with these requirements in mind and is what sipstack.io is using to encode/decode SIP messages and will be the library your application will interact with the most.

<h1 id="app-intro">Your First Application - UAS</h1>

The first application we are going to build is a very basic SIP User Agent Server (UAS). In SIP, a User Agent Client (UAC) generates requests and the UAS terminates the request by sending back a response. It is important to understand that these are roles a UA (User Agent) undertakes at different points in time. Hence, a SIP element can one moment act as a UAC and as a UAS the next. If you want to learn more about SIP, head over to [aboutsip.com](http://www.aboutsip.com) and go through those presentations.

<h2 id="app-maven">Setting up Maven</h2>

sipstack.io is available through [Maven Central](http://search.maven.org/) and we will be using [Maven](http://maven.apache.org/) in these examples. If you are new to Maven, then check out [Maven: The Complete Reference](http://books.sonatype.com/mvnref-book/reference/), which should have you up and running in no time.

<h3 id="app-maven-core-library">Adding Dependencies</h3>
First, add a property to your POM with the current version of sipstack.io (which is {{ site.sipstackio_version }}):

``` xml
<properties>
    <sipstackio.version>{{ site.sipstackio_version }}</sipstackio.version>
</properties>
```

Add the sipstack-netty-codec-sip library as a dependency:

``` xml
<dependencies>
    <dependency>
        <groupId>io.sipstack</groupId>
        <artifactId>sipstack-netty-codec-sip</artifactId>
        <version>${sipstackio.version}</version>
    </dependency>
</dependencies>
```

<h3 id="app-maven-core-library">Configure for Java 8</h3>

Sipstack.io requires Java 8 so you need to configure your Maven setup to use Java 8. To do this, you need to configure your compiler, which in Maven is accomplished by re-configuring the `maven-compiler-plugin` like so:

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.1</version>
                <!-- compile for Java 1.8 -->
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

Your final pom.xml should look like something like the following [gist](https://gist.github.com/aboutsip/b697b39b692101370415)

Now that we have Maven setup we are ready to start coding.

<h2 id="app-netty">Configure Netty</h2>
<h2 id="app-uas">Build the UAS</h2>
<h2 id="app-fat-jar">Build a fat jar</h2>
<h2 id="app-run-it">Run your application</h2>
<h2 id="app-send-traffic">Send some traffic!</h2>

