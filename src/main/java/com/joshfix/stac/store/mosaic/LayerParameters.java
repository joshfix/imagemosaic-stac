package com.joshfix.stac.store.mosaic;

import com.joshfix.stac.store.vector.factory.StacDataStoreFactorySpi;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;

import java.io.Serializable;
import java.util.Map;

/**
 * @author joshfix
 * Created on 2019-04-04
 */
@Data
@NoArgsConstructor
public class LayerParameters {

    private String layerQuery;
    private String urlFilter;
    private String assetId = "B2";
    private String collection = COLLECTION_DEFAULT;
    private int maxGranules;
    private int maxFeatures = MAX_FEATURES_DEFAULT;
    private int gridWidth = GRID_WIDTH_DEFAULT;
    private int gridHeight = GRID_HEIGHT_DEFAULT;
    private boolean useBbox = USE_BBOX_DEFAULT;
    private double maxResolutionPixelSizeX = MAX_RESOLUTION_X_DEFAULT;
    private double maxResolutionPixelSizeY = MAX_RESOLUTION_Y_DEFAULT;

    public static final int GRID_WIDTH_DEFAULT = 4096;
    public static final int GRID_HEIGHT_DEFAULT = 2048;
    public static final int MAX_GRANULES_DEFAULT = 3;
    public static final int MAX_FEATURES_DEFAULT = 10000;
    public static final double MAX_RESOLUTION_X_DEFAULT = 360.0 / (double) GRID_WIDTH_DEFAULT;
    public static final double MAX_RESOLUTION_Y_DEFAULT = 180.0 / (double) GRID_HEIGHT_DEFAULT;
    public static final boolean USE_BBOX_DEFAULT = true;
    public static final String COLLECTION_DEFAULT = "landsat-8-l1";

    public LayerParameters(GeneralParameterValue[] parameters) {

        for (GeneralParameterValue parameter : parameters) {
            if (parameter.getDescriptor() != null && parameter.getDescriptor().getName().getCode().equals(StacMosaicFormat.STAC_FILTER.getName().getCode())) {
                this.layerQuery = (String) ((DefaultParameterDescriptor) parameter.getDescriptor()).getDefaultValue();
            } else if (parameter.getDescriptor() != null && parameter.getDescriptor().getName().getCode().equals(StacMosaicFormat.ASSET_ID.getName().getCode())) {
                this.assetId = (String) ((DefaultParameterDescriptor) parameter.getDescriptor()).getDefaultValue();
            } else if (parameter.getDescriptor() != null && parameter.getDescriptor().getName().getCode().equals(StacMosaicFormat.USE_BBOX.getName().getCode())) {
                useBbox = (boolean) ((DefaultParameterDescriptor) parameter.getDescriptor()).getDefaultValue();
            } else if (parameter.getDescriptor() != null && parameter.getDescriptor().getName().getCode().equals(StacMosaicFormat.MAX_GRANULES.getName().getCode())) {
                maxGranules = (int) ((DefaultParameterDescriptor) parameter.getDescriptor()).getDefaultValue();
            } else if (parameter.getDescriptor() != null && parameter.getDescriptor().getName().getCode().equals(StacMosaicFormat.MAX_RESOLUTION_PIXEL_SIZE_X.getName().getCode())) {
                maxResolutionPixelSizeX = (double) ((DefaultParameterDescriptor) parameter.getDescriptor()).getDefaultValue();
            } else if (parameter.getDescriptor() != null && parameter.getDescriptor().getName().getCode().equals(StacMosaicFormat.MAX_RESOLUTION_PIXEL_SIZE_Y.getName().getCode())) {
                maxResolutionPixelSizeY = (double) ((DefaultParameterDescriptor) parameter.getDescriptor()).getDefaultValue();
            //} else if (parameter.getDescriptor() != null && parameter.getDescriptor().getName().getCode().equals(StacMosaicFormat.COLLECTION.getName().getCode())) {
            //    collection = (String) ((DefaultParameterDescriptor) parameter.getDescriptor()).getDefaultValue();
            } else if (parameter.getDescriptor() != null && parameter.getDescriptor().getName().getCode().equals(StacMosaicFormat.GRID_WIDTH.getName().getCode())) {
                gridWidth = (int) ((DefaultParameterDescriptor) parameter.getDescriptor()).getDefaultValue();
            } else if (parameter.getDescriptor() != null && parameter.getDescriptor().getName().getCode().equals(StacMosaicFormat.GRID_HEIGHT.getName().getCode())) {
                gridHeight = (int) ((DefaultParameterDescriptor) parameter.getDescriptor()).getDefaultValue();
            }
        }
    }

    public LayerParameters(Map<String, Serializable> params) {
        layerQuery = (String) params.get(StacDataStoreFactorySpi.STAC_QUERY.key);
        useBbox = (boolean) params.get(StacDataStoreFactorySpi.USE_BBOX.key);
        collection = (String) params.get(StacDataStoreFactorySpi.COLLECTION.key);
        maxFeatures = Integer.valueOf((String) params.get(StacDataStoreFactorySpi.MAX_FEATURES.key));
    }

}
