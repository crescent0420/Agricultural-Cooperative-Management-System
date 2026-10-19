package com.acms.dto;
import com.acms.entity.FarmerStatus;
import jakarta.validation.constraints.NotNull;
public record FarmerStatusChangeRequest(
        @NotNull(message = "Status is required.")
        FarmerStatus status) {
}
