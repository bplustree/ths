package org.bplustree.ths;

import java.io.IOException;

/**
 * 
 * @author Tim Kong [timkong@zju.edu.cn]
 */
@FunctionalInterface
public interface RequestHandler {
    void service(HttpRequest request, HttpResponse response) throws RequestHandlerException, IOException;
}
