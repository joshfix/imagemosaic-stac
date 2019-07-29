package com.joshfix.stac.store.vector.feature;

import com.joshfix.stac.store.LayerParameters;
import com.joshfix.stac.store.utility.EnvelopeUtils;
import com.joshfix.stac.store.utility.SearchRequest;
import com.joshfix.stac.store.vector.feature.CqlFilter;
import org.geotools.data.Query;
import org.geotools.filter.AndImpl;
import org.geotools.filter.IsEqualsToImpl;
import org.geotools.gce.imagemosaic.Utils;
import org.locationtech.jts.geom.Envelope;
import org.opengis.filter.BinaryLogicOperator;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

import java.util.List;

/**
 * @author joshfix
 * Created on 2019-07-22
 */
public class StacRequestBuilder {

    public static SearchRequest buildMosaicRequest(Query query, LayerParameters layerParameters) {
        SearchRequest request = new SearchRequest();
        buildStacMosaicRequestLimit(query, request, layerParameters);
        buildRequest(query, request, layerParameters);
        return request;
    }

    public static SearchRequest buildVectorRequest(Query query, LayerParameters layerParameters) {
        SearchRequest request = new SearchRequest();
        buildStacVectorRequestLimit(query, request, layerParameters);
        buildRequest(query, request, layerParameters);
        return request;
    }

    private static void buildRequest(Query query, SearchRequest request, LayerParameters layerParameters) {
        if (query.getStartIndex() != null) {
            request.setPage(query.getStartIndex());
        }

        if (layerParameters.isUseBbox()) {
            buildStacRequestBbox(query, request, layerParameters);
        }

        getRequestCqlFilter(query, request);
        buildStacFilters(request, layerParameters);
    }


    public static void buildStacVectorRequestLimit(Query query, SearchRequest request, LayerParameters layerParameters) {
        if (query.getMaxFeatures() < Integer.MAX_VALUE) {
            request.setLimit(query.getMaxFeatures());
        } else if (query.getMaxFeatures() == Integer.MAX_VALUE) {
            request.setLimit(layerParameters.getMaxFeatures());
        }

        if (request.getLimit() == 0) {
            request.setLimit(layerParameters.getMaxFeatures());
        }
    }

    public static void buildStacMosaicRequestLimit(Query query, SearchRequest request, LayerParameters layerParameters) {
        if (query.getMaxFeatures() < Integer.MAX_VALUE) {
            request.setLimit(query.getMaxFeatures());
        } else if (query.getMaxFeatures() == Integer.MAX_VALUE) {
            request.setLimit(layerParameters.getMaxGranules());
        }

        if (request.getLimit() == 0) {
            request.setLimit(layerParameters.getMaxGranules());
        }
    }

    public static void buildStacFilters(SearchRequest request, LayerParameters layerParameters) {
        String stacQuery = request.getQuery();

        String storeStacFilter = layerParameters.getStoreStacFilter();
        if (storeStacFilter != null && !storeStacFilter.isBlank()) {
            stacQuery = (stacQuery == null || stacQuery.isBlank())
                    ? storeStacFilter
                    : storeStacFilter + " AND " + stacQuery;
        }

        String layerStacFilter = layerParameters.getLayerStacFilter();
        if (layerStacFilter != null && !layerStacFilter.isBlank()) {
            stacQuery = (stacQuery == null || stacQuery.isBlank())
                    ? layerStacFilter
                    : layerStacFilter + " AND " + stacQuery;
        }

        request.setQuery(stacQuery);
    }

    public static void buildStacRequestBbox(Query query, SearchRequest request, LayerParameters layerParameters) {
        Utils.BBOXFilterExtractor bboxExtractor = new Utils.BBOXFilterExtractor();
        query.getFilter().accept(bboxExtractor, null);

        if (bboxExtractor.getBBox() == null) {
            return;
        }

        Envelope aoiEnvelope = EnvelopeUtils.buildEnvelope(layerParameters.getAoiFilter());
        Envelope requestEnvelope = EnvelopeUtils.buildEnvelope(bboxExtractor);
        request.setBbox(EnvelopeUtils.getbbox(aoiEnvelope.intersection(requestEnvelope)));
    }

    // TODO: this is a poor implementation of parsing CQL query.  At the time of this comment, this plugin only supports
    // `ids` and `query` parameters, but in the future this should be able to be expanded.
    public static void getRequestCqlFilter(Query query, SearchRequest request) {
        CqlFilter cqlFilter = new CqlFilter();
        Filter filter = query.getFilter();

        if (filter instanceof PropertyIsEqualTo) {
            PropertyIsEqualTo eq = (PropertyIsEqualTo) filter;
            getRequestCqlFilter(eq, cqlFilter);
        } else if (filter instanceof BinaryLogicOperator) {
            List<Filter> childFilters = ((BinaryLogicOperator) filter).getChildren();
            for (Filter childFilter : childFilters) {
                if (childFilter instanceof AndImpl) {
                    for (Object child : ((AndImpl) childFilter).getChildren()) {
                        if (child instanceof IsEqualsToImpl) {
                            getRequestCqlFilter((IsEqualsToImpl) child, cqlFilter);
                        }
                    }
                } else if (childFilter instanceof IsEqualsToImpl) {
                    getRequestCqlFilter((IsEqualsToImpl) childFilter, cqlFilter);
                }
            }
        }

        if (cqlFilter.getQuery() != null && !cqlFilter.getQuery().isBlank()) {
            request.setQuery(cqlFilter.getQuery());
        }
        if (cqlFilter.getIds() != null && !cqlFilter.getIds().isBlank()) {
            request.setIds(cqlFilter.getIds().split(","));
        }
    }

    public static CqlFilter getRequestCqlFilter(PropertyIsEqualTo filter, CqlFilter cqlFilter) {
        if (cqlFilter == null) {
            cqlFilter = new CqlFilter();
        }
        if (filter.getExpression1() instanceof PropertyName && filter.getExpression2() instanceof Literal) {
            if (((PropertyName) filter.getExpression1()).getPropertyName().equalsIgnoreCase("query")) {
                cqlFilter.setQuery(((Literal) filter.getExpression2()).getValue().toString());
            }
            if (((PropertyName) filter.getExpression1()).getPropertyName().equalsIgnoreCase("ids")) {
                cqlFilter.setIds(((Literal) filter.getExpression2()).getValue().toString());
            }
        }
        return cqlFilter;
    }
}
