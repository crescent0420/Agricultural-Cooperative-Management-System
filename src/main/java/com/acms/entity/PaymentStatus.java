package com.acms.entity;

/** PENDING -> CONFIRMED or PENDING -> FAILED, once. Only CONFIRMED payments count towards the balance. */
public enum PaymentStatus {
    PENDING,
    CONFIRMED,
    FAILED
}
