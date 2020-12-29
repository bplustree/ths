package org.bplustree.ths;

import java.io.IOException;
import java.io.OutputStream;

/**
 * HTTP response object that is passed as a parameter to {@link RequestHandler}.
 * 
 * Programmers can use methods like {@code setHeader}, {@code setHttpVersion} to 
 * change parameters of the header of a response. To write response body, please 
 * use {@link #getOutputStream()} to retrieve an {@link OutputStream}.
 * 
 * @author Tim Kong [timkong@zju.edu.cn]
 * 
 */
public interface HttpResponse {
    
    /**
     * Send an error response. It uses error pages specified by configuration file.
     * 
     * @param sc HTTP error status code, e.g. 403, 404, 405
     * @throws IOException if an I/O error occurs when sending the response
     * @throws IllegalStateException if the response has been committed
     */
    void sendError(int sc) throws IOException;

    /**
     * Send an error response with specified response body.
     * 
     * @param sc HTTP error status code, e.g. 403, 404, 405
     * @param message response body to send
     * @throws IOException if an I/O error occurs when sending the response
     * @throws IllegalStateException if the response has been committed
     * 
     */
    void sendError(int sc, String message) throws IOException;


    void sendRedirect(String target) throws IOException;

    /**
     * Set a response header with the given name and value.
     * 
     * @param name header name
     * @param value header value
     */
    void setHeader(String name, String value);
    void setStatusCode(int statusCode);
    void setHttpVersion(String version);
    void setContentLength(int length);
    void setContentType(String contentType);
    void setReasonPhrase(String reasonPhrase);
    OutputStream getOutputStream();

    String getHeader(String header);
    String getHttpVersion();
    int getContentLength();
    String getContentType();
    String getReasonPhrase();

    boolean isCommitted();
    boolean isEnded();

}
