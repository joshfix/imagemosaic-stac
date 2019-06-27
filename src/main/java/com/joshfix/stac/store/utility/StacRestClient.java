package com.joshfix.stac.store.utility;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.net.URI;
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

        client = new OkHttpClient();
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
        byte[] requestBytes = jsonUtils.toJson(request);
        RequestBody rb = RequestBody.create(JSON, requestBytes);
        Request req = new Request.Builder().url(itemsUrlBuilder.build()).post(rb).build();
        log.debug("Executing search request to {}", req.url().toString());
        Response response = null;
        try {
            response = client.newCall(req).execute();
            byte[] body = response.body().bytes();
            return jsonUtils.itemCollectionFromJson(body);
        } catch (Exception e) {
            throw new StacException("An error was encountered while executing search. " + e.getLocalizedMessage());
        } finally {
            if (null != response) {
                response.body().close();
            }
        }
    }

    public byte[] searchBytes(SearchRequest request) throws StacException {
        log.debug("searchBytes method called. Request: " + request.toString());
        byte[] requestBytes = jsonUtils.toJson(request);
        RequestBody rb = RequestBody.create(JSON, requestBytes);
        Request req = new Request.Builder().url(itemsUrlBuilder.build()).post(rb).build();
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
        byte[] requestBytes = jsonUtils.toJson(request);
        RequestBody rb = RequestBody.create(JSON, requestBytes);
        Request req = new Request.Builder().url(itemsUrlBuilder.build()).post(rb).header("Accept", "text/event-stream").build();
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

    private boolean executeInternal(Request req) throws StacException {
        Response response = null;
        try {
            log.debug("Executing put request to {}", req.url().toString());
            response = client.newCall(req).execute();

            if (response.isSuccessful()) {
                log.debug("Add item succeeded");
                return true;
            }
            log.debug("Executing request failed");
            log.debug("Response from server: {}", response.body().string());
            log.debug("Response code from server: {}", response.code());
            return false;
        } catch (IOException ex) {
            throw new StacException("Error adding item: " + ex.getMessage());
        } finally {
            if (null != response) {
                response.body().close();
            }
        }
    }

}
