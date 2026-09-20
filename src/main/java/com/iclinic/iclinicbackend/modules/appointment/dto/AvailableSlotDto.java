package com.iclinic.iclinicbackend.modules.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableSlotDto {
    private Instant start;
    private Instant end;
}

