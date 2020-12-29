package org.bplustree.ths;

/**
 * Thrown when an HTTP server encounters a problem (unable to start/terminate, memory 
 * overflow, etc.)
 * 
 * @author Tim Kong [timkong@zju.edu.cn]
 */
public class HttpServerException extends Exception {

    private static final long serialVersionUID = -390107528035727139L;
    
    public HttpServerException() {
    }

    public HttpServerException(String message) {
        super(message);
    }

    public HttpServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpServerException(Throwable cause) {
        super(cause);
    }
}
