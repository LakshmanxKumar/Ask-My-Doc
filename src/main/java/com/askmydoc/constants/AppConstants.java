package com.askmydoc.constants;


public class AppConstants {

    private AppConstants() {
    }

    public static final String COHERE_BASE_URL = "https://api.cohere.com";

    public static final String COHERE_RERANKED_ENDPOINT = "/v2/rerank";

    public static final String AUTHORIZATION = "Authorization";

    public static final String RERANKER_MODEL = "rerank-v4.0-pro";

    public static final int RERANKED_TOP_N = 5;

    public static final int DB_TOP_K = 10;

    public static final String QNA_MODEL = "gemini-2.5-flash";

    public static final int QNA_MODEL_THINKING_BUDGET = 256;

    public static final String NOT_ENOUGH_INFO_MSG  = "I don't have enough information to answer.";

}
