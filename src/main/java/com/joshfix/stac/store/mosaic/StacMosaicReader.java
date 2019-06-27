package com.joshfix.stac.store.mosaic;

import com.joshfix.stac.store.utility.AssetLocator;
import com.joshfix.stac.store.utility.StacException;
import com.joshfix.stac.store.utility.StacRestClient;
import com.joshfix.stac.store.vector.factory.StacDataStoreFactorySpi;
import com.joshfix.stac.store.vector.factory.StacMosaicVectorDataStoreFactorySpi;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.data.DataSourceException;
import org.geotools.feature.NameImpl;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.imagemosaic.*;
import org.geotools.gce.imagemosaic.catalog.CatalogConfigurationBean;
import org.geotools.gce.imagemosaic.catalog.GTDataStoreGranuleCatalog;
import org.geotools.gce.imagemosaic.catalog.GranuleCatalog;
import org.geotools.gce.imagemosaic.catalog.index.Indexer;
import org.geotools.gce.imagemosaic.catalog.index.IndexerUtils;
import org.geotools.gce.imagemosaic.granulecollector.ReprojectingSubmosaicProducerFactory;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.parameter.Parameter;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.Format;
import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Properties;


/**
 * @author joshfix
 */
@Slf4j
public class StacMosaicReader extends AbstractGridCoverage2DReader {

    private MathTransform transform;
    private StacRestClient client;
    private String defaultItemId = "default";
    private static Map defaultItem;
    private MosaicConfigurationProperties configProps = new MosaicConfigurationProperties();
    private LayerParameters layerParameters;

    public StacMosaicReader(URI uri) throws DataSourceException {
        this(uri, null);
    }

