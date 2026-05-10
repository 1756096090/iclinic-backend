package com.iclinic.iclinicbackend.modules.appointment.entity;

import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.user.entity.User;
import com.iclinic.iclinicbackend.shared.entity.ActivableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(
        name = "branch_schedules",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"doctor_id", "day_of_week"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BranchSchedule extends ActivableEntity {

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @JoinColumn(name = "branch_id", nullable = false)
        private Branch branch;

        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        @JoinColumn(name = "doctor_id", nullable = false)
        private User doctor;

        @Enumerated(EnumType.STRING)
        @Column(name = "day_of_week", nullable = false, length = 20)
        private DayOfWeek dayOfWeek;

        @Column(name = "start_time", nullable = false)
        private LocalTime startTime;

        @Column(name = "end_time", nullable = false)
        private LocalTime endTime;

        @Column(name = "slot_duration_minutes", nullable = false)
        private Integer slotDurationMinutes;
}