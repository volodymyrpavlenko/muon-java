package io.muoncore.extension.amqp.discovery;

import com.google.gson.Gson;
import io.muoncore.Discovery;
import io.muoncore.Muon;
import io.muoncore.ServiceDescriptor;
import io.muoncore.codec.GsonTextCodec;
import io.muoncore.codec.TextBinaryCodec;
import io.muoncore.config.AutoConfiguration;
import io.muoncore.config.MuonBuilder;
import io.muoncore.exception.MuonException;
import io.muoncore.extension.amqp.AMQPEventTransport;
import io.muoncore.extension.amqp.AmqpBroadcast;
import io.muoncore.extension.amqp.AmqpConnection;
import io.muoncore.transport.MuonMessageEvent;
import io.muoncore.transport.MuonMessageEventBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class AmqpDiscovery implements Discovery {

    static {

        MuonBuilder.registerDiscovery( config -> {
            try {
                return new AmqpDiscovery(config.getDiscoveryUrl());
            } catch (URISyntaxException | KeyManagementException | NoSuchAlgorithmException | IOException e) {
                throw new MuonException("Unable to create AMQP discovery", e);
            }
        });
    }

    private static final String RESOURCE_CONNECTIONS = "resourceConnections";
    private static final String STREAM_CONNECTIONS = "streamConnections";
    private static final String IDENTIFIER = "identifier";
    private static final String TAGS = "tags";
    private Logger log = Logger.getLogger(AMQPEventTransport.class.getName());
    private ExecutorService spinner;
    public static final String SERVICE_ANNOUNCE = "serviceAnnounce";

    private ServiceCache serviceCache;
    private AmqpBroadcast amqpBroadcast;
    private ServiceDescriptor descriptor;
    private final static int PING_TIME=3000;
    private Gson gson;

    private boolean isReady=false;
    private List<Runnable> onReadyNotification = new ArrayList<Runnable>();

    public AmqpDiscovery(String amqpUrl) throws URISyntaxException, KeyManagementException, NoSuchAlgorithmException, IOException {
        this(new AmqpBroadcast(
                new AmqpConnection(amqpUrl)));
    }

    public AmqpDiscovery(AmqpBroadcast amqpBroadcast) {
        this.gson = new Gson();
        this.amqpBroadcast = amqpBroadcast;
        serviceCache = new ServiceCache();
        spinner = Executors.newCachedThreadPool();
        connect();
    }

    private Map decode(MuonMessageEvent event) {
        return new TextBinaryCodec(new GsonTextCodec()).decode(
                event.getBinaryEncodedContent());
    }

    private byte[] encode(MuonMessageEvent event) throws UnsupportedEncodingException {
        return new GsonTextCodec().encode(event.getDecodedContent()).getBytes("UTF8");
    }

    public void connect() {
        amqpBroadcast.listenOnBroadcastEvent(SERVICE_ANNOUNCE, new Muon.EventMessageTransportListener() {
            @Override
            public void onEvent(String name, MuonMessageEvent obj) {
                Map announce = decode(obj);

                serviceCache.addService(announce);
            }
        });

        startAnnouncePing();
        spinner.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(PING_TIME + 200);
                    isReady=true;
                    notifyListeners();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void notifyListeners() {
        for (Runnable run: onReadyNotification) {
            spinner.execute(run);
        }
    }

    private void startAnnouncePing() {
        spinner.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (descriptor != null) {
                            Map<String,Object> discoveryMessage = new HashMap<String, Object>();

                            discoveryMessage.put(IDENTIFIER, descriptor.getIdentifier());
                            discoveryMessage.put(TAGS, descriptor.getTags());
                            discoveryMessage.put(RESOURCE_CONNECTIONS, descriptor.getResourceConnectionUrls());
                            discoveryMessage.put(STREAM_CONNECTIONS, descriptor.getStreamConnectionUrls());

                            MuonMessageEvent ev = MuonMessageEventBuilder.named(SERVICE_ANNOUNCE)
                                    .withContent(discoveryMessage).build();

                            ev.setEncodedBinaryContent(encode(ev));

                            amqpBroadcast.broadcast(SERVICE_ANNOUNCE, ev);
                        }
                        Thread.sleep(3000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public List<ServiceDescriptor> getKnownServices() {
        List<ServiceDescriptor> services = new ArrayList<ServiceDescriptor>();

        try {
            for (Map data : serviceCache.getServices()) {
                List<URI> connectionList = readResourceConnectionUrls(data);
                List<URI> streamConnectionList = readStreamConnectionUrls(data);

                List tagList = readTags(data);

                services.add(new ServiceDescriptor(
                        (String) data.get(IDENTIFIER),
                        tagList,
                        connectionList,
                        streamConnectionList));
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "Unable to read a service descriptor, are you using the same protocol version?", e);
        }

        return services;
    }

    private List<URI> readStreamConnectionUrls(Map data) throws URISyntaxException {
        return readConnectionUrls(STREAM_CONNECTIONS, data);
    }

    private List<URI> readResourceConnectionUrls(Map data) throws URISyntaxException {
        return readConnectionUrls(RESOURCE_CONNECTIONS, data);
    }

    private List<URI> readConnectionUrls(String name, Map data) throws URISyntaxException {
        Object connectionUrls = data.get(name);
        List<URI> ret = new ArrayList<URI>();

        if (connectionUrls != null && connectionUrls instanceof List) {
            List urls = (List) connectionUrls;
            for (Object url : urls) {
                ret.add(new URI(url.toString()));
            }
        }
        return ret;
    }

    private List readTags(Map data) {
        Object tags = data.get(TAGS);
        List ret = new ArrayList();

        if (tags != null && tags instanceof List) {
            ret = (List) tags;
        }
        return ret;
    }

    @Override
    public ServiceDescriptor getService(URI searchUri) {

        if (!searchUri.getScheme().equals("muon")) {
            throw new IllegalArgumentException("Discovery requires muon://XXX scheme urls for lookup");
        }

        String serviceName = searchUri.getHost();

        for(ServiceDescriptor desc: getKnownServices()) {
            if (desc.getIdentifier().equals(serviceName)) {
                return desc;
            }
        }
        return null;
    }

    @Override
    public void advertiseLocalService(ServiceDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public void onReady(Runnable onReady) {
        if (isReady) {
            spinner.execute(onReady);
        } else {
            onReadyNotification.add(onReady);
        }
    }
}
