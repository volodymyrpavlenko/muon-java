package com.simplicity.services;

import io.muoncore.Muon;
import io.muoncore.extension.amqp.discovery.AmqpDiscovery;
import io.muoncore.extension.amqp.AmqpTransportExtension;
import reactor.rx.Streams;
import reactor.rx.broadcast.Broadcaster;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class ServicePublishHotStream {

    public static void main(String[] args) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException {

        final Muon muon = new Muon(
                new AmqpDiscovery("amqp://localhost:5672"));

        muon.setServiceIdentifer("cl");
        new AmqpTransportExtension("amqp://localhost:5672").extend(muon);
        muon.start();

        final Broadcaster stream = Broadcaster.create();

        muon.streamSource("/counter", Map.class, stream);

        //generate data every so often
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(true) {
                        Thread.sleep(5000);
                        System.out.println("Sending data");

                        stream.accept(new Awesome("I am a teapot", System.currentTimeMillis()));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    static class Awesome {
        private String myname;
        private long something;

        public Awesome(String myname, long something) {
            this.myname = myname;
            this.something = something;
        }

        public long getSomething() {
            return something;
        }

        public String getMyname() {
            return myname;
        }
    }
}
