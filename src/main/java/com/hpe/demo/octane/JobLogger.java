package com.hpe.demo.octane;

import com.hpe.demo.octane.rest.IllegalRestStateException;
import com.hpe.demo.octane.rest.OctaneRestClient;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by panuska on 10/14/13.
 */
public class JobLogger {
    private static Logger log = Logger.getLogger(JobLogger.class.getName());
    public static final String JOBS_DIR = "jobs";
    public static final String JOB_PREFIX = "job-";
    public static final String JOB_SUFFIX = ".log";

    private static File jobLog = null;                                      //todo hack; make it non-static
    private Settings settings;
    private OctaneRestClient client;

    public JobLogger() {
        this.settings = Settings.getSettings();
        jobLog = new File(JOBS_DIR, JOB_PREFIX + settings.getWorkspaceId() + "-" + settings.getSpaceId() + "-" + getHostName(settings.getRestUrl()) + JOB_SUFFIX);  //todo hack; make it non-static
        client = OctaneRestClient.getOctaneClient();
    }

    public static void writeLogLine(String entityName, String entityId, String excelId) {   //todo hack; make it non-static (e.g. use context to register/retrieve common objects like JobLogger is)
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(jobLog, true));
            writer.write(entityName + "#" + excelId + "=" + entityId + System.lineSeparator());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    void deletePreviousData() {
        if (jobLog.exists()) {
            try {
                BufferedReader logReader = new BufferedReader(new FileReader(jobLog));
                Map<String, List<String>> entities = new LinkedHashMap<>();         // follow the order
                for (String line = logReader.readLine(); line != null; line = logReader.readLine()) {
                    String[] items = line.split("(=|#)");
                    if (items.length != 3) {
                        throw new IllegalStateException("Job log " + jobLog.getName() + " does not follow the expected format!");
                    }

                    String entityName = items[0];
                    String entityId = items[2];

                    List<String> currentEntity = entities.get(entityName);
                    if (currentEntity == null) {
                        currentEntity = new LinkedList<>();
                        entities.put(entityName, currentEntity);
                    }
                    currentEntity.add(0, entityId); // add it in the reverse order!
                }
                logReader.close();

                String[] entityNames = entities.keySet().toArray(new String[entities.keySet().size()]);
                for (int i = entityNames.length - 1; i >= 0; i--) {  // iterate in reverse order!
                    String entityName = entityNames[i];
                    List<String> entityIds = entities.get(entityName);
                    StringBuilder query = _initQuery();
                    int j;
                    for (j = 0; j < entityIds.size(); j++) {
                        String entityId = entityIds.get(j);
                        query.append("id%3D%27").append(entityId).append("%27%7C%7C"); // id='entityId'||
                        if ((j + 1) % 100 == 0) {                                          // split entities into groups
                            _finalizeQueryAndremoveEntities(query, entityName, 100);
                            query = _initQuery();
                        }
                    }
                    int remnant = (j + 1) % 100 - 1;
                    if (remnant > 0) {
                        _finalizeQueryAndremoveEntities(query, entityName, remnant);
                    }
                }
                if (!jobLog.delete()) {
                    log.error("Cannot delete the original job log file when removing all previously created entities: " + jobLog.getName());
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void _finalizeQueryAndremoveEntities(StringBuilder query, String entityName, int howMany) {
        query.replace(query.length() - 6, query.length(), ")%22");  // remove the last double ||

        log.info("Removing " + howMany + " " + entityName);

        String finalQuery = null;
        try {
            finalQuery = entityName + query;
            client.delete(finalQuery);
        } catch (IllegalRestStateException e) {
            log.error("Can't remove " + howMany + " " + entityName + " using this HTTP DELETE: " + finalQuery);
        }
    }

    private StringBuilder _initQuery() {
        return new StringBuilder("?query=%22("); // ?query="(
    }

    public static String getHostName(String url) {
        try {
            return new URI(url).getHost();
        } catch (URISyntaxException e) {
            log.error("Invalid URL syntax: "+url);
            return null;
        }
    }
}
