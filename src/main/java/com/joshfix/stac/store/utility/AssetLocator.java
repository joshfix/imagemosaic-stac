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

        Map assets = (Map) item.get("assets");
        Object assetObj = assets.get(assetId);
        return assetObj == null ? null : jsonUtils.getAsset(assetObj).getHref();
    }

    public static String getRandomAssetImageUrl(Map item) {
        if (null == item.get("assets")) {
            return null;
        }

        Map<String, Object> assets = (Map) item.get("assets");
        for (Map.Entry entry : assets.entrySet()) {
            Map<String, Object> asset = (Map) entry.getValue();

            if (asset.containsKey("type")) {

                String type = asset.get("type").toString();
                if (type.equalsIgnoreCase("image/vnd.stac.geotiff") ||
                        type.equalsIgnoreCase("image/x.geotiff") ||
                        type.toLowerCase().contains("geotiff")) {

                    return asset.get("href").toString();
                }

            }
        }
        return null;
    }

}
