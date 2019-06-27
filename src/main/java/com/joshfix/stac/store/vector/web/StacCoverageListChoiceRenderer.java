package com.joshfix.stac.store.vector.web;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.StoreInfo;

/**
 * @author joshfix
 * Created on 2019-04-17
 */
public class StacCoverageListChoiceRenderer extends ChoiceRenderer<CoverageInfo> {

    public Object getDisplayValue(CoverageInfo info) {
        return new StringBuilder(info.getStore().getWorkspace().getName()).append(':').append(info.getName());
    }

    public String getIdValue(StoreInfo store, int arg1) {
        return store.getId();
    }
}
