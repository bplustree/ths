package org.bplustree.ths.asyncimpl;

public class InvalidRequestException extends java.io.IOException {

    private static final long serialVersionUID = -2982326171828982126L;
    
    public InvalidRequestException() {
    }

    public InvalidRequestException(String message) {
        super(message);
    }
}
