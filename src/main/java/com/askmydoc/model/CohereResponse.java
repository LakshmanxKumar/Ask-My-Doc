package com.askmydoc.model;

import java.util.List;

public class CohereResponse {
    private List<CohereResult> results;

    public List<CohereResult> getResults() {
        return results;
    }

    public void setResults(List<CohereResult> results) {
        this.results = results;
    }
}
