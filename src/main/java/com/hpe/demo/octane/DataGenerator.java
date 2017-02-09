package com.hpe.demo.octane;

import com.hpe.demo.octane.excel.OctaneEntityIterator;
import com.hpe.demo.octane.handler.EntityHandler;
import com.hpe.demo.octane.handler.ReleaseHandler;
import com.hpe.demo.octane.handler.SheetHandler;
import com.hpe.demo.octane.handler.SheetHandlerRegistry;
import com.hpe.demo.octane.entity.User;
import com.hpe.demo.octane.excel.ExcelEntity;
import com.hpe.demo.octane.excel.ExcelReader;
import com.hpe.demo.octane.rest.HttpResponse;
import com.hpe.demo.octane.rest.OctaneRestClient;
import com.jayway.jsonpath.JsonPath;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Sheet;

import javax.xml.bind.JAXBException;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Created by panuska on 10/12/12.
 */
public class DataGenerator {

    private static Logger log = Logger.getLogger(DataGenerator.class.getName());

    private static Settings settings;
    private static OctaneRestClient client;

    private static SheetHandlerRegistry registry;
    public static final String OCTANE_DATA_GENERATOR = "octane-data-generator.jar";

    private static void printUsage() {
        System.out.println(
                " /============================================================================\\" + System.lineSeparator() +
                        " | Octane data generator " + DataGenerator.class.getPackage().getImplementationVersion() + " (build time: " + getBuildTime() + ")                  |" + System.lineSeparator() +
                        " |============================================================================|" + System.lineSeparator() +
                        " | For more information and release notes, go to                              |" + System.lineSeparator() +
                        " |   https://irock.jiveon.com/docs/DOC-122103                                 |" + System.lineSeparator() +
                        " |============================================================================|" + System.lineSeparator() +
                        " | Usage:                                                                     |" + System.lineSeparator() +
                        " |   java -jar " + OCTANE_DATA_GENERATOR + " [excel-configuration-file.xlsx]      |" + System.lineSeparator() +
                        " |   [--ParameterName1=value1] [--ParameterName2=value2] ....                 |" + System.lineSeparator() +
                        " |   admin_name admin_password                                                |" + System.lineSeparator() +
                        " |----------------------------------------------------------------------------|" + System.lineSeparator() +
                        " |     excel-configuration-file.xlsx                                          |" + System.lineSeparator() +
                        " |       - optional                                                           |" + System.lineSeparator() +
                        " |       - data to generate the project from                                  |" + System.lineSeparator() +
                        " |       - built-in file used if this parameter is not specified              |" + System.lineSeparator() +
                        " |       - provide as the very first parameter!                               |" + System.lineSeparator() +
                        " |     --ParameterName=                                                       |" + System.lineSeparator() +
                        " |       - optional                                                           |" + System.lineSeparator() +
                        " |       - name of any parameter whose value should be changed                |" + System.lineSeparator() +
                        " |       - if not specified, the value is taken from conf/settings.properties |" + System.lineSeparator() +
                        " |         file; if not specified there, the value is taken from the provided |" + System.lineSeparator() +
                        " |         excel sheet; if no sheet specified, built-in excel file is used    |" + System.lineSeparator() +
                        " |     admin_name and admin_password are the only mandatory parameters        |" + System.lineSeparator() +
                        " |       - always provide as the very last parameters                         |" + System.lineSeparator() +
                        " |============================================================================|" + System.lineSeparator() +
                        " | Example of how to provide parameters:                                      |" + System.lineSeparator() +
                        " |   custom-data.xlsx \"--SpaceId=53030\" \"--WorkspaceId=1002\"                  |" + System.lineSeparator() +
                        " |   petr.panuska@hpe.com my_octane_password                                  |" + System.lineSeparator() +
                        " \\============================================================================/" + System.lineSeparator() + System.lineSeparator());
    }

