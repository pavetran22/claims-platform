package com.example.claims.claim;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitClaimRequest(
        @NotNull Market market,
        @NotNull ProductType productType,
        @NotNull @PastOrPresent LocalDate incidentDate,
        @NotBlank @Size(max = 4000) String description,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal claimedAmount) {
}
