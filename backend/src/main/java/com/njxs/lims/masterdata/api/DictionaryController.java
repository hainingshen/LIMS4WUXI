package com.njxs.lims.masterdata.api;

import com.njxs.lims.masterdata.service.DictionaryItemService;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(prefix = "lims.persistence", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DictionaryController {

  private final DictionaryItemService dictionaryItemService;

  public DictionaryController(DictionaryItemService dictionaryItemService) {
    this.dictionaryItemService = dictionaryItemService;
  }

  @GetMapping("/dictionaries")
  public List<DictionaryItemResponse> list(@RequestParam(required = false) String dictCode) {
    return dictionaryItemService.list(dictCode).stream()
        .map(DictionaryItemResponse::from)
        .toList();
  }
}
