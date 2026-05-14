package com.njxs.lims.submission.api;

import java.util.List;
import java.util.Map;

public record SubmissionSplitProjectsRequest(
    String splitStrategy,
    List<Map<String, Object>> projectDrafts
) {
}
