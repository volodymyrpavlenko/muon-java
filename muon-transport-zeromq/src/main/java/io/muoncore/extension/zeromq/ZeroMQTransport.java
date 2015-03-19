package io.muoncore.extension.zeromq;

import io.muoncore.MuonStreamGenerator;
import io.muoncore.codec.TransportCodecType;
import io.muoncore.transport.stream.MuonStreamTransport;
import org.reactivestreams.Subscriber;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class ZeroMQTransport implements MuonStreamTransport {

    @Override
    public void provideStreamSource(String streamName, MuonStreamGenerator sourceOfData) {

    }


    @Override
    public <T> void subscribeToStream(String url, Class<T> type, Map<String, String> params, Subscriber<T> subscriber) throws URISyntaxException {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public String getUrlScheme() {
        return "zeromq";
    }

    @Override
    public URI getLocalConnectionURI() throws URISyntaxException {
        return new URI("zeromq://localhost");
    }

    @Override
    public TransportCodecType getCodecType() {
        return TransportCodecType.BINARY;
    }
}