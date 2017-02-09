package com.hpe.demo.octane.handler;

import com.hpe.demo.octane.excel.ExcelEntity;

/**
 * Created by panuska on 3/14/13.
 */
public interface SheetHandler {

    void init(String sheetName);

    void row(ExcelEntity entity);

    void terminate();
}
