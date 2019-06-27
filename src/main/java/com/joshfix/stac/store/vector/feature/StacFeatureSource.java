package com.joshfix.stac.store.vector.feature;

import com.joshfix.stac.store.mosaic.LayerParameters;
import com.joshfix.stac.store.utility.ItemIterator;
import com.joshfix.stac.store.utility.SearchRequest;
import com.joshfix.stac.store.utility.StacException;
import com.joshfix.stac.store.utility.StacRestClient;
import com.joshfix.stac.store.vector.store.StacDataStore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.FeatureListener;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.sort.SortedFeatureReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.gce.imagemosaic.Utils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.*;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.FactoryException;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.util.*;

import static com.joshfix.stac.store.mosaic.MosaicConfigurationProperties.DEFAULT_TYPENAME_VALUE;

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
    public static final String FILTER = null;
    protected QueryCapabilities queryCapabilities;
    protected Set<Map> resultSet;

    @SuppressWarnings("unchecked")
    public StacFeatureSource(Name name, StacDataStore stacDataStore, StacRestClient client, LayerParameters layerParameters) {
        store = stacDataStore;
        this.client = client;
        this.layerParameters = layerParameters;
        this.name = new NameImpl(name.getNamespaceURI(), DEFAULT_TYPENAME_VALUE);
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

            if (layerParameters.getCollection() != null) {
                req.setCollections(layerParameters.getCollection().split(","));
            } else {
                req.setQuery(FILTER);
            }
            log.debug("Search STAC for schema item with request: " + req);

            try {
                ItemIterator itemIterator = client.searchStreaming(req);
                resultSet = buildResultSet(itemIterator);
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

    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        if (null != referencedEnvelope) {
            return referencedEnvelope;
        }

        try {
            referencedEnvelope = new ReferencedEnvelope(-180, 180, -90, 90, CRS.decode("EPSG:4326"));
            return referencedEnvelope;
        } catch (FactoryException e) {
            throw new IOException(e);
        }
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

    protected SearchRequest initializeStacSearchRequest(Query query) {
        SearchRequest request = new SearchRequest();
        buildStacRequestLimit(query, request);

        if (query.getStartIndex() != null) {
            request.setPage(query.getStartIndex());
        }

        buildStacRequestBbox(query, request);
        return request;
    }

    protected CqlWrapper getStacFilter(Query query) {
        Filter filter = query.getFilter();
        //String stacFilter = "";

        if (filter instanceof PropertyIsEqualTo) {
            PropertyIsEqualTo eq = (PropertyIsEqualTo) filter;
            return getStacFilter(eq);
        } else if (filter instanceof BinaryLogicOperator) {
            for (Filter child : ((BinaryLogicOperator) filter).getChildren()) {
                if (child instanceof PropertyIsEqualTo) {
                    PropertyIsEqualTo eq = (PropertyIsEqualTo) child;
                    return getStacFilter(eq);
                }
            }
        }
        return null;
    }

    @Data
    static class CqlWrapper {
        private String filter;
        private String ids;
    }

    protected CqlWrapper getStacFilter(PropertyIsEqualTo filter) {
        CqlWrapper cqlWrapper = new CqlWrapper();
        if (filter.getExpression1() instanceof PropertyName && filter.getExpression2() instanceof Literal) {
            if (((PropertyName) filter.getExpression1()).getPropertyName().equalsIgnoreCase("stacFilter")) {
                cqlWrapper.setFilter(((Literal) filter.getExpression2()).getValue().toString());
            }
            if (((PropertyName) filter.getExpression1()).getPropertyName().equalsIgnoreCase("ids")) {
                cqlWrapper.setIds(((Literal) filter.getExpression2()).getValue().toString());
            }
        }
        return cqlWrapper;
    }

    protected void buildStacRequestBbox(Query query, SearchRequest request) {
        if (layerParameters.isUseBbox()) {
            Utils.BBOXFilterExtractor bboxExtractor = new Utils.BBOXFilterExtractor();
            query.getFilter().accept(bboxExtractor, null);
            if (bboxExtractor.getBBox() != null) {
                request.setBbox(
                        new double[]{
                                Math.max(bboxExtractor.getBBox().getMinX(), -180.0),
                                Math.max(bboxExtractor.getBBox().getMinY(), -90.0),
                                Math.min(bboxExtractor.getBBox().getMaxX(), 180.0),
                                Math.min(bboxExtractor.getBBox().getMaxY(), 90.0)
                        });
            }
        }
    }

    protected String addDefaultStacFilter(String stacFilter) {
        if (layerParameters.getDefaultStacFilter() != null && !layerParameters.getDefaultStacFilter().isEmpty()) {
            String defaultStacFilter = layerParameters.getDefaultStacFilter();
            return (stacFilter == null || stacFilter.isEmpty()) ? defaultStacFilter : defaultStacFilter + " AND " + stacFilter;
        }
        return stacFilter;
    }

    /*
        protected String addCollectionFilter(String stacFilter) {
            if (null != layerParameters.getCollection() && !layerParameters.getCollection().isEmpty()) {
                String itemTypeFilter = "properties.type=" + layerParameters.getCollection();
                return (stacFilter == null || stacFilter.isEmpty()) ? itemTypeFilter : itemTypeFilter + " AND " + stacFilter;
            }
            return stacFilter;
        }
    */
    protected void buildStacRequestLimit(Query query, SearchRequest request) {
        if (query.getMaxFeatures() < Integer.MAX_VALUE) {
            request.setLimit(query.getMaxFeatures());
        }
        if (query.getMaxFeatures() == Integer.MAX_VALUE) {
            request.setLimit(layerParameters.getMaxFeatures());
        }
    }


    protected Filter createWhitelistedFilters(Query query) {
        FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        FilterVisitor blacklistingFilterVisitor = new FilteringFilterVisitor();

        return (Filter) query.getFilter().accept(blacklistingFilterVisitor, null);
    }

    /**
     * Responsible for "filtering" the filter in order to remove unsupported filter operations. Presently this largely
     * means removing BBOX from requests from the image store (these are handled elsewhere). Unfortunately since CQL
     * (the elastic search version that is) does not support any form of identity or tautological filter, so we need
     * to tear apart ands/ors/etc. in order to remove BBOX and then use a single filter if need be.
     */
    protected class FilteringFilterVisitor extends DuplicatingFilterVisitor {
        @Override
        public Object visit(BBOX filter, Object extraData) {
            return null;
        }

        @Override
        public Object visit(And filter, Object extraData) {
            List<Filter> children = filter.getChildren();
            List<Filter> newChildren = new ArrayList<>();
            for (Iterator<Filter> iter = children.iterator(); iter.hasNext(); ) {
                Filter child = iter.next();
                if (child != null) {
                    Filter newChild = (Filter) child.accept(this, extraData);
                    if (newChild != null) {
                        newChildren.add(newChild);
                    }
                }
            }

            if (newChildren.size() > 1) {
                return getFactory(extraData).and(newChildren);
            } else {
                return newChildren.get(0);
            }
        }
    }

}
