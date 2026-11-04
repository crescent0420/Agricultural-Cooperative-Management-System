package com.acms.controller;

import com.acms.common.ApiResponse;
import com.acms.common.BaseController;
import com.acms.config.ApiTags;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Management reports. All filters are optional; district staff are limited to their own district anyway.
 * A report is a whole document (rows + totals of the same snapshot), so it is not paginated.
 */
@Tag(name = ApiTags.MANAGEMENT_REPORTS)
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController extends BaseController {

    private final ReportService reportService;

    @Operation(
            summary = "Report: farmers per cooperative",
            description = """
                    Report: farmers per cooperative and status.

                    - **Permission:** `REPORT_FARMERS_PER_COOPERATIVE` (Field Officer, Agronomist, Registrar, Admin)
                    - **Scope:** district staff see only their own district (Field Officers: their own offerings); asking for another district gives 403
                    - **Success:** 200
                    """)
    @GetMapping("/farmers-per-cooperative")
    public ApiResponse<ReportResponse<FarmersPerCooperativeRow, FarmerCountTotals>> farmersPerCooperative(
            @RequestParam(required = false) Long districtId) {
        return ok(reportService.farmersPerCooperative(districtId));
    }

    @Operation(
            summary = "Report: enrolments and capacity per offering",
            description = """
                    Report: enrolments and capacity per offering.

                    - **Permission:** `REPORT_OFFERING_CAPACITY` (Field Officer, Agronomist, Registrar, Admin)
                    - **Scope:** district staff see only their own district (Field Officers: their own offerings); asking for another district gives 403
                    - **Success:** 200
                    """)
    @GetMapping("/offering-capacity")
    public ApiResponse<ReportResponse<OfferingCapacityRow, OfferingCapacityTotals>> offeringCapacity(
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) Long districtId) {
        return ok(reportService.offeringCapacity(seasonId, districtId));
    }

    @Operation(
            summary = "Report: outstanding contributions per season",
            description = """
                    Report: outstanding contributions per season and district.

                    - **Permission:** `REPORT_OUTSTANDING_CONTRIBUTIONS` (Registrar, Finance)
                    - **Scope:** district staff see only their own district (Field Officers: their own offerings); asking for another district gives 403
                    - **Success:** 200
                    """)
    @GetMapping("/outstanding-contributions")
    public ApiResponse<ReportResponse<OutstandingContributionRow, OutstandingContributionTotals>> outstandingContributions(
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) Long districtId) {
        return ok(reportService.outstandingContributions(seasonId, districtId));
    }

    @Operation(
            summary = "Report: grade distribution per offering",
            description = """
                    Report: grade distribution per offering.

                    - **Permission:** `REPORT_GRADE_DISTRIBUTION` (Field Officer, Agronomist, Registrar, Admin)
                    - **Scope:** district staff see only their own district (Field Officers: their own offerings); asking for another district gives 403
                    - **Success:** 200
                    """)
    @GetMapping("/grade-distribution")
    public ApiResponse<ReportResponse<GradeDistributionRow, GradeDistributionTotals>> gradeDistribution(
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) Long districtId) {
        return ok(reportService.gradeDistribution(seasonId, districtId));
    }
}
