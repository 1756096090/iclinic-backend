package com.iclinic.iclinicbackend.modules.appointment.service;

import com.iclinic.iclinicbackend.modules.appointment.dto.*;
import com.iclinic.iclinicbackend.modules.appointment.entity.Appointment;
import com.iclinic.iclinicbackend.modules.appointment.entity.BranchBlockedSlot;
import com.iclinic.iclinicbackend.modules.appointment.entity.BranchSchedule;
import com.iclinic.iclinicbackend.modules.appointment.mapper.AppointmentMapper;
import com.iclinic.iclinicbackend.modules.appointment.repository.AppointmentRepository;
import com.iclinic.iclinicbackend.modules.appointment.repository.BranchBlockedSlotRepository;
import com.iclinic.iclinicbackend.modules.appointment.repository.BranchScheduleRepository;
import com.iclinic.iclinicbackend.modules.branch.entity.Branch;
import com.iclinic.iclinicbackend.modules.branch.repository.BranchRepository;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.modules.crm.contact.repository.CrmContactRepository;
import com.iclinic.iclinicbackend.shared.enums.AppointmentStatus;
import com.iclinic.iclinicbackend.shared.exception.BranchNotFoundException;
import com.iclinic.iclinicbackend.shared.exception.CompanyNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private static final List<AppointmentStatus> OCCUPYING_STATUSES =
            List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointmentRepository;
    private final BranchScheduleRepository branchScheduleRepository;
    private final BranchBlockedSlotRepository branchBlockedSlotRepository;
    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;
    private final CrmContactRepository crmContactRepository;
    private final AppointmentMapper appointmentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlotDto> getAvailableSlots(Long branchId, LocalDate date) {
        log.info("Calculating available slots for branch={} date={}", branchId, date);
        Branch branch = loadBranch(branchId);

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        BranchSchedule schedule = branchScheduleRepository
                .findByBranchIdAndDayOfWeekAndActiveTrue(branch.getId(), dayOfWeek)
                .orElse(null);

        if (schedule == null) {
            return List.of();
        }

        LocalDateTime dayStart = LocalDateTime.of(date, schedule.getStartTime());
        LocalDateTime dayEnd = LocalDateTime.of(date, schedule.getEndTime());

        List<BranchBlockedSlot> blockedSlots =
                branchBlockedSlotRepository
                        .findByBranchIdAndActiveTrueAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                                branchId, dayEnd, dayStart
                        );

        List<Appointment> appointments =
                appointmentRepository.findByBranchIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThan(
                        branchId,
                        OCCUPYING_STATUSES,
                        dayEnd,
                        dayStart
                );

        int slotDuration = schedule.getSlotDurationMinutes();
        LocalDateTime current = dayStart;
        java.util.ArrayList<AvailableSlotDto> slots = new java.util.ArrayList<>();

        while (!current.plusMinutes(slotDuration).isAfter(dayEnd)) {
            LocalDateTime slotStart = current;
            LocalDateTime slotEnd = current.plusMinutes(slotDuration);

            boolean blocked = overlapsBlockedSlot(slotStart, slotEnd, blockedSlots);
            boolean occupied = overlapsAppointment(slotStart, slotEnd, appointments, null);

            if (!blocked && !occupied) {
                slots.add(AvailableSlotDto.builder()
                        .start(slotStart)
                        .end(slotEnd)
                        .build());
            }

            current = slotEnd;
        }

        return slots;
    }

    @Override
    public AppointmentResponseDto createAppointment(CreateAppointmentRequestDto dto) {
        log.info("Creating appointment for company={} branch={} contact={}",
                dto.getCompanyId(), dto.getBranchId(), dto.getContactId());

        validateDateRange(dto.getScheduledStart(), dto.getScheduledEnd());

        Company company = loadCompany(dto.getCompanyId());
        Branch branch = loadBranch(dto.getBranchId());
        CrmContact contact = loadContact(dto.getContactId());

        validateBranchBelongsToCompany(branch, company);
        validateContactBelongsToCompany(contact, company);

        validateTimeInsideSchedule(branch.getId(), dto.getScheduledStart(), dto.getScheduledEnd());
        validateNotBlocked(branch.getId(), dto.getScheduledStart(), dto.getScheduledEnd());
        validateNoAppointmentOverlap(branch.getId(), dto.getScheduledStart(), dto.getScheduledEnd(), null);

        Appointment appointment = Appointment.builder()
                .company(company)
                .branch(branch)
                .contact(contact)
                .scheduledStart(dto.getScheduledStart())
                .scheduledEnd(dto.getScheduledEnd())
                .status(AppointmentStatus.SCHEDULED)
                .notes(dto.getNotes())
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        return appointmentMapper.toResponseDto(saved);
    }

    @Override
    public AppointmentResponseDto rescheduleAppointment(Long appointmentId, RescheduleAppointmentRequestDto dto) {
        log.info("Rescheduling appointment={}", appointmentId);

        validateDateRange(dto.getScheduledStart(), dto.getScheduledEnd());

        Appointment appointment = findEntityById(appointmentId);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalArgumentException("No se puede reagendar una cita cancelada");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException("No se puede reagendar una cita completada");
        }

        validateTimeInsideSchedule(
                appointment.getBranch().getId(),
                dto.getScheduledStart(),
                dto.getScheduledEnd()
        );

        validateNotBlocked(
                appointment.getBranch().getId(),
                dto.getScheduledStart(),
                dto.getScheduledEnd()
        );

        validateNoAppointmentOverlap(
                appointment.getBranch().getId(),
                dto.getScheduledStart(),
                dto.getScheduledEnd(),
                appointment.getId()
        );

        appointment.setScheduledStart(dto.getScheduledStart());
        appointment.setScheduledEnd(dto.getScheduledEnd());

        if (dto.getNotes() != null && !dto.getNotes().isBlank()) {
            appointment.setNotes(dto.getNotes());
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            appointment.setStatus(AppointmentStatus.SCHEDULED);
        }

        Appointment saved = appointmentRepository.save(appointment);
        return appointmentMapper.toResponseDto(saved);
    }

    @Override
    public AppointmentResponseDto cancelAppointment(Long appointmentId, CancelAppointmentRequestDto dto) {
        log.info("Cancelling appointment={}", appointmentId);

        Appointment appointment = findEntityById(appointmentId);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalArgumentException("La cita ya está cancelada");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);

        if (dto != null && dto.getReason() != null && !dto.getReason().isBlank()) {
            String currentNotes = appointment.getNotes() == null ? "" : appointment.getNotes() + System.lineSeparator();
            appointment.setNotes(currentNotes + "Cancelación: " + dto.getReason());
        }

        Appointment saved = appointmentRepository.save(appointment);
        return appointmentMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> findByBranch(Long branchId) {
        loadBranch(branchId);
        return appointmentRepository.findByBranchIdOrderByScheduledStartAsc(branchId).stream()
                .map(appointmentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponseDto> findByContact(Long contactId) {
        loadContact(contactId);
        return appointmentRepository.findByContactIdOrderByScheduledStartDesc(contactId).stream()
                .map(appointmentMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Appointment findEntityById(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Cita no encontrada: " + appointmentId));
    }

    // ── Private Helpers ────────────────────────────────────────────────────────

    private Company loadCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
    }

    private Branch loadBranch(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));
    }

    private CrmContact loadContact(Long contactId) {
        return crmContactRepository.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contacto CRM no encontrado: " + contactId));
    }

    private void validateBranchBelongsToCompany(Branch branch, Company company) {
        if (!branch.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("La sucursal no pertenece a la empresa indicada");
        }
    }

    private void validateContactBelongsToCompany(CrmContact contact, Company company) {
        if (!contact.getCompany().getId().equals(company.getId())) {
            throw new IllegalArgumentException("El contacto no pertenece a la empresa indicada");
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("La fecha/hora de inicio y fin son requeridas");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("La fecha/hora de fin debe ser mayor a la de inicio");
        }
    }

    private void validateTimeInsideSchedule(Long branchId, LocalDateTime start, LocalDateTime end) {
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new IllegalArgumentException("La cita debe estar dentro del mismo día");
        }

        DayOfWeek dayOfWeek = start.getDayOfWeek();
        BranchSchedule schedule = branchScheduleRepository
                .findByBranchIdAndDayOfWeekAndActiveTrue(branchId, dayOfWeek)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La sucursal no tiene horario activo para el día " + dayOfWeek));

        LocalDateTime scheduleStart = LocalDateTime.of(start.toLocalDate(), schedule.getStartTime());
        LocalDateTime scheduleEnd = LocalDateTime.of(start.toLocalDate(), schedule.getEndTime());

        if (start.isBefore(scheduleStart) || end.isAfter(scheduleEnd)) {
            throw new IllegalArgumentException("La cita está fuera del horario configurado para la sucursal");
        }
    }

    private void validateNotBlocked(Long branchId, LocalDateTime start, LocalDateTime end) {
        List<BranchBlockedSlot> blockedSlots =
                branchBlockedSlotRepository
                        .findByBranchIdAndActiveTrueAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                                branchId, end, start
                        );

        if (!blockedSlots.isEmpty()) {
            throw new IllegalArgumentException("La sucursal tiene un bloqueo en ese horario");
        }
    }

    private void validateNoAppointmentOverlap(
            Long branchId,
            LocalDateTime start,
            LocalDateTime end,
            Long ignoredAppointmentId
    ) {
        List<Appointment> overlapping =
                appointmentRepository.findByBranchIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThan(
                        branchId,
                        OCCUPYING_STATUSES,
                        end,
                        start
                );

        boolean hasConflict = overlapping.stream()
                .anyMatch(a -> ignoredAppointmentId == null || !a.getId().equals(ignoredAppointmentId));

        if (hasConflict) {
            throw new IllegalArgumentException("Ya existe una cita en ese horario para la sucursal");
        }
    }

    private boolean overlapsBlockedSlot(
            LocalDateTime start,
            LocalDateTime end,
            List<BranchBlockedSlot> blockedSlots
    ) {
        return blockedSlots.stream()
                .anyMatch(blocked -> start.isBefore(blocked.getEndDateTime())
                        && end.isAfter(blocked.getStartDateTime()));
    }

    private boolean overlapsAppointment(
            LocalDateTime start,
            LocalDateTime end,
            List<Appointment> appointments,
            Long ignoredAppointmentId
    ) {
        return appointments.stream()
                .filter(a -> ignoredAppointmentId == null || !a.getId().equals(ignoredAppointmentId))
                .anyMatch(a -> start.isBefore(a.getScheduledEnd())
                        && end.isAfter(a.getScheduledStart()));
    }
}

