package com.joshfix.stac.store.vector.web;

import com.joshfix.stac.store.FieldNames;
import com.joshfix.stac.store.KeyNames;
import com.joshfix.stac.store.mosaic.LayerParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.wicket.Select2DropDownChoice;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

import java.io.Serializable;
import java.util.Map;

/**
 * @author joshfix
 * Created on 2019-04-16
 */
public class StacVectorStoreEditPanel extends StoreEditPanel {

    public StacVectorStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);

        final IModel model = storeEditForm.getModel();
        setDefaultModel(model);
        setOutputMarkupId(true);

        DataStoreInfo dataStore = (DataStoreInfo) model.getObject();

        NamespaceInfo namespaceInfo = getCatalog().getNamespaceByPrefix(dataStore.getWorkspace().getName());
        Name name = new NameImpl(namespaceInfo.getURI(), namespaceInfo.getPrefix());
        dataStore.getConnectionParameters().put("namespace", (Serializable)name);

        final IModel paramsModel = new PropertyModel(model, "connectionParameters");

        TextParamPanel assetIdPanel = new TextParamPanel(
                "assetIdPanel",
                new PropertyModel(paramsModel, KeyNames.ASSET_ID_KEY),
                new ResourceModel(KeyNames.ASSET_ID_KEY, FieldNames.ASSET_ID_NAME),
                true);
        assetIdPanel.setOutputMarkupId(true);
        add(assetIdPanel);

        TextParamPanel urlPanel = new TextParamPanel(
                "urlPanel",
                new PropertyModel(paramsModel, KeyNames.SERVICE_URL_KEY),
                new ResourceModel(KeyNames.SERVICE_URL_KEY, FieldNames.SERVICE_URL_NAME),
                true);
        urlPanel.setOutputMarkupId(true);
        add(urlPanel);

        TextParamPanel collectionPanel = new TextParamPanel(
                "collectionPanel",
                new PropertyModel(paramsModel, KeyNames.COLLECTION_KEY),
                new ResourceModel(KeyNames.COLLECTION_KEY, FieldNames.COLLECTION_NAME),
                true);
        collectionPanel.setOutputMarkupId(true);
        add(collectionPanel);

        TextParamPanel stacFilterPanel = new TextParamPanel(
                "stacFilterPanel",
                new PropertyModel(paramsModel, KeyNames.STAC_QUERY_KEY),
                new ResourceModel(KeyNames.STAC_QUERY_KEY, FieldNames.STAC_QUERY_NAME),
                false);
        stacFilterPanel.setOutputMarkupId(true);
        add(stacFilterPanel);

        TextParamPanel maxFeaturesPanel = new TextParamPanel(
                "maxFeaturesPanel",
                new PropertyModel(paramsModel, KeyNames.MAX_FEATURES_KEY),
                new ResourceModel(KeyNames.MAX_FEATURES_KEY, FieldNames.MAX_FEATURES_NAME),
                true);
        maxFeaturesPanel.setOutputMarkupId(true);
        add(maxFeaturesPanel);

        CheckBoxParamPanel useBboxPanel = new CheckBoxParamPanel(
                "useBboxPanel",
                new PropertyModel(paramsModel, KeyNames.USE_BBOX_KEY),
                new ResourceModel(KeyNames.USE_BBOX_KEY, FieldNames.USE_BBOX_NAME)
        );
        useBboxPanel.setOutputMarkupId(true);
        add(useBboxPanel);

        final Select2DropDownChoice<CoverageInfo> coverages =
                new Select2DropDownChoice<>(
                        "coveragesDropDown",
                        new Model<>(),
                        new StacCoverageListModel(),
                        new StacCoverageListChoiceRenderer());
        coverages.setOutputMarkupId(true);
        coverages.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        if (coverages.getModelObject() != null) {
                            CoverageInfo store = coverages.getModelObject();
                            Map<String, Serializable> params = store.getParameters();
                            Map<String, Serializable> connectionParams = dataStore.getConnectionParameters();
                            connectionParams.put(KeyNames.USE_BBOX_KEY, params.get(FieldNames.USE_BBOX_NAME));
                            connectionParams.put(KeyNames.STAC_QUERY_KEY, params.get(FieldNames.STAC_QUERY_NAME));
                            connectionParams.put(KeyNames.COLLECTION_KEY, params.get(FieldNames.COLLECTION_NAME));
                            connectionParams.put(KeyNames.ASSET_ID_KEY, params.get(FieldNames.ASSET_ID_NAME));
                        } else {
                            Map<String, Serializable> connectionParams = dataStore.getConnectionParameters();
                            connectionParams.put(KeyNames.USE_BBOX_KEY, LayerParameters.USE_BBOX_DEFAULT);
                            connectionParams.put(KeyNames.STAC_QUERY_KEY, "");
                            connectionParams.put(KeyNames.COLLECTION_KEY, LayerParameters.COLLECTION_DEFAULT);
                            connectionParams.put(KeyNames.MAX_FEATURES_KEY, LayerParameters.MAX_FEATURES_DEFAULT);
                            connectionParams.put(KeyNames.ASSET_ID_KEY, "");
                        }

                        target.add(stacFilterPanel);
                        target.add(useBboxPanel);
                        target.add(collectionPanel);
                        target.add(maxFeaturesPanel);
                    }
                });
        add(coverages);
    }

}