package com.joshfix.stac.store.vector.web;

import com.joshfix.stac.store.mosaic.StacMosaicFormat;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.web.GeoServerApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author joshfix
 * Created on 2019-04-17
 */
public class StacCoverageListModel extends LoadableDetachableModel<List<CoverageInfo>> {
    //private static final long serialVersionUID = -7742496075623731474L;

    @Override
    protected List<CoverageInfo> load() {
        List<CoverageInfo> coverages = new ArrayList<>();
        for (CoverageInfo coverageStore : GeoServerApplication.get().getCatalog().getCoverages()) {
            if (coverageStore.getNativeFormat().equals(StacMosaicFormat.NAME)) {
                coverages.add(coverageStore);
            }
        }

        Collections.sort(
                coverages,
                (o1, o2) -> {
                    if (o1.getStore().getWorkspace().equals(o2.getStore().getWorkspace())) {
                        return o1.getName().compareTo(o2.getName());
                    }
                    return o1.getStore().getWorkspace().getName().compareTo(o2.getStore().getWorkspace().getName());
                });
        return coverages;
    }
}
