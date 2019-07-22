package com.joshfix.stac.store.mosaic;

import com.joshfix.stac.store.KeyNames;
import com.joshfix.stac.store.LayerParameters;
import com.joshfix.stac.store.utility.*;
import com.joshfix.stac.store.vector.factory.StacDataStoreFactorySpi;
import com.joshfix.stac.store.vector.factory.StacMosaicVectorDataStoreFactorySpi;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.geoserver.data.util.CoverageUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
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
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * @author joshfix
 */
@Slf4j
public class StacMosaicReader extends AbstractGridCoverage2DReader {

    protected String collection;
    protected String sampleItemId;
    protected String assetId;
    protected static Map sampleItem;
    protected StacRestClient client;
    protected double[][] resolutions;
    protected String storeStacFilter;
    protected GridCoverage2D sampleCoverage;
    protected AssetDescriptor assetDescriptor;
    protected Map<String, String> metadata = new HashMap<>();
    protected MosaicConfigurationProperties configProps = new MosaicConfigurationProperties();

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
                case KeyNames.COLLECTION:
                    collection = param.getValue();
                    break;
                case KeyNames.ASSET_ID:
                    assetId = param.getValue();
            }
        });

        populateSampledData();
    }

    protected void populateSampledData() throws DataSourceException {
        try {
            client = StacClientFactory.create((String) source);
        } catch (Exception e) {
            log.error("Error connecting to STAC at URL " + source.toString());
            throw new DataSourceException("Error connecting to STAC at URL " + source.toString());
        }

        // use the provided default item id or a random item, given the store's stac filter and item type to extract
        // the imagery resolution and CRS.  note this implies all imagery of the given type has the same resolution.
        GridCoverage2DReader sampleReader = (sampleItemId != null && !sampleItemId.isBlank()) ?
                getGridCoverageReader(sampleItemId) :
                getGridCoverageReader(getRandomItem());

        crs = sampleReader.getCoordinateReferenceSystem();

        try {
            resolutions = sampleReader.getResolutionLevels();
        } catch (Exception e) {
            throw new DataSourceException("Error reading resolution levels from sample image.", e);
        }

        originalGridRange = new GridEnvelope2D(
                0,
                0,
                (int) Math.round(360.0 / resolutions[0][0]),
                (int) Math.round(180.0 / resolutions[0][1])
        );

        try {
            this.originalEnvelope = CRS.transform(
                    new Envelope2D(
                            new DirectPosition2D(crs, -180.0, -90.0),
                            new DirectPosition2D(crs, 180.0, 90.0)),
                    crs);
        } catch (TransformException e) {
            throw new DataSourceException("Error building envelope with global bounds.", e);
        }

        // get a tiny portion of the coverage to use later when geoserver tries to query for it
        try {
            MathTransform gridToWorldCorner = sampleReader.getOriginalGridToWorld(PixelInCell.CELL_CORNER);
            GridEnvelope2D testRange = new GridEnvelope2D(0, 0, 5, 5);

            GeneralEnvelope testEnvelope =
                    CRS.transform(gridToWorldCorner, new GeneralEnvelope(testRange.getBounds()));
            testEnvelope.setCoordinateReferenceSystem(crs);

            GridGeometry2D gridGeometry2D = new GridGeometry2D(testRange, testEnvelope);
            Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString(), gridGeometry2D);
            sampleCoverage = sampleReader.read(
                    CoverageUtils.getParameters(getFormat().getReadParameters(), parameterMap, true));
        } catch (Exception e) {
            throw new DataSourceException("Error reading sample coverage.", e);
        }
    }

    @Override
    public Format getFormat() {
        return new StacMosaicFormat();
    }

    @Override
    @SuppressWarnings("unchecked")
    public GridCoverage2D read(GeneralParameterValue[] parameters) throws IOException {
        return isSampleCoverageRequest(parameters) ? sampleCoverage : getCoverage(parameters);
    }

    protected boolean isSampleCoverageRequest(GeneralParameterValue[] parameters) {
        for (GeneralParameterValue paramValue : parameters) {
            if (StacMosaicFormat.READ_GRIDGEOMETRY2D.getName().getCode().equals(
                    paramValue.getDescriptor().getName().getCode())) {

                GridGeometry2D gridGeometry2D = (GridGeometry2D) ((Parameter) paramValue).getValue();
                GridEnvelope2D gridRange2D = gridGeometry2D.getGridRange2D();

                return gridRange2D.getMinX() == 0
                        && gridRange2D.getMinY() == 0
                        && gridRange2D.getMaxX() == 5
                        && gridRange2D.getMaxY() == 5
                        && gridGeometry2D.getEnvelope().getLowerCorner().getCoordinate()[0] == -180.0
                        && gridGeometry2D.getEnvelope().getUpperCorner().getCoordinate()[1] == 90.0;
            }
        }
        return false;
    }

    protected GridCoverage2D getCoverage(GeneralParameterValue[] parameters) throws IOException {
        try {
            LayerParameters layerParameters = new LayerParameters(parameters, storeStacFilter);
            layerParameters.setCollection(collection);
            layerParameters.setResolutions(resolutions[0]);
            return getStacMosaicReader(layerParameters).read(configProps.getTypename(), parameters);
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
        props.put(Utils.Prop.TYPENAME, configProps.getTypename());
        props.put(Utils.Prop.LOCATION_ATTRIBUTE, configProps.getLocationAttribute());
        props.put(Utils.Prop.SUGGESTED_IS_SPI, UrlStringImageInputStreamSpi.class.getCanonicalName());
        props.put(Utils.Prop.SUGGESTED_SPI, MosaicConfigurationProperties.SUGGESTED_SPI);
        props.put(Utils.Prop.CACHING, false);
        props.put(Utils.Prop.CHECK_AUXILIARY_METADATA, false);
        props.put(StacDataStoreFactorySpi.SERVICE_URL.getName(), getSource().toString());
        props.put(StacDataStoreFactorySpi.DBTYPE.getName(), StacDataStoreFactorySpi.DBTYPE_ID);
        props.put(StacDataStoreFactorySpi.NAMESPACE.getName(), new NameImpl(configProps.getTypename()));
        switch (assetDescriptor.getType()) {
            case "image/jp2":
                props.put(Utils.Prop.SUGGESTED_FORMAT, JP2KFormat.class.getCanonicalName());
            case "image/vnd.stac.geotiff":
            case "image/x.geotiff":
                props.put(Utils.Prop.SUGGESTED_FORMAT, UrlStringGeoTiffFormat.class.getCanonicalName());
        }

        GranuleCatalog catalog = new GTDataStoreGranuleCatalog(
                props,
                false,
                new StacMosaicVectorDataStoreFactorySpi(client, layerParameters),
                null);

        MosaicConfigurationBean mosaicConfigurationBean = new MosaicConfigurationBean();
        mosaicConfigurationBean.setCrs(CRS.decode(configProps.getCrs()));
        mosaicConfigurationBean.setName(configProps.getTypename());
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

    public GridCoverage2DReader getGridCoverageReader(String itemId) throws DataSourceException {
        Map item = getItem(itemId);
        if (null == item) {
            throw new DataSourceException("Unable to find item with id '" + itemId + "' in STAC.");
        }
        return getGridCoverageReader(item);
    }

    @SuppressWarnings("unchecked")
    public GridCoverage2DReader getGridCoverageReader(Map item) throws DataSourceException {
        try {
            // TODO: need to determine a smart way to dynamically grab a legitimate band for the sample image
            //assetDescriptor = AssetLocator.getRandomAssetImageUrl(item);
            assetDescriptor = AssetLocator.getAsset(item, assetId);
        } catch (Exception e) {
            throw new DataSourceException("Unable to determine the image URL from the STAC item.");
        }

        if (assetDescriptor == null) {
            throw new IllegalArgumentException("Unable to determine the image URL from the STAC item.");
        }
/*
        int protocolDelimiter = imageUrl.indexOf(":");
        if (protocolDelimiter <= 0) {
            throw new IllegalArgumentException("Unable to determine the protocol STAC item's asset URL: " + imageUrl);
        }

        String protocol = imageUrl.substring(0, protocolDelimiter).toLowerCase();
*/


        try {
            switch (assetDescriptor.getType()) {
                case "image/jp2":
                    return new JP2KReader(assetDescriptor.getUrl());
                case "image/vnd.stac.geotiff":
                case "image/x.geotiff":
                case "image/geo+tiff":
                case "image/geotiff":
                case "image/tiff":
                    return new GeoTiffReader(assetDescriptor.getUrl());
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
        try {
            SearchRequest request = new SearchRequest()
                    .ids(new String[]{itemId});

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

        if (null != collection && !collection.isBlank()) {
            searchRequest.setCollections(new String[]{collection});
        }

        String stacQuery = null;
        if (storeStacFilter != null && !storeStacFilter.isBlank()) {
            stacQuery = (stacQuery == null || stacQuery.isBlank())
                    ? storeStacFilter
                    : storeStacFilter + " AND " + stacQuery;
        }

        searchRequest.setQuery(stacQuery);
        searchRequest.setLimit(1);
        try {
            Map itemCollection = client.search(searchRequest);
            sampleItem = (Map)((Map)itemCollection.get("features")).get(0);
            return sampleItem;
        } catch (Exception e) {
            log.error("Error querying stac with filter " + stacQuery, e);
            throw new DataSourceException("Error querying stac with filter " + stacQuery, e);
        }
    }

}
