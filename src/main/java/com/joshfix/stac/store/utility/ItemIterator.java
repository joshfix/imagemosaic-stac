package com.joshfix.stac.store.utility;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * @author joshfix
 * Created on 2019-04-01
 */
@Slf4j
public class ItemIterator implements Iterator, Closeable {

    private final Response response;
    private final JsonUtils jsonUtils;
    private int count;

    public ItemIterator(Response response, JsonUtils jsonUtils) {
        this.response = response;
        this.jsonUtils = jsonUtils;
    }

    @Override
    public void close() {
        log.debug("Iterated over " + count + " items.");
        response.body().close();
    }

    @Override
    public boolean hasNext() {
        try {
            return !response.body().source().exhausted();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Map next() {
        try {
            String itemString = response.body().source().readUtf8Line();
            response.body().source().readUtf8Line(); // read blank line that follows each record
            itemString = itemString.substring(5);
            count++;
            Map item = jsonUtils.fromJson(itemString);
            return item;
        } catch (Exception e) {
            log.error("Error occurred acquiring the next item from the stream. ", e);
        }
        return null;
    }
}
