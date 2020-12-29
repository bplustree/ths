### Simple HTTP Server

#### 1 Introduction

`Simple HTTP Server` is an HTTP server written in Java. Programmers can write `RequestHandler`, which seems like Servlet, to handle HTTP requests and send back responses.

#### 2 How to use

Assume the JAR file of Simple HTTP Server named simplehttpserver.jar is located in `/path/to/jar/`, and your own request handler `YourHandler.class` is put in directory `/your/own/path/com/yourcompany/yourname/`, we can run:

```shell
java -cp /path/to/jar/simplehttpserver.jar:/your/own/path \
 -Dserver.request.handler=com.yourcompany.yourname.YourHandler \
com.bplustree.httpserver.RunServer
```

or with more parameters:

```shell
java -cp /path/to/jar/simplehttpserver.jar:/your/own/path \
 -Dserver.port=9000 \
 -Dserver.name=SimpleHttpServer \
 -Dserver.request.handler=com.yourcompany.yourname.YourHandler \
 com.bplustree.httpserver.RunServer
```

to start the server.

You can specify some parameters that may override default values:

| Parameter | Default Value | Description |
| ----- | ----- | ----- |
| server.port | 8000 | Listen port of the server |
| server.listen.address | 0.0.0.0 | Listen address of the server |
| server.name | SimpleHttpServer | Server name, used in HTTP response header |
| server.version | 1.0 | Server version, used in HTTP response header |
| server.request.handler | com.bplustree.httpserver.DefaultHandler | Request handler class |
| server.directory.index | index.html,index.htm,default.html,default.htm | Directory Index |

#### 3 RequestHandler example

You can write a `RequestHandler` like `Servlet`.

```java
public class HandlerExample implements RequestHandler {
    @Override
    public void service(HttpRequest request, HttpResponse response) throws RequestHandlerException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        response.getOutputStream().write(("<h2>You are accessing <i>" + request.getUri() + "</i></h2>\n").getBytes());
    }
}
```

