package com.askmydoc.constants;

public class Prompts {

    private Prompts() {
    }

    public static final String USER_PROMPT_TEMPLATE = """
            You are given a context and a question.
            
            Follow these rules strictly:
            - Answer ONLY using the provided context.
            - Provide one supporting quote copied EXACTLY from the context.
            - Do NOT paraphrase the quote.
            - If the answer is not present, set:
              answer = "I don't have enough information to answer."
              support = ""
            
            Return ONLY valid JSON in the following format:
            {
              "answer": "...",
              "support": "..."
            }
            
            Do NOT include any explanation, text, or markdown outside the JSON.
            
            Context:
            %s
            
            Question:
            %s
            """;

    public static final String SYSTEM_PROMPT = """
            You are a helpful assistant.
            
            Answer ONLY using the provided context.
            
            You may make simple logical inferences ONLY if they are strongly supported by the context.
            
            Do NOT use outside knowledge.
            Do NOT invent facts.
            
            For every answer:
            - Include a supporting quote from the context.
            - The quote must be copied EXACTLY from the context.
            - Do NOT paraphrase the quote.
            
            If the answer cannot be reasonably supported or inferred from the context:
            - Respond: "I don't have enough information to answer."
            """;

}