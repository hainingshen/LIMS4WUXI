package com.njxs.lims.quotation.persistence;

import com.njxs.lims.quotation.domain.Quotation;
import com.njxs.lims.quotation.domain.QuotationItem;
import java.util.List;

public interface QuotationRepository {

  String nextQuotationBizNo(int year);

  void save(Quotation quotation, List<QuotationItem> items);
}
