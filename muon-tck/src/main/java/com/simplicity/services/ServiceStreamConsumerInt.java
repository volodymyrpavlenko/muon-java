package com.simplicity.services;

import io.muoncore.Muon;
import io.muoncore.extension.amqp.AmqpTransportExtension;
import io.muoncore.extension.amqp.discovery.AmqpDiscovery;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.fn.Consumer;
import reactor.rx.Stream;
import reactor.rx.Streams;
import reactor.rx.broadcast.Broadcaster;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServiceStreamConsumerInt {

    public static void main(String[] args) throws URISyntaxException, InterruptedException, NoSuchAlgorithmException, KeyManagementException, IOException {

        final Muon muon = new Muon(
                new AmqpDiscovery("amqp://localhost:5672"));

        muon.setServiceIdentifer("consumer-" + UUID.randomUUID().toString());
        new AmqpTransportExtension("amqp://localhost:5672").extend(muon);
        muon.start();

        //amqp discovery settle time.
        Thread.sleep(5000);

        Broadcaster<Integer> sub = Broadcaster.create();

        sub.consume(new Consumer<Integer>() {
            @Override
            public void accept(Integer o) {
                System.out.println("I have a message " + o);
            }
        });

        Map<String,String> params  = new HashMap<String, String>();

        params.put("max", "500");

        muon.subscribe("muon://cl/counter", Integer.class, params, sub);


        sub.subscribe(
                new Subscriber<Integer>() {
                    public void onSubscribe(Subscription s) {}
                    public void onNext(Integer consume) {}
                    public void onError(Throwable t) {
                        System.out.println("Stream completed with ERROR");
                        t.printStackTrace();
                    }
                    public void onComplete() {
                        System.out.println("Stream completed successfully and is disconnected");
                    }
                });
    }

    static class Consume {
        private String myname;
        private long something;

        public void setMyname(String myname) {
            this.myname = myname;
        }

        public void setSomething(long something) {
            this.something = something;
        }

        public long getSomething() {
            return something;
        }

        public String getMyname() {
            return myname;
        }

        @Override
        public String toString() {
            return "Consume{" +
                    "myname='" + myname + '\'' +
                    ", something=" + something +
                    '}';
        }
    }
}
