package com.askmydoc.model;

import java.util.List;

public class QueryRewriteResponse {

    private String correctedQuery;

    private List<String> rephrasedQueries;

    public QueryRewriteResponse() {
    }

    public QueryRewriteResponse(String correctedQuery, List<String> rephrasedQueries) {
        this.correctedQuery = correctedQuery;
        this.rephrasedQueries = rephrasedQueries;
    }

    public String getCorrectedQuery() {
        return correctedQuery;
    }

    public void setCorrectedQuery(String correctedQuery) {
        this.correctedQuery = correctedQuery;
    }

    public List<String> getRephrasedQueries() {
        return rephrasedQueries;
    }

    public void setRephrasedQueries(List<String> rephrasedQueries) {
        this.rephrasedQueries = rephrasedQueries;
    }

    @Override
    public String toString() {
        return "QueryRewriteResponse{" +
                "correctedQuery='" + correctedQuery + '\'' +
                ", rephrasedQueries=" + rephrasedQueries +
                '}';
    }
}