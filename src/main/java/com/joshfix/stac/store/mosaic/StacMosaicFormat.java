package com.joshfix.stac.store.mosaic;

import com.joshfix.stac.store.FieldNames;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.feature.NameImpl;
import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.ParameterGroup;
import org.geotools.util.factory.Hints;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterDescriptor;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author joshfix
 */
public class StacMosaicFormat extends AbstractGridFormat {

    private static final Logger LOGGER = Logger.getLogger(StacMosaicFormat.class.getName());
    public static final String NAME = "STAC Mosaic Store";

    public static final DefaultParameterDescriptor<Filter> PARAM_FILTER =
            new DefaultParameterDescriptor<>(FieldNames.PARAM_FILTER_NAME, Filter.class, null, null);

    public static final DefaultParameterDescriptor<NameImpl> NAMESPACE =
            new DefaultParameterDescriptor<>(FieldNames.NAMESPACE_NAME, NameImpl.class, null, null);

    public static final ParameterDescriptor<String> STAC_FILTER =
            new DefaultParameterDescriptor(FieldNames.STAC_QUERY_NAME, String.class, null, null);

    public static final ParameterDescriptor<Boolean> USE_BBOX =
            new DefaultParameterDescriptor(FieldNames.USE_BBOX_NAME, Boolean.class, null, LayerParameters.USE_BBOX_DEFAULT);

    public static final ParameterDescriptor<Integer> MAX_GRANULES =
            new DefaultParameterDescriptor(FieldNames.MAX_GRANULES_NAME, Integer.class, null, LayerParameters.MAX_GRANULES_DEFAULT);

    public static final ParameterDescriptor<Integer> MAX_RESOLUTION_PIXEL_SIZE_X =
            new DefaultParameterDescriptor(FieldNames.MAX_RESOLUTION_PIXEL_SIZE_X_NAME, Double.class, null, LayerParameters.MAX_RESOLUTION_X_DEFAULT);

    public static final ParameterDescriptor<Integer> MAX_RESOLUTION_PIXEL_SIZE_Y =
            new DefaultParameterDescriptor(FieldNames.MAX_RESOLUTION_PIXEL_SIZE_Y_NAME, Double.class, null, LayerParameters.MAX_RESOLUTION_Y_DEFAULT);

    public static final ParameterDescriptor<String> COLLECTION =
            new DefaultParameterDescriptor(FieldNames.COLLECTION_NAME, String.class, null, LayerParameters.COLLECTION_DEFAULT);

    // TODO: don't use "B2" as default asset id :)
    public static final ParameterDescriptor<String> ASSET_ID =
            new DefaultParameterDescriptor(FieldNames.ASSET_ID_NAME, String.class, null, "B2");

    public static final ParameterDescriptor<Integer> GRID_WIDTH =
            new DefaultParameterDescriptor(FieldNames.GRID_WIDTH_NAME, Integer.class, null, LayerParameters.GRID_WIDTH_DEFAULT);

    public static final ParameterDescriptor<Integer> GRID_HEIGHT =
            new DefaultParameterDescriptor(FieldNames.GRID_HEIGHT_NAME, Integer.class, null, LayerParameters.GRID_HEIGHT_DEFAULT);

    public StacMosaicFormat() {
        writeParameters = null;
        mInfo = new HashMap<>();
        mInfo.put("name", NAME);
        mInfo.put("description", "Utilizes asset URLs in STAC items to read GeoTIFF images hosted on the web.");
        mInfo.put("vendor", "Josh Fix");
        mInfo.put("version", "1.0.0");

        // ImageMosaicFormat.MAX_ALLOWED_TILES

        // reading parameters
        readParameters = new ParameterGroup(
                new DefaultParameterDescriptorGroup(mInfo, new GeneralParameterDescriptor[]{
                        ASSET_ID,
                        STAC_FILTER,
                        //NAMESPACE,
                        USE_BBOX,
                        MAX_GRANULES,
                        MAX_RESOLUTION_PIXEL_SIZE_X,
                        MAX_RESOLUTION_PIXEL_SIZE_Y,
                        //READ_GRIDGEOMETRY2D,
                        INPUT_TRANSPARENT_COLOR,
                        SUGGESTED_TILE_SIZE,
                        PARAM_FILTER,
                        COLLECTION,
                        //BANDS,
                        GRID_WIDTH,
                        GRID_HEIGHT
                }));
    }

    @Override
    public StacMosaicReader getReader(Object source, Hints hints) {
        //in practice here source is probably almost always going to be a string.
        try {
            URI uri;
            if (source instanceof File) {
                throw new UnsupportedOperationException("Cannot instantiate STAC reader with file handle.");
            } else if (source instanceof String) {
                uri = new URI((String) source);
            } else if (source instanceof URI) {
                uri = (URI) source;
            } else {
                throw new IllegalArgumentException("Can't create STAC reader from input of type: " + source.getClass());
            }
            return new StacMosaicReader(uri, hints);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Exception raised trying to instantiate STAC mosaic reader from source.", e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean accepts(Object o, Hints hints) {
        if (o == null) {
            return false;
        }
        //TODO: actually perform some kind of check
        return true;
    }

    @Override
    public boolean accepts(Object source) {
        return this.accepts(source, null);
    }

    @Override
    public AbstractGridCoverage2DReader getReader(Object source) {
        return getReader(source, null);
    }

    @Override
    public GridCoverageWriter getWriter(Object destination) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GeoToolsWriteParams getDefaultImageIOWriteParameters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GridCoverageWriter getWriter(Object destination, Hints hints) {
        throw new UnsupportedOperationException();
    }
}
