package org.bplustree.ths.asyncimpl;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bplustree.ths.HttpMethod;
import org.bplustree.ths.HttpServer;
import org.bplustree.ths.HttpServerException;
import org.bplustree.ths.RequestHandler;
import org.bplustree.ths.RequestHandlerException;
import org.bplustree.ths.common.HttpUtils;

import org.apache.log4j.Logger;

public class HttpServerImpl extends HttpServer {
    class TimeoutInputStream extends FilterInputStream {
        protected TimeoutInputStream(InputStream in) {
            super(in);
        }

        byte[] buffer = new byte[2048];
        int length, position;
        @Override
        public int read() throws IOException {
            if (position == length) {
                if (! fillBuffer()) {
                    return -1;
                }
            }
            return buffer[position++];
        }

        boolean fillBuffer() throws IOException {
            Future<Integer> future =  threadPool.submit(() -> {
                int b = in.read(buffer);
                if (b == -1) {
                    return b;
                }
                length = b;
                position = 0;
                return b;
            });
            try {
                return future.get(keepAliveTimeout, TimeUnit.SECONDS) != -1;
            } catch (TimeoutException e) {
                throw new ReadRequestTimeoutException();
            } catch (Throwable e) {
                throw new IOException(e);
            }
            
        }
        
    }

    final static String BAD_REQUEST_RETURN_CONTENT = 
    "HTTP/1.1 400 Bad Request\r\n" +
    "Date: %s\r\n" +
    "Server: %s\r\n" + 
    "Connection: close\r\n" + 
    "Content-Length: 38\r\n" +
    "\r\n" +
    "<h3>You have sent a bad request.</h3>\n";

    class RequestCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, HttpServerImpl> {

        @Override
        public void completed(AsynchronousSocketChannel channel, HttpServerImpl server) {
            serverChannel.accept(HttpServerImpl.this, this);
            threadPool.execute(() -> {
                try {
                    while (true) {
                        HttpRequestImpl request = null;
                        HttpResponseImpl response = null;
                        try {
                            request = readRequestHeader(channel); 
                            response = new HttpResponseImpl(Channels.newOutputStream(channel));
                            request.setRemoteAddress(channel.getRemoteAddress().toString());
                            
                            requestHandler.service(request, response);
                            
                            if (request.getContentLength() >= 0) {
                                request.getInputStream().readAllBytes();
                            }
                            if (! response.isEnded()) {
                                response.getOutputStream().close();
                            }
                        } catch (InvalidRequestException ire) {
                            ByteBuffer bb = ByteBuffer.allocate(512);
                            bb.put(String.format(BAD_REQUEST_RETURN_CONTENT, 
                                HttpUtils.getDateString(), 
                                HttpUtils.getServerProperty("server.name")
                                ).getBytes());
                            bb.flip();
                            try {
                                channel.write(bb).get();
                            } catch (Throwable e) {
                                // ignore
                            }
                            return;
                        } catch (ClientAbortException | ReadRequestTimeoutException err) {
                            return;

                        }  catch (IOException | RequestHandlerException err) {
                            if (! response.isCommitted()) {
                                try {
                                    response.sendError(500);
                                } catch (Throwable e) {
                                    // ignore
                                }
                            } else {
                                return;
                            }
                        }
                        finally {
                            if (request != null && response != null)
                                logger.info(String.format("\"%s %s HTTP/%s\" %03d %d \"%s\"", request.getMethod(), request.getUri(), request.getHttpVersion(), response.getStatusCode(), response.getBytesWritten(), request.getParameter("user-agent")));
                        }
                    }
                }
                finally {
                    try {
                        channel.close();
                    } catch (IOException ioe) {
                        // ignore
                    }
                    
                }
            });
            
        }

        @Override
        public void failed(Throwable exc, HttpServerImpl server) {
            // try again
            server.serverChannel.accept(HttpServerImpl.this, this);
        }

    }

    private AsynchronousServerSocketChannel serverChannel;
    private ExecutorService threadPool;
    private Thread backgroundThread;
    private boolean started;
    private RequestHandler requestHandler;
    private int threadPoolSize;
    private int keepAliveTimeout;
    private int listenPort;
    private String listenAddress;
    private String headerEncoding;

    Logger logger = Logger.getLogger("SimpleHttpServer");
    public HttpServerImpl() throws HttpServerException {
        keepAliveTimeout = Integer.valueOf(HttpUtils.getServerProperty("http.keepalive.timeout"));
        listenPort = Integer.valueOf(HttpUtils.getServerProperty("server.listen.port"));
        listenAddress = HttpUtils.getServerProperty("server.listen.address");
        threadPoolSize = Integer.valueOf(HttpUtils.getServerProperty("server.thread.pool.size"));
        headerEncoding = HttpUtils.getServerProperty("http.header.encoding");
        initRequestHandler();
    }

    @Override
    public void terminate() {
        backgroundThread.interrupt();
        threadPool.shutdownNow();
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    private String readFirstLine(InputStream input) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int p = 0;
            int c;
            while ((c = input.read()) != '\r') {
                if (c == -1) {
                    throw new ClientAbortException();
                }
                buffer[p++] = (byte) c;
            }
            if ((c = input.read()) == -1) {
                throw new ClientAbortException();
            }

            if (c != '\n') {
                throw new InvalidRequestException();
            }
            return new String(buffer, 0, p);
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    private HttpRequestImpl readRequestHeader(AsynchronousSocketChannel channel) throws IOException {
        InputStream input = new TimeoutInputStream(Channels.newInputStream(channel));
        byte[] buffer = new byte[1024];
        HttpRequestImpl request = new HttpRequestImpl(input);
        String firstLine = readFirstLine(input);
        String[] f = firstLine.split(" ");
        request.setMethod(HttpMethod.valueOf(f[0]));
        request.setUri(f[1]);
        request.setHttpVersion(f[2].substring(5));
        while (true) {
            int firstChar = input.read();
            int secondChar = input.read();
            if (firstChar == '\r' && secondChar == '\n') {
                break;
            }
            buffer[0] = (byte) firstChar;
            buffer[1] = (byte) secondChar;
            int p = 2;
            int b;
            while ((b = input.read()) != '\r') {
                buffer[p++] = (byte) b;
            }
            if (input.read() != '\n') {
                throw new InvalidRequestException();
            }
            String line = new String(buffer, 0, p, headerEncoding);
            String[] ss = line.split(":");
            request.setParameter(ss[0].toLowerCase(), ss[1].trim());
        }
        if (request.getParameter("content-length") != null) {
            request.setContentLength(Integer.parseInt(request.getParameter("content-length")));
        }

        return request;
    }

    private void initRequestHandler() throws HttpServerException {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends RequestHandler> c = (Class<? extends RequestHandler>) 
            ClassLoader.getSystemClassLoader().loadClass(HttpUtils.getServerProperty("server.request.handler"));
            requestHandler = c.getConstructor().newInstance();
        } catch (Throwable e) {
            throw new HttpServerException(e);
        }
    }

    @Override
    public void start() throws IOException {
        serverChannel = AsynchronousServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(listenAddress, listenPort));
        threadPool = Executors.newFixedThreadPool(threadPoolSize);
        (backgroundThread = new Thread(() -> {
            serverChannel.accept(this, new RequestCompletionHandler());
            while (true) {
                try {
                    Thread.sleep(Integer.MAX_VALUE);
                } catch (InterruptedException ie) {
                    try {
                        serverChannel.close();
                    } catch (IOException ioe) {
                        //
                    }
                    break;
                }
            }

        })).start();
    }
}
