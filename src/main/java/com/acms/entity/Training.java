package com.acms.entity;

import com.acms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "trainings")
public class Training extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    protected Training() {
    }

    public Training(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
