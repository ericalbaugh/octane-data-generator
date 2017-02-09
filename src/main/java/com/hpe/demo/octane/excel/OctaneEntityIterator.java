package com.hpe.demo.octane.excel;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by panuska on 3/29/13.
 */
public class OctaneEntityIterator extends EntityIterator implements Iterable, Iterator {
    private static Logger log = Logger.getLogger(OctaneEntityIterator.class.getName());

    private Set<String> referenceColumns = new HashSet<>();
    private Set<String> multipleValueColumns = new HashSet<>();
    private static Map<String, String> idTranslationTable = new HashMap<>();

    public OctaneEntityIterator(Sheet sheet) {
        super(sheet);
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            if (fieldName.charAt(0) == '#') {        // todo make it configurable
                fieldName = fieldName.substring(1);  // remove the prefix
                fieldNames[i] = fieldName;           // store the corrected field name
                referenceColumns.add(fieldName);     // also store it as a column containing a reference
            }
            if (fieldName.charAt(0) == '*') {        // todo make it configurable
                fieldName = fieldName.substring(1);  // remove the prefix
                fieldNames[i] = fieldName;           // store the corrected field name
                multipleValueColumns.add(fieldName);     // also store it as a column containing a reference
            }
        }
    }

    @Override
    public ExcelEntity next() {
        Object[] row = rowIterator.next();
        Map<String, Object> fields = new LinkedHashMap<>(row.length);
        for (int i = 0; i < row.length; i++) {  // do not skip the very first column (it contains the original entity ID - written in Excel file)
            Object value = row[i];
            if (!NULL.equals(value)) {
                String fieldName = fieldNames[i];
                if (fieldName.charAt(0) == '_') {
                    continue;
                }
                if (referenceColumns.contains(fieldName)) {  // dereference the value first
                    value = dereference(fieldName, (String) value);
                }
                if (multipleValueColumns.contains(fieldName)) {
                    String v = (String) value;               // todo is it really a string?
                    String[] tokens = v.split(";");
                    for (int j = 0; j < tokens.length; j++) {
                        tokens[j] = dereference(fieldName, tokens[j]);
                    }
                    value = tokens;
                }
                fields.put(fieldName, value);
            }
        }

        return new ExcelEntity(fields);
    }

    private String dereference(String fieldName, String value) {
        String originalValue = value;
        value = idTranslationTable.get(value);
        if (value == null) {
            log.warn("Cannot translate as the value not found in table; column: " + fieldName + "; value: " + originalValue);
            value = originalValue; // leave the original value
        }
        return value;
    }

    public static void putReference(String key, String value) {
        log.debug("Putting translation: " + key + " = " + value);
        idTranslationTable.put(key, value);
    }

    public static void logReferences() {
        log.debug("Transition table content: ");
        log.debug(idTranslationTable);
    }

    public static String dereference(String key) {
        return idTranslationTable.get(key);
    }
}
