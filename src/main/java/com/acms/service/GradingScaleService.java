package com.acms.service;

import com.acms.common.BaseService;
import com.acms.dto.GradingBandDto;
import com.acms.dto.GradingScaleRequest;
import com.acms.entity.AppUser;
import com.acms.entity.GradingBand;
import com.acms.enums.AuditEventType;
import com.acms.exception.BadRequestException;
import com.acms.repository.GradingBandRepository;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The configurable grading scale. A final score gets the grade of the highest band whose minScore it
 * reaches. A result keeps the grade it received when it was published: changing the scale later does not
 * silently rewrite published grades (only a correction recalculates, with the scale in force at that time).
 */
@Service
@RequiredArgsConstructor
public class GradingScaleService extends BaseService {

    private final GradingBandRepository bandRepository;
    private final FarmerAccessService farmerAccessService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<GradingBandDto> current() {
        return bandRepository.findAllByOrderByMinScoreDesc().stream().map(GradingBandDto::from).toList();
    }

    @Transactional(readOnly = true)
    public String gradeFor(BigDecimal finalScore) {
        return bandRepository.findAllByOrderByMinScoreDesc().stream()
                .filter(band -> finalScore.compareTo(band.getMinScore()) >= 0)
                .findFirst()
                .map(GradingBand::getGrade)
                .orElseThrow(() -> new IllegalStateException("The grading scale has no band starting at 0."));
    }

    /** Replaces the whole scale (Union Registrar). */
    @Transactional
    public List<GradingBandDto> replace(GradingScaleRequest request) {
        List<GradingBandDto> bands = request.bands();
        Set<String> grades = new HashSet<>();
        Set<BigDecimal> minimums = new HashSet<>();
        boolean startsAtZero = false;
        for (GradingBandDto band : bands) {
            if (!grades.add(band.grade().trim().toUpperCase(Locale.ROOT))) {
                throw new BadRequestException("Grade '" + band.grade().trim() + "' appears twice.");
            }
            // stripTrailingZeros: 70 and 70.00 are the same minimum.
            if (!minimums.add(band.minScore().stripTrailingZeros())) {
                throw new BadRequestException("Two bands have the same minScore " + band.minScore() + ".");
            }
            startsAtZero |= band.minScore().signum() == 0;
        }
        if (!startsAtZero) {
            throw new BadRequestException("One band must have minScore 0, so that every score gets a grade.");
        }

        String before = describe(current());
        bandRepository.deleteAllInBatch();
        bandRepository.saveAllAndFlush(bands.stream()
                .map(b -> new GradingBand(b.grade().trim(), b.minScore(),
                        b.description() == null ? null : b.description().trim()))
                .toList());

        AppUser actor = farmerAccessService.getCurrentUser();
        List<GradingBandDto> after = current();
        auditLogService.recordInCurrentTransaction(AuditEventType.GRADING_SCALE_CHANGED, true, actor.getId(),
                actor.getUsername(), "GRADING_SCALE", null,
                "Grading scale changed from [" + before + "] to [" + describe(after) + "]", null);
        return after;
    }

    private static String describe(List<GradingBandDto> bands) {
        return bands.stream().map(b -> b.grade() + ">=" + b.minScore()).collect(Collectors.joining(", "));
    }
}
