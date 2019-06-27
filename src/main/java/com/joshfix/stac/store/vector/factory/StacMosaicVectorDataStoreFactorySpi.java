package com.joshfix.stac.store.vector.factory;

import com.joshfix.stac.store.mosaic.LayerParameters;
import com.joshfix.stac.store.utility.StacRestClient;
import com.joshfix.stac.store.vector.store.StacMosaicVectorDataStore;
import org.geotools.data.DataStore;
import org.geotools.feature.NameImpl;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * @author joshfix
 */
public class StacMosaicVectorDataStoreFactorySpi extends StacDataStoreFactorySpi {

    public StacMosaicVectorDataStoreFactorySpi() {
        super();
    }

    public StacMosaicVectorDataStoreFactorySpi(StacRestClient client, LayerParameters layerParameters) {
        super(client, layerParameters);
    }

    @Override
    public DataStore createDataStore(Map<String, Serializable> params) throws IOException {
        return new StacMosaicVectorDataStore((NameImpl) NAMESPACE.lookUp(params), client, layerParameters);
    }

    @Override
    public String getDisplayName() {
        return "STAC Vector Store supporting STAC Mosaic Store";
    }

    @Override
    public String getDescription() {
        return "Backing vector store for STAC Mosaic Store.";
    }

}