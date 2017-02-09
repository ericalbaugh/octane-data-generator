package com.hpe.demo.octane.excel;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by panuska on 12/3/13.
 */
public class ExcelEntity {

    private Map<String, Object> fields;

    public ExcelEntity() {
        this.fields = new LinkedHashMap<>();
    }

    public ExcelEntity(Map<String, Object> fields) {
        this.fields = fields;
    }

    public void setValue(String name, Object value) {
        this.fields.put(name, value);
    }

    public Object getValue(String name) {
        return this.fields.get(name);
    }

    public String getStringValue(String name) {
        return (String) this.fields.get(name);
    }

    public int getIntValue(String name) {
        return (Integer) getValue(name);
    }

    public long getLongValue(String name) {
        return (Long) getValue(name);
    }

    public Set<String> getFieldNames() {
        return fields.keySet();
    }

    @Override
    public String toString() {
        return "ExcelEntity{" +
                "fields=" + fields +
                '}';
    }
}
