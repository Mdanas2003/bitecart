package com.bitecart.service;

import com.bitecart.dto.AdminReportDTO;
import java.time.LocalDate;

public interface AdminReportService {
    AdminReportDTO reportFor(LocalDate startDate, LocalDate endDate);
}
