package com.joshfix.stac.store.utility;

import java.util.Map;

/**
 * @author joshfix
 * Created on 2/20/18
 */
public class AssetLocator {

    private static JsonUtils jsonUtils = new JsonUtils();

    public static String getAssetImageUrl(Map item, String assetId) {
        if (null == item.get("assets")) {
            return null;
        }

        Map assets = (Map)item.get("assets");
        Object assetObj = assets.get(assetId);
        return assetObj == null ? null : jsonUtils.getAsset(assetObj).getHref();

    }

}
