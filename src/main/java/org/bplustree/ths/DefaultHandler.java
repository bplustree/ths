package org.bplustree.ths;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Stack;

import org.bplustree.ths.common.HttpUtils;

public class DefaultHandler implements RequestHandler {
    private String rootDirectory;
    private boolean acceptBytesRange;
    private String urlEncoding;
    private String[] directoryIndex;

    public DefaultHandler() {
        if ((rootDirectory = HttpUtils.getServerProperty("default.handler.root.directory")) == null) {
            rootDirectory = System.getProperty("user.home");
        };
        acceptBytesRange = Boolean.valueOf(HttpUtils.getServerProperty("default.handler.accept.bytesrange"));
        urlEncoding = HttpUtils.getServerProperty("http.url.encoding");
        directoryIndex = HttpUtils.getServerProperty("server.directory.index").split(",");
    }
    @Override
    public void service(HttpRequest request, HttpResponse response) throws RequestHandlerException, IOException {
        switch (request.getMethod()) {
            case HEAD:
            doHead(request, response);
            break;
            default:
            doGet(request, response);
        }
    }

    void doHead(HttpRequest request, HttpResponse response) throws IOException {
        try {
            String uri = URLDecoder.decode(request.getUri(), urlEncoding);
            String queryString = null;
            if (uri.contains("?")) {
                queryString = uri.substring(uri.indexOf('?') + 1);
                uri = uri.substring(0, uri.indexOf('?'));
            }

            String[] urlParts = uri.split("/");
            Stack<String> urlStack = new Stack<>();
            for (int i = 1; i < urlParts.length; i++) {
                if ("..".equals(urlParts[i])) {
                    if (! urlStack.empty()) {
                        urlStack.pop();
                    }
                } else if (! ".".equals(urlParts[i])) {
                    urlStack.push(urlParts[i]);
                }
            }
            if (urlStack.size() > 0 && urlStack.lastElement().startsWith(".")) {
                response.sendError(403, "<h3>You don't have permission to access a URI starting with \".\".</h3>\n");
                return;
            }

            String localFile = "file://" + rootDirectory + File.separator + String.join(File.separator, urlStack);
            
            Path localFilePath = Paths.get(new URI(localFile));
            Path realLocalFilePath = null;
            if (Files.isDirectory(localFilePath)) {
                if (! uri.endsWith("/")) {
                    response.sendRedirect(uri + "/" + (queryString != null ? "?" + queryString : ""));
                    return;
                }
                for (int i = 0; i < directoryIndex.length; i++) {
                    Path pi = Paths.get(new URI(localFile + File.separator + directoryIndex[i]));
                    if (Files.exists(pi)) {
                        realLocalFilePath = pi;
                        break;
                    }
                }
            } else {
                realLocalFilePath = localFilePath;
            }

            if (realLocalFilePath != null && ! Files.exists(realLocalFilePath)) {
                String content = "<h3>URI <i>" + uri + "</i> is not found on this server.</h3>\n";
                response.setStatusCode(404);
                response.setContentLength(content.getBytes().length);
                response.setContentType("text/html; charset=" + HttpUtils.getServerProperty("http.default.content.encoding"));
                return;
            }

            if (realLocalFilePath == null) {
                String content = "<h3>You don't have permission to access <i>" + uri + "</i>.</h3>\n";
                response.setStatusCode(403);
                response.setContentLength(content.getBytes().length);
                response.setContentType("text/html; charset=" + HttpUtils.getServerProperty("http.default.content.encoding"));
                return;
            }

            if (! Files.isReadable(realLocalFilePath)) {
                String content = "<h3>You don't have permission to access <i>" + uri + "</i>.</h3>\n";
                response.setStatusCode(403);
                response.setContentLength(content.getBytes().length);
                response.setContentType("text/html; charset=" + HttpUtils.getServerProperty("http.default.content.encoding"));
                return;
            }
            
            if (Files.size(realLocalFilePath) > Integer.MAX_VALUE) {
                String content = "<h3>The server encountered an error while serving the requested resource.</h3>\n";
                response.setStatusCode(500);
                response.setContentLength(content.getBytes().length);
                response.setContentType("text/html; charset=" + HttpUtils.getServerProperty("http.default.content.encoding"));
                return;
            }

            Date lastModifiedTime = new Date(Files.getLastModifiedTime(realLocalFilePath).toMillis());
            response.setHeader("Last-Modified", HttpUtils.getDateString(lastModifiedTime));
            response.setStatusCode(200);
            if (acceptBytesRange) {
                response.setHeader("Accept-Ranges", "bytes");
            }
            String etag = getEtag(realLocalFilePath);
            response.setHeader("ETag", "\"" + etag + "\"");
            String ext = getExtension(realLocalFilePath.toString());
            if (ext != null) {
                String mimeType = HttpUtils.getMimeType(ext);
                if (mimeType == null) {
                    mimeType = HttpUtils.getServerProperty("server.default.mime.type");
                }
                if (useCharacterEncoding(ext)) {
                    response.setContentType(mimeType + "; charset=" + HttpUtils.getServerProperty("http.default.content.encoding"));
                } else {
                    response.setContentType(mimeType);
                }
            } else {
                response.setContentType(HttpUtils.getServerProperty("server.default.mime.type"));
            }

            response.setContentLength((int) Files.size(realLocalFilePath));
            
        } catch (Exception e) {
            // ignore
        }
    }

