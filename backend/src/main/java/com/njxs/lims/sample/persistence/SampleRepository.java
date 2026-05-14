package com.njxs.lims.sample.persistence;

import com.njxs.lims.sample.domain.Sample;
import java.util.Optional;
import java.util.UUID;

public interface SampleRepository {

  String nextSampleBizNo(int year);

  void saveSample(Sample sample);

  Optional<Sample> findById(UUID id);

  Sample updateWorkflow(Sample sample, UUID actorUserId);

  boolean allProjectSamplesInStock(UUID projectId);
}
