package com.joshfix.stac.store.mosaic;

import it.geosolutions.imageio.stream.input.FileImageInputStreamExtImpl;
import it.geosolutions.imageio.stream.input.spi.StringImageInputStreamSpi;
import it.geosolutions.imageio.stream.input.spi.URLImageInputStreamSpi;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author joshfix
 * Created on 2019-06-26
 */
public class UrlStringImageInputStreamSpi extends StringImageInputStreamSpi {

    private final static Logger LOGGER = Logger.getLogger(UrlStringImageInputStreamSpi.class.getCanonicalName());

    public UrlStringImageInputStreamSpi() {
        super();
    }

    /**
     * @see javax.imageio.spi.ImageInputStreamSpi#createInputStreamInstance(java.lang.Object,
     *      boolean, java.io.File)
     */
    public ImageInputStream createInputStreamInstance(Object input,
                                                      boolean useCache, File cacheDir) throws IOException {

        // is it a String?
        if (!(input instanceof String || input instanceof URL)) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.fine("The provided input is not a valid String or URL.");
            return null;
        }


        final String sourceString = input.toString();
        //final String sourceString = input instanceof String ? (String) input : ((URL)input).toString() ;

        //
        // as a URL
        //
        try{
            // the needed checks are done inside the constructor
            final URL tempURL = new URL(sourceString);
            return new URLImageInputStreamSpi().createInputStreamInstance(tempURL, ImageIO.getUseCache(),ImageIO.getCacheDirectory());
        }catch (Throwable e) {
            if(LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE,e.getLocalizedMessage(),e);
        }


        //
        // as a FILE
        //
        final File tempFile = new File(sourceString);
        try{
            // the needed checks are done inside the constructor
            return new FileImageInputStreamExtImpl(tempFile);
        }catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }
}
