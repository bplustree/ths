package org.bplustree.ths.common;

import java.util.Date;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

public class HttpUtils {
    static Map<Integer, String> reasonPhraseMap = new HashMap<>();
    static Map<String, String> mimeTypesMap = new HashMap<>();
    static Map<String, String> serverParameters = new HashMap<>();

    static void loadReasonPhrase() {

        InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream("org/bplustree/ths/reason.phrases");
        Scanner scanner = new Scanner(in);
        while (scanner.hasNextLine()) {
            String[] s = scanner.nextLine().split("=");
            reasonPhraseMap.put(Integer.valueOf(s[0]), s[1]);
        }
        scanner.close();
    }
    static {

        loadReasonPhrase();
        InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream("org/bplustree/ths/mime.types");
        Scanner scanner = new Scanner(in);
        while (scanner.hasNextLine()) {
            String[] s = scanner.nextLine().split("=");
            mimeTypesMap.put(s[0], s[1]);
        }
        scanner.close();

        Properties p = new Properties();
        in = ClassLoader.getSystemClassLoader().getResourceAsStream("org.bplustree.ths/server.properties");
        try {
            p.load(in);
        } catch (IOException e) {
            // ignore
        }
        for (var e : p.entrySet()) {
            String overridenValue = System.getProperty((String) e.getKey());
            if (overridenValue != null) {
                serverParameters.put((String) e.getKey(), overridenValue);
            } else {
                serverParameters.put((String) e.getKey(), (String) e.getValue());
            }
        }
    }

    static SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
    public static String getDateString() {
        return dateFormat.format(new Date());
    }

    public static String getDateString(Date time) {
        return dateFormat.format(time);
    }

    public static String getReasonPhrase(int statusCode) {
        return reasonPhraseMap.get(statusCode);
    }

    public static String getMimeType(String ext) {
        return mimeTypesMap.get(ext);
    }

    public static String getServerProperty(String key) {
        return serverParameters.get(key);
    }

}
