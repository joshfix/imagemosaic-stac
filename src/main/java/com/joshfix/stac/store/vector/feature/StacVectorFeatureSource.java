package com.joshfix.stac.store.vector.feature;

import com.joshfix.stac.store.LayerParameters;
import com.joshfix.stac.store.utility.SearchRequest;
import com.joshfix.stac.store.utility.StacException;
import com.joshfix.stac.store.utility.StacRestClient;
import com.joshfix.stac.store.vector.store.StacDataStore;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.Query;
import org.opengis.feature.type.Name;

import java.io.IOException;


/**
 * @author joshfix
 */
@Slf4j
public class StacVectorFeatureSource extends StacFeatureSource {

    @SuppressWarnings("unchecked")
    public StacVectorFeatureSource(Name name, StacDataStore stacDataStore, StacRestClient client, LayerParameters layerParameters) {
        super(name, stacDataStore, client, layerParameters);
    }

    @Override
    public StacFeatureCollection getFeatures(Query query) throws IOException {
        SearchRequest request = StacRequestBuilder.buildVectorRequest(query, layerParameters);
        try {
            return getFeatureCollection(request);
        } catch (StacException e) {
            throw new IOException(e);
        }

    }

}
