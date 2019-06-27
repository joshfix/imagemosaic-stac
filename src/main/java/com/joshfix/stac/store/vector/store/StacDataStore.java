package com.joshfix.stac.store.vector.store;

import com.joshfix.stac.store.vector.feature.StacFeatureSource;
import org.geotools.data.*;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author joshfix
 */
public abstract class StacDataStore implements DataStore {
    
    protected StacFeatureSource singleSource;
    
    protected Name namespace;

    public StacDataStore(Name namespace) {
    	this.namespace = namespace;
    }

    @Override
    public List<Name> getNames() throws IOException {
        return Collections.singletonList(singleSource.getName());
    }
    
    public Name getNamespace() {
		return namespace;
	}

	@Override
    public SimpleFeatureType getSchema(Name name) throws IOException {
        if (singleSource.getName().equals(name)) {
            return singleSource.getSchema();
        } else {
            return null;
        }
    }

    @Override
    public SimpleFeatureSource getFeatureSource(Name typeName)
            throws IOException {                        
        if (singleSource.getName().equals(typeName)) {
            return singleSource;
        } else {
            return null;
        }
    }

    @Override
    public void dispose() {
        //nothing to do
    }

	@Override
	public String[] getTypeNames() throws IOException {
		return new String[] { singleSource.getName().getLocalPart() };
	}

	@Override
	public SimpleFeatureType getSchema(String typeName) throws IOException {
		String namespaceLocalPart = singleSource.getName().getLocalPart();
		if (namespaceLocalPart.equals(typeName)) {
            return singleSource.getSchema();
        } else {
            return null;
        }
	}

	@Override
	public SimpleFeatureSource getFeatureSource(String typeName) throws IOException {
		if (singleSource.getName().getLocalPart().equals(typeName)) {
            return singleSource;
        } else {
            return null;
        }
	}

	@Override
	public FeatureReader<SimpleFeatureType, SimpleFeature> getFeatureReader(Query query, Transaction transaction)
			throws IOException {
		return singleSource.getFeatures(query).features();
	}


    @Override
    public ServiceInfo getInfo() {
        throw new UnsupportedOperationException();
    }


    @Override
    public void removeSchema(Name typeName) throws IOException {
        throw new UnsupportedOperationException();        
    }
	
	@Override
	public void createSchema(SimpleFeatureType featureType) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateSchema(Name typeName, SimpleFeatureType featureType) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateSchema(String typeName, SimpleFeatureType featureType) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeSchema(String typeName) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName, Filter filter,
			Transaction transaction) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriter(String typeName, Transaction transaction)
			throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public FeatureWriter<SimpleFeatureType, SimpleFeature> getFeatureWriterAppend(String typeName,
			Transaction transaction) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public LockingManager getLockingManager() {
		throw new UnsupportedOperationException();
	}

}
