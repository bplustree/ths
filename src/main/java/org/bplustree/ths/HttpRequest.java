package org.bplustree.ths;

import java.io.IOException;
import java.io.InputStream;

/**
 * HTTP request object, which is passed as a parameter to {@link RequestHandler}.
 * 
 * <p> Programmers can use methods in {@code HttpRequest} to get information about a request.
 * If programmers need to obtain the body of the request, use {@link #getInputStream} method.
 * 
 * @author Tim Kong [timkong@zju.edu.cn]
 */
public interface HttpRequest {
    /**
     * Returns HTTP version of a request, which may be {@code 0.9}, {@code 1.0}, {@code 1.1}.
     * 
     * @return HTTP version string
     */
    String getHttpVersion();

    /**
     * Returns HTTP method of a request, which may be {@code GET}, {@code POST}, {@code HEAD}, {@code PUT},
     * {@code PUT}, etc.
     * 
     * @return HTTP method
     */
    HttpMethod getMethod();
    /**
     * Returns the URI of a request, which is not decoded.
     * 
     * @return a string specifying the URI part of a request
     */
    String getUri();

    /**
     * Returns the remote address of a request.
     * 
     * @return a string specifying the remote address
     */
    String getRemoteAddress();

    /**
     * Returns an {@link InputStream} object that can be used to read the body of a request.
     * 
     * @return an {@link InputStream} object, {@code null} if {@code Content-Length} is not in the header.
     */
    InputStream getInputStream() throws IOException;

    /**
     * Returns the body length of a request. If {@code Content-Length} is not in the header, 
     * {@code -1} is returned.
     * 
     * @return an integer specifying the body length of a request
     */
    int getContentLength();
    
    String getParameter(String key);
}
