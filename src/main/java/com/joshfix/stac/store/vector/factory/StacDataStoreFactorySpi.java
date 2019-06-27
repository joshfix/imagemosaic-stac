package com.joshfix.stac.store.vector.factory;

import com.joshfix.stac.store.FieldNames;
import com.joshfix.stac.store.KeyNames;
import com.joshfix.stac.store.mosaic.LayerParameters;
import com.joshfix.stac.store.utility.StacRestClient;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.util.KVP;

import java.awt.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * @author joshfix
 */
public abstract class StacDataStoreFactorySpi implements DataStoreFactorySpi {

    protected StacRestClient client;
    protected LayerParameters layerParameters;

    public static final String DBTYPE_STRING = "stac-store-feature-vector";
    public static final Param DBTYPE = new Param(KeyNames.DBTYPE_KEY, String.class,
            "Fixed value '" + DBTYPE_STRING + "'", true, DBTYPE_STRING,
            Collections.singletonMap(Parameter.LEVEL, "program"));

    public static final Param SERVICE_URL = new Param(KeyNames.SERVICE_URL_KEY, String.class,
            "Service URL", true);

    public static final Param NAMESPACE = new Param(KeyNames.NAMESPACE_KEY, NameImpl.class,
            "uri to a the namespace", false, null, // not required
            new KVP(Param.LEVEL, "advanced"));

    public static final Param COLLECTION = new Param(KeyNames.COLLECTION_KEY, String.class, FieldNames.COLLECTION_NAME,
            true, LayerParameters.COLLECTION_DEFAULT, new KVP(Param.LEVEL, "advanced"));

    public static final Param STAC_QUERY = new Param(KeyNames.STAC_QUERY_KEY, String.class,
            FieldNames.STAC_QUERY_NAME, false, "", new KVP(Param.LEVEL, "advanced"));

    public static final Param ASSET_ID = new Param(KeyNames.ASSET_ID_KEY, String.class,
            FieldNames.ASSET_ID_NAME, false, "", new KVP(Param.LEVEL, "advanced"));

    public static final Param MAX_FEATURES = new Param(KeyNames.MAX_FEATURES_KEY, Integer.class, FieldNames.MAX_FEATURES_NAME,
            true, LayerParameters.MAX_FEATURES_DEFAULT, new KVP(Param.LEVEL, "advanced"));

    public static final Param USE_BBOX = new Param(KeyNames.USE_BBOX_KEY, Boolean.class, FieldNames.USE_BBOX_NAME,
            true, LayerParameters.USE_BBOX_DEFAULT, new KVP(Param.LEVEL, "advanced"));

    public StacDataStoreFactorySpi() {}

    public StacDataStoreFactorySpi(StacRestClient client, LayerParameters layerParameters) {
        this.client = client;
        this.layerParameters = layerParameters;
    }

    @Override
    public Map<RenderingHints.Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }

    @Override
    public abstract DataStore createDataStore(Map<String, Serializable> params) throws IOException;

    @Override
    public abstract String getDisplayName();

    @Override
    public String getDescription() {
        return "Allows publishing feature collection located on STAC service.";
    }

    @Override
    public Param[] getParametersInfo() {
        return new Param[]{
                ASSET_ID,
                DBTYPE,
                NAMESPACE,
                SERVICE_URL,
                COLLECTION,
                STAC_QUERY,
                MAX_FEATURES,
                USE_BBOX,
        };
    }

    @Override
    public boolean canProcess(Map<String, Serializable> params) {
        try {
            return DBTYPE_STRING.equals(DBTYPE.lookUp(params)) && SERVICE_URL.lookUp(params) != null;
        } catch (Exception e) {}
        return false;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        return createDataStore(params);
    }


}