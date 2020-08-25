package com.joshfix.stac.store.mosaic;

import com.joshfix.stac.store.FieldNames;
import com.joshfix.stac.store.LayerParameters;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
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


/**
 * @author joshfix
 */
@Slf4j
public class StacMosaicFormat extends AbstractGridFormat {

    public static final DefaultParameterDescriptor<Filter> PARAM_FILTER =
            new DefaultParameterDescriptor<>(FieldNames.PARAM_FILTER, Filter.class, null, null);

    public static final ParameterDescriptor<String> LAYER_STAC_FILTER =
            new DefaultParameterDescriptor(FieldNames.LAYER_STAC_FILTER, String.class, null, null);

    public static final ParameterDescriptor<Boolean> USE_BBOX =
            new DefaultParameterDescriptor(FieldNames.USE_BBOX, Boolean.class, null, LayerParameters.USE_BBOX_DEFAULT);

    public static final ParameterDescriptor<Integer> MAX_GRANULES =
            new DefaultParameterDescriptor(FieldNames.MAX_GRANULES, Integer.class, null, LayerParameters.MAX_GRANULES_DEFAULT);

    public static final ParameterDescriptor<String> ASSET_ID =
            new DefaultParameterDescriptor(FieldNames.ASSET_ID, String.class, null, "B2");

    public static final ParameterDescriptor<String> AOI_FILTER =
            new DefaultParameterDescriptor(FieldNames.AOI_FILTER, String.class, null, LayerParameters.AOI_FILTER_DEFAULT);

    public StacMosaicFormat() {
        writeParameters = null;
        mInfo = new HashMap<>();
        mInfo.put("name", FieldNames.MOSAIC_STORE);
        mInfo.put("description", "Utilizes asset URLs in STAC items to read GeoTIFF images hosted on the web.");
        mInfo.put("vendor", "Josh Fix");
        mInfo.put("version", "1.0.0");

        // reading parameters
        readParameters = new ParameterGroup(
                new DefaultParameterDescriptorGroup(mInfo, new GeneralParameterDescriptor[]{
                        LAYER_STAC_FILTER,
                        USE_BBOX,
                        MAX_GRANULES,
                        READ_GRIDGEOMETRY2D,
                        INPUT_TRANSPARENT_COLOR,
                        SUGGESTED_TILE_SIZE,
                        PARAM_FILTER,
                        AOI_FILTER,
                        BANDS
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
            log.error("Exception raised trying to instantiate STAC mosaic reader from source.", e);
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