    void doGet(HttpRequest request, HttpResponse response) throws IOException {
        try {
            String uri = URLDecoder.decode(request.getUri(), urlEncoding);
            String queryString = null;
            if (uri.contains("?")) {
                queryString = uri.substring(uri.indexOf('?') + 1);
                uri = uri.substring(0, uri.indexOf('?'));
            }

            String[] urlParts = uri.split("/");
            Stack<String> urlStack = new Stack<>();
            for (int i = 1; i < urlParts.length; i++) {
                if ("..".equals(urlParts[i])) {
                    if (! urlStack.empty()) {
                        urlStack.pop();
                    }
                } else if (! ".".equals(urlParts[i])) {
                    urlStack.push(urlParts[i]);
                }
            }
            if (urlStack.size() > 0 && urlStack.lastElement().startsWith(".")) {
                response.sendError(403, "<h3>You don't have permission to access a URI starting with \".\".</h3>\n");
                return;
            }

            String localFile = "file://" + rootDirectory + File.separator + String.join(File.separator, urlStack);
            
            Path localFilePath = Paths.get(new URI(localFile));
            Path realLocalFilePath = null;
            if (Files.isDirectory(localFilePath)) {
                if (! uri.endsWith("/")) {
                    response.sendRedirect(uri + "/" + (queryString != null ? "?" + queryString : ""));
                    return;
                }
                for (int i = 0; i < directoryIndex.length; i++) {
                    Path pi = Paths.get(new URI(localFile + File.separator + directoryIndex[i]));
                    if (Files.exists(pi)) {
                        realLocalFilePath = pi;
                        break;
                    }
                }
            } else {
                realLocalFilePath = localFilePath;
            }

            if (realLocalFilePath != null && ! Files.exists(realLocalFilePath)) {
                response.sendError(404, "<h3>URI <i>" + uri + "</i> is not found on this server.</h3>\n");
                return;
            }

            if (realLocalFilePath == null) {
                response.sendError(403, "<h3>You don't have permission to access <i>" + uri + "</i>.</h3>\n");
                return;
            }


            
            if (! Files.isReadable(realLocalFilePath)) {
                response.sendError(403);
                return;
            }
            
            if (Files.size(realLocalFilePath) > Integer.MAX_VALUE) {
                response.sendError(500);
                return;
            }

            Date lastModifiedTime = new Date(Files.getLastModifiedTime(realLocalFilePath).toMillis());
            response.setHeader("Last-Modified", HttpUtils.getDateString(lastModifiedTime));
            response.setStatusCode(200);
            if (acceptBytesRange) {
                response.setHeader("Accept-Ranges", "bytes");
            }
            String etag = getEtag(realLocalFilePath);
            response.setHeader("ETag", "\"" + etag + "\"");
            String ext = getExtension(realLocalFilePath.toString());
            if (ext != null) {
                String mimeType = HttpUtils.getMimeType(ext);
                if (mimeType == null) {
                    mimeType = HttpUtils.getServerProperty("server.default.mime.type");
                }
                if (useCharacterEncoding(ext)) {
                    response.setContentType(mimeType + "; charset=" + HttpUtils.getServerProperty("http.default.content.encoding"));
                } else {
                    response.setContentType(mimeType);
                }
            } else {
                response.setContentType(HttpUtils.getServerProperty("server.default.mime.type"));
            }

            response.setContentLength((int) Files.size(realLocalFilePath));
            FileChannel c = FileChannel.open(realLocalFilePath);
            InputStream in = Channels.newInputStream(c);
            
            byte[] buffer = new byte[2 << 11];
            int count;
            OutputStream os = response.getOutputStream();
            while ((count = in.read(buffer)) != -1) {
                os.write(buffer, 0, count);
            }

            in.close();
            os.close();
        } catch (Exception e) {
            // ignore
        }
    }

    boolean useCharacterEncoding(String ext) {
        return ("txt".equals(ext) || "html".equals(ext) || "htm".equals(ext) || "json".equals(ext) || "log".equals(ext)
        || "conf".equals(ext));
    }

    String getExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        if (idx < 0) {
            return null;
        }
        return fileName.substring(idx + 1);
    }

    String getEtag(Path filePath) throws IOException {
        return Long.toHexString(Files.getLastModifiedTime(filePath).toMillis())
         + "-"
         + Long.toHexString(Files.size(filePath));
        
    }
    
}