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
public class StacMosaicVectorFeatureSource extends StacFeatureSource {

    @SuppressWarnings("unchecked")
    public StacMosaicVectorFeatureSource(Name name, StacDataStore stacDataStore, StacRestClient client, LayerParameters layerParameters) {
        super(name, stacDataStore, client, layerParameters);
    }

    @Override
    public StacFeatureCollection getFeatures(Query query) throws IOException {
        if (resultSet != null) {
            return new StacFeatureCollection(resultSet, getName(), layerParameters, getSchema());
        }

        SearchRequest request = initializeStacSearchRequest(query);
        String stacFilter = null;
        CqlWrapper cqlWrapper = getStacFilter(query);

        if (layerParameters.getCollection() != null) {
            request.setCollections(new String[]{layerParameters.getCollection()});
        }


        // if there are no CQL filters, layer filters, and we're requesting max features, it's likely geotools gathering
        // metadata before the layer is built, so there is no reason to make huge queries
        if (null == cqlWrapper && query.getMaxFeatures() == Integer.MAX_VALUE && layerParameters.getMaxGranules() == 0) {
            // && (layerParameters.getDefaultStacFilter() == null || layerParameters.getDefaultStacFilter().isEmpty())) {
            stacFilter = FILTER;
            request.setLimit(1);
        } else if (null != cqlWrapper) {
            // this means there is a query filter or a request for specific item IDs, so it should be a legit service request
            if (cqlWrapper.getFilter() != null && !cqlWrapper.getFilter().isBlank()) {
                request.setQuery(cqlWrapper.getFilter());
            }
            if (cqlWrapper.getIds() != null && !cqlWrapper.getIds().isBlank()) {
                request.setIds(cqlWrapper.getIds().split(","));
            }
            request.setLimit(layerParameters.getMaxGranules());

        } else if (request.getBbox() == null && request.getLimit() == layerParameters.getMaxFeatures()
                && (stacFilter == null || stacFilter.isEmpty())) {
            // DescribeFeature does not give us an id or any positional info, so if we have a request for max features and
            // no bounding box, just restrict it to a single image.  note this is not going to return valid results.
            request.setLimit(1);
        } else {
            request.setLimit(layerParameters.getMaxGranules());
        }

        if (layerParameters.getCollection() != null) {
            request.setCollections(new String[]{layerParameters.getCollection()});
        }

        stacFilter = addDefaultStacFilter(stacFilter);
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