package com.joshfix.stac.store.vector.factory;

import com.joshfix.stac.store.FieldNames;
import com.joshfix.stac.store.KeyNames;
import com.joshfix.stac.store.LayerParameters;
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

    public static final String DBTYPE_ID = "stac-store-feature-vector";
    public static final Param DBTYPE = new Param(KeyNames.DBTYPE, String.class,
            "Fixed value '" + DBTYPE_ID + "'", true, DBTYPE_ID,
            Collections.singletonMap(Parameter.LEVEL, "program"));

    public static final Param SERVICE_URL = new Param(KeyNames.SERVICE_URL, String.class,
            FieldNames.SERVICE_URL, true);

    public static final Param NAMESPACE = new Param(KeyNames.NAMESPACE, NameImpl.class,
            FieldNames.NAMESPACE, false, null, // not required
            new KVP(Param.LEVEL, "advanced"));

    public static final Param COLLECTION = new Param(KeyNames.COLLECTION, String.class, FieldNames.COLLECTION,
            true, LayerParameters.COLLECTION_DEFAULT, new KVP(Param.LEVEL, "advanced"));

    public static final Param STAC_CQL_FILTER = new Param(KeyNames.STORE_STAC_FILTER, String.class,
            FieldNames.STORE_STAC_FILTER, false, "", new KVP(Param.LEVEL, "advanced"));

    public static final Param AOI_FILTER = new Param(KeyNames.AOI_FILTER, String.class,
            FieldNames.AOI_FILTER, false, "", new KVP(Param.LEVEL, "advanced"));

    public static final Param MAX_FEATURES = new Param(KeyNames.MAX_FEATURES, Integer.class, FieldNames.MAX_FEATURES,
            true, LayerParameters.MAX_FEATURES_DEFAULT, new KVP(Param.LEVEL, "advanced"));

    public static final Param USE_BBOX = new Param(KeyNames.USE_BBOX, Boolean.class, FieldNames.USE_BBOX,
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
                AOI_FILTER,
                DBTYPE,
                NAMESPACE,
                SERVICE_URL,
                COLLECTION,
                STAC_CQL_FILTER,
                MAX_FEATURES,
                USE_BBOX,
        };
    }

    @Override
    public boolean canProcess(Map<String, Serializable> params) {
        try {
            return DBTYPE_ID.equals(DBTYPE.lookUp(params)) && SERVICE_URL.lookUp(params) != null;
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