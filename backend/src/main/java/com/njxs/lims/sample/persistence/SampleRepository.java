package com.njxs.lims.sample.persistence;

import com.njxs.lims.sample.domain.Sample;

public interface SampleRepository {

  void saveSample(Sample sample);
}
