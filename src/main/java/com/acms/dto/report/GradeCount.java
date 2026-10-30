package com.acms.dto.report;

import java.math.BigDecimal;

/** How many published results have this grade, and their share of all published results. */
public record GradeCount(String grade, long count, BigDecimal percent) {
}
