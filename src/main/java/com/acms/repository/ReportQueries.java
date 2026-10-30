package com.acms.repository;

import com.acms.common.ReportMath;
import com.acms.dto.report.FarmersPerCooperativeRow;
import com.acms.dto.report.GradeCount;
import com.acms.dto.report.GradeDistributionRow;
import com.acms.dto.report.OfferingCapacityRow;
import com.acms.dto.report.OutstandingContributionRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * The SQL of the management reports. Read-only, calculated from the source records at the moment of the
 * request (nothing is stored or cached), so a report can never be out of date or disagree with the data.
 *
 * Filters are optional: each one adds a fixed condition with a bound parameter, so no request value is ever
 * written into the SQL text. Every column has its own alias: rows are read by position, and two columns both
 * called "id" or "name" could otherwise be confused.
 */
@Repository
public class ReportQueries {

    @PersistenceContext
    private EntityManager entityManager;

    // ---------- 1. farmers per cooperative ----------

    public List<FarmersPerCooperativeRow> farmersPerCooperative(Long districtId) {
        Filters filters = new Filters()
                .add(districtId, "c.district_id = :districtId", "districtId");
        String sql = """
                SELECT c.id AS cooperative_id, c.name AS cooperative_name, d.id AS district_id, d.name AS district_name,
                       count(f.id) FILTER (WHERE f.status = 'ACTIVE')    AS active,
                       count(f.id) FILTER (WHERE f.status = 'SUSPENDED') AS suspended,
                       count(f.id) FILTER (WHERE f.status = 'WITHDRAWN') AS withdrawn,
                       count(f.id) FILTER (WHERE f.status = 'RETIRED')   AS retired,
                       count(f.id)                                       AS total
                  FROM cooperatives c
                  JOIN districts d ON d.id = c.district_id
                  LEFT JOIN farmers f ON f.cooperative_id = c.id
                """ + filters.where() + """
                 GROUP BY c.id, c.name, d.id, d.name
                 ORDER BY d.name, c.name, c.id
                """;
        return rows(filters.bind(entityManager.createNativeQuery(sql))).stream()
                .map(r -> new FarmersPerCooperativeRow(
                        lng(r[0]), str(r[1]), lng(r[2]), str(r[3]),
                        lng(r[4]), lng(r[5]), lng(r[6]), lng(r[7]), lng(r[8])))
                .toList();
    }

    // ---------- 2. enrolments and capacity per programme offering ----------

    public List<OfferingCapacityRow> offeringCapacity(Long seasonId, Long districtId, Long fieldOfficerId) {
        Filters filters = offeringFilters(seasonId, districtId, fieldOfficerId);
        String sql = """
                SELECT o.id AS offering_id, p.id AS programme_id, p.name AS programme_name,
                       s.id AS season_id, s.name AS season_name, d.id AS district_id, d.name AS district_name,
                       u.id AS officer_id, u.username AS officer_username,
                       o.max_participants, o.hectares_per_participant,
                       count(e.id) FILTER (WHERE e.status = 'ENROLLED')  AS enrolled,
                       count(e.id) FILTER (WHERE e.status = 'CANCELLED') AS cancelled,
                       coalesce(sum(e.hectares) FILTER (WHERE e.status = 'ENROLLED'), 0) AS hectares_enrolled
                  FROM programme_offerings o
                  JOIN crop_programmes p ON p.id = o.programme_id
                  JOIN seasons s         ON s.id = o.season_id
                  JOIN districts d       ON d.id = p.district_id
                  JOIN app_users u       ON u.id = o.field_officer_id
                  LEFT JOIN enrolments e ON e.offering_id = o.id
                """ + filters.where() + """
                 GROUP BY o.id, p.id, p.name, s.id, s.name, s.starts_on, d.id, d.name, u.id, u.username
                 ORDER BY s.starts_on DESC, d.name, p.name, o.id
                """;
        return rows(filters.bind(entityManager.createNativeQuery(sql))).stream()
                .map(r -> {
                    int max = (int) lng(r[9]);
                    long enrolled = lng(r[11]);
                    return new OfferingCapacityRow(
                            lng(r[0]), lng(r[1]), str(r[2]), lng(r[3]), str(r[4]), lng(r[5]), str(r[6]),
                            lng(r[7]), str(r[8]),
                            max, enrolled, lng(r[12]), Math.max(0, max - enrolled),
                            ReportMath.percent(enrolled, max), enrolled >= max,
                            dec(r[10]), dec(r[13]));
                })
                .toList();
    }

