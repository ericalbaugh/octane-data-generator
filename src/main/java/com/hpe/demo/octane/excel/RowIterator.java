package com.hpe.demo.octane.excel;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by panuska on 10/18/12.
 */
public class RowIterator implements Iterable, Iterator {
    private static Logger log = Logger.getLogger(RowIterator.class.getName());

    private Iterator<Row> iterator;
    private static DataFormatter formatter = new DataFormatter(true);

    private Object[] buffer;

    private FormulaEvaluator evaluator;

    RowIterator(Sheet sheet) {
        iterator = sheet.iterator();
        evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
    }

    /**
     * Transforms the row content into an array of string values and (unlike poi library):
     * 1. puts in the array also empty (blank/null) cells (does not skip them)
     * 2. removes the empty ending cells (trims the row)
     *
     * @param row which row to read.
     * @return array of strings where each string is the excel cell value.
     */
    private Object[] _readLine(Row row) {
        Iterator<Cell> cellIterator = row.cellIterator();
        List<Object> cells = new LinkedList<>();
        // transform iterator to list
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();
            cell = evaluator.evaluateInCell(cell);
            cells.add(cell.getColumnIndex(), formatter.formatCellValue(cell).trim());
        }
        // clean the ending "empty" columns
        ListIterator<Object> valueIterator = cells.listIterator(cells.size());
        while (valueIterator.hasPrevious()) {
            Object value = valueIterator.previous();
            if (value instanceof String && (((String) value).length()) == 0) {
                valueIterator.remove();
            } else {
                break; // once first non-empty value found, leave it
            }
        }
        return cells.toArray(new Object[cells.size()]);
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public Object[] next() {
        Row row = iterator.next();

        while (row.getSheet().getWorkbook().getFontAt(row.getCell(row.getFirstCellNum()).getCellStyle().getFontIndex()).getBoldweight() == Font.BOLDWEIGHT_BOLD) {
            log.debug("At sheet " + row.getSheet().getSheetName() + " skipping row number " + row.getRowNum());
            row = iterator.next();   // todo if the very last row is bold, this result in exception!
        }

        // initialize buffer
        if (buffer == null) {
            buffer = _readLine(row);
            return Arrays.copyOf(buffer, buffer.length);  // return copy of buffer as buffer is used as static cache
        }

        Object[] array = new Object[buffer.length];

        for (int i = 0; i < buffer.length; i++) {
            Cell cell = row.getCell(i, Row.RETURN_BLANK_AS_NULL);
            if (cell == null) {
                array[i] = buffer[i];
            } else {
                cell = evaluator.evaluateInCell(cell);
                if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        array[i] = jsonDateFormat.format(date);                        // dates are returned as strings
                    } else {
                        //todo Dates should be returned as Dates and formatted to String elsewhere
                        array[i] = ((Double) cell.getNumericCellValue()).intValue();    // numbers are returned as integers
                    }
                } else {
                    String value = formatter.formatCellValue(cell).trim();
                    if (value.toLowerCase().equals("true")) {
                        array[i] = true;
                    } else if (value.toLowerCase().equals("false")) {
                        array[i] = false;
                    } else {
                        array[i] = value;
                    }
                }
            }
            buffer[i] = array[i]; //remember the value in buffer
        }
        return array;
    }

    private static SimpleDateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

    @Override
    public void remove() {
        iterator.remove();
    }

    @Override
    public Iterator iterator() {
        return this;
    }
}
