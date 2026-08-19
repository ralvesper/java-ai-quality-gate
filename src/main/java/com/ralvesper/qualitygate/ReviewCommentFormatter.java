package com.ralvesper.qualitygate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class ReviewCommentFormatter {

    public String format(AiReviewResult result, AiReviewPolicy.Decision decision, String commitRef) {
        StringBuilder sb = new StringBuilder();

        sb.append("## AI Review — ").append(decision.status()).append("\n\n");

        sb.append("**Provider:** ").append(nullSafe(result.provider()));
        if (commitRef != null && !commitRef.isBlank()) {
            sb.append(" | **Commit:** `").append(commitRef).append("`");
        }
        sb.append(" | **Date:** ").append(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        sb.append("\n\n");

        if (result.summary() != null && !result.summary().isBlank()) {
            sb.append("### Summary\n\n");
            sb.append(result.summary()).append("\n\n");
        }

        if (result.findings() != null && !result.findings().isEmpty()) {
            sb.append("### Findings\n\n");
            sb.append("| Severity | Confidence | Category | File | Message |\n");
            sb.append("|----------|------------|----------|------|---------|\n");
            for (AiFinding f : result.findings()) {
                String file = f.file() == null ? "-" : f.file()
                        + (f.line() == null ? "" : ":" + f.line());
                sb.append("| ")
                        .append(nullSafe(f.severity())).append(" | ")
                        .append(nullSafe(f.confidence())).append(" | ")
                        .append(nullSafe(f.category())).append(" | ")
                        .append(escapePipe(file)).append(" | ")
                        .append(escapePipe(nullSafe(f.message()))).append(" |\n");
            }
            sb.append("\n");
        } else {
            sb.append("### Findings\n\n_No findings._\n\n");
        }

        sb.append("---\n");
        sb.append("Policy: CRITICAL/HIGH + HIGH confidence = BLOCK\n");

        return sb.toString();
    }

    private String nullSafe(Object value) {
        return value == null ? "-" : value.toString();
    }

    private String escapePipe(String text) {
        if (text == null) return "-";
        return text.replace("|", "\\|");
    }
}
