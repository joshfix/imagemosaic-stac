package com.joshfix.stac.store.vector.factory;

import com.joshfix.stac.store.mosaic.LayerParameters;
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

    public StacVectorDataStoreFactorySpi() {
        super();
    }

    @Override
    public DataStore createDataStore(Map<String, Serializable> params) throws IOException {
        if (null == client && params != null && params.containsKey(SERVICE_URL_KEY)) {
            client = StacClientFactory.create((String) params.get(SERVICE_URL_KEY));
        }
        if (null == layerParameters) {
            layerParameters = new LayerParameters(params);
        }
        return new StacVectorDataStore((NameImpl) NAMESPACE.lookUp(params), client, layerParameters);
    }

    @Override
    public String getDisplayName() {
        return "STAC Vector Store";
    }

    @Override
    public String getDescription() {
        return "Allows publishing feature collection located on STAC service.";
    }

}