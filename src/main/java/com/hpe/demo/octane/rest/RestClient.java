package com.hpe.demo.octane.rest;

import com.hpe.demo.octane.Settings;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by panuska on 10/26/12.
 */
public class RestClient {

    private static Logger log = Logger.getLogger(RestClient.class.getName());

    private HashMap<String, String> cookies = new HashMap<>();

    private void addCookieList(List<String> cookieList) {
        if (cookieList == null) {
            return;
        }
        log.debug("Adding cookies:");
        for (String cookie : cookieList) {
            log.debug(cookie);
            String key = cookie.substring(0, cookie.indexOf('='));
            String value = cookie.substring(key.length() + 1, cookie.indexOf(";", key.length()));
            cookies.put(key, value);
        }
        log.debug("New cookies: " + cookies.toString());
    }

    public void addCookieValue(String cookie, String value) {
        cookies.put(cookie, value);
    }

    private String getCookieList() {
        if (cookies.size() == 0) {
            return "";
        }
        Set<String> keys = cookies.keySet();
        StringBuilder cookieList = new StringBuilder();
        for (String key : keys) {
            String value = cookies.get(key);
            cookieList.append(key).append('=').append(value).append(';');
        }
        return cookieList.substring(0, cookieList.length() - 1); // remove the last ';'
    }

    public String getCookie(String cookieName) {
        return cookies.get(cookieName);
    }

    /**
     * Posts given data to the given address and collects (re-send) cookies.
     * Also handles redirects; only first time it does POST, then it does GET.
     *
     * @param urlAddress where the request is being sent
     * @param formData   if null, GET method is used; POST otherwise
     * @param method     which method will be used
     * @return response of the request
     */
    public synchronized HttpResponse doRequest(String urlAddress, String formData, Method method, ContentType contentType) throws IllegalRestStateException {
        HttpURLConnection conn = null;
        try {
            boolean redirect = false;
            do {
                if (method == Method.GET && formData != null) {
                    urlAddress = urlAddress + "?" + formData;
                    formData = null;
                }
                log.debug("At: " + urlAddress);
                URL url = new URL(urlAddress);
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(formData != null);
                conn.setDoInput(true);
                conn.setAllowUserInteraction(false);
                conn.setInstanceFollowRedirects(false);
                int connectionTimeout = Settings.getSettings().getConnectionTimeout();
                conn.setReadTimeout(connectionTimeout);
                conn.setConnectTimeout(connectionTimeout);
                String methodName = redirect ? "GET" : method.toString();
                log.debug("Doing " + methodName);
                conn.setRequestMethod(methodName);
                switch (contentType) {
                    case JSON_JSON: {
                        log.debug("JSON_JSON documents");
                        conn.setRequestProperty("Content-type", "application/json;type=collection");
                        conn.setRequestProperty("Accept", "application/json");
                        break;
                    }
                }
                if (headerName != null) {
                    conn.setRequestProperty(headerName, headerValue);
                    log.debug("Setting " + headerName + ": " + headerValue);
                }
                conn.setRequestProperty("HPECLIENTTYPE", "HPE_MQM_UI");

                String cookieList = getCookieList();
                log.debug("Sending cookies: " + cookieList);
                conn.setRequestProperty("Cookie", cookieList);

                // write the data
                if (!redirect && formData != null) {
                    log.debug("Data size: " + formData.length());
                    conn.setRequestProperty("Content-Length", Integer.toString(formData.length()));
                    log.debug("Posting: " + formData);
                    IOUtils.write(formData, conn.getOutputStream());
                    conn.getOutputStream().flush();
                    conn.getOutputStream().close();
                }
                log.debug("Code: " + conn.getResponseCode() + "; Message: " + conn.getResponseMessage());

                if (conn.getResponseCode() == 301 || conn.getResponseCode() == 302) {
                    urlAddress = conn.getHeaderField("Location");
                    log.debug("Redirect to: " + urlAddress);
                    redirect = true;
                    conn.disconnect();
                } else {
                    redirect = false;
                }
                addCookieList(conn.getHeaderFields().get("Set-Cookie"));
            } while (redirect);

            // Get the response

            log.debug("Receiving:");
            String response = IOUtils.toString(conn.getInputStream());
            conn.getInputStream().close();
            log.debug(response);

            return new HttpResponse(response, conn.getResponseCode());
        } catch (IOException e) {
            log.debug("Exception caught", e);
            String errorStream = null;
            int responseCode = 0;
            try {
                if (conn != null && conn.getErrorStream() != null) {
                    responseCode = conn.getResponseCode();
                    log.debug("Response Code: " + responseCode);
                    errorStream = IOUtils.toString(conn.getErrorStream());
                    log.debug("Error stream: " + errorStream);
                }
            } catch (IOException e1) {
                log.debug("Cannot convert error stream to string");
            }
            throw new IllegalRestStateException(responseCode, errorStream, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String headerName = null;
    private String headerValue = null;

    public void setCustomHeader(String headerName, String headerValue) {
        this.headerName = headerName;
        this.headerValue = headerValue;
    }

    public void clearCustomHeader() {
        this.headerName = null;
    }

    private String serializeParameters(String[][] data) {
        if (data == null) return null;
        StringBuilder returnValue = new StringBuilder();
        for (String[] parameter : data) {
            assert parameter.length == 2;
            String key = parameter[0];
            String value = parameter[1];
            if (value == null) {
                value = "";
            }
            try {
                returnValue.
                        append('&').                    // even the very first parameter starts with '&'
                        append(key).append('=').
                        append(URLEncoder.encode(value, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
        return returnValue.substring(1);                // remove the starting '&' character
    }

    public HttpResponse doGet(String url) throws IllegalRestStateException {
        return doRequest(url, null, Method.GET, ContentType.JSON_JSON);
    }

    public HttpResponse doPost(String url, String data) throws IllegalRestStateException {
        return doRequest(url, data, Method.POST, ContentType.JSON_JSON);
    }

    public HttpResponse doPost(String url, String[][] data) throws IllegalRestStateException {
        return doRequest(url, serializeParameters(data), Method.POST, ContentType.NONE);
    }

    public HttpResponse doPut(String url, String data) throws IllegalRestStateException {
        return doRequest(url, data, Method.PUT, ContentType.JSON_JSON);
    }

    public HttpResponse doDelete(String url) throws IllegalRestStateException {
        return doRequest(url, null, Method.DELETE, ContentType.NONE);
    }

}
