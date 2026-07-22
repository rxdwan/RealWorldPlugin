package com.rxdwan.realworld.util;

import java.util.ArrayList;
import java.util.List;

public class TableUtil {

    /**
     * Builds a bordered, column-aligned table as a list of strings (one per line),
     * ready to be sent via player.sendMessage() line by line.
     *
     * @param header column headers
     * @param rows   list of rows, each row is a list of cell values (same size as header)
     * @return list of formatted lines including borders
     */
    public static List<String> tabulize(List<String> header, List<List<String>> rows) {
        if (header == null || header.isEmpty()) {
            throw new IllegalArgumentException("Header cannot be null or empty.");
        }

        int cols = header.size();
        int[] colWidths = new int[cols];

        for (int i = 0; i < cols; i++) {
            colWidths[i] = stripColor(header.get(i)).length();
        }

        for (List<String> row : rows) {
            if (row.size() != cols) {
                throw new IllegalArgumentException("Row size must match header size.");
            }
            for (int i = 0; i < cols; i++) {
                int length = stripColor(row.get(i)).length();
                if (length > colWidths[i]) {
                    colWidths[i] = length;
                }
            }
        }

        List<String> output = new ArrayList<>();

        // Build borders
        StringBuilder borderBuilder = new StringBuilder("*");
        for (int i = 0; i < cols; i++) {
            borderBuilder.append(repeat("-", colWidths[i] + 4)).append("*");
        }
        String border = borderBuilder.toString();

        // Build header
        output.add(border);
        output.add(buildRow(header, colWidths));
        output.add(border);

        // Build data
        for (List<String> row : rows) {
            output.add(buildRow(row, colWidths));
        }

        // Build bottom border
        output.add(border);

        return output;
    }

    private static String buildRow(List<String> data, int[] colWidths) {
        StringBuilder rowBuilder = new StringBuilder("|");
        for (int i = 0; i < data.size(); i++) {
            String cell = data.get(i);
            int visibleLen = stripColor(cell).length();
            int padding = colWidths[i] - visibleLen;
            rowBuilder.append("  ").append(cell).append(repeat(" ", padding)).append("  |");
        }
        return rowBuilder.toString();
    }

    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private static String stripColor(String input) {
        if (input == null) return "";
        return input.replaceAll("(?i)§[0-9A-FK-OR]", "").replaceAll("(?i)&[0-9A-FK-OR]", "");
    }
}
