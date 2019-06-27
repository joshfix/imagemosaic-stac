package com.joshfix.stac.store.vector.feature;

import com.joshfix.stac.store.mosaic.LayerParameters;
import com.joshfix.stac.store.utility.ItemIterator;
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
        if (resultSet != null) {
            return new StacFeatureCollection(resultSet, getName(), layerParameters, getSchema());
        }

        SearchRequest request = initializeStacSearchRequest(query);

        if (request.getLimit() == 0) {
            request.setLimit(layerParameters.getMaxFeatures());
        }

        String stacFilter = null;
        CqlWrapper cqlWrapper = getStacFilter(query);

        // if there are no CQL filters, layer filters, and we're requesting max features, it's likely geotools gathering
        // metadata before the layer is built, so there is no reason to make huge queries
        if (null == cqlWrapper && query.getMaxFeatures() == Integer.MAX_VALUE
                && (layerParameters.getDefaultStacFilter() == null || layerParameters.getDefaultStacFilter().isEmpty() ||
                layerParameters.getCollection() == null || layerParameters.getCollection().isBlank())) {
            stacFilter = FILTER;
            // setting the limit to 1 or 2 always results in some invalid width exception. for some reason setting this to 3 works
            request.setLimit(1);
        } else if (null != cqlWrapper && cqlWrapper.getFilter() != null && !cqlWrapper.getFilter().isBlank()) {
            request.setQuery(cqlWrapper.getFilter());

            if (cqlWrapper.getIds() != null && !cqlWrapper.getIds().isBlank()) {
                request.setIds(cqlWrapper.getIds().split(","));
            }
            request.setLimit(1);
        } else {
            request.setLimit(layerParameters.getMaxFeatures());
        }

        if (layerParameters.getCollection() != null) {
            request.setCollections(new String[]{layerParameters.getCollection()});
        }
        request.setQuery(stacFilter);
        log.debug("STAC request: " + request);

        try {
            ItemIterator itemIterator = client.searchStreaming(request);
            resultSet = buildResultSet(itemIterator);
            return new StacFeatureCollection(resultSet, getName(), layerParameters, getSchema());
        } catch (StacException e) {
            throw new IOException(e);
        }
    }

}
