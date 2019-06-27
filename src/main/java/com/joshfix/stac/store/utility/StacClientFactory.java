package com.joshfix.stac.store.utility;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;

/**
 * @author joshfix
 * Created on 2019-04-16
 */
@Slf4j
public class StacClientFactory {

    private StacClientFactory() {}

    public static StacRestClient create(String uri) {
        try {
            return create(new URI(uri));
        } catch (Exception e) {
            throw new RuntimeException("Error creating URI from " + uri, e);
        }
    }

    public static StacRestClient create(URI uri) {
        String scheme = uri.getScheme().toLowerCase();
        String username = null;
        String password = null;

        String userinfo = uri.getUserInfo();
        if (null != userinfo && !userinfo.isEmpty() && userinfo.indexOf(":") > 0) {
            String[] up = userinfo.split(":");
            username = up[0];
            password = up[1];
        }
        int port = uri.getPort();

        switch (scheme) {
            case "http":

                if (port == -1) {
                    port = 80;
                }
                return new StacRestClient(uri.getScheme(), uri.getHost(), uri.getPath(), port, username, password);
            case "https":
                if (port == -1) {
                    port = 443;
                }
                return new StacRestClient(uri.getScheme(), uri.getHost(), uri.getPath(), port, username, password);
            default:
                throw new RuntimeException("Unknown scheme: " + scheme);
        }

    }

}
