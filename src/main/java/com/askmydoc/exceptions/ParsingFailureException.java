package com.askmydoc.exceptions;

public class ParsingFailureException extends AskMyDocException {
    public ParsingFailureException(String msg) {
        super(msg);
    }

    public ParsingFailureException(Exception e) {
        super(e);
    }

    public ParsingFailureException(String msg, Exception e) {
        super(msg, e);
    }

}

