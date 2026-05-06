package com.askmydoc.constants;


public class AppConstants {

    private AppConstants() {
    }

    public static final String CHUNKS_COLLECTION_NAME = "chunks";

    public static final String COHERE_BASE_URL = "https://api.cohere.com";

    public static final String COHERE_RERANKED_ENDPOINT = "/v2/rerank";

    public static final String AUTHORIZATION = "Authorization";

    public static final String RERANKER_MODEL = "rerank-v4.0-pro";

    public static final int RERANKED_TOP_N = 3;

    public static final String QNA_MODEL = "gemini-3-flash-preview";

    public static final String REWRITE_MODEL = "gemini-2.5-flash";

    public static final int QNA_MODEL_THINKING_BUDGET = 1024;

    public static final int REWRITE_MODEL_THINKING_BUDGET = 0;

}
