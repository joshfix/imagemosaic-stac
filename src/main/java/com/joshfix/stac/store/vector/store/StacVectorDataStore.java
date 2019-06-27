package com.joshfix.stac.store.vector.store;

import com.joshfix.stac.store.mosaic.LayerParameters;
import com.joshfix.stac.store.utility.StacRestClient;
import com.joshfix.stac.store.vector.feature.StacVectorFeatureSource;
import org.opengis.feature.type.Name;

/**
 * @author joshfix
 */
public class StacVectorDataStore extends StacDataStore {

    public StacVectorDataStore(Name namespace, StacRestClient client, LayerParameters layerParameters) {
    	super(namespace);
        singleSource = new StacVectorFeatureSource(namespace,this, client, layerParameters);
    }

}
