package org.bplustree.ths.asyncimpl;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.bplustree.ths.HttpResponse;
import org.bplustree.ths.common.HttpUtils;

class HttpResponseImpl implements HttpResponse {
    private String httpVersion = "1.1";
    private int statusCode = 200;
    private String reasonPhrase = "OK";
    private int contentLength = -1;
    private Map<String, String> parametersMap = new HashMap<>();
    private OutputStream innerOutputStream;
    private OutputStream responseOutputStream;
    private boolean committed;
    private boolean ended;
    private int bytesWritten;
    class ResponseOutputStream extends OutputStream {
        byte[] buffer = new byte[2 << 11];
        int current;
        @Override
        public synchronized void write(int b) throws IOException {
            if (! committed) {
                sendHeader();
            }
            if (current < buffer.length) {
                buffer[current++] = (byte) b;
            } else {
                flushBuffer();
                buffer[0] = (byte) b;
                current = 1;
            }

            bytesWritten++;
        }

        @Override
        public synchronized void close() throws IOException {
            if (! committed) {
                sendHeader();
            }
            if (current > 0) {
                flushBuffer();
                endChunk();
            } else {
                endChunk();
            }
            end();
        }

        void endChunk() throws IOException {
            if (contentLength < 0) {
                innerOutputStream.write("0\r\n\r\n".getBytes());
            }
        }

        synchronized void flushBuffer() throws IOException {
            try {
                if (contentLength < 0) {
                    innerOutputStream.write((Integer.toHexString(current) + "\r\n").getBytes());
                    innerOutputStream.write(buffer, 0, current);
                    innerOutputStream.write("\r\n".getBytes());
                } else {
                    innerOutputStream.write(buffer, 0, current);
                }
            } catch (IOException ioe) {
                throw new ClientAbortException(ioe);
            }

            current = 0;
        }
    }

    HttpResponseImpl(OutputStream os) {
        innerOutputStream = os;
        setContentType("text/html; charset=" + HttpUtils.getServerProperty("http.default.content.encoding"));
        setStatusCode(200);
        setHeader("Server", HttpUtils.getServerProperty("server.name") + "/" + HttpUtils.getServerProperty("server.version"));
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public synchronized void setStatusCode(int statusCode) {
        String reasonPhrase = HttpUtils.getReasonPhrase(statusCode);
        if (reasonPhrase == null) {
            throw new IllegalArgumentException();
        }
        this.reasonPhrase = reasonPhrase;
        this.statusCode = statusCode;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public synchronized void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }

    public int getContentLength() {
        return contentLength;
    }

    public synchronized void setContentLength(int contentLength) {
        this.contentLength = contentLength;
        parametersMap.put("Content-Length", String.valueOf(contentLength));
    }

    public Map<String, String> getParametersMap() {
        return parametersMap;
    }
    
    void sendHeader() throws IOException {
        innerOutputStream.write(String.format("HTTP/%s %03d %s\r\n", httpVersion, statusCode, reasonPhrase).getBytes());
        parametersMap.put("Date", HttpUtils.getDateString());
        if (contentLength < 0) {
            parametersMap.put("Transfer-Encoding", "chunked");
        }
        for (var e: parametersMap.entrySet()) {
            innerOutputStream.write(String.format("%s: %s\r\n", e.getKey(), e.getValue()).getBytes());
        }
        innerOutputStream.write("\r\n".getBytes());
        innerOutputStream.flush();
        committed = true;
    }

    public void sendError(int statusCode) throws IOException {
        if (committed) {
            throw new IllegalStateException();
        }
        setStatusCode(statusCode);
        setReasonPhrase(HttpUtils.getReasonPhrase(statusCode));
        String content = HttpUtils.getReasonPhrase(statusCode) + "\n";
        setContentLength(content.length());
        sendHeader();
        OutputStream os = getOutputStream();
        os.write(content.getBytes());
        os.close();
    }

    public void end() throws IOException {
        if (! committed) {
            sendHeader();
        }
        ended = true;
    }

    public boolean isCommitted() {
        return committed;
    }

    public boolean isEnded() {
        return ended;
    }

    public OutputStream getOutputStream() {
        if (responseOutputStream == null) {
            responseOutputStream = new ResponseOutputStream();
        }
        return responseOutputStream;
    }

    @Override
    public void sendRedirect(String target) throws IOException {
        if (committed) {
            throw new IllegalStateException();
        }
        setStatusCode(302);
        setContentType("text/html");
        byte[] content = ("<h3>Redirect to " + target + ".</h3>\n").getBytes();
        setContentLength(content.length);
        setHeader("Location", target);
        sendHeader();
        OutputStream os = getOutputStream();
        os.write(content);
        os.close();
    }

    @Override
    public void setHeader(String header, String value) {
        parametersMap.put(header, value);

    }


    @Override
    public void setContentType(String contentType) {
        setHeader("Content-Type", contentType);
    }

    @Override
    public String getHeader(String header) {
        return parametersMap.get(header);
    }


    @Override
    public String getContentType() {
        return parametersMap.get("Content-Type");
    }

    int getBytesWritten() {
        return bytesWritten;
    }

    @Override
    public void sendError(int statusCode, String message) throws IOException {
        if (committed) {
            throw new IllegalStateException();
        }
        setStatusCode(statusCode);
        byte[] content = message.getBytes(HttpUtils.getServerProperty("http.default.content.encoding"));
        setContentLength(content.length);
        sendHeader();
        OutputStream os = getOutputStream();
        os.write(content);
        os.close();

    }
}