    public static void main(String[] args) throws JAXBException, IOException {
        if (args.length < 2) {
            System.err.println("Provide at least two arguments as credentials to login!");
            System.err.println("Provide also the Octane SpaceId and WorkspaceId parameters!");
            System.err.println();
            printUsage();
            System.exit(-1);
        }
        log.debug("Options: " + Arrays.toString(args));
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        long startTime = System.currentTimeMillis();
        log.info("Octane data generator " + DataGenerator.class.getPackage().getImplementationVersion() + " (build time: " + getBuildTime() + ")");
        log.info("Starting at: " + sdf.format(new Date(startTime)));
        try {
            int argIndex = 0;
            ExcelReader reader;
            if (args.length > 2 && args[0].endsWith(".xlsx")) {
                reader = new ExcelReader(new FileInputStream(args[0]));
                log.info("Using configuration " + args[0]);
                argIndex++;
            } else {
                reader = new ExcelReader(DataGenerator.class.getResourceAsStream("/data.xlsx")); // use built-in excel file
                log.info("Using built-in configuration...");
            }
            Settings.initSettings(reader);
            settings = Settings.getSettings();

            for (; argIndex < args.length - 2; argIndex++) {
                String argument = args[argIndex];
                if (!argument.startsWith("--")) {
                    log.error("To set an option value, start with '--' prefix");
                    System.exit(-1);
                }
                String[] tokens = argument.substring(2).split("=");
                if (tokens.length != 2) {
                    log.error("To set an option value, provide the option name and its wanted value delimited by a single '=' character");
                    System.exit(-1);
                }
                if (!settings.setProperty(tokens[0], tokens[1])) {
                    log.error("Can't set property " + tokens[0] + " into value " + tokens[1]);
                    System.exit(-1);
                }
            }
            if (isNullOrEmpty("RestUrl", settings.getRestUrl())) return;
            if (isNullOrEmpty("SpaceId", settings.getSpaceId())) return;
            if (isNullOrEmpty("WorkspaceId", settings.getWorkspaceId())) return;

            log.info("Populating Octane running at " + settings.getRestUrl());
            log.info("Populating space " + settings.getSpaceId());
            log.info("Populating workspace " + settings.getWorkspaceId());


            User admin = new User(settings.getAdmin(), args[argIndex++], args[argIndex]);
            User.addUser(admin);

            OctaneRestClient.initClient();
            client = OctaneRestClient.getOctaneClient();

            login();

            learnPhases();
            learnListNodes();
            learnTaxonomyNodes();
            learnProductAreas();
            learnUsers();
            learnBacklogId();

            JobLogger jobLogger = new JobLogger();
            jobLogger.deletePreviousData();

            generateProject(reader);

        } catch (RuntimeException e) {
            log.debug("Exception thrown:", e);
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            log.info("Finished at: " + sdf.format(new Date(endTime)));
            long total = endTime - startTime;

            log.info(String.format("The generator ran for: %02d:%02d.%03d", total / 60000, (total % 60000) / 1000, total % 100));
        }
    }

    public static void login() {
        User admin = User.getUser(settings.getAdmin());
        log.info("Logging in " + admin.getLogin() + "...");
        HttpResponse response = client.login(admin.getLogin(), admin.getPassword());
        if (response == null) {
            log.error("Incorrect credentials: "+admin.getLogin() + " / " + admin.getPassword());
            System.exit(-1);
        }
    }

    public static void learnPhases() {
        log.debug("Learning phases...");
        HttpResponse response = client.read("phases");
        List<Map> phases = JsonPath.read(response.getResponse(), "$.data");
        for (Map phase : phases) {
            String id = (String) phase.get("id");
            String logicalName = (String) phase.get("logical_name");
            OctaneEntityIterator.putReference(logicalName, id);
        }
        log.info("There are " + phases.size() + " phases...");
    }

    public static final String BACKLOG_ID = "work_item.root";
    public static final String BACKLOG_NAME = "Backlog";

    public static void learnListNodes() {
        log.debug("Learning list nodes...");
        HttpResponse response = client.read("list_nodes");
        List<Map> nodes = JsonPath.read(response.getResponse(), "$.data");
        for (Map node : nodes) {
            String id = (String) node.get("id");
            String logicalName = (String) node.get("logical_name");
            OctaneEntityIterator.putReference(logicalName, id);
        }
        log.info("There are " + nodes.size() + " list nodes...");
    }

    public static void learnTaxonomyNodes() {
        log.debug("Learning taxonomy nodes...");
        HttpResponse response = client.read("taxonomy_nodes");
        List<Map> nodes = JsonPath.read(response.getResponse(), "$.data");
        for (Map node : nodes) {
            String id = (String) node.get("id");
            String logicalName = (String) node.get("logical_name");
            OctaneEntityIterator.putReference(logicalName, id);
        }
        log.info("There are " + nodes.size() + " taxonomy nodes...");
    }

    public static final String PRODUCT_AREA_ROOT = "product_area.root";

