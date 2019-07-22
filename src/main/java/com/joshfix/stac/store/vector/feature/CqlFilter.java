package com.joshfix.stac.store.vector.feature;

import lombok.Data;

/**
 * @author joshfix
 * Created on 2019-07-22
 */
@Data
public class CqlFilter {

    private String query;
    private String ids;

    public CqlFilter query(String query) {
        this.query = query;
        return this;
    }

    public CqlFilter ids(String ids) {
        this.ids = ids;
        return this;
    }
}
