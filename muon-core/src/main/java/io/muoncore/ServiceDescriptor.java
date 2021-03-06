package io.muoncore;

import java.net.URI;
import java.util.List;

public class ServiceDescriptor {

    private String identifier;
    private List<String> tags;
    private List<URI> resourceConnectionUrls;
    private List<URI> streamConnectionUrls;

    public ServiceDescriptor(String identifier,
                             List<String> tags,
                             List<URI> resourceConnectionUrls,
                             List<URI> streamConnectionUrls) {
        this.identifier = identifier;
        this.tags = tags;
        this.resourceConnectionUrls = resourceConnectionUrls;
        this.streamConnectionUrls = streamConnectionUrls;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<URI> getResourceConnectionUrls() {
        return resourceConnectionUrls;
    }

    public List<URI> getStreamConnectionUrls() {
        return streamConnectionUrls;
    }
}
