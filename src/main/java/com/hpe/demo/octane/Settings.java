package com.hpe.demo.octane;

import com.hpe.demo.octane.excel.ExcelReader;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Created by panuska on 1/14/13.
 */
public class Settings {
    private static Logger log = Logger.getLogger(Settings.class.getName());

    public static final String CONF_DIR = "conf";
    public static final String SETTINGS_PROPERTIES_FILE = "settings.properties";

    private String loginUrl;
    private String restUrl;
    private String admin;
    private String backlogName;
    private String spaceId;
    private String workspaceId;
    private int connectionTimeout = 60 * 1000;

    private static DataFormatter formatter = new DataFormatter(true);

    private Sheet sheet;
    private static FormulaEvaluator evaluator;

    private Settings(Sheet sheet) {
        this.sheet = sheet;
        this.evaluator = this.sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        log.debug("Setting Login URL: " + loginUrl);
        this.loginUrl = loginUrl;
    }

    public String getRestUrl() {
        return restUrl;
    }

    public void setRestUrl(String restUrl) {
        log.debug("Setting REST URL: " + restUrl);
        this.restUrl = restUrl;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }


    public String getBacklogName() {
        return backlogName;
    }

    public void setBacklogName(String backlogName) {
        log.debug("Setting backlogName: " + backlogName);
        this.backlogName = backlogName;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        log.debug("Setting workspace ID: " + workspaceId);
        this.workspaceId = workspaceId;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(String spaceId) {
        log.debug("Setting space ID: " + spaceId);
        this.spaceId = spaceId;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(String timeout) {
        try {
            this.connectionTimeout = Integer.parseInt(timeout);
        } catch (NumberFormatException e) {
            log.error("Cannot parse this string into an int: " + timeout, e);
        }
    }

    private static Settings settings = null;

    public static void initSettings(ExcelReader reader) {
        Sheet sheet = reader.getSheet("Settings");
        Settings.settings = new Settings(sheet);
        log.info("Reading settings...");

        // initialize from Excel file
        for (Row row : sheet) {
            Cell cell = row.getCell(1);
            if (cell == null) continue;  // skip an empty row
            cell = evaluator.evaluateInCell(cell);
            String propertyName = formatter.formatCellValue(cell).trim();
            if (propertyName.length() == 0) continue; // skip an empty row

            cell = row.getCell(2);
            if (cell == null) {
                log.error("No value found for property called " + propertyName);
                continue;
            }
            cell = evaluator.evaluateInCell(cell);
            String propertyValue = formatter.formatCellValue(cell).trim();

            settings.setProperty(propertyName, propertyValue);
        }

        // initialize from text file
        File file = new File(CONF_DIR, SETTINGS_PROPERTIES_FILE);
        if (file.exists()) {
            Properties properties = new Properties();
            try {
                log.debug("Settings properties file found, loading");
                properties.load(new FileInputStream(file));
            } catch (IOException e) {
                log.error("Cannot open file: " + file.getAbsolutePath(), e);
                return;
            }
            for (String propertyName : properties.stringPropertyNames()) {
                String propertyValue = properties.getProperty(propertyName);
                settings.setProperty(propertyName, propertyValue);
                settings.setExcelProperty(propertyName, propertyValue);
            }
        }
    }

    private void setExcelProperty(String propertyName, String propertyValue) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                cell = evaluator.evaluateInCell(cell);
                String value = formatter.formatCellValue(cell).trim();
                if (propertyName.equals(value)) {
                    Cell newCell = row.getCell(cell.getColumnIndex() + 1);  //on the very same row, take the next cell
                    if (newCell == null) {
                        newCell = row.createCell(cell.getColumnIndex() + 1);
                    }
                    newCell.setCellValue(propertyValue);             //and set its value
                }
            }
        }
    }

    boolean setProperty(String propertyName, String propertyValue) {
        String methodName = "set" + propertyName;
        try {
            log.debug("Calling " + methodName + "(" + propertyValue + ")");
            Method method = Settings.class.getMethod(methodName, String.class);
            if (method != null) {
                method.invoke(settings, propertyValue);
                return true;
            } else {
                log.error("On " + Settings.class.getSimpleName() + " class, this method does not exist: " + methodName);
                return false;
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.error("On " + Settings.class.getSimpleName() + " class, cannot call the method " + methodName, e);
            return false;
        }
    }

    public static Settings getSettings() {
        if (settings == null) {
            log.error("Settings not initialized!");
            throw new IllegalStateException("Settings not initialized!");
        }
        return settings;
    }

}
