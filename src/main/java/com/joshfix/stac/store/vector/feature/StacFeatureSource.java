package com.joshfix.stac.store.vector.feature;

import com.joshfix.stac.store.LayerParameters;
import com.joshfix.stac.store.mosaic.StacMosaicReader;
import com.joshfix.stac.store.utility.*;
import com.joshfix.stac.store.vector.store.StacDataStore;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.FeatureListener;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.sort.SortedFeatureReader;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author joshfix
 */
@Slf4j
public abstract class StacFeatureSource implements SimpleFeatureSource {

    protected Name name;
    protected StacDataStore store;
    protected LayerParameters layerParameters;
    protected StacRestClient client;
    protected ReferencedEnvelope referencedEnvelope;
    protected static Map<String, SimpleFeatureType> featureTypeMap = new HashMap<>();
    protected QueryCapabilities queryCapabilities;
    protected static CoordinateReferenceSystem ENVELOPE_CRS = null;

    static {
        try {
            ENVELOPE_CRS = CRS.decode("EPSG:4326");
        } catch (FactoryException e) {
            // if this happens, we should all go home
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public StacFeatureSource(Name name, StacDataStore stacDataStore, StacRestClient client, LayerParameters layerParameters) {
        store = stacDataStore;
        this.client = client;
        this.layerParameters = layerParameters;
        this.name = new NameImpl(name.getNamespaceURI(), StacMosaicReader.TYPENAME);
    }

    @Override
    public StacFeatureCollection getFeatures() throws IOException {
        return getFeatures(Filter.INCLUDE);
    }

    @Override
    public StacFeatureCollection getFeatures(Filter filter) throws IOException {
        Query query = new Query();
        query.setFilter(filter);
        return getFeatures(query);
    }

    @Override
    public abstract StacFeatureCollection getFeatures(Query query) throws IOException;

    protected StacFeatureCollection getFeatureCollection(SearchRequest request) throws StacException {
        log.debug("STAC request: " + request);
        Set<Map> resultSet = buildResultSet(client.search(request));
        log.debug("Built collection with " + resultSet.size() + " items.");
        return new StacFeatureCollection(resultSet, getName(), layerParameters, getSchema());
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public ResourceInfo getInfo() {
        throw new UnsupportedOperationException();
    }

    @Override
    public StacDataStore getDataStore() {
        return store;
    }

    protected QueryCapabilities buildQueryCapabilities() {
        return new QueryCapabilities();
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        if (queryCapabilities == null) {
            queryCapabilities = buildQueryCapabilities();
        }

        return new QueryCapabilities() {
            public boolean isOffsetSupported() {
                // we always support offset since we support sorting
                return true;
            }

            public boolean supportsSorting(SortBy[] sortAttributes) {
                if (queryCapabilities.supportsSorting(sortAttributes)) {
                    // natively supported
                    return true;
                } else {
                    // check if we can use the merge-sort support
                    return SortedFeatureReader.canSort(getSchema(), sortAttributes);
                }
            }

            public boolean isReliableFIDSupported() {
                return queryCapabilities.isReliableFIDSupported();
            }

            public boolean isUseProvidedFIDSupported() {
                return queryCapabilities.isUseProvidedFIDSupported();
            }
        };
    }

    @Override
    public void addFeatureListener(FeatureListener listener) {
        // do nothing
    }

    @Override
    public void removeFeatureListener(FeatureListener listener) {
        // do nothing
    }

    @Override
    public SimpleFeatureType getSchema() {
        String itemType = layerParameters.getCollection();
        if (!featureTypeMap.containsKey(itemType)) {
            // TODO: get a schema directly from client
            SearchRequest req = new SearchRequest();
            req.setLimit(1);
            StacRequestBuilder.buildStacFilters(req, layerParameters);

            if (layerParameters.getCollection() != null) {
                req.setCollections(layerParameters.getCollection().split(","));
            }
            log.debug("Search STAC for schema item with request: " + req);

            try {
                ItemIterator itemIterator = client.searchStreaming(req);
                Set<Map> resultSet = buildResultSet(itemIterator);
                itemIterator.close();

                StacSchemaFeatureIterator it = new StacSchemaFeatureIterator(resultSet, getName(), layerParameters);
                featureTypeMap.put(itemType, it.next().getType());
            } catch (NoSuchElementException | StacException e) {
                throw new IllegalStateException(e);
            }

        }
        return featureTypeMap.get(itemType);
    }

    protected Set<Map> buildResultSet(ItemIterator itemIterator) {
        Set<Map> resultSet = new HashSet<>();
        while (itemIterator.hasNext()) {
            Map next = itemIterator.next();
            log.debug("Adding item to final set: " + next.get("id"));
            resultSet.add(next);
        }
        itemIterator.close();
        return resultSet;
    }

    protected Set<Map> buildResultSet(Map stacResponse) {
        Set<Map<String, Object>> resultSet = new HashSet((List) stacResponse.get("features"));
        List<String> ids = resultSet.stream()
                .map(item -> (String)item.get("id"))
                .collect(Collectors.toList());
        log.debug("Items: " + Arrays.toString(ids.toArray()));
        return new HashSet((List) stacResponse.get("features"));
    }

    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        if (null != referencedEnvelope) {
            return referencedEnvelope;
        }
        referencedEnvelope = new ReferencedEnvelope(-180, 180, -90, 90, ENVELOPE_CRS);
        return referencedEnvelope;

    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        return getFeatures(query).getBounds();
    }

    @Override
    public int getCount(Query query) throws IOException {
        return getFeatures(query).size();
    }

    @Override
    public Set<Key> getSupportedHints() {
        return Collections.emptySet();
    }


}
