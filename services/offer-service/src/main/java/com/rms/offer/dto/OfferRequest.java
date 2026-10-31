package com.rms.offer.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class OfferRequest {

    @NotNull
    private UUID applicationId;

    @NotNull
    private UUID candidateId;

    @NotNull
    @Min(1)
    private BigDecimal proposedSalary;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase ISO 4217 code")
    private String currency;

    @NotNull
    @Future
    private LocalDate startDate;

    @FutureOrPresent
    private LocalDate expiryDate;

    @Size(max = 2000)
    private String notes;

    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }

    public UUID getCandidateId() { return candidateId; }
    public void setCandidateId(UUID candidateId) { this.candidateId = candidateId; }

    public BigDecimal getProposedSalary() { return proposedSalary; }
    public void setProposedSalary(BigDecimal proposedSalary) { this.proposedSalary = proposedSalary; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @AssertTrue(message = "Expiry date must be on or before the start date")
    public boolean isValidExpiryDate() {
        if (startDate == null || expiryDate == null) {
            return true;
        }
        return expiryDate.isBefore(startDate) || expiryDate.isEqual(startDate);
    }
}
