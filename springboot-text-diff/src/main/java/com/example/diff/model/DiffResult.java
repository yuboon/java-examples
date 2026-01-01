package com.example.diff.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class DiffResult {
    private boolean hasChanges;
    private List<DiffChange> changes = new ArrayList<>();
    private List<DiffLine> diffLines = new ArrayList<>();

    public String toUnifiedFormat() {
        StringBuilder sb = new StringBuilder();
        for (DiffChange change : changes) {
            sb.append(String.format("@@ -%d,%d +%d,%d @@%n",
                    change.getSourceLine(),
                    change.getOriginalLines().size(),
                    change.getTargetLine(),
                    change.getRevisedLines().size()));

            for (String line : change.getOriginalLines()) {
                sb.append("- ").append(line).append("\n");
            }
            for (String line : change.getRevisedLines()) {
                sb.append("+ ").append(line).append("\n");
            }
        }
        return sb.toString();
    }

    public String toHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<div class='space-y-0 font-mono text-sm'>");

        for (DiffChange change : changes) {
            int srcLine = change.getSourceLine();
            int tgtLine = change.getTargetLine();

            html.append("<div class='diff-header bg-gray-200 font-bold px-2 py-1'>")
                .append(String.format("@@ -%d +%d @@ [%s]", srcLine, tgtLine, change.getType()))
                .append("</div>");

            for (String line : change.getOriginalLines()) {
                html.append("<div class='diff-line diff-remove bg-red-100 px-2 py-0.5'>")
                    .append("- ").append(escapeHtml(line))
                    .append("</div>");
            }

            for (String line : change.getRevisedLines()) {
                html.append("<div class='diff-line diff-add bg-green-100 px-2 py-0.5'>")
                    .append("+ ").append(escapeHtml(line))
                    .append("</div>");
            }
        }

        html.append("</div>");
        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }
}
