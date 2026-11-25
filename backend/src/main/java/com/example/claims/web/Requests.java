package com.example.claims.web;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class Requests {

    private Requests() {
    }

    public record Message(@NotBlank @Size(max = 4000) String message) {
    }

    public record Amount(@NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
                         @Size(max = 4000) String note) {
    }
}
