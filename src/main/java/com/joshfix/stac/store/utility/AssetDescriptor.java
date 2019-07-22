package com.joshfix.stac.store.utility;

import lombok.Data;

/**
 * @author joshfix
 * Created on 2019-06-27
 */
@Data
public class AssetDescriptor {

    private String type;
    private String url;

    public AssetDescriptor type(String type) {
        setType(type);
        return this;
    }

    public AssetDescriptor url(String url) {
        setUrl(url);
        return this;
    }
}
