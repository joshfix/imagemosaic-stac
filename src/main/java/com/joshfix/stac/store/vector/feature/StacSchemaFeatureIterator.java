package com.joshfix.stac.store.vector.feature;

import com.joshfix.stac.store.LayerParameters;
import lombok.extern.slf4j.Slf4j;
import org.opengis.feature.type.Name;

import java.util.Map;
import java.util.Set;

/**
 * @author joshfix
 */
@Slf4j
public class StacSchemaFeatureIterator extends StacFeatureIterator {

    public StacSchemaFeatureIterator(Set<Map> resultSet, Name name, LayerParameters layerParameters) {
        super(resultSet, name, layerParameters);
    }

    @Override
    public void close() {
        try {
            //TODO: only need to close if using ItemIterator with StacClient.searchStreaming
            //((ItemIterator)iterator).close();
        } catch (Exception ioe) {
            log.warn("Error closing iterator.", ioe);
        }
    }

}
