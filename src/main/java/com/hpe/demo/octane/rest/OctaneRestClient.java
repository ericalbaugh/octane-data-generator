package com.hpe.demo.octane.rest;

import com.hpe.demo.octane.Settings;
import org.apache.log4j.Logger;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

/**
 * Created by panuska on 7/18/13.
 */
public class OctaneRestClient {
    private static Logger log = Logger.getLogger(OctaneRestClient.class.getName());

    private final String url;
    private final RestClient client;
    private final String loginUrl;

    public OctaneRestClient() {
        Settings settings = Settings.getSettings();
        String spaceId = settings.getSpaceId();
        String workspaceId = settings.getWorkspaceId();
        url = settings.getRestUrl() + spaceId + "/workspaces/" + workspaceId + "/";
        loginUrl = settings.getLoginUrl();
        client = new RestClient();
    }

    private static OctaneRestClient octaneClient = null;

    public static void initClient() {
        octaneClient = new OctaneRestClient();
    }

    public static OctaneRestClient getOctaneClient() {
        if (octaneClient == null) {
            log.error("Octane REST client not initialized yet!");
            throw new IllegalStateException("Octane REST client not initialized yet!");
        }
        return octaneClient;
    }

    public static final int WAIT_TIME = 5000;

    public static final int LOGIN_RETRIES = 1;

    public HttpResponse login(final String username, final String password) throws IllegalRestStateException {
        return new RetriableTask<>(LOGIN_RETRIES, WAIT_TIME,
                new Callable<HttpResponse>() {
                    public HttpResponse call() throws IllegalRestStateException {
                        String[][] data = {
                                {"username", username},
                                {"password", password},
                        };
                        HttpResponse response = client.doPost(loginUrl, data);
                        String csrfCookie = client.getCookie("HPSSO_COOKIE_CSRF");
                        if (csrfCookie == null) {
                            return null;
                        }
                        client.setCustomHeader("HPSSO-HEADER-CSRF", csrfCookie);
                        return response;
                    }
                }).call();
    }

    public static final int DELETE_RETRIES = 1;

    public HttpResponse delete(final String suffix) throws IllegalRestStateException {
        return new RetriableTask<>(DELETE_RETRIES, WAIT_TIME,
                new Callable<HttpResponse>() {
                    public HttpResponse call() throws IllegalRestStateException {
                        return client.doDelete(url + suffix);
                    }
                }).call();
    }

    public static final int READ_RETRIES = 5;

    public HttpResponse read(final String suffix) throws IllegalRestStateException {
        return new RetriableTask<>(READ_RETRIES, WAIT_TIME,
                new Callable<HttpResponse>() {
                    public HttpResponse call() throws IllegalRestStateException {
                        return client.doGet(url + suffix);
                    }
                }).call();
    }

    public static final int CREATE_RETRIES = 5;

    public HttpResponse create(final String suffix, final String json) throws IllegalRestStateException {
        return new RetriableTask<>(CREATE_RETRIES, WAIT_TIME,
                new Callable<HttpResponse>() {
                    public HttpResponse call() throws IllegalRestStateException {
                        return client.doPost(url + suffix, json);
                    }
                }).call();
    }

    public static final int UPDATE_RETRIES = 5;

    public HttpResponse update(final String suffix, final String json) throws IllegalRestStateException {
        return new RetriableTask<>(UPDATE_RETRIES, WAIT_TIME,
                new Callable<HttpResponse>() {
                    public HttpResponse call() throws IllegalRestStateException {
                        return client.doPost(url + suffix, json);
                    }
                }).call();
    }

    //copied from http://fahdshariff.blogspot.com/2009/08/retrying-operations-in-java.html
    class RetriableTask<T> implements Callable<T> {

        private Callable<T> task;
        private int numberOfTriesLeft; // number left
        private long timeToWait; // wait interval

        public RetriableTask(int numberOfRetries, long timeToWait,
                             Callable<T> task) {
            numberOfTriesLeft = numberOfRetries;
            this.timeToWait = timeToWait;
            this.task = task;
        }

        public T call() throws IllegalRestStateException {
            while (true) {
                try {
                    return task.call();
                } catch (CancellationException e) {
                    throw e;
                } catch (IllegalRestStateException e) {
                    numberOfTriesLeft--;
                    if (numberOfTriesLeft == 0) {
                        throw e;
                    } else {
                        log.debug("An exception caught; attempts to retry: " + numberOfTriesLeft);
                        log.debug("Going to sleep for " + timeToWait + " ms");
                        log.debug(e);
                        try {
                            Thread.sleep(timeToWait);
                        } catch (InterruptedException e1) {
                            throw new IllegalStateException(e1);
                        }
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("This exception should never be thrown", e);
                }
            }
        }
    }
}
