package org.bplustree.ths.asyncimpl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.bplustree.ths.HttpMethod;
import org.bplustree.ths.HttpRequest;

class HttpRequestImpl implements HttpRequest {
    private String httpVersion;
    private HttpMethod method;
    private String uri;
    private String remoteAddress;
    private int contentLength = -1;
    private Map<String, String> parametersMap;
    private InputStream innerInputStream;

    class RequestInputStream extends InputStream {
        long byteRead;
        @Override
        public int read() throws IOException {
            if (byteRead == getContentLength()) {
                return -1;
            }
            try {
                return innerInputStream.read();
            } finally {
                byteRead++;
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            len = (int) Math.min((long) len, getContentLength() - byteRead);
            return innerInputStream.read(b, off, len);
        }
    }

    private InputStream requestInputStream;

    HttpRequestImpl(InputStream is) {
        innerInputStream = new BufferedInputStream(is);
        parametersMap = new HashMap<>();
    }
    public String getHttpVersion() {
        return httpVersion;
    }

    public HttpMethod getMethod() {
        return method;
    }

    void setMethod(HttpMethod method) {
        this.method = method;
    }
    void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public String getUri() {
        return uri;
    }

    void setUri(String uri) {
        this.uri = uri;
    }

    public int getContentLength() {
        return contentLength;
    }

    void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public String getParameter(String key) {
        return parametersMap.get(key);
    }

    void setParameter(String key, String value) {
        parametersMap.put(key, value);
    }

    public InputStream getInputStream() {
        if (requestInputStream == null) {
            synchronized (this) {
                if (requestInputStream == null) {
                    requestInputStream = new RequestInputStream();
                }
            }
        }
        return requestInputStream;
    }

    public OutputStream getOutputStream() {
        return null;
    }

    @Override
    public String getRemoteAddress() {
        return remoteAddress;
    }

    void setRemoteAddress(String addr) {
        remoteAddress = addr;
    }
}