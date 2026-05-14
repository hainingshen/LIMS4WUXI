package com.njxs.lims.workcenter.domain;

public record ProjectHealth(
    int score,
    String status
) {

  public static ProjectHealth of(int score, String status) {
    return new ProjectHealth(score, status);
  }
}
