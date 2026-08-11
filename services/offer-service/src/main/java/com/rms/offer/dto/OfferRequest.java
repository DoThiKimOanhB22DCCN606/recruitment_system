package com.rms.offer.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.validation.constraints.AssertTrue;

public class OfferRequest {

    @NotNull
    private UUID applicationId;

    @NotNull
    private UUID candidateId;

    @NotNull
    @Min(0)
    private BigDecimal proposedSalary;

    @NotBlank
    private String currency;

    @NotNull
    @Future
    private LocalDate startDate;

    @NotNull
    @Future
    private LocalDate expiryDate;

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