    // ---------- 3. outstanding contributions per season ----------

    public List<OutstandingContributionRow> outstandingContributions(Long seasonId, Long districtId) {
        Filters filters = new Filters()
                .add(seasonId, "o.season_id = :seasonId", "seasonId")
                .add(districtId, "c.district_id = :districtId", "districtId");
        // paid = CONFIRMED payments only; PENDING ones are shown apart and do not reduce what is owed.
        String sql = """
                SELECT s.id AS season_id, s.name AS season_name, d.id AS district_id, d.name AS district_name,
                       count(o.id)                     AS contributions,
                       sum(o.amount_due)               AS amount_due,
                       coalesce(sum(pay.confirmed), 0) AS confirmed_paid,
                       coalesce(sum(pay.pending), 0)   AS pending_payments,
                       count(o.id) FILTER (WHERE coalesce(pay.confirmed, 0) >= o.amount_due) AS fully_paid,
                       count(o.id) FILTER (WHERE coalesce(pay.confirmed, 0) > 0
                                             AND coalesce(pay.confirmed, 0) < o.amount_due) AS partly_paid,
                       count(o.id) FILTER (WHERE coalesce(pay.confirmed, 0) = 0)            AS unpaid
                  FROM contribution_obligations o
                  JOIN seasons s      ON s.id = o.season_id
                  JOIN farmers f      ON f.id = o.farmer_id
                  JOIN cooperatives c ON c.id = f.cooperative_id
                  JOIN districts d    ON d.id = c.district_id
                  LEFT JOIN (SELECT obligation_id,
                                    sum(amount) FILTER (WHERE status = 'CONFIRMED') AS confirmed,
                                    sum(amount) FILTER (WHERE status = 'PENDING')   AS pending
                               FROM payments
                              GROUP BY obligation_id) pay ON pay.obligation_id = o.id
                """ + filters.where() + """
                 GROUP BY s.id, s.name, s.starts_on, d.id, d.name
                 ORDER BY s.starts_on DESC, s.id, d.name
                """;
        return rows(filters.bind(entityManager.createNativeQuery(sql))).stream()
                .map(r -> {
                    BigDecimal due = ReportMath.money(dec(r[5]));
                    BigDecimal paid = ReportMath.money(dec(r[6]));
                    return new OutstandingContributionRow(
                            lng(r[0]), str(r[1]), lng(r[2]), str(r[3]),
                            lng(r[4]), due, paid, ReportMath.money(dec(r[7])),
                            // never negative: an amount can never be lowered below what was confirmed (Step 6)
                            due.subtract(paid), ReportMath.collectedPercent(paid, due),
                            lng(r[8]), lng(r[9]), lng(r[10]));
                })
                .toList();
    }

    // ---------- 4. grade distribution per programme offering ----------

