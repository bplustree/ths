package org.bplustree.ths.asyncimpl;

import java.io.IOException;

class ClientAbortException extends IOException {

    private static final long serialVersionUID = -529290647608492112L;
    
    public ClientAbortException() {
    }

    public ClientAbortException(Throwable cause) {
        super(cause);
    }

    public ClientAbortException(String message, Throwable cause) {
        super(message, cause);
    }
}
