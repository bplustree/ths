package org.bplustree.ths;

import java.io.IOException;

import org.bplustree.ths.common.HttpUtils;

/**
 * HTTP Server base class. All implementations should extends this class.
 * 
 * @author Tim Kong [timkong@zju.edu.cn]
 * 
 */
public abstract class HttpServer {
    public abstract void start() throws IOException, HttpServerException;
    public abstract boolean isStarted();
    public abstract void terminate() throws IOException, HttpServerException;

    public static HttpServer createServer() throws HttpServerException {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends HttpServer> c = (Class<? extends HttpServer>) ClassLoader.getSystemClassLoader().loadClass(HttpUtils.getServerProperty("server.impl.class"));
            return c.getConstructor().newInstance();
        } catch (Throwable e) {
            throw new HttpServerException(e);
        }
    }
}
