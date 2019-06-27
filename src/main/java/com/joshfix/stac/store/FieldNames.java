package com.joshfix.stac.store;

/**
 * @author joshfix
 * Created on 2019-05-22
 */
public interface FieldNames {

    String MOSAIC_STORE_NAME = "STAC Mosaic Store";
    String PARAM_FILTER_NAME = "Filter";
    String STAC_FILTER_NAME = "STAC query filter";
    String USE_BBOX_NAME = "Include request BBOX in STAC request";
    String MAX_GRANULES_NAME = "Maximum number of granules to load";
    String MAX_RESOLUTION_PIXEL_SIZE_X_NAME = "Maximum resolution pixel size (X)";
    String MAX_RESOLUTION_PIXEL_SIZE_Y_NAME = "Maximum resolution pixel size (Y)";
    String ITEM_TYPE_NAME = "Item type";
    String GRID_WIDTH_NAME = "Grid width (pixels)";
    String GRID_HEIGHT_NAME = "Grid height (pixels)";
    String MAX_FEATURES_NAME = "Maximum number of features to load";
    String SERVICE_URL_NAME = "Service URL";
    String NAMESPACE_NAME = "URI to a the namespace";
    String COLLECTION_NAME = "STAC collection";
    String ASSET_ID_NAME = "Asset ID";

}
