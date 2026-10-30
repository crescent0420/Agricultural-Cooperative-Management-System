package com.acms.service;

import com.acms.common.BaseService;
import com.acms.common.ReportMath;
import com.acms.dto.report.FarmerCountTotals;
import com.acms.dto.report.FarmersPerCooperativeRow;
import com.acms.dto.report.GradeCount;
import com.acms.dto.report.GradeDistributionRow;
import com.acms.dto.report.GradeDistributionTotals;
import com.acms.dto.report.OfferingCapacityRow;
import com.acms.dto.report.OfferingCapacityTotals;
import com.acms.dto.report.OutstandingContributionRow;
import com.acms.dto.report.OutstandingContributionTotals;
import com.acms.dto.report.ReportResponse;
import com.acms.dto.report.ReportScope;
import com.acms.entity.AppUser;
import com.acms.entity.Role;
import com.acms.repository.DistrictRepository;
import com.acms.repository.ReportQueries;
import com.acms.repository.SeasonRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The management reports. They answer the Union's complaint that "the reports prepared by different
 * districts do not agree":
 *  - ONE definition per report (the SQL in ReportQueries and the basis text below), used for every district;
 *    a district report is the Union report with a district filter, never a separate calculation;
 *  - calculated from the source records at the moment of the request, never stored or typed in;
 *  - REPEATABLE READ: every query of one report reads the same snapshot, so rows and totals always agree
 *    even while payments or enrolments are being recorded at the same time;
 *  - the totals are added up from the rows themselves.
 *
 * Who sees what (permissions in PermissionMatrix, records checked here):
 *  - Union Registrar, System Admin (and Finance Officer for contributions): the whole Union, or one district;
 *  - District Agronomist: only their own district (asking for another district is a 403);
 *  - Field Officer: their own district, and in the offering reports only the offerings assigned to them.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class ReportService extends BaseService {

    private static final String NO_PERMISSION = "You do not have permission to perform this action.";

    static final String FARMERS_BASIS = "Farmers counted by their current cooperative and status (Active, Suspended, "
            + "Withdrawn, Retired). Every cooperative is listed, also those without farmers. A district's farmers are "
            + "those of its cooperatives.";
    static final String CAPACITY_BASIS = "enrolled = enrolments with status ENROLLED (cancelled ones free their "
            + "place and are counted apart); placesLeft = maxParticipants - enrolled; capacityUsedPercent = "
            + "enrolled x 100 / maxParticipants, rounded half-up to 2 decimals; hectaresEnrolled = sum of the "
            + "hectares of the ENROLLED enrolments.";
    static final String CONTRIBUTIONS_BASIS = "Per season and district (the district of the farmer's cooperative). "
            + "amountDue = the amounts currently owed (after adjustments); confirmedPaid = CONFIRMED payments only; "
            + "pendingPayments are shown but not counted as paid; outstanding = amountDue - confirmedPaid; "
            + "collectedPercent = confirmedPaid x 100 / amountDue, rounded down to 2 decimals (the same rule as a "
            + "farmer's own balance). fullyPaid / partlyPaid / unpaid count the contributions by confirmed payments.";
    static final String GRADES_BASIS = "Only PUBLISHED results count (drafts and results awaiting approval are not "
            + "official). percent = results with that grade x 100 / published results, rounded half-up to 2 "
            + "decimals (so the shares may add up to 99.99 or 100.01); averageFinalScore = mean final score of the "
            + "published results; notYetPublished = active enrolments without a published result. Grades follow "
            + "the current grading scale, best first; a grade of an older scale is listed after them.";

    private final ReportQueries queries;
    private final FarmerAccessService farmerAccessService;
    private final DistrictRepository districtRepository;
    private final SeasonRepository seasonRepository;

    // ---------- 1. farmers per cooperative ----------

    public ReportResponse<FarmersPerCooperativeRow, FarmerCountTotals> farmersPerCooperative(Long districtId) {
        ReportScope scope = scope(districtId, null, false);
        List<FarmersPerCooperativeRow> rows = queries.farmersPerCooperative(scope.districtId());
        FarmerCountTotals totals = new FarmerCountTotals(
                rows.size(),
                sum(rows, FarmersPerCooperativeRow::active),
                sum(rows, FarmersPerCooperativeRow::suspended),
                sum(rows, FarmersPerCooperativeRow::withdrawn),
                sum(rows, FarmersPerCooperativeRow::retired),
                sum(rows, FarmersPerCooperativeRow::total));
        return new ReportResponse<>("Farmers per cooperative", FARMERS_BASIS, scope, Instant.now(), rows, totals);
    }

    // ---------- 2. enrolments and capacity per programme offering ----------

    public ReportResponse<OfferingCapacityRow, OfferingCapacityTotals> offeringCapacity(Long seasonId, Long districtId) {
        ReportScope scope = scope(districtId, seasonId, true);
        List<OfferingCapacityRow> rows =
                queries.offeringCapacity(scope.seasonId(), scope.districtId(), scope.fieldOfficerId());
        long max = sum(rows, OfferingCapacityRow::maxParticipants);
        long enrolled = sum(rows, OfferingCapacityRow::enrolled);
        OfferingCapacityTotals totals = new OfferingCapacityTotals(
                rows.size(),
                rows.stream().filter(OfferingCapacityRow::full).count(),
                max,
                enrolled,
                sum(rows, OfferingCapacityRow::cancelled),
                sum(rows, OfferingCapacityRow::placesLeft),
                ReportMath.percent(enrolled, max),
                total(rows, OfferingCapacityRow::hectaresEnrolled));
        return new ReportResponse<>("Enrolments and capacity per programme offering", CAPACITY_BASIS, scope,
                Instant.now(), rows, totals);
    }

    // ---------- 3. outstanding contributions per season ----------

    public ReportResponse<OutstandingContributionRow, OutstandingContributionTotals> outstandingContributions(
            Long seasonId, Long districtId) {
        ReportScope scope = scope(districtId, seasonId, false);
        List<OutstandingContributionRow> rows = queries.outstandingContributions(scope.seasonId(), scope.districtId());
        BigDecimal due = total(rows, OutstandingContributionRow::amountDue);
        BigDecimal paid = total(rows, OutstandingContributionRow::confirmedPaid);
        OutstandingContributionTotals totals = new OutstandingContributionTotals(
                sum(rows, OutstandingContributionRow::contributions),
                due,
                paid,
                total(rows, OutstandingContributionRow::pendingPayments),
                total(rows, OutstandingContributionRow::outstanding),
                ReportMath.collectedPercent(paid, due),
                sum(rows, OutstandingContributionRow::fullyPaid),
                sum(rows, OutstandingContributionRow::partlyPaid),
                sum(rows, OutstandingContributionRow::unpaid));
        return new ReportResponse<>("Outstanding contributions per season", CONTRIBUTIONS_BASIS, scope,
                Instant.now(), rows, totals);
    }

    // ---------- 4. grade distribution per programme offering ----------

    public ReportResponse<GradeDistributionRow, GradeDistributionTotals> gradeDistribution(Long seasonId,
                                                                                          Long districtId) {
        ReportScope scope = scope(districtId, seasonId, true);
        List<GradeDistributionRow> rows =
                queries.gradeDistribution(scope.seasonId(), scope.districtId(), scope.fieldOfficerId());

        long published = sum(rows, GradeDistributionRow::published);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (GradeDistributionRow row : rows) {
            for (GradeCount grade : row.grades()) {
                counts.merge(grade.grade(), grade.count(), Long::sum);
            }
        }
        // every published result weighs the same (this is NOT the average of the offering averages)
        BigDecimal scoreSum = total(rows, GradeDistributionRow::totalFinalScore);
        GradeDistributionTotals totals = new GradeDistributionTotals(
                rows.size(),
                published,
                sum(rows, GradeDistributionRow::notYetPublished),
                ReportMath.average(scoreSum, published),
                ReportQueries.gradeCounts(queries.gradingScale(), counts, published));
        return new ReportResponse<>("Grade distribution per programme offering", GRADES_BASIS, scope,
                Instant.now(), rows, totals);
    }

    // ---------- who sees what ----------

    /**
     * The filters a report really runs with. District staff are pinned to their own district; a Field Officer
     * also to their own offerings when ownOfferingsForOfficers is true. Unknown district or season: 404.
     */
    private ReportScope scope(Long districtId, Long seasonId, boolean ownOfferingsForOfficers) {
        AppUser user = farmerAccessService.getCurrentUser();
        Long district = districtId;
        Long fieldOfficerId = null;

        if (FarmerAccessService.isDistrictRestricted(user)) {
            if (user.getDistrict() == null) {
                throw new AccessDeniedException(NO_PERMISSION);
            }
            Long own = user.getDistrict().getId();
            if (districtId != null && !districtId.equals(own)) {
                throw new AccessDeniedException(NO_PERMISSION);
            }
            district = own;
            if (ownOfferingsForOfficers && user.getRole() == Role.FIELD_OFFICER) {
                fieldOfficerId = user.getId();
            }
        } else if (districtId != null && !districtRepository.existsById(districtId)) {
            throw notFound("District", districtId);
        }

        if (seasonId != null && !seasonRepository.existsById(seasonId)) {
            throw notFound("Season", seasonId);
        }
        return new ReportScope(district, seasonId, fieldOfficerId);
    }

    // ---------- adding up ----------

    private static <R> long sum(List<R> rows, ToLongFunction<R> value) {
        return rows.stream().mapToLong(value).sum();
    }

    private static <R> BigDecimal total(List<R> rows, Function<R, BigDecimal> value) {
        return ReportMath.money(rows.stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
