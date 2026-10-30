package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.dto.report.FarmerCountTotals;
import com.acms.dto.report.FarmersPerCooperativeRow;
import com.acms.dto.report.GradeDistributionRow;
import com.acms.dto.report.GradeDistributionTotals;
import com.acms.dto.report.OfferingCapacityRow;
import com.acms.dto.report.OfferingCapacityTotals;
import com.acms.dto.report.OutstandingContributionRow;
import com.acms.dto.report.OutstandingContributionTotals;
import com.acms.dto.report.ReportResponse;
import com.acms.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Management reports. All filters are optional; district staff are limited to their own district anyway.
 * A report is a whole document (rows + totals of the same snapshot), so it is not paginated.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController extends BaseController {

    private final ReportService reportService;

    @GetMapping("/farmers-per-cooperative")
    public ApiResponse<ReportResponse<FarmersPerCooperativeRow, FarmerCountTotals>> farmersPerCooperative(
            @RequestParam(required = false) Long districtId) {
        return ok(reportService.farmersPerCooperative(districtId));
    }

    @GetMapping("/offering-capacity")
    public ApiResponse<ReportResponse<OfferingCapacityRow, OfferingCapacityTotals>> offeringCapacity(
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) Long districtId) {
        return ok(reportService.offeringCapacity(seasonId, districtId));
    }

    @GetMapping("/outstanding-contributions")
    public ApiResponse<ReportResponse<OutstandingContributionRow, OutstandingContributionTotals>> outstandingContributions(
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) Long districtId) {
        return ok(reportService.outstandingContributions(seasonId, districtId));
    }

    @GetMapping("/grade-distribution")
    public ApiResponse<ReportResponse<GradeDistributionRow, GradeDistributionTotals>> gradeDistribution(
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) Long districtId) {
        return ok(reportService.gradeDistribution(seasonId, districtId));
    }
}
