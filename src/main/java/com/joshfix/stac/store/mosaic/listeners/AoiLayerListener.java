package com.joshfix.stac.store.mosaic.listeners;


import com.joshfix.stac.store.FieldNames;
import com.joshfix.stac.store.KeyNames;
import com.joshfix.stac.store.mosaic.StacMosaicReader;
import com.joshfix.stac.store.vector.factory.StacVectorDataStoreFactorySpi;
import lombok.extern.slf4j.Slf4j;
import org.geoserver.catalog.*;
import org.geoserver.catalog.event.*;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.coverage.grid.GridCoverageReader;

import java.io.IOException;

/**
 * @author joshfix
 * Created on 2019-07-08
 */
@Slf4j
public class AoiLayerListener implements CatalogListener {

    private final Catalog catalog;
    private static final String BBOX_PROPERTY_NAME = "latLonBoundingBox";

    public AoiLayerListener(Catalog catalog) {
        this.catalog = catalog;
        catalog.addListener(this);
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        CatalogInfo catalogInfo = event.getSource();
        if (catalogInfo instanceof CoverageInfo) {
           handleCoverageAdd(event);
        } else if (catalogInfo instanceof FeatureTypeInfo) {
           handleDataStoreAdd(event);
        }
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        CatalogInfo catalogInfo = event.getSource();

        if (catalogInfo instanceof CoverageInfo) {
            handleCoverageModify(event);
        } else if (catalogInfo instanceof FeatureTypeInfo) {
            handleDataStoreModify(event);
        }
    }

    @Override
    public void reloaded() {
    }

    protected void handleCoverageAdd(CatalogAddEvent event) {
        try {
            CoverageInfo coverageInfo =  ModificationProxy.unwrap((CoverageInfo) event.getSource());
            GridCoverageReader gridCoverageReader = coverageInfo.getGridCoverageReader(null, null);
            if (!shouldExecute(gridCoverageReader)) {
                return;
            }
            ReferencedEnvelope envelope = coverageInfo.getLatLonBoundingBox();
            String aoi =  "" + envelope.getMinX() + ", " +
                    envelope.getMinY() + ", " +
                    envelope.getMaxX() + ", " +
                    envelope.getMaxY();
            coverageInfo.getParameters().put(FieldNames.AOI_FILTER, aoi);
            catalog.save(ModificationProxy.create(coverageInfo, CoverageInfo.class));
        } catch (Exception e) {
            log.error("Error updating grid geometry", e);
        }
    }

    protected void handleDataStoreAdd(CatalogAddEvent event) {
        FeatureTypeInfo featureTypeInfo = ModificationProxy.unwrap((FeatureTypeInfo) event.getSource());

        DataStoreInfo dataStoreInfo = featureTypeInfo.getStore();
        if (!shouldExecute(dataStoreInfo)) {
            return;
        }
        ReferencedEnvelope envelope = featureTypeInfo.getLatLonBoundingBox();
        String aoi =  "" + envelope.getMinX() + ", " +
                envelope.getMinY() + ", " +
                envelope.getMaxX() + ", " +
                envelope.getMaxY();
        dataStoreInfo.getConnectionParameters().put(KeyNames.AOI_FILTER, aoi);
        catalog.save(ModificationProxy.create(dataStoreInfo, DataStoreInfo.class));
    }

    protected void handleCoverageModify(CatalogPostModifyEvent event) {
        try {
            CoverageInfo coverageInfo = (CoverageInfo) event.getSource();
            GridCoverageReader gridCoverageReader = coverageInfo.getGridCoverageReader(null, null);
            if (!shouldExecute(gridCoverageReader)) {
                return;
            }

            String aoi = getAoi(event);
            if (aoi == null) {
                return;
            }
            coverageInfo.getParameters().put(FieldNames.AOI_FILTER, aoi);
            catalog.save(ModificationProxy.create(coverageInfo, CoverageInfo.class));
        } catch (Exception e) {
            log.error("Error updating grid geometry", e);
        }
    }

    protected void handleDataStoreModify(CatalogPostModifyEvent event) {
        FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) event.getSource();
        DataStoreInfo dataStoreInfo = featureTypeInfo.getStore();
        if (!shouldExecute(dataStoreInfo)) {
            return;
        }
        String aoi = getAoi(event);
        if (aoi == null) {
            return;
        }
        dataStoreInfo.getConnectionParameters().put(KeyNames.AOI_FILTER, aoi);
        catalog.save(ModificationProxy.create(dataStoreInfo, DataStoreInfo.class));

    }


    protected String getAoi(CatalogPostModifyEvent event) {
        int index = event.getPropertyNames().indexOf(BBOX_PROPERTY_NAME);
        if (index <= -1) {
            return null;
        }

        ReferencedEnvelope oldEnvelope = (ReferencedEnvelope) event.getOldValues().get(index);
        ReferencedEnvelope newEnvelope = (ReferencedEnvelope) event.getNewValues().get(index);

        if (oldEnvelope.equals(newEnvelope)) {
            return null;
        }

        return "" + newEnvelope.getMinX() + ", " +
                newEnvelope.getMinY() + ", " +
                newEnvelope.getMaxX() + ", " +
                newEnvelope.getMaxY();
    }

    protected boolean shouldExecute(DataStoreInfo dataStoreInfo) {
        return dataStoreInfo.getType().equals(StacVectorDataStoreFactorySpi.DISPLAY_NAME);
    }

    protected boolean shouldExecute(GridCoverageReader gridCoverageReader) throws IOException {
        return gridCoverageReader.getMetadataValue("class") != null &&
                gridCoverageReader.getMetadataValue("class").equals(StacMosaicReader.class.getName());
    }

}
