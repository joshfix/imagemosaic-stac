package com.joshfix.stac.store.mosaic;

import com.joshfix.geotools.geotiff.CogFormat;
import com.joshfix.geotools.geotiff.CogReader;
import com.joshfix.stac.store.KeyNames;
import com.joshfix.stac.store.LayerParameters;
import com.joshfix.stac.store.utility.*;
import com.joshfix.stac.store.vector.factory.StacDataStoreFactorySpi;
import com.joshfix.stac.store.vector.factory.StacMosaicVectorDataStoreFactorySpi;
import it.geosolutions.imageioimpl.plugins.tiff.CogImageReaderSpi;
import it.geosolutions.imageioimpl.plugins.tiff.stream.CachingHttpCogImageInputStream;
import it.geosolutions.imageioimpl.plugins.tiff.stream.CachingHttpCogImageInputStreamSpi;
import it.geosolutions.imageioimpl.plugins.tiff.stream.HttpCogImageInputStreamSpi;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.geoserver.data.util.CoverageUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.processing.Operations;
import org.geotools.coverageio.jp2k.JP2KFormat;
import org.geotools.coverageio.jp2k.JP2KReader;
import org.geotools.data.DataSourceException;
import org.geotools.feature.NameImpl;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.imagemosaic.*;
import org.geotools.gce.imagemosaic.catalog.CatalogConfigurationBean;
import org.geotools.gce.imagemosaic.catalog.GTDataStoreGranuleCatalog;
import org.geotools.gce.imagemosaic.catalog.GranuleCatalog;
import org.geotools.gce.imagemosaic.catalog.index.IndexerUtils;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.parameter.Parameter;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import javax.measure.Unit;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;


/**
 * @author joshfix
 */
@Slf4j
public class StacMosaicReader extends AbstractGridCoverage2DReader {

    protected String collection;
    protected String sampleItemId;
    protected String assetId;
    protected Map sampleItem;
    protected StacRestClient client;
    protected double[][] resolutions;
    protected GridCoverage2D sampleCoverage5x5;
    protected GridCoverage2D sampleCoverage6x3;
    protected GridCoverage2D sampleCoverage10x10;
    protected String storeStacFilter;
    protected GridCoverage2D sampleCoverage;
    protected AssetDescriptor assetDescriptor;
    protected Map<String, String> metadata = new HashMap<>();

    //protected MosaicConfigurationProperties configProps = new MosaicConfigurationProperties();

    private final Logger LOGGER = Logger.getLogger(StacMosaicReader.class.getName());

    double minX = -180.0;
    double maxX = 180.0;
    double minY = -90.0;
    double maxY = 90.0;

    public static final String TYPENAME = "stac-item";
    public static final String LOCATION_ATTRIBUTE = "image";
    //public static final String SUGGESTED_SPI = "it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi";
    public static final String SUGGESTED_SPI = CogImageReaderSpi.class.getCanonicalName();

    public StacMosaicReader(URI uri) throws DataSourceException {
        this(uri, null);
    }

    public StacMosaicReader(URI uri, Hints uHints) throws DataSourceException {
        super(uri, uHints);


        metadata.put("class", StacMosaicReader.class.getName());

        int port = uri.getPort();
        source = port == 80 || port == -1 ? uri.getScheme() + "://" + uri.getHost() + uri.getPath() :
                uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();

        List<NameValuePair> params = URLEncodedUtils.parse(uri.toString(), Charset.forName("UTF-8"), '?', '&');
        params.forEach(param -> {
            switch (param.getName()) {
                case KeyNames.SAMPLE_ITEM_ID:
                    sampleItemId = param.getValue();
                    break;
                case KeyNames.STORE_STAC_FILTER:
                    storeStacFilter = param.getValue();
                    break;
                case KeyNames.ASSET_ID:
                    assetId = param.getValue();
                    break;
                case KeyNames.COLLECTION:
                    collection = param.getValue();
                    break;
                case KeyNames.MIN_X:
                    minX = Double.valueOf(param.getValue());
                    break;
                case KeyNames.MIN_Y:
                    minY = Double.valueOf(param.getValue());
                    break;
                case KeyNames.MAX_X:
                    maxX = Double.valueOf(param.getValue());
                    break;
                case KeyNames.MAX_Y:
                    maxY = Double.valueOf(param.getValue());
                    break;
            }
        });

        populateSampledData();
        calculateOriginalBounds();
    }

