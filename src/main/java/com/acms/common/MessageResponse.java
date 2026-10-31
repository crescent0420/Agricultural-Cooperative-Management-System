package com.acms.common;

/** A plain message returned by a service; the controller sends it as the message of an ApiResponse. */
public record MessageResponse(String message) {
}
