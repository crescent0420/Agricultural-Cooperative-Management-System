package com.acms.entity;

import com.acms.common.BaseEntity;
import com.acms.enums.WindowStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One period during which farmers may enrol in a season's programme offerings. */
@Entity
@Table(name = "enrolment_windows")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnrolmentWindow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false, updatable = false)
    private Season season;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WindowStatus status;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opened_by", nullable = false, updatable = false)
    private AppUser openedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private AppUser closedBy;

    public EnrolmentWindow(Season season, AppUser openedBy, Instant openedAt) {
        this.season = season;
        this.openedBy = openedBy;
        this.openedAt = openedAt;
        this.status = WindowStatus.OPEN;
    }

    public boolean isOpen() {
        return status == WindowStatus.OPEN;
    }

    public void close(AppUser closedBy, Instant closedAt) {
        this.status = WindowStatus.CLOSED;
        this.closedBy = closedBy;
        this.closedAt = closedAt;
    }
}
