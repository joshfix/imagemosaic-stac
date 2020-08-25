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
    double minX = -180.0;
    double maxX = 180.0;
    double minY = -90.0;
    double maxY = 90.0;

    public String getUrlFilter() {
        return urlFilter;
    }

    public void setUrlFilter(String urlFilter) {
        this.urlFilter = urlFilter;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getStoreStacFilter() {
        return storeStacFilter;
    }

    public void setStoreStacFilter(String storeStacFilter) {
        this.storeStacFilter = storeStacFilter;
    }

    public String getLayerStacFilter() {
        return layerStacFilter;
    }

    public void setLayerStacFilter(String layerStacFilter) {
        this.layerStacFilter = layerStacFilter;
    }

    public double[] getResolutions() {
        return resolutions;
    }

    public void setResolutions(double[] resolutions) {
        this.resolutions = resolutions;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public int getMaxGranules() {
        return maxGranules;
    }

    public void setMaxGranules(int maxGranules) {
        this.maxGranules = maxGranules;
    }

    public int getMaxFeatures() {
        return maxFeatures;
    }

    public void setMaxFeatures(int maxFeatures) {
        this.maxFeatures = maxFeatures;
    }

    public boolean isUseBbox() {
        return useBbox;
    }

    public void setUseBbox(boolean useBbox) {
        this.useBbox = useBbox;
    }

    public String getAoiFilter() {
        return aoiFilter;
    }

    public void setAoiFilter(String aoiFilter) {
        this.aoiFilter = aoiFilter;
    }

    private double[] resolutions;
    private String collection;
    private int maxGranules = MAX_GRANULES_DEFAULT;
    private int maxFeatures = MAX_FEATURES_DEFAULT;
    private boolean useBbox = USE_BBOX_DEFAULT;
    private String aoiFilter = AOI_FILTER_DEFAULT;

    public static final int MAX_GRANULES_DEFAULT = 3;
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
        storeStacFilter = (String) params.get(StacDataStoreFactorySpi.STORE_STAC_FILTER.key);
        aoiFilter = (String) params.get(StacDataStoreFactorySpi.AOI_FILTER.key);
    }

}
