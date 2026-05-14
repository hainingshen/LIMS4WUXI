package com.njxs.lims.submission.persistence;

import com.njxs.lims.submission.domain.Submission;
import com.njxs.lims.submission.domain.SubmissionTestSelection;

public interface SubmissionBaseRepository {

  void saveSubmission(Submission submission);

  void saveTestSelection(SubmissionTestSelection selection);
}
