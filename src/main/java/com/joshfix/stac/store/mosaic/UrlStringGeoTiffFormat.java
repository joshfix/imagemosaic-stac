package com.joshfix.stac.store.mosaic;

import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.util.factory.Hints;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author joshfix
 * Created on 2019-06-26
 */
public class UrlStringGeoTiffFormat extends GeoTiffFormat {

    /**
     * Logger.
     */
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(UrlStringGeoTiffFormat.class);

    public UrlStringGeoTiffFormat() {
        super();
    }

    @Override
    public GeoTiffReader getReader(Object source, Hints hints) {
        if (source instanceof URL) {
            URL url = (URL) source;
            try {
                return new GeoTiffReader(url.toString(), hints);
            } catch (DataSourceException e) {
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
                return null;
            }
        }
        try {
            return new GeoTiffReader(source, hints);
        } catch (DataSourceException e) {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            return null;
        }
    }
}
