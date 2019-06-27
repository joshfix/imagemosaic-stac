package com.joshfix.stac.store.vector.store;

import com.joshfix.stac.store.mosaic.LayerParameters;
import com.joshfix.stac.store.utility.StacRestClient;
import com.joshfix.stac.store.vector.feature.StacMosaicVectorFeatureSource;
import org.opengis.feature.type.Name;

/**
 * @author joshfix
 */
public class StacMosaicVectorDataStore extends StacDataStore {

    public StacMosaicVectorDataStore(Name namespace, StacRestClient client, LayerParameters layerParameters) {
    	super(namespace);
		singleSource = new StacMosaicVectorFeatureSource(namespace,this, client, layerParameters);
    }

}
