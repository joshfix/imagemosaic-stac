package com.joshfix.stac.store.vector.feature;

import com.joshfix.stac.store.LayerParameters;
import com.joshfix.stac.store.mosaic.MosaicConfigurationProperties;
import com.joshfix.stac.store.utility.AssetLocator;
import lombok.extern.slf4j.Slf4j;
import org.geotools.data.FeatureReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author joshfix
 */
@Slf4j
public class StacFeatureIterator implements SimpleFeatureIterator, FeatureReader<SimpleFeatureType, SimpleFeature> {

    protected LayerParameters layerParameters;
    protected static final Set<String> IGNORE_PROPERTIES = new HashSet<>();
    protected Iterator<Map> iterator;
    protected Set<Map> finalSet;
    protected SimpleFeature next;
    protected GeometryFactory geomFac = new GeometryFactory();
    protected Name name;
    public static CoordinateReferenceSystem FEATURE_CRS;

    static {
        IGNORE_PROPERTIES.add("class");
        try {
            FEATURE_CRS = CRS.decode("EPSG:4326");
        } catch (FactoryException e) {
            throw new RuntimeException("Error decoding EPSG:4326.  Something is wrong in the world.", e);
        }
    }

    public StacFeatureIterator() {
    }

    public StacFeatureIterator(Set<Map> resultSet, Name name, LayerParameters layerParameters) {
        this.name = name;
        this.layerParameters = layerParameters;
        this.finalSet = resultSet;
        iterator = finalSet.iterator();
    }

    public void resetIterator() {
        iterator = finalSet.iterator();
    }

    @Override
    public boolean hasNext() {
        if (next == null && iterator.hasNext()) {
            next = buildFeature(iterator.next());
        }
        return next != null;
    }

    @Override
    public SimpleFeature next() throws NoSuchElementException {
        if (next == null) {
            hasNext();
        }
        SimpleFeature res = next;
        next = null;
        return res;
    }

    @Override
    public void close() {
        // do nothing
    }

    private SimpleFeature buildFeature(Map item) {
        if (item == null) {
            return null;
        }
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(name);
        typeBuilder.setCRS(FEATURE_CRS);

        Map<String, Object> properties = null;

        if (item.get("properties") != null) {
            //properties = PropertyUtils.describe(item.get("properties"));
            properties = (Map) item.get("properties");
            for (Entry<String, Object> property : properties.entrySet()) {
                if (!IGNORE_PROPERTIES.contains(property.getKey())) {
                    if (property.getValue() == null) {
                        typeBuilder.add(property.getKey(), String.class);
                    } else if (property.getValue().getClass().isAssignableFrom(String.class)) {
                        typeBuilder.add(property.getKey(), String.class);
                    } else if (property.getValue().getClass().isAssignableFrom(Integer.class)) {
                        typeBuilder.add(property.getKey(), Integer.class);
                    } else if (property.getValue().getClass().isAssignableFrom(Double.class)) {
                        typeBuilder.add(property.getKey(), Double.class);
                    } else if (property.getValue().getClass().isAssignableFrom(Boolean.class)) {
                        typeBuilder.add(property.getKey(), Boolean.class);
                    } else {
                        typeBuilder.add(property.getKey(), String.class);
                    }
                }
            }
        }

        if (item.get("geometry") != null) {
            //TODO: set the geometry type from the actual geometry type value???
            typeBuilder.add("geometry", Polygon.class);
            typeBuilder.setDefaultGeometry("geometry");
        }
        typeBuilder.add(MosaicConfigurationProperties.DEFAULT_LOCATION_ATTRIBUTE_VALUE, String.class);
        typeBuilder.add("crs", String.class);
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());

        if (properties != null) {
            for (Entry<String, Object> property : properties.entrySet()) {
                if (!IGNORE_PROPERTIES.contains(property.getKey())) {
                    featureBuilder.set(property.getKey(), property.getValue() == null ? null : property.getValue().toString());
                }
            }
        }


        // get the actual CRS from the image
        /*
        try {
            String url = AssetLocator.getAssetImageUrl(item, "B3");
            HttpGeoTiffReader reader = new HttpGeoTiffReader(new HttpImageInputStreamImpl(url));
            CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
            String srs = CRS.toSRS(crs);
            System.out.println("SRS: " + srs);

            featureBuilder.set("crs", srs);
        } catch (Exception e) {
            log.error("Error determining image CRS");
            throw new RuntimeException("Error determining image CRS", e);
        }
*/

        if (item.get("geometry") != null) {
            LinkedHashMap<?, ?> geom = (LinkedHashMap<?, ?>) item.get("geometry");
            String type = (String) geom.get("type");

            switch (type) {
                case "MultiPolygon": {
                    List<List<List<Double>>> coords = (ArrayList<List<List<Double>>>) geom.get("coordinates");
                    List<Polygon> results = new ArrayList<>();
                    for (List<List<Double>> inner : coords) {
                        List<Double> points = inner.get(0);
                        Polygon resultPolygon = buildPolygon(points);
                        results.add(resultPolygon);
                    }
                    MultiPolygon mp = geomFac.createMultiPolygon(results.toArray(new Polygon[results.size()]));
                    featureBuilder.set("geometry", mp);
                    break;
                }
                case "Polygon": {
                    List<List<Double>> coords = (ArrayList<List<Double>>) geom.get("coordinates");
                    List<Double> points = coords.get(0);
                    Polygon resultPolygon = buildPolygon(points);

                    featureBuilder.set("geometry", resultPolygon);
                    break;
                }
            }

        }

        featureBuilder.set(
                MosaicConfigurationProperties.DEFAULT_LOCATION_ATTRIBUTE_VALUE,
                AssetLocator.getAssetImageUrl(item, layerParameters.getAssetId()));
        return featureBuilder.buildFeature((String) item.get("id"));
    }

    private Polygon buildPolygon(List<Double> points) {
        ArrayList<Coordinate> resultCoordinates = new ArrayList<>();
        for (Object p : points) {
            @SuppressWarnings("unchecked")
            ArrayList<Double> point = (ArrayList<Double>) p;
            Coordinate c = new Coordinate(point.get(0), point.get(1));
            resultCoordinates.add(c);
        }
        Polygon resultPolygon = geomFac.createPolygon(resultCoordinates.toArray(new Coordinate[resultCoordinates.size()]));
        return resultPolygon;
    }

    @Override
    public SimpleFeatureType getFeatureType() {
        if (next == null) {
            hasNext();
        }
        return next.getFeatureType();
    }

}
