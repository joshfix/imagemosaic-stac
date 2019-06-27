package com.joshfix.stac.store.mosaic;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;

import java.io.Serializable;


/**
 * @author joshfix
 * Created on 2019-04-02
 */
@Slf4j
public class StacCoverageStoreEditPanel extends StoreEditPanel {

    private static final long serialVersionUID = 1L;

    public StacCoverageStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);

        final IModel formModel = storeEditForm.getModel();
        Object o = formModel.getObject();

        try {
            o = ModificationProxy.unwrap(o);
        } catch (Exception e) {
            // it wasn't a proxy... do nothing
        }

        // set up the model with the values in the vector
        CoverageStoreInfoImpl coverageStoreInfo = (CoverageStoreInfoImpl)o;

        // TODO remove
        coverageStoreInfo.setName("stac");

        StacModel model = new StacModel(coverageStoreInfo);
        String url = coverageStoreInfo.getURL();
        if (null != url && !url.isEmpty() && !url.equalsIgnoreCase("file:data/example.extension")) {
            if (url.indexOf("?") > 0) {
                String[] urlAndDefaultItemId = url.split("\\?");
                model.setUrl(urlAndDefaultItemId[0]);
                model.setDefaultItemId(urlAndDefaultItemId[1]);
            } else {
                model.setUrl(url);
            }
        } else {
            // TODO remove
            model.setUrl("https://stac.boundlessgeo.io/stac/search");
            model.setDefaultItemId("LC81780802019134LGN00");
        }

        TextParamPanel urlPanel = new TextParamPanel(
                "urlPanel",
                new PropertyModel(model, "url"),
                new ResourceModel("url", "STAC URL"),
                true);
        add(urlPanel);

        TextParamPanel defaultItemIdPanel = new TextParamPanel(
                "defaultItemIdPanel",
                new PropertyModel(model, "defaultItemId"),
                new ResourceModel("defaultItemId", "STAC default item ID"),
                true);
        add(defaultItemIdPanel);

    }

    /**
     * A little hacky... basically after wicket saves both the url and default item id to this model class, combine
     * the values and save them in the url field of the coverage vector
     */
    @Data
    static class StacModel implements Serializable {
        private CoverageStoreInfoImpl coverageStoreInfo;
        private String url;
        private String defaultItemId = "default";

        public StacModel(CoverageStoreInfoImpl coverageStoreInfo) {
            this.coverageStoreInfo = coverageStoreInfo;
        }

        private void populateFormModel() {
            if (null != url) {
                coverageStoreInfo.setURL(url + "?" + defaultItemId);
            }
        }

        public void setUrl(String url) {
            this.url = url;
            populateFormModel();
        }

        public void setDefaultItemId(String defaultItemId) {
            this.defaultItemId = defaultItemId;
            populateFormModel();
        }
    }
}



