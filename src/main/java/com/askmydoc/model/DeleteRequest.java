package com.askmydoc.model;

import java.util.List;

public class DeleteRequest {
    private List<String> docIds;

    public List<String> getDocIds() {
        return docIds;
    }

    public void setDocIds(List<String> docIds) {
        this.docIds = docIds;
    }
}
