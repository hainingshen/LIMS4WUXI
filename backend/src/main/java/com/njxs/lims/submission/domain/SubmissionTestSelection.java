package com.njxs.lims.submission.domain;

import com.njxs.lims.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "submission_test_selection")
public class SubmissionTestSelection extends BaseEntity {

  @Column(name = "submission_id", nullable = false)
  private UUID submissionId;

  @Column(name = "category", nullable = false)
  private String category;

  @Column(name = "test_code", nullable = false)
  private String testCode;

  @Column(name = "test_name", nullable = false)
  private String testName;

  @Column(name = "method_name")
  private String methodName;

  @Column(name = "extractant")
  private String extractant;

  @Column(name = "standard")
  private String standard;

  @Column(name = "instruction")
  private String instruction;

  @Column(name = "note")
  private String note;

  public UUID getSubmissionId() {
    return submissionId;
  }

  public void setSubmissionId(UUID submissionId) {
    this.submissionId = submissionId;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getTestCode() {
    return testCode;
  }

  public void setTestCode(String testCode) {
    this.testCode = testCode;
  }

  public String getTestName() {
    return testName;
  }

  public void setTestName(String testName) {
    this.testName = testName;
  }

  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public String getExtractant() {
    return extractant;
  }

  public void setExtractant(String extractant) {
    this.extractant = extractant;
  }

  public String getStandard() {
    return standard;
  }

  public void setStandard(String standard) {
    this.standard = standard;
  }

  public String getInstruction() {
    return instruction;
  }

  public void setInstruction(String instruction) {
    this.instruction = instruction;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
