package com.joshfix.stac.store.vector.feature;

import com.joshfix.stac.store.mosaic.LayerParameters;
import com.joshfix.stac.store.utility.SearchRequest;
import com.joshfix.stac.store.utility.StacException;
import com.joshfix.stac.store.utility.StacRestClient;
import com.joshfix.stac.store.vector.store.StacDataStore;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.Query;
import org.opengis.feature.type.Name;

import java.io.IOException;
import java.util.HashSet;


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
        CqlFilter cqlFilter = parseCqlFilters(query);

        // if there are no CQL filters, layer filters, and we're requesting max features, it's likely geotools gathering
        // metadata before the layer is built, so there is no reason to make huge queries
        if (null == cqlFilter && query.getMaxFeatures() == Integer.MAX_VALUE
                && (layerParameters.getLayerQuery() == null || layerParameters.getLayerQuery().isEmpty() ||
                layerParameters.getCollection() == null || layerParameters.getCollection().isBlank())) {
            //stacFilter = QUERY;
            // setting the limit to 1 or 2 always results in some invalid width exception. for some reason setting this to 3 works
            resultSet = new HashSet<>();
            return new StacFeatureCollection(resultSet, getName(), layerParameters, getSchema());
        } else if (null != cqlFilter && cqlFilter.getQuery() != null && !cqlFilter.getQuery().isBlank()) {
            request.setQuery(cqlFilter.getQuery());

            if (cqlFilter.getIds() != null && !cqlFilter.getIds().isBlank()) {
                request.setIds(cqlFilter.getIds().split(","));
            }
            resultSet = new HashSet<>();
            return new StacFeatureCollection(resultSet, getName(), layerParameters, getSchema());
        } else {
            request.setLimit(layerParameters.getMaxFeatures());
        }

        if (layerParameters.getCollection() != null) {
            request.setCollections(new String[]{layerParameters.getCollection()});
        }
        request.setQuery(stacFilter);
        try {
            return getFeatureCollection(request);
        } catch (StacException e) {
            throw new IOException(e);
        }
    }

}
