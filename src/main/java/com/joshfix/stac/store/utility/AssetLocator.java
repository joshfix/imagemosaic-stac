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

    public static AssetDescriptor getRandomAssetImageUrl(Map item) {
        if (null == item.get("assets")) {
            return null;
        }

        Map<String, Object> assets = (Map) item.get("assets");
        for (Map.Entry entry : assets.entrySet()) {
            Map<String, Object> asset = (Map) entry.getValue();

            if (asset.containsKey("type")) {

                String type = asset.get("type").toString().toLowerCase();
                if (type.equals("image/vnd.stac.geotiff") ||
                        type.equals("image/x.geotiff") ||
                        type.contains("geotiff") ||
                        type.equals("image/jp2")) {

                    AssetDescriptor assetDescriptor = new AssetDescriptor();
                    assetDescriptor.setType(type);
                    assetDescriptor.setUrl(asset.get("href").toString());
                    return assetDescriptor;
                }

            }
        }
        return null;
    }

}
