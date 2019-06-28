package com.joshfix.stac.store.utility;

import lombok.Data;

@Data
public class SearchRequest {

    private double[] bbox;
    private String time;
    private String query;
    private Integer limit;
    private Integer page;
    private String[] ids;
    private String[] collections;
    private String[] fields;

    public SearchRequest bbox(double[] bbox) {
        setBbox(bbox);
        return this;
    }

    public SearchRequest time(String time) {
        setTime(time);
        return this;
    }

    public SearchRequest query(String query) {
        setQuery(query);
        return this;
    }

    public SearchRequest limit(Integer limit) {
        setLimit(limit);
        return this;
    }

    public SearchRequest page(Integer page) {
        setPage(page);
        return this;
    }

    public SearchRequest ids(String[] ids) {
        setIds(ids);
        return this;
    }

    public SearchRequest collections(String[] collections) {
        setCollections(collections);
        return this;
    }

    public SearchRequest fields(String[] fields) {
        setFields(fields);
        return this;
    }

}