    public StacMosaicReader(URI uri, Hints uHints) throws DataSourceException {
        super(uri, uHints);

        String uriString = uri.toString();
        if (uriString.indexOf("?") > 0) {
            String[] uriAndDefaultItemId = uriString.split("\\?");
            //source = new URI(uriAndDefaultItemId[0]);
            source = uriAndDefaultItemId[0];
            defaultItemId = uriAndDefaultItemId[1];
        }

        client = new StacRestClient(uri);

        try {
            originalEnvelope = new GeneralEnvelope(CRS.decode(configProps.getCrs()));
            originalEnvelope.setEnvelope(-180.0, -90.0, 180.0, 90.0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.originalGridRange = new GridEnvelope2D(
                0,
                0,
                LayerParameters.GRID_WIDTH_DEFAULT,
                LayerParameters.GRID_HEIGHT_DEFAULT);
    }

    @Override
    public Format getFormat() {
        return new StacMosaicFormat();
    }

    private boolean mosaic = true;

    @Override
    @SuppressWarnings("unchecked")
    public GridCoverage2D read(GeneralParameterValue[] parameters) throws IOException {
        if (mosaic) {
            layerParameters = new LayerParameters(parameters);
            try {
                return getStacMosaicReader(layerParameters).read(configProps.getTypename(), parameters);
            } catch (FactoryException e) {
                throw new RuntimeException("Factory exception while creating store. "
                        + "Most likely an issue with the EPSG database.", e);
            }
        } else {
            String itemId = null;
            for (GeneralParameterValue parameter : parameters) {
                if (parameter.getDescriptor().equals(StacMosaicFormat.PARAM_FILTER)) {
                    Filter filter = ((Parameter<Filter>) parameter).getValue();
                    if (filter instanceof PropertyIsEqualTo) {
                        PropertyIsEqualTo eq = (PropertyIsEqualTo) filter;
                        if (eq.getExpression1() instanceof PropertyName &&
                                ((PropertyName) eq.getExpression1()).getPropertyName().equals("item_id") &&
                                eq.getExpression2() instanceof Literal) {
                            itemId = ((Literal) eq.getExpression2()).getValue().toString();
                        }
                    }

                }
            }

            GridCoverage2D g = getGeoTiffReader(itemId).read(parameters);
            return getGeoTiffReader(itemId).read(parameters);
        }
    }

    private ImageMosaicReader getStacMosaicReader(LayerParameters layerParameters) throws FactoryException, IOException {
        //this.originalGridRange = new GridEnvelope2D(0, 0, layerParameters.getGridWidth(), layerParameters.getGridHeight());
        //overViewResolutions = new double[1][2];
        //overViewResolutions[0] = new double[]{layerParameters.getMaxResolutionPixelSizeX(), layerParameters.getMaxResolutionPixelSizeY()};
        //numOverviews = 1;
        Properties props = new Properties();
        props.put(Utils.Prop.CRS_ATTRIBUTE, "crs");
        // TODO: heterogeneous should likely be false, however it doesn't seem to be happy
        props.put(Utils.Prop.HETEROGENEOUS, true);
        props.put(Utils.Prop.HETEROGENEOUS_CRS, true);
        props.put(Utils.Prop.PATH_TYPE, PathType.URL);
        props.put(Utils.Prop.TYPENAME, configProps.getTypename());
        props.put(Utils.Prop.LOCATION_ATTRIBUTE, configProps.getLocationAttribute());

        props.put(Utils.Prop.SUGGESTED_IS_SPI, UrlStringImageInputStreamSpi.class.getCanonicalName());
        props.put(Utils.Prop.SUGGESTED_FORMAT, UrlStringGeoTiffFormat.class.getCanonicalName());
        props.put(Utils.Prop.SUGGESTED_SPI, MosaicConfigurationProperties.SUGGESTED_SPI);

        props.put(Utils.Prop.CACHING, false);
        props.put(Utils.Prop.MOSAIC_CRS, configProps.getCrs());
        props.put(Utils.Prop.CHECK_AUXILIARY_METADATA, false);
        props.put(StacDataStoreFactorySpi.SERVICE_URL.getName(), getSource());
        props.put(StacDataStoreFactorySpi.DBTYPE.getName(), StacMosaicVectorDataStoreFactorySpi.DBTYPE_STRING);
        props.put(StacDataStoreFactorySpi.NAMESPACE.getName(), new NameImpl(configProps.getTypename()));

        GranuleCatalog catalog = new GTDataStoreGranuleCatalog(props, false, new StacMosaicVectorDataStoreFactorySpi(client, layerParameters), null);

        MosaicConfigurationBean configurationBean = new MosaicConfigurationBean();
        configurationBean.setCrs(CRS.decode(configProps.getCrs()));
        configurationBean.setCRSAttribute(props.getProperty(Utils.Prop.CRS_ATTRIBUTE));
        configurationBean.setName(configProps.getTypename());
        configurationBean.setExpandToRGB(false);
        Indexer indexer = IndexerUtils.createDefaultIndexer();
        IndexerUtils.setParam(
                indexer,
                Utils.Prop.GRANULE_COLLECTOR_FACTORY,
                ReprojectingSubmosaicProducerFactory.class.getName());
        configurationBean.setIndexer(indexer);

        double[][] levels = new double[1][2];
        levels[0] = new double[]{layerParameters.getMaxResolutionPixelSizeX(), layerParameters.getMaxResolutionPixelSizeY()};
        CatalogConfigurationBean ccb = new CatalogConfigurationBean();
        configurationBean.setLevelsNum(levels.length);
        configurationBean.setLevels(levels);
        ccb.setHeterogeneous((boolean) props.get(Utils.Prop.HETEROGENEOUS));
        ccb.setHeterogeneousCRS((boolean) props.get(Utils.Prop.HETEROGENEOUS_CRS));
        ccb.setLocationAttribute((String) props.get(Utils.Prop.LOCATION_ATTRIBUTE));
        ccb.setSuggestedFormat((String) props.get(Utils.Prop.SUGGESTED_FORMAT));
        ccb.setSuggestedIsSPI((String) props.get(Utils.Prop.SUGGESTED_IS_SPI));
        ccb.setSuggestedSPI((String) props.get(Utils.Prop.SUGGESTED_SPI));
        ccb.setCaching((boolean) props.get(Utils.Prop.CACHING));
        ccb.setTypeName((String) props.get(Utils.Prop.TYPENAME));
        ccb.setPathType((PathType) props.get(Utils.Prop.PATH_TYPE));
        configurationBean.setCatalogConfigurationBean(ccb);
        configurationBean.setCheckAuxiliaryMetadata((boolean) props.get(Utils.Prop.CHECK_AUXILIARY_METADATA));

        ImageMosaicDescriptor desc = new ImageMosaicDescriptor(configurationBean, catalog);
        return new ImageMosaicReader(desc, null);
    }

    @SuppressWarnings("unchecked")
    public GeoTiffReader getGeoTiffReader(String itemId) throws DataSourceException {

        Map item = getItem(itemId);

        if (null == item) {
            throw new DataSourceException("Unable to find item with id '" + itemId + "' in STAC.");
        }

        String imageUrl = null;
        try {
            // TODO: need to determine  a smart way to dynamically grab a legitimate band for the sample image
            imageUrl = AssetLocator.getRandomAssetImageUrl(item);
        } catch (Exception e) {
            throw new DataSourceException("Unable to determine the image URL from the STAC item.");
        }

        if (imageUrl == null) {
            throw new IllegalArgumentException("Unable to determine the image URL from the STAC item.");
        }

        int protocolDelimiter = imageUrl.indexOf(":");
        if (protocolDelimiter <= 0) {
            throw new IllegalArgumentException("Unable to determine the protocol STAC item's asset URL: " + imageUrl);
        }

        String protocol = imageUrl.substring(0, protocolDelimiter).toLowerCase();

        try {
            switch (protocol) {
                case "http":
                case "https":
                    return new GeoTiffReader(imageUrl);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to build GeoTIFF reader for URL: " + imageUrl, e);
        }

        throw new DataSourceException("Unable to create GeoTiff reader for STAC item ID: " + itemId);

    }

    public Map getItem(String itemId) throws DataSourceException {
        // if no item id was provided, use the default item id
        if (null == itemId || itemId.isEmpty()) {
            // if we already have the default item, just return it
            if (null != defaultItem) {
                return defaultItem;
            }
            itemId = defaultItemId;
        }
        try {
            Map item = client.searchById(itemId);
            // if we just got the default item for the first time, save it so we don't have to search for it again
            if (null == defaultItem && itemId.equalsIgnoreCase(defaultItemId)) {
                defaultItem = item;
            }
            return item;
        } catch (StacException e) {
            throw new DataSourceException(e.getMessage());
        }
    }

    @Override
    public MathTransform getOriginalGridToWorld(String coverageName, PixelInCell pixInCell) {
        if (null != transform) {
            return transform;
        }
        try {
            /**
             * This was originally. built to pass the itemId as null, which would in turn use the default item  This
             * works great for mosaics, but for either WCS requests or vector store requests (can't remember), it breaks.
             * The code was trying to validate the CRS (or something). The fix was to pull a random imagery item instead
             * of using the default item.  This however breaks mosaics.  RasterLayerRequest tries to set the bbox and
             * does so by requesting the granules.  Using a real image results in a STAC query where the max features is
             * 10k and it uses a real bbox (default item results in a bbox that is a small corner near -180, -90).  Using
             * a real bbox, the results contain other items, like fields, which break the ItemScorer.
             */
/*
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setQuery(StacFeatureSource.QUERY);
            searchRequest.setLimit(1);
            ItemCollection itemCollection = client.search(searchRequest);
            Item item = itemCollection.getFeatures().get(0);
            return getGeoTiffReader(item.getId()).getOriginalGridToWorld("geotiff_coverage", pixInCell);
*/
            transform = getGeoTiffReader(null).getOriginalGridToWorld("geotiff_coverage", pixInCell);
            return transform;
            //return getGeoTiffReader(null).getOriginalGridToWorld(coverageName, pixInCell);
        } catch (DataSourceException e) {
            throw new IllegalStateException(e);
        }
    }

}
