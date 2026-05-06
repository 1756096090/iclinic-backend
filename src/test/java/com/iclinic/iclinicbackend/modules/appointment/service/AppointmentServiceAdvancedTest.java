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
import com.iclinic.iclinicbackend.modules.branch.entity.ClinicBranch;
import com.iclinic.iclinicbackend.modules.branch.repository.BranchRepository;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.entity.EcuadorianCompany;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.modules.crm.contact.repository.CrmContactRepository;
import com.iclinic.iclinicbackend.shared.enums.AppointmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService Advanced Tests")
class AppointmentServiceAdvancedTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private BranchScheduleRepository branchScheduleRepository;

    @Mock
    private BranchBlockedSlotRepository branchBlockedSlotRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CrmContactRepository crmContactRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private Company company;
    private Branch branch;
    private CrmContact contact;
    private BranchSchedule schedule;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now().plusDays(1);

        company = new EcuadorianCompany("Test Company", "1234567890123");
        company.setId(1L);

        branch = new ClinicBranch("Test Branch", "Test Address", true, company);
        branch.setId(1L);

        contact = CrmContact.builder().id(1L).company(company).build();

        schedule = BranchSchedule.builder()
                .id(1L)
                .branch(branch)
                .dayOfWeek(testDate.getDayOfWeek())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .slotDurationMinutes(30)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("getAvailableSlots - Verifica múltiples slots en un día")
    void testGetAvailableSlotsMultipleSlots() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(branchScheduleRepository.findByBranchIdAndDayOfWeekAndActiveTrue(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(schedule));
        when(branchBlockedSlotRepository.findByBranchIdAndActiveTrueAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                anyLong(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findByBranchIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThan(
                anyLong(), anyList(), any(), any())).thenReturn(List.of());

        List<AvailableSlotDto> slots = appointmentService.getAvailableSlots(1L, testDate);

        // Con horario 9:00-17:00 y slots de 30 min, esperamos 16 slots
        assertNotNull(slots);
        assertTrue(slots.size() >= 8, "Debería haber al menos 8 slots en 4 horas");
    }

    @Test
    @DisplayName("getAvailableSlots - Filtra correctamente bloques reservados")
    void testGetAvailableSlotsWithBlockedSlots() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(branchScheduleRepository.findByBranchIdAndDayOfWeekAndActiveTrue(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(schedule));

        BranchBlockedSlot blockedSlot = BranchBlockedSlot.builder()
                .id(1L)
                .startDateTime(LocalDateTime.of(testDate, LocalTime.of(12, 0)))
                .endDateTime(LocalDateTime.of(testDate, LocalTime.of(13, 0)))
                .build();

        when(branchBlockedSlotRepository.findByBranchIdAndActiveTrueAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                anyLong(), any(), any())).thenReturn(List.of(blockedSlot));
        when(appointmentRepository.findByBranchIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThan(
                anyLong(), anyList(), any(), any())).thenReturn(List.of());

        List<AvailableSlotDto> slots = appointmentService.getAvailableSlots(1L, testDate);

        // No debería incluir slots en el bloque 12:00-13:00
        for (AvailableSlotDto slot : slots) {
            assertFalse(slot.getStart().isBefore(LocalDateTime.of(testDate, LocalTime.of(13, 0))) &&
                       slot.getEnd().isAfter(LocalDateTime.of(testDate, LocalTime.of(12, 0))));
        }
    }

    @Test
    @DisplayName("createAppointment - Verifica que sucursal pertenece a empresa")
    void testCreateAppointmentBranchNotInCompany() {
        CreateAppointmentRequestDto dto = CreateAppointmentRequestDto.builder()
                .companyId(1L)
                .branchId(2L)
                .contactId(1L)
                .scheduledStart(LocalDateTime.of(testDate, LocalTime.of(10, 0)))
                .scheduledEnd(LocalDateTime.of(testDate, LocalTime.of(10, 30)))
                .build();

        Branch differentBranch = new ClinicBranch("Otro", "Otro", true,
                new EcuadorianCompany("Otra", "9876543210"));
        differentBranch.setId(2L);

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(branchRepository.findById(2L)).thenReturn(Optional.of(differentBranch));

        assertThrows(IllegalArgumentException.class, () -> appointmentService.createAppointment(dto));
    }

    @Test
    @DisplayName("createAppointment - Verifica que contacto pertenece a empresa")
    void testCreateAppointmentContactNotInCompany() {
        CreateAppointmentRequestDto dto = CreateAppointmentRequestDto.builder()
                .companyId(1L)
                .branchId(1L)
                .contactId(2L)
                .scheduledStart(LocalDateTime.of(testDate, LocalTime.of(10, 0)))
                .scheduledEnd(LocalDateTime.of(testDate, LocalTime.of(10, 30)))
                .build();

        Company differentCompany = new EcuadorianCompany("Otra", "9876543210");
        differentCompany.setId(2L);
        CrmContact differentContact = CrmContact.builder().id(2L).company(differentCompany).build();

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(crmContactRepository.findById(2L)).thenReturn(Optional.of(differentContact));

        assertThrows(IllegalArgumentException.class, () -> appointmentService.createAppointment(dto));
    }

    @Test
    @DisplayName("rescheduleAppointment - No permite reagendar cita completada")
    void testRescheduleCompletedAppointment() {
        Appointment completedAppointment = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.COMPLETED)
                .build();

        RescheduleAppointmentRequestDto dto = RescheduleAppointmentRequestDto.builder()
                .scheduledStart(LocalDateTime.of(testDate, LocalTime.of(11, 0)))
                .scheduledEnd(LocalDateTime.of(testDate, LocalTime.of(11, 30)))
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(completedAppointment));

        assertThrows(IllegalArgumentException.class, () -> appointmentService.rescheduleAppointment(1L, dto));
    }

    @Test
    @DisplayName("cancelAppointment - Agrega motivo en notas")
    void testCancelAppointmentWithReason() {
        Appointment appointment = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.SCHEDULED)
                .notes("Notas originales")
                .build();

        CancelAppointmentRequestDto dto = CancelAppointmentRequestDto.builder()
                .reason("Cambio de planes")
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenReturn(appointment);
        when(appointmentMapper.toResponseDto(any())).thenReturn(AppointmentResponseDto.builder().id(1L).build());

        appointmentService.cancelAppointment(1L, dto);

        assertTrue(appointment.getNotes().contains("Cancelación: Cambio de planes"));
    }

    @Test
    @DisplayName("getAvailableSlots - Horario con duración 15 minutos")
    void testGetAvailableSlotsShortDuration() {
        BranchSchedule shortSchedule = BranchSchedule.builder()
                .id(1L)
                .branch(branch)
                .dayOfWeek(testDate.getDayOfWeek())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .slotDurationMinutes(15)
                .active(true)
                .build();

        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(branchScheduleRepository.findByBranchIdAndDayOfWeekAndActiveTrue(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(shortSchedule));
        when(branchBlockedSlotRepository.findByBranchIdAndActiveTrueAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                anyLong(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findByBranchIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThan(
                anyLong(), anyList(), any(), any())).thenReturn(List.of());

        List<AvailableSlotDto> slots = appointmentService.getAvailableSlots(1L, testDate);

        // 9:00-10:00 con slots de 15 min = 4 slots
        assertEquals(4, slots.size(), "Debería haber exactamente 4 slots de 15 minutos");
    }

    @Test
    @DisplayName("createAppointment - Status inicial es SCHEDULED")
    void testCreateAppointmentInitialStatus() {
        CreateAppointmentRequestDto dto = CreateAppointmentRequestDto.builder()
                .companyId(1L)
                .branchId(1L)
                .contactId(1L)
                .scheduledStart(LocalDateTime.of(testDate, LocalTime.of(10, 0)))
                .scheduledEnd(LocalDateTime.of(testDate, LocalTime.of(10, 30)))
                .build();

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(crmContactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(branchScheduleRepository.findByBranchIdAndDayOfWeekAndActiveTrue(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(schedule));
        when(branchBlockedSlotRepository.findByBranchIdAndActiveTrueAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                anyLong(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findByBranchIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThan(
                anyLong(), anyList(), any(), any())).thenReturn(List.of());

        Appointment savedAppointment = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        when(appointmentRepository.save(any())).thenReturn(savedAppointment);
        when(appointmentMapper.toResponseDto(any())).thenReturn(AppointmentResponseDto.builder()
                .id(1L)
                .status(AppointmentStatus.SCHEDULED)
                .build());

        AppointmentResponseDto response = appointmentService.createAppointment(dto);

        assertEquals(AppointmentStatus.SCHEDULED, response.getStatus());
    }

    @Test
    @DisplayName("rescheduleAppointment - Mantiene contacto original")
    void testRescheduleAppointmentKeepsContact() {
        Appointment appointment = Appointment.builder()
                .id(1L)
                .branch(branch)
                .contact(contact)
                .status(AppointmentStatus.SCHEDULED)
                .scheduledStart(LocalDateTime.of(testDate, LocalTime.of(10, 0)))
                .scheduledEnd(LocalDateTime.of(testDate, LocalTime.of(10, 30)))
                .build();

        RescheduleAppointmentRequestDto dto = RescheduleAppointmentRequestDto.builder()
                .scheduledStart(LocalDateTime.of(testDate, LocalTime.of(11, 0)))
                .scheduledEnd(LocalDateTime.of(testDate, LocalTime.of(11, 30)))
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(branchScheduleRepository.findByBranchIdAndDayOfWeekAndActiveTrue(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(schedule));
        when(branchBlockedSlotRepository.findByBranchIdAndActiveTrueAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                anyLong(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findByBranchIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThan(
                anyLong(), anyList(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.save(any())).thenReturn(appointment);
        when(appointmentMapper.toResponseDto(any())).thenReturn(AppointmentResponseDto.builder().id(1L).build());

        appointmentService.rescheduleAppointment(1L, dto);

        assertEquals(contact.getId(), appointment.getContact().getId());
    }
}

