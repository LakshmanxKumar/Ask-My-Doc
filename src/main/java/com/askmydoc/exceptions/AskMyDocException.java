package com.askmydoc.exceptions;

public class AskMyDocException extends Exception {
    public AskMyDocException(String msg) {
        super(msg);
    }

    public AskMyDocException(Exception e) {
        super(e);
    }

    public AskMyDocException(String msg, Exception e) {
        super(msg, e);
    }
}
