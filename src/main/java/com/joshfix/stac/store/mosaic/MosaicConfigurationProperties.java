package com.joshfix.stac.store.mosaic;

import com.joshfix.stac.store.utility.PropertyResolver;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author joshfix
 * Created on 3/14/19
 */
@Data
@Slf4j
public class MosaicConfigurationProperties {

    private String crs;
    private String typename;
    private String locationAttribute;
    private double[][] levels = new double[1][2];

    public static final String SUGGESTED_SPI = "it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi";

    // environment variable names
    public static final String PREFIX = "STAC_MOSAIC_";
    public static final String CRS_VAR = PREFIX + "CRS";
    public static final String TYPENAME_VAR = PREFIX + "TYPENAME";
    public static final String LOCATION_ATTRIBUTE_VAR = PREFIX + "LOCATION_ATTRIBUTE";

    // default values
    public static final String DEFAULT_CRS_VALUE = "EPSG:4326";
    public static final String DEFAULT_TYPENAME_VALUE = "stac-item";
    public static final String DEFAULT_LOCATION_ATTRIBUTE_VALUE = "stac-image";

    public MosaicConfigurationProperties() {
        crs = PropertyResolver.getPropertyValue(CRS_VAR , DEFAULT_CRS_VALUE);
        locationAttribute = PropertyResolver.getPropertyValue(LOCATION_ATTRIBUTE_VAR, DEFAULT_LOCATION_ATTRIBUTE_VALUE);
        typename = PropertyResolver.getPropertyValue(TYPENAME_VAR, DEFAULT_TYPENAME_VALUE);
    }

}
