package com.hpe.demo.octane.rest;

/**
 * Created by panuska on 2/9/17.
 */
public class HttpResponse {
    private final String response;
    private final int responseCode;

    public HttpResponse(String response, int responseCode) {
        this.response = response;
        this.responseCode = responseCode;
    }

    public String getResponse() {
        return response;
    }

    public int getResponseCode() {
        return responseCode;
    }

}
