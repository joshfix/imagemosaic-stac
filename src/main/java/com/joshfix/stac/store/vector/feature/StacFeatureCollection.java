package com.joshfix.stac.store.vector.feature;

import com.joshfix.stac.store.mosaic.LayerParameters;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.ProgressListener;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class StacFeatureCollection implements SimpleFeatureCollection {

    private final Set<Map> resultSet;
    private SimpleFeatureType schema;
    protected LayerParameters layerParameters;
    protected Name name;

    public StacFeatureCollection(Set<Map> resultSet, Name name, LayerParameters layerParameters, SimpleFeatureType schema) {
        this.resultSet = resultSet;
        this.name = name;
        this.layerParameters = layerParameters;
        this.schema = schema;
    }

    /**
     *
     * @see org.geotools.feature.FeatureCollection#accepts(FeatureVisitor,
     *      ProgressListener)
     */
    @Override
    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
        DataUtilities.visit(this, visitor, progress);
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StacFeatureIterator features() {
        // need to return a fresh iterator!!
        return new StacFeatureIterator(resultSet, name, layerParameters);
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public boolean isEmpty() {
        return resultSet.isEmpty();
    }

    @Override
    public int size() {
        return resultSet.size();
    }

    @Override
    public SimpleFeatureCollection sort(SortBy order) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        return resultSet.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return resultSet.toArray(a);
    }

    @Override
    public String getID() {
        return Integer.toString(resultSet.hashCode());
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return DataUtilities.bounds(this);
    }

}
