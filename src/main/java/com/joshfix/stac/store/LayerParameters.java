package com.joshfix.stac.store;

import com.joshfix.stac.store.mosaic.StacMosaicFormat;
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

    private String urlFilter;
    private String assetId;
    private String storeStacFilter;
    private String layerStacFilter;
    private double[] resolutions;
    private String collection;
    private int maxGranules = MAX_GRANULES_DEFAULT;
    private int maxFeatures = MAX_FEATURES_DEFAULT;
    private boolean useBbox = USE_BBOX_DEFAULT;
    private String aoiFilter = AOI_FILTER_DEFAULT;

    public static final int MAX_GRANULES_DEFAULT = 200;
    public static final int MAX_FEATURES_DEFAULT = 10000;
    public static final boolean USE_BBOX_DEFAULT = true;
    public static final String AOI_FILTER_DEFAULT = "-180.0, -90.0, 180.0, 90.0";
    public static final String COLLECTION_DEFAULT = "";

    public LayerParameters(GeneralParameterValue[] parameters, String storeStacFilter) {
        this.storeStacFilter = storeStacFilter;

        for (GeneralParameterValue parameter : parameters) {
            if (parameter.getDescriptor() != null && parameter.getDescriptor().getName().getCode().equals(StacMosaicFormat.LAYER_STAC_FILTER.getName().getCode())) {
                this.layerStacFilter = (String) ((DefaultParameterDescriptor) parameter.getDescriptor()).getDefaultValue();
            } else if (parameter.getDescriptor() != null && parameter.getDescriptor().getName().getCode().equals(StacMosaicFormat.ASSET_ID.getName().getCode())) {
                this.assetId = (String) ((DefaultParameterDescriptor) parameter.getDescriptor()).getDefaultValue();
            } else if (parameter.getDescriptor() != null && parameter.getDescriptor().getName().getCode().equals(StacMosaicFormat.USE_BBOX.getName().getCode())) {
                useBbox = (boolean) ((DefaultParameterDescriptor) parameter.getDescriptor()).getDefaultValue();
            } else if (parameter.getDescriptor() != null && parameter.getDescriptor().getName().getCode().equals(StacMosaicFormat.MAX_GRANULES.getName().getCode())) {
                maxGranules = (int) ((DefaultParameterDescriptor) parameter.getDescriptor()).getDefaultValue();
            } else if (parameter.getDescriptor() != null && parameter.getDescriptor().getName().getCode().equals(StacMosaicFormat.AOI_FILTER.getName().getCode())) {
                aoiFilter = (String) ((DefaultParameterDescriptor) parameter.getDescriptor()).getDefaultValue();
            }
        }
    }

    public LayerParameters(Map<String, Serializable> params) {
        useBbox = (boolean) params.get(StacDataStoreFactorySpi.USE_BBOX.key);
        collection = (String) params.get(StacDataStoreFactorySpi.COLLECTION.key);
        maxFeatures = Integer.valueOf((String) params.get(StacDataStoreFactorySpi.MAX_FEATURES.key));
        storeStacFilter = (String) params.get(StacDataStoreFactorySpi.STAC_CQL_FILTER.key);
        aoiFilter = (String) params.get(StacDataStoreFactorySpi.AOI_FILTER.key);
    }

}
