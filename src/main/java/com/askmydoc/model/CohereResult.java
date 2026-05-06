package com.askmydoc.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CohereResult {
    private int index;
    @JsonProperty("relevance_score")
    private double relevanceScore;

    public double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
