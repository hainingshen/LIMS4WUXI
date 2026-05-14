package com.njxs.lims.device.persistence;

import com.njxs.lims.device.domain.DeviceFileRecord;
import com.njxs.lims.device.domain.DeviceRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository {

  List<DeviceRecord> list(String keyword, int limit, int offset);

  int count(String keyword);

  boolean existsDevice(UUID id);

  boolean existsFile(UUID fileId);

  List<DeviceFileRecord> listFiles(UUID deviceId);

  DeviceFileRecord saveFile(DeviceFileRecord record, UUID actorUserId);

  Optional<DeviceFileRecord> disableFile(UUID id, UUID actorUserId);

  boolean deviceUsedByAssignedWorkOrder(UUID deviceId, UUID userId);

  void clearCurrent(UUID deviceId, String fileType, UUID actorUserId);
}
