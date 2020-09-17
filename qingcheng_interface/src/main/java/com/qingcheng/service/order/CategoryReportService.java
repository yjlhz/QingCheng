package com.qingcheng.service.order;

import com.qingcheng.pojo.order.CategoryReport;

import java.time.LocalDate;
import java.util.List;

public interface CategoryReportService {

    public List<CategoryReport> categoryReport(LocalDate date);
}