    public static void learnProductAreas() {
        log.debug("Learning product areas...");
        HttpResponse response = client.read("product_areas?query=%22logical_name%3D%27" + PRODUCT_AREA_ROOT + "%27%22");
        List<Map> areas = JsonPath.read(response.getResponse(), "$.data");
        for (Map area : areas) {
            String id = (String) area.get("id");
            OctaneEntityIterator.putReference(PRODUCT_AREA_ROOT, id);
        }
        if (areas.size() == 1) {
            log.info("There is " + areas.size() + " root product area...");
        } else {
            log.error("There are " + areas.size() + " root -product areas...");
        }
    }

    public static final String ADMIN_USER = "users.admin";

    public static void learnUsers() {
        log.debug("Learning users...");

        User admin = User.getUser(settings.getAdmin());
        settings.getAdmin();
        HttpResponse response = client.read("workspace_users?query=%22email%3D%27" + admin.getLogin() + "%27%22");
        List<Map> users = JsonPath.read(response.getResponse(), "$.data");
        for (Map user : users) {
            String id = (String) user.get("id");
            OctaneEntityIterator.putReference(ADMIN_USER, id);
        }
        if (users.size() == 1) {
            log.info("There is " + users.size() + " admin user...");
        } else {
            log.error("There are " + users.size() + " admin users...");
        }
    }

    public static void learnBacklogId() {
        log.debug("Learning ID of " + BACKLOG_ID + " (backlog)...");
        String backlogName = settings.getBacklogName();
        HttpResponse response = client.read("work_items?query=%22logical_name%3D%27" + BACKLOG_ID + "%27%22");

        List<String> backlogIds = JsonPath.read(response.getResponse(), "$.data[*].id");
        if (backlogIds.size() < 1) {
            log.error("There is no " + backlogName + " work item!!");
            System.exit(-1);
            throw (null); // will never happen
        }
        if (backlogIds.size() > 1) {
            log.warn("There is more than one " + backlogName + " work items: " + backlogIds.size());
            log.warn("Taking the very first one...");
        }
        String backlogId = backlogIds.get(0);
        OctaneEntityIterator.putReference(BACKLOG_ID, backlogId);
        log.info(BACKLOG_NAME + " ID: " + backlogId);
    }

    private static void generateProject(ExcelReader reader) {
        log.info("Generating project data...");
        registry = new SheetHandlerRegistry();
        SheetHandler genericHandler = new EntityHandler();
        List<Sheet> entitySheets = reader.getAllEntitySheets();
        for (Sheet sheet : entitySheets) {
            String entityName = sheet.getSheetName();
            registry.registerHandler(entityName, genericHandler);
        }
        // register any specialized handler here; it will overwrite the generic handler above
        registry.registerHandler("releases", new ReleaseHandler());

        for (Sheet sheet : entitySheets) {
            String entityName = sheet.getSheetName();
            generateEntity(reader, entityName);
        }
        OctaneEntityIterator.logReferences();
        EntityHandler.printStatistics();
    }

    private static void generateEntity(ExcelReader reader, String sheetName) {
        Sheet sheet = reader.getSheet(sheetName);
        OctaneEntityIterator iterator = new OctaneEntityIterator(sheet);

        SheetHandler handler = registry.getHandler(sheetName);
        handler.init(sheetName);
        while (iterator.hasNext()) {
            ExcelEntity entity = iterator.next();
            handler.row(entity);
        }
        handler.terminate();
    }

    static boolean isNullOrEmpty(String name, String s) {
        if (s != null && s.length() > 0) return false;
        log.error("Parameter "+name+" not set!!");
        return true;
    }

    /**
     * Opens the manifest file of the given JAR file and returns the Build-Time attribute.
     * Returns null when an exception is thrown while reading the Build-Time attribute.
     */
    public static String getBuildTime(String jarFileName) {
        try {
            JarFile jarFile = new JarFile(jarFileName);
            Manifest manifest = jarFile.getManifest();
            Attributes attr = manifest.getMainAttributes();
            return attr.getValue("Build-Time");
        } catch (IOException e) {
            log.debug("Exception when reading build time from manifest file!", e);
            return null;
        }
    }

    /**
     * Opens the manifest file of the ADG JAR file and returns the Build-Time attribute.
     * Returns null when an exception is thrown while reading the Build-Time attribute.
     */
    public static String getBuildTime() {
        return getBuildTime(getThisJarFileName());
    }

    public static String getThisJarFileName() {
        return DataGenerator.class.getProtectionDomain().getCodeSource().getLocation().getFile();
    }
}
