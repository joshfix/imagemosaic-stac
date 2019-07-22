package com.joshfix.stac.store;

/**
 * @author joshfix
 * Created on 2019-05-22
 */
public interface FieldNames {

    String MOSAIC_STORE = "STAC Mosaic Store";
    String PARAM_FILTER = "Filter";
    String LAYER_STAC_FILTER = "STAC CQL filter";
    String STORE_STAC_FILTER = "STAC CQL filter";
    String USE_BBOX = "Include request BBOX in STAC request";
    String MAX_GRANULES = "Maximum number of granules to load";
    String MAX_FEATURES = "Maximum number of features to load";
    String SERVICE_URL = "Service URL";
    String NAMESPACE = "URI to a the namespace";
    String AOI_FILTER = "AOI";
    String COLLECTION = "STAC collection";
    String ASSET_ID = "Asset ID";
    String SAMPLE_ITEM_ID = "STAC sample item ID";
    String URL = "STAC URL";

}
