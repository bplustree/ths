package org.bplustree.ths;

import org.apache.log4j.Logger;

public class RunServer {
    public static void main(String[] args)  {
        try {
            HttpServer server = HttpServer.createServer();
            server.start();
        } catch (Throwable e) {
            Logger.getLogger("main").error("An error occurs while the server is starting", e);
            System.exit(127);
        }
    }
}