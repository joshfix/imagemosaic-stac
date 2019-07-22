package com.joshfix.stac.store.vector.factory;

import com.joshfix.stac.store.KeyNames;
import com.joshfix.stac.store.LayerParameters;
import com.joshfix.stac.store.utility.StacClientFactory;
import com.joshfix.stac.store.vector.store.StacVectorDataStore;
import org.geotools.data.DataStore;
import org.geotools.feature.NameImpl;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * @author joshfix
 */
public class StacVectorDataStoreFactorySpi extends StacDataStoreFactorySpi {

    public static final String DISPLAY_NAME = "STAC Vector Store";

    public StacVectorDataStoreFactorySpi() {
        super();
    }

    @Override
    public DataStore createDataStore(Map<String, Serializable> params) throws IOException {
        if (null == client && params != null && params.containsKey(KeyNames.SERVICE_URL)) {
            client = StacClientFactory.create((String) params.get(KeyNames.SERVICE_URL));
        }
        if (null == layerParameters) {
            layerParameters = new LayerParameters(params);
        }
        return new StacVectorDataStore((NameImpl) NAMESPACE.lookUp(params), client, layerParameters);
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public String getDescription() {
        return "Allows publishing feature collection located on STAC service.";
    }

}