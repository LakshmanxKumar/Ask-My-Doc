package com.askmydoc.model;

public class ModelResponse {

    public ModelResponse() {
    }

    private String answer;
    private String support;

    public ModelResponse(String answer, String support) {
        this.answer = answer;
        this.support = support;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getSupport() {
        return support;
    }

    public void setSupport(String support) {
        this.support = support;
    }

}
