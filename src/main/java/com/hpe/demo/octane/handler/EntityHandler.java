package com.hpe.demo.octane.handler;

import com.hpe.demo.octane.JobLogger;
import com.hpe.demo.octane.excel.OctaneEntityIterator;
import com.hpe.demo.octane.excel.ExcelEntity;
import com.hpe.demo.octane.rest.HttpResponse;
import com.hpe.demo.octane.rest.IllegalRestStateException;
import com.hpe.demo.octane.rest.OctaneRestClient;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by panuska on 3/14/13.
 */
public class EntityHandler extends AbstractSheetHandler {
    private static Logger log = Logger.getLogger(EntityHandler.class.getName());
    private static int createdAllEntities = 0;
    private int createdEntities;

    protected OctaneRestClient client;

    @Override
    public void init(String sheetName) {
        client = OctaneRestClient.getOctaneClient();

        int index = sheetName.indexOf('#');
        String sheetNamePrefix = index < 0 ? sheetName : sheetName.substring(0, index);
        super.init(sheetNamePrefix);
        createdEntities = 0;
        log.info("Generating entity: " + sheetName);
    }

    @Override
    public void row(ExcelEntity entity) {
        Set<String> fieldNames = entity.getFieldNames();

        JSONObject root = new JSONObject();
        String excelId = null;
        for (String fieldName : fieldNames) {
            try {
                if (excelId == null && fieldName.equals("id")) {  // ID column is not serialized
                    try {
                        excelId = entity.getStringValue("id");
                    } catch (ClassCastException e) {
                        int number = entity.getIntValue("id");
                        excelId = "" + number;
                        log.error("At " + sheetName + " row " + excelId + " wrong data format (ID should be text)");
                    }
                } else {
                    String[] tokens = fieldName.split("!");
                    JSONObject pointer = root;
                    for (int i = 0; i < tokens.length - 1; i++) {     // iterate through all the prefixes
                        String token = tokens[i];
                        JSONObject child = (JSONObject) root.get(token);
                        if (child == null) {
                            child = new JSONObject();
                            root.put(token, child);
                        }
                        pointer = child;
                    }
                    String token = tokens[tokens.length - 1];
                    Object value = entity.getValue(fieldName);
                    if (value instanceof String[]) {                  // field name starting with *
                        List<Map<String, String>> list = new LinkedList<>();
                        for (String v : (String[]) value) {
                            Map<String, String> entry = new HashMap<>();
                            entry.put("id", v);
                            entry.put("type", token);
                            list.add(entry);
                        }
                        pointer.put("data", list);                    // serialize as list of items
                    } else {
                        pointer.put(token, value);
                    }
                }
            } catch (ClassCastException e) {
                log.error("ClassCastException");
                throw e;
            }
        }
        JSONObject data = new JSONObject();
        JSONArray content = new JSONArray();
        content.add(root);
        data.put("data", content);

        String finalQuery = data.toJSONString();

        try {
            HttpResponse response = client.create(sheetName, finalQuery);
            createdAllEntities++;
            createdEntities++;
            String octaneId = JsonPath.read(response.getResponse(), "$.data[0].id");
            OctaneEntityIterator.putReference(sheetName + "." + excelId, octaneId);
            JobLogger.writeLogLine(sheetName, octaneId, excelId);
            log.debug("Creating " + entity + ": " + response.getResponse());
        } catch (IllegalRestStateException e) {
            log.error(e.getErrorStream());
            log.error(e.getMessage());
            log.error("HTTP Response: " + e.getResponseCode());
            log.debug("Exception caught when creating an entity: " + entity, e);
        }
    }

    @Override
    public void terminate() {
        log.info("Generated " + createdEntities + " " + sheetName);
    }

    public static void printStatistics() {
        log.info("Generated entities: " + createdAllEntities);
    }
}
