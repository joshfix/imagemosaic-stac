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
    private String[] propertyname;

}