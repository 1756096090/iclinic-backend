package com.iclinic.iclinicbackend.modules.appointment.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Entity
@Table(
        name = "branch_schedules",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"branch_id", "day_of_week"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchSchedule {}