package com.askmydoc.model;

import java.util.List;

public class QueryRequest {

    private String userQuery;

    private List<String> docIds;

    public String getUserQuery() {
        return userQuery;
    }

    public void setUserQuery(String userQuery) {
        this.userQuery = userQuery;
    }

    public List<String> getDocIds() {
        return docIds;
    }

    public void setDocIds(List<String> docIds) {
        this.docIds = docIds;
    }
}
