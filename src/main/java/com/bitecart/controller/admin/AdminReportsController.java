package com.bitecart.controller.admin;

import com.bitecart.service.AdminReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportsController {
    private static final int DEFAULT_REPORT_DAYS = 30;
    private final AdminReportService adminReportService;
    public AdminReportsController(AdminReportService adminReportService) { this.adminReportService = adminReportService; }
    @GetMapping public String reports(@RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate, Model model) {
        LocalDate today = LocalDate.now(); LocalDate start = today.minusDays(DEFAULT_REPORT_DAYS - 1L); LocalDate end = today; String error = null;
        if (startDate != null || endDate != null) {
            try { start = LocalDate.parse(startDate); end = LocalDate.parse(endDate); if (start.isAfter(end)) throw new DateTimeParseException("Invalid range", startDate, 0); }
            catch (DateTimeParseException | NullPointerException exception) { error = "Choose a valid start and end date. Showing the last 30 days instead."; start = today.minusDays(DEFAULT_REPORT_DAYS - 1L); end = today; }
        }
        model.addAttribute("report", adminReportService.reportFor(start, end)); model.addAttribute("startDate", start); model.addAttribute("endDate", end); model.addAttribute("reportError", error); return "admin/reports";
    }
}