    public void calculateOriginalBounds() throws DataSourceException {
        originalGridRange = new GridEnvelope2D(
                0,
                0,
                (int) Math.round((maxX - minX) / resolutions[0][0]),
                (int) Math.round((maxY - minY) / resolutions[0][1])
        );

        try {
            this.originalEnvelope = CRS.transform(
                    new Envelope2D(
                            new DirectPosition2D(crs, minX, minY),
                            new DirectPosition2D(crs, maxX, maxY)),
                    crs);
        } catch (TransformException e) {
            throw new DataSourceException("Error building envelope with global bounds.", e);
        }

    }


    /**
     * GeoServer / GeoTools / QGIS sometimes make funky requests for 3x6, 5x5, or 10x10 tiles either multiple times, or
     * using a query with the full extent of the mosaic, so we simply create those coverages up front and store them
     *
     * @throws DataSourceException
     */
    protected void populateSampledData() throws DataSourceException {
        try {
            client = StacClientFactory.create((String) source);
        } catch (Exception e) {
            LOGGER.severe("Error connecting to STAC at URL " + source.toString());
            throw new DataSourceException("Error connecting to STAC at URL " + source.toString());
        }

        // use the provided default item id or a random item, given the store's stac filter and item type to extract
        // the imagery resolution and CRS.  note this implies all imagery of the given type has the same resolution.
        GridCoverage2DReader sampleReader = getGridCoverageReader(null);

        crs = sampleReader.getCoordinateReferenceSystem();

        try {
            resolutions = sampleReader.getResolutionLevels();
        } catch (Exception e) {
            throw new DataSourceException("Error reading resolution levels from sample image.", e);
        }

        GridCoverage2D sampleCoverage = null;
        try {
            sampleCoverage = sampleReader.read(null);

            int width = sampleCoverage.getRenderedImage().getWidth();
            int height = sampleCoverage.getRenderedImage().getHeight();

            Operations operations = new Operations(null);
            sampleCoverage5x5 = (GridCoverage2D) operations.scale(sampleCoverage, 5.0 / width, 5.0 / height, 0, 0);
            sampleCoverage6x3 = (GridCoverage2D) operations.scale(sampleCoverage, 6.0 / width, 3.0 / height, 0, 0);
            sampleCoverage10x10 = (GridCoverage2D) operations.scale(sampleCoverage, 10.0 / width, 10.0 / height, 0, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Format getFormat() {
        return new StacMosaicFormat();
    }

    @Override
    @SuppressWarnings("unchecked")
    public GridCoverage2D read(GeneralParameterValue[] parameters) throws IOException {
        RequestType requestType = RequestTypeHelper.determineRequestType(parameters, minX, maxY);
        switch (requestType) {
            case SAMPLE_5X5:
                return getModifiedGridCoverage(sampleCoverage5x5, parameters);
            case SAMPLE_6X3:
                return getModifiedGridCoverage(sampleCoverage6x3, parameters);
            case SAMPLE_10X10:
                return getModifiedGridCoverage(sampleCoverage10x10, parameters);
            default:
                return getCoverage(parameters);
        }
    }

    /**
     * WCS does not like it when the requested envelope does not match up with the returned sample image envelope, so
     * we just copy the value from the request into a clone of the sample image
     *
     * @param source
     * @param parameters
     * @return
     */
    protected GridCoverage2D getModifiedGridCoverage(GridCoverage2D source, GeneralParameterValue[] parameters) {
        Envelope2D env = null;
        GridCoverageFactory f = new GridCoverageFactory();
        for (GeneralParameterValue paramValue : parameters) {
            if (StacMosaicFormat.READ_GRIDGEOMETRY2D.getName().getCode().equals(
                    paramValue.getDescriptor().getName().getCode())) {
                env = ((GridGeometry2D) ((Parameter) paramValue).getValue()).getEnvelope2D();
            }
        }
        return f.create("geotiff_coverage", source.getRenderedImage(), env);
    }


    protected GridCoverage2D getCoverage(GeneralParameterValue[] parameters) throws IOException {
        try {
            LayerParameters layerParameters = new LayerParameters(parameters, storeStacFilter);
            layerParameters.setAssetId(assetId);
            layerParameters.setCollection(collection);
            layerParameters.setMinX(minX);
            layerParameters.setMinY(minY);
            layerParameters.setMaxX(maxX);
            layerParameters.setMaxY(maxY);
            layerParameters.setResolutions(resolutions[0]);
            return getStacMosaicReader(layerParameters).read(TYPENAME, parameters);
        } catch (FactoryException e) {
            throw new RuntimeException("Factory exception while creating store. "
                    + "Most likely an issue with the EPSG database.", e);
        }
    }

    protected ImageMosaicReader getStacMosaicReader(LayerParameters layerParameters) throws FactoryException, IOException {
        Properties props = new Properties();
        props.put(Utils.Prop.HETEROGENEOUS, false);
        props.put(Utils.Prop.HETEROGENEOUS_CRS, false);
        props.put(Utils.Prop.PATH_TYPE, PathType.URL);
        props.put(Utils.Prop.TYPENAME, TYPENAME);
        props.put(Utils.Prop.LOCATION_ATTRIBUTE, LOCATION_ATTRIBUTE);
        props.put(Utils.Prop.SUGGESTED_IS_SPI, UrlStringImageInputStreamSpi.class.getCanonicalName());
        props.put(Utils.Prop.SUGGESTED_FORMAT, UrlStringGeoTiffFormat.class.getCanonicalName());
        props.put(Utils.Prop.SUGGESTED_SPI, SUGGESTED_SPI);
        props.put(Utils.Prop.CACHING, false);
        props.put(Utils.Prop.CHECK_AUXILIARY_METADATA, false);
        props.put(StacDataStoreFactorySpi.SERVICE_URL.getName(), getSource().toString());
        props.put(StacDataStoreFactorySpi.DBTYPE.getName(), StacDataStoreFactorySpi.DBTYPE_ID);
        props.put(StacDataStoreFactorySpi.NAMESPACE.getName(), new NameImpl(TYPENAME));


        switch (assetDescriptor.getType()) {
            case "image/jp2":
                props.put(Utils.Prop.SUGGESTED_FORMAT, JP2KFormat.class.getCanonicalName());
                break;
            case "image/tiff":
            case "image/vnd.stac.geotiff":
            case "image/x.geotiff":
            case "image/tiff; application=geotiff; profile=cloud-optimized":
                //props.put(Utils.Prop.SUGGESTED_FORMAT, UrlStringGeoTiffFormat.class.getCanonicalName());
                props.put(Utils.Prop.SUGGESTED_FORMAT, CogFormat.class.getCanonicalName());
                //props.put(Utils.Prop.SUGGESTED_FORMAT, HttpCogI)
        }

        GranuleCatalog catalog = new GTDataStoreGranuleCatalog(
                props,
                false,
                new StacMosaicVectorDataStoreFactorySpi(client, layerParameters),
                null);

        MosaicConfigurationBean mosaicConfigurationBean = new MosaicConfigurationBean();
        mosaicConfigurationBean.setCrs(CrsUtils.DEFAULT_CRS);
        mosaicConfigurationBean.setName(TYPENAME);
        mosaicConfigurationBean.setExpandToRGB(false);
        mosaicConfigurationBean.setIndexer(IndexerUtils.createDefaultIndexer());
        mosaicConfigurationBean.setLevelsNum(resolutions.length);
        mosaicConfigurationBean.setLevels(resolutions);

        CatalogConfigurationBean catalogConfigurationBean = new CatalogConfigurationBean();
        catalogConfigurationBean.setHeterogeneous((boolean) props.get(Utils.Prop.HETEROGENEOUS));
        catalogConfigurationBean.setHeterogeneousCRS((boolean) props.get(Utils.Prop.HETEROGENEOUS_CRS));
        catalogConfigurationBean.setLocationAttribute((String) props.get(Utils.Prop.LOCATION_ATTRIBUTE));
        catalogConfigurationBean.setSuggestedFormat((String) props.get(Utils.Prop.SUGGESTED_FORMAT));
        catalogConfigurationBean.setSuggestedIsSPI((String) props.get(Utils.Prop.SUGGESTED_IS_SPI));
        catalogConfigurationBean.setSuggestedSPI((String) props.get(Utils.Prop.SUGGESTED_SPI));
        catalogConfigurationBean.setCaching((boolean) props.get(Utils.Prop.CACHING));
        catalogConfigurationBean.setTypeName((String) props.get(Utils.Prop.TYPENAME));
        catalogConfigurationBean.setPathType((PathType) props.get(Utils.Prop.PATH_TYPE));
        mosaicConfigurationBean.setCatalogConfigurationBean(catalogConfigurationBean);
        mosaicConfigurationBean.setCheckAuxiliaryMetadata((boolean) props.get(Utils.Prop.CHECK_AUXILIARY_METADATA));

        ImageMosaicDescriptor imageMosaicDescriptor = new ImageMosaicDescriptor(mosaicConfigurationBean, catalog);

        return new ImageMosaicReader(imageMosaicDescriptor, null);
    }

    @SuppressWarnings("unchecked")
    public GridCoverage2DReader getGridCoverageReader(String itemId) throws DataSourceException {

        Map item = getItem(itemId);

        if (null == item) {
            throw new DataSourceException("Unable to find item with id '" + itemId + "' in STAC.");
        }

        String imageUrl = null;
        try {
            imageUrl = AssetLocator.getAssetImageUrl(item, assetId);
            //URL tmpImageUrl = new URL(imageUrl);
            //imageUrl = "s3:/" + tmpImageUrl.getPath() + "?useAnon=true&&awsRegion=US_WEST_2";
        } catch (Exception e) {
            throw new DataSourceException("Unable to determine the image URL from the STAC item.");
        }

        if (imageUrl == null) {
            throw new IllegalArgumentException("Unable to determine the image URL from the STAC item.");
        }

        assetDescriptor = AssetLocator.getAsset(item, assetId);

        try {
            switch (assetDescriptor.getType()) {
                case "image/jp2":
                    return new JP2KReader(assetDescriptor.getUrl());
                case "image/vnd.stac.geotiff":
                case "image/x.geotiff":
                case "image/geo+tiff":
                case "image/geotiff":
                case "image/tiff":
                case "image/tiff; application=geotiff; profile=cloud-optimized":
                    //return new GeoTiffReader(assetDescriptor.getUrl());
                    return new CogReader(assetDescriptor.getUrl());
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to build GeoTIFF reader for URL: " + assetDescriptor.getUrl(), e);
        }

        throw new DataSourceException("Unable to create GeoTiff reader for STAC item ID: " + item.get("id"));

    }

    public Map getItem(String itemId) throws DataSourceException {
        // if no item id was provided, use the default item id
        if (null != sampleItem) {
            return sampleItem;
        }

        if (itemId == null || itemId.isEmpty()) {
            return getRandomItem();
        }

        try {
            SearchRequest request = new SearchRequest()
                    .ids(new String[]{itemId})
                    .collections(new String[]{collection});
            Map<String, Object> itemCollection = client.search(request);
            List<Map> items = (List<Map>) itemCollection.get("features");

            if (!items.isEmpty()) {
                sampleItem = items.get(0);
            }

            return sampleItem;
        } catch (StacException e) {
            throw new DataSourceException(e.getMessage());
        }
    }

    public Map getRandomItem() throws DataSourceException {
        if (null != sampleItem && null != sampleItem.get("id")) {
            return sampleItem;
        }

        SearchRequest searchRequest = new SearchRequest();

        if (null != collection && !collection.isEmpty()) {
            searchRequest.setCollections(new String[]{collection});
        }

        String stacQuery = null;
        if (storeStacFilter != null && !storeStacFilter.isEmpty()) {
            stacQuery = (stacQuery == null || stacQuery.isEmpty())
                    ? storeStacFilter
                    : storeStacFilter + " AND " + stacQuery;
        }
        searchRequest.collections(new String[]{collection}).query(stacQuery).limit(1);

        try {
            Map itemCollection = client.search(searchRequest);
            sampleItem = (Map) ((List) itemCollection.get("features")).get(0);
            return sampleItem;
        } catch (Exception e) {
            log.error("Error querying stac with filter " + stacQuery, e);
            throw new DataSourceException("Error querying stac with filter " + stacQuery, e);
        }
    }

}
