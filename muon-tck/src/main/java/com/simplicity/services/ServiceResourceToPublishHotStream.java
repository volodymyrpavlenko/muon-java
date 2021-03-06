package com.simplicity.services;

import io.muoncore.Muon;
import io.muoncore.future.MuonFuture;
import io.muoncore.future.MuonFutures;
import io.muoncore.MuonService;
import io.muoncore.extension.amqp.discovery.AmqpDiscovery;
import io.muoncore.extension.amqp.AmqpTransportExtension;
import io.muoncore.transport.resource.MuonResourceEvent;
import reactor.rx.broadcast.Broadcaster;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class ServiceResourceToPublishHotStream {

    public static void main(String[] args) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException {

        final Muon muon = new Muon(
                new AmqpDiscovery("amqp://localhost:5672"));

        muon.setServiceIdentifer("resourcePublisher");
        new AmqpTransportExtension("amqp://localhost:5672").extend(muon);
        muon.start();

        final Broadcaster<Map> stream = Broadcaster.create();

        muon.onQuery("/data", Map.class, new MuonService.MuonQuery<Map>() {
            @Override
            public MuonFuture onQuery(MuonResourceEvent queryEvent) {

                Map<String, String> data = new HashMap<String, String>();

                stream.accept(data);

                return MuonFutures.immediately(data);
            }
        });

        muon.streamSource("/livedata", Map.class, stream);
    }
}
