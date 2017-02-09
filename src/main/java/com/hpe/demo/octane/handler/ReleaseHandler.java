package com.hpe.demo.octane.handler;

import com.hpe.demo.octane.excel.OctaneEntityIterator;
import com.hpe.demo.octane.rest.HttpResponse;
import com.jayway.jsonpath.JsonPath;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Created by panuska on 2/9/17.
 */
public class ReleaseHandler extends EntityHandler {
    private static Logger log = Logger.getLogger(ReleaseHandler.class.getName());

    @Override
    public void terminate() {
        super.terminate();
        log.debug("Learning sprints...");
        HttpResponse response = client.read("sprints");
        List<Map> sprints = JsonPath.read(response.getResponse(), "$.data[*]");
        for (Map sprint : sprints) {
            String id = (String) sprint.get("id");
            String name = (String) sprint.get("name");
            String tokens[] = name.split(" ");
            OctaneEntityIterator.putReference("sprints." + tokens[1], id);
        }
        log.info("There are " + sprints.size() + " sprints...");
    }
}