    public List<GradeDistributionRow> gradeDistribution(Long seasonId, Long districtId, Long fieldOfficerId) {
        Filters filters = offeringFilters(seasonId, districtId, fieldOfficerId);

        // (a) one line per offering: enrolments, published results and the sum of their final scores
        String offeringsSql = """
                SELECT o.id AS offering_id, p.name AS programme_name, s.id AS season_id, s.name AS season_name,
                       d.id AS district_id, d.name AS district_name, u.username AS officer_username,
                       count(e.id) FILTER (WHERE e.status = 'ENROLLED')  AS active_enrolments,
                       count(r.id) FILTER (WHERE r.status = 'PUBLISHED') AS published,
                       count(e.id) FILTER (WHERE e.status = 'ENROLLED'
                                             AND (r.id IS NULL OR r.status <> 'PUBLISHED')) AS not_yet_published,
                       coalesce(sum(r.final_score) FILTER (WHERE r.status = 'PUBLISHED'), 0) AS total_final_score
                  FROM programme_offerings o
                  JOIN crop_programmes p ON p.id = o.programme_id
                  JOIN seasons s         ON s.id = o.season_id
                  JOIN districts d       ON d.id = p.district_id
                  JOIN app_users u       ON u.id = o.field_officer_id
                  LEFT JOIN enrolments e         ON e.offering_id = o.id
                  LEFT JOIN inspection_results r ON r.enrolment_id = e.id
                """ + filters.where() + """
                 GROUP BY o.id, p.name, s.id, s.name, s.starts_on, d.id, d.name, u.username
                 ORDER BY s.starts_on DESC, d.name, p.name, o.id
                """;
        List<Object[]> offerings = rows(filters.bind(entityManager.createNativeQuery(offeringsSql)));

        // (b) the published grades of the same offerings, counted per grade
        String gradesSql = """
                SELECT o.id AS offering_id, r.grade AS grade, count(*) AS results
                  FROM inspection_results r
                  JOIN enrolments e          ON e.id = r.enrolment_id
                  JOIN programme_offerings o ON o.id = e.offering_id
                  JOIN crop_programmes p     ON p.id = o.programme_id
                """ + filters.where("r.status = 'PUBLISHED'") + """
                 GROUP BY o.id, r.grade
                """;
        Map<Long, Map<String, Long>> gradesByOffering = new LinkedHashMap<>();
        for (Object[] r : rows(filters.bind(entityManager.createNativeQuery(gradesSql)))) {
            gradesByOffering.computeIfAbsent(lng(r[0]), id -> new LinkedHashMap<>()).put(str(r[1]), lng(r[2]));
        }

        List<String> scale = gradingScale();
        return offerings.stream()
                .map(r -> {
                    long published = lng(r[8]);
                    BigDecimal scoreTotal = dec(r[10]).setScale(2, RoundingMode.HALF_UP);
                    return new GradeDistributionRow(
                            lng(r[0]), str(r[1]), lng(r[2]), str(r[3]), lng(r[4]), str(r[5]), str(r[6]),
                            lng(r[7]), published, lng(r[9]),
                            scoreTotal, ReportMath.average(scoreTotal, published),
                            gradeCounts(scale, gradesByOffering.getOrDefault(lng(r[0]), Map.of()), published));
                })
                .toList();
    }

    /** The grades of the current scale, best first. */
    @SuppressWarnings("unchecked")
    public List<String> gradingScale() {
        List<Object> grades = entityManager
                .createNativeQuery("SELECT grade FROM grading_bands ORDER BY min_score DESC")
                .getResultList();
        return grades.stream().map(ReportQueries::str).toList();
    }

    /**
     * Every grade of the current scale (with 0 when nobody got it), then any grade that only exists in an older
     * scale (a published grade keeps the scale in force when it was published).
     */
    public static List<GradeCount> gradeCounts(List<String> scale, Map<String, Long> counts, long published) {
        List<GradeCount> result = new ArrayList<>();
        for (String grade : scale) {
            long count = counts.getOrDefault(grade, 0L);
            result.add(new GradeCount(grade, count, ReportMath.percent(count, published)));
        }
        counts.keySet().stream()
                .filter(grade -> !scale.contains(grade))
                .sorted()
                .forEach(grade -> result.add(new GradeCount(grade, counts.get(grade),
                        ReportMath.percent(counts.get(grade), published))));
        return result;
    }

    // ---------- helpers ----------

    private static Filters offeringFilters(Long seasonId, Long districtId, Long fieldOfficerId) {
        return new Filters()
                .add(seasonId, "o.season_id = :seasonId", "seasonId")
                .add(districtId, "p.district_id = :districtId", "districtId")
                .add(fieldOfficerId, "o.field_officer_id = :fieldOfficerId", "fieldOfficerId");
    }

    @SuppressWarnings("unchecked")
    private static List<Object[]> rows(Query query) {
        return query.getResultList();
    }

    private static long lng(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static BigDecimal dec(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }

    private static String str(Object value) {
        return value == null ? null : value.toString();
    }

    /** Optional WHERE conditions, each with its bound parameter. */
    private static final class Filters {

        private final List<String> conditions = new ArrayList<>();
        private final Map<String, Object> parameters = new LinkedHashMap<>();

        Filters add(Object value, String condition, String parameter) {
            if (value != null) {
                conditions.add(condition);
                parameters.put(parameter, value);
            }
            return this;
        }

        /** "WHERE a AND b" (or nothing), plus any fixed conditions given here. */
        String where(String... fixed) {
            List<String> all = new ArrayList<>(List.of(fixed));
            all.addAll(conditions);
            return all.isEmpty() ? "" : " WHERE " + String.join(" AND ", all) + "\n";
        }

        Query bind(Query query) {
            parameters.forEach(query::setParameter);
            return query;
        }
    }
}
