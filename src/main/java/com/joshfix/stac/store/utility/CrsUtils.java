package com.joshfix.stac.store.utility;

import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class CrsUtils {
    public static CoordinateReferenceSystem DEFAULT_CRS;

    static {
        try {
            DEFAULT_CRS = CRS.decode("EPSG:4326");
        } catch (FactoryException e) {
            e.printStackTrace();
        }
    }
}
