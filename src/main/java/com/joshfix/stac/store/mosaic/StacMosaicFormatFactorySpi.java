package com.joshfix.stac.store.mosaic;

import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFactorySpi;

import java.awt.*;
import java.util.Collections;
import java.util.Map;

/**
 * @author joshfix
 */
public class StacMosaicFormatFactorySpi implements GridFormatFactorySpi {

    @Override
    public AbstractGridFormat createFormat() {
        return new StacMosaicFormat();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Map<RenderingHints.Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }

}
