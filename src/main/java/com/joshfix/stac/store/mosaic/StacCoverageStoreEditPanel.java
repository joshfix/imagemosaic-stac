package com.joshfix.stac.store.mosaic;

import com.joshfix.stac.store.FieldNames;
import com.joshfix.stac.store.KeyNames;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.Serializable;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;


/**
 * @author joshfix
 * Created on 2019-04-02
 */
@Slf4j
public class StacCoverageStoreEditPanel extends StoreEditPanel {

    private static final long serialVersionUID = 1L;
    public static final String DEFAULT_STAC_FILTER = "id<>default";

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
        StacModel model = new StacModel(coverageStoreInfo);
        String url = coverageStoreInfo.getURL();

        // TODO remove
        coverageStoreInfo.setName("stac");

        if (null != url && !url.isEmpty() && !url.equalsIgnoreCase("file:data/example.extension")) {
            URI uri = new DefaultUriBuilderFactory(url).builder().build();
            int port = uri.getPort();
            String source = port == 80 || port == -1 ? uri.getScheme() + "://" + uri.getHost() + uri.getPath() :
                    uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
            model.setUrl(source);

            List<NameValuePair> params = URLEncodedUtils.parse(url, Charset.forName("UTF-8"), '?', '&');
            params.forEach(param -> {
                switch (param.getName()) {
                    case KeyNames.SAMPLE_ITEM_ID:
                        model.setSampleItemId(param.getValue());
                        break;
                    case KeyNames.ASSET_ID:
                        model.setAssetId(param.getValue());
                    case KeyNames.STORE_STAC_FILTER:
                        model.setStoreStacFilter(param.getValue());
                        break;
                    case KeyNames.COLLECTION:
                        model.setCollection(param.getValue());
                        break;
                }
            });
        } else {
            // TODO remove
            model.setUrl("https://stac.boundlessgeo.io/search");
            model.setCollection("landsat-8-l1");
            model.setStoreStacFilter("");
            model.setAssetId("B2");
        }

        TextParamPanel<String> urlPanel = new TextParamPanel<>(
                KeyNames.URL + "Panel",
                new PropertyModel<>(model, KeyNames.URL),
                new ResourceModel(KeyNames.URL, FieldNames.URL),
                true);
        add(urlPanel);

        TextParamPanel<String> assetIdPanel = new TextParamPanel<>(
                KeyNames.ASSET_ID + "Panel",
                new PropertyModel<>(model, KeyNames.ASSET_ID),
                new ResourceModel(KeyNames.ASSET_ID, FieldNames.ASSET_ID),
                true);
        add(assetIdPanel);

        TextParamPanel<String> collectionPanel = new TextParamPanel<>(
                KeyNames.COLLECTION + "Panel",
                new PropertyModel<>(model, KeyNames.COLLECTION),
                new ResourceModel(KeyNames.COLLECTION, FieldNames.COLLECTION),
                true);
        add(collectionPanel);

        TextParamPanel<String> sampleItemIdPanel = new TextParamPanel<>(
                KeyNames.SAMPLE_ITEM_ID + "Panel",
                new PropertyModel<>(model, KeyNames.SAMPLE_ITEM_ID),
                new ResourceModel(KeyNames.SAMPLE_ITEM_ID, FieldNames.SAMPLE_ITEM_ID),
                false);
        add(sampleItemIdPanel);

        TextParamPanel<String> storeStacFilterPanel = new TextParamPanel<>(
                KeyNames.STORE_STAC_FILTER + "Panel",
                new PropertyModel<>(model, KeyNames.STORE_STAC_FILTER),
                new ResourceModel(KeyNames.STORE_STAC_FILTER, FieldNames.STORE_STAC_FILTER),
                false);
        add(storeStacFilterPanel);

    }

    /**
     * A little hacky... basically after wicket saves both the url and default item id to this model class, combine
     * the values and save them in the url field of the coverage vector
     */
    @Data
    public static class StacModel implements Serializable {
        private CoverageStoreInfoImpl coverageStoreInfo;
        private String url = "https://stac.boundlessgeo.io/search";
        private String collection;
        private String sampleItemId;
        private String storeStacFilter = DEFAULT_STAC_FILTER;
        private String assetId;

        public StacModel(CoverageStoreInfoImpl coverageStoreInfo) {
            this.coverageStoreInfo = coverageStoreInfo;
        }

        private void populateFormModel() {
            if (null != url) {

                String uri = new DefaultUriBuilderFactory(url).builder()
                        .queryParam(KeyNames.SAMPLE_ITEM_ID, sampleItemId)
                        .queryParam(KeyNames.STORE_STAC_FILTER, storeStacFilter)
                        .queryParam(KeyNames.COLLECTION, collection)
                        .queryParam(KeyNames.ASSET_ID, assetId)
                        .build()
                        .toString();
                coverageStoreInfo.setURL(uri);
            }

        }

        public void setUrl(String url) {
            this.url = url;
            populateFormModel();
        }

        public void setSampleItemId(String sampleItemId) {
            this.sampleItemId = sampleItemId;
            populateFormModel();
        }

        public void setStoreStacFilter(String storeStacFilter) {
            this.storeStacFilter = storeStacFilter;
            populateFormModel();
        }

        public void setCollection(String collection) {
            this.collection = collection;
            populateFormModel();
        }

        public void setAssetId(String assetId) {
            this.assetId = assetId;
            populateFormModel();
        }

    }
}



