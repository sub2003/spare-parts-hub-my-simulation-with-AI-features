package com.sliit.sparepartshub.reporting.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Tiny dependency-free PDF writer for text reports. It deliberately supports
 * only what Function 6 needs: paged Helvetica text. Avoiding a new Maven PDF
 * dependency keeps the existing working build surface unchanged.
 */
public final class SimplePdfWriter {

    private static final int LINES_PER_PAGE = 48;

    private SimplePdfWriter() {
    }

    public static byte[] textReport(List<String> sourceLines) {
        List<String> lines = sourceLines == null ? List.of() : sourceLines;
        List<List<String>> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += LINES_PER_PAGE) {
            pages.add(lines.subList(i, Math.min(lines.size(), i + LINES_PER_PAGE)));
        }
        if (pages.isEmpty()) {
            pages.add(List.of("No data available."));
        }

        int objectCount = 3 + pages.size() * 2;
        List<byte[]> objects = new ArrayList<>(objectCount + 1);
        objects.add(null); // object 0 is the PDF free-list sentinel

        objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            int pageObject = 4 + (i * 2);
            kids.append(pageObject).append(" 0 R ");
        }
        objects.add(bytes("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));

        for (int i = 0; i < pages.size(); i++) {
            int contentObject = 5 + (i * 2);
            String page = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                    + "/Resources << /Font << /F1 3 0 R >> >> /Contents "
                    + contentObject + " 0 R >>";
            objects.add(bytes(page));

            StringBuilder content = new StringBuilder("BT\n/F1 9 Tf\n44 752 Td\n13 TL\n");
            for (String line : pages.get(i)) {
                content.append('(').append(pdfText(line)).append(") Tj\nT*\n");
            }
            content.append("ET\n");
            byte[] stream = bytes(content.toString());
            String head = "<< /Length " + stream.length + " >>\nstream\n";
            String tail = "endstream";
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            obj.writeBytes(bytes(head));
            obj.writeBytes(stream);
            obj.writeBytes(bytes(tail));
            objects.add(obj.toByteArray());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(bytes("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n"));
        long[] offsets = new long[objectCount + 1];

        for (int i = 1; i <= objectCount; i++) {
            offsets[i] = out.size();
            out.writeBytes(bytes(i + " 0 obj\n"));
            out.writeBytes(objects.get(i));
            out.writeBytes(bytes("\nendobj\n"));
        }

        long xref = out.size();
        out.writeBytes(bytes("xref\n0 " + (objectCount + 1) + "\n"));
        out.writeBytes(bytes("0000000000 65535 f \n"));
        for (int i = 1; i <= objectCount; i++) {
            out.writeBytes(bytes(String.format("%010d 00000 n \n", offsets[i])));
        }
        out.writeBytes(bytes("trailer\n<< /Size " + (objectCount + 1) + " /Root 1 0 R >>\n"));
        out.writeBytes(bytes("startxref\n" + xref + "\n%%EOF\n"));
        return out.toByteArray();
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String pdfText(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (char c : value.toCharArray()) {
            char safe = c <= 255 ? c : '?';
            if (safe == '\\' || safe == '(' || safe == ')') {
                result.append('\\');
            }
            if (safe == '\r' || safe == '\n' || safe == '\t') {
                result.append(' ');
            } else {
                result.append(safe);
            }
        }
        return result.toString();
    }
}
