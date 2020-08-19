package com.joshfix.stac.store.utility;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.URI;
import java.security.cert.CertificateException;
import java.util.Map;

/**
 * @author joshfix
 * Created on 1/10/18
 */
@Slf4j
public class StacRestClient {

    private OkHttpClient client;
    private HttpUrl.Builder itemsUrlBuilder;
    private JsonUtils jsonUtils = new JsonUtils();
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public StacRestClient(String host, int port) {
        this("http", host, null, port, null, null);
    }

    public StacRestClient(String host, int port, String username, String password) {
        this("http", host, null, port, username, password);
    }

    public StacRestClient(String scheme, String host, String path, int port, String username, String password) {

        //client = new OkHttpClient();

        client = configureToIgnoreCertificate(new OkHttpClient.Builder()).build();


        itemsUrlBuilder = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .port(port);

        if (null != username && !username.isEmpty()) {
            itemsUrlBuilder.username(username);
        }

        if (null != password && !password.isEmpty()) {
            itemsUrlBuilder.password(password);
        }

        if (null != path) {
            if (path.startsWith("/")){
                path = path.substring(1);
            }
            itemsUrlBuilder.addPathSegments(path);
        }
    }

    public StacRestClient(URI uri) {
        this(uri.toString(), null, null);
    }

    public StacRestClient(String url) {
       this(url, null, null);
    }

    public StacRestClient(String url, String username, String password) {
        this(HttpUrl.parse(url).scheme(), HttpUrl.parse(url).host(), HttpUrl.parse(url).encodedPath(),
                HttpUrl.parse(url).port(), username, password);
    }


    public Map search(SearchRequest request) throws StacException {
        log.debug("search method called. Request: " + request.toString());
        Request req = buildOkHttpRequest(request).build();
        log.debug("Executing search request to {}", req.url().toString());
        Response response = null;
        try {
            response = client.newCall(req).execute();
            if (response.code() < 200 || response.code() > 299) {
                log.error("Error querying STAC.  " + response.body().string());
            }
            byte[] body = response.body().bytes();
            return jsonUtils.itemCollectionFromJson(body);
        } catch (Exception e) {
            throw new StacException("An error was encountered while executing search. " + e.getLocalizedMessage());
        } finally {
            if (null != response && null != response.body()) {
                response.body().close();
            }
        }
    }

    public byte[] searchBytes(SearchRequest request) throws StacException {
        log.debug("searchBytes method called. Request: " + request.toString());
        Request req = buildOkHttpRequest(request).build();
        log.debug("Executing search request to {}", req.url().toString());
        Response response = null;
        try {
            response = client.newCall(req).execute();
            return response.body().bytes();
        } catch (Exception e) {
            throw new StacException("An error was encountered while executing search. " + e.getLocalizedMessage());
        } finally {
            if (null != response) {
                response.body().close();
            }
        }
    }

    public ItemIterator searchStreaming(SearchRequest request) throws StacException {
        log.debug("searchStreaming method called. Request: " + request.toString());
        Request.Builder builder = buildOkHttpRequest(request);
        Request req = builder.header("Accept", "text/event-stream").build();
        log.debug("Executing search request to {}", req.url().toString());

        Response response;
        try {
            response = client.newCall(req).execute();
        } catch (Exception e) {
            throw new StacException("An error was encountered while executing search. " + e.getLocalizedMessage());
        }

        if (!response.isSuccessful()) {
            try {
                throw new StacException("The request to STAC was not successful. " + response.body().string());
            } catch (IOException e) {
                throw new StacException("The request to STAC was not successful.");
            }
        }

        return new ItemIterator(response, jsonUtils);
    }

    protected Request.Builder buildOkHttpRequest(SearchRequest request) throws StacException {
        byte[] requestBytes = jsonUtils.toJson(request);
        RequestBody rb = RequestBody.create(JSON, requestBytes);
        return new Request.Builder().url(itemsUrlBuilder.build()).post(rb);
    }

    public Map searchById(String id) throws StacException {
        log.debug("searchById method called. ID: " + id);
        HttpUrl url = itemsUrlBuilder.build().newBuilder().addPathSegment(id).build();
        Request req = new Request.Builder().url(url).get().build();
        Response response = null;
        try {
            response = client.newCall(req).execute();
            byte[] body = response.body().bytes();
            return jsonUtils.fromJsonBytes(body);
        } catch (Exception e) {
            throw new StacException("An error was encountered while executing search. " + e.getLocalizedMessage());
        } finally {
            if (null != response) {
                response.body().close();
            }
        }
    }

    private OkHttpClient.Builder configureToIgnoreCertificate(OkHttpClient.Builder builder) {

        try {

            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder;
    }

}
