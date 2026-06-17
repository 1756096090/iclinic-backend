package com.iclinic.iclinicbackend.modules.appointment.service;

import com.iclinic.iclinicbackend.modules.appointment.dto.*;
import com.iclinic.iclinicbackend.modules.appointment.entity.Appointment;
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
import com.iclinic.iclinicbackend.modules.user.entity.EcuadorianUser;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.shared.enums.AppointmentStatus;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import com.iclinic.iclinicbackend.shared.exception.BranchNotFoundException;
import com.iclinic.iclinicbackend.shared.exception.CompanyNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService Tests")
class AppointmentServiceImplTest {

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
    private UserRepository userRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    @Mock
    private com.iclinic.iclinicbackend.modules.auth.service.CurrentUserService currentUserService;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private Company company;
    private Branch branch;
    private CrmContact contact;
    private EcuadorianUser doctor;
    private BranchSchedule schedule;
    private Appointment appointment;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now().plusDays(1);

        company = new EcuadorianCompany("Test Company", "1234567890123");
        company.setId(1L);

        branch = new ClinicBranch("Test Branch", "Test Address", true, company);
        branch.setId(1L);

        contact = CrmContact.builder().id(1L).company(company).build();

        doctor = new EcuadorianUser();
        doctor.setId(2L);
        doctor.setFirstName("Dr");
        doctor.setLastName("House");
        doctor.setEmail("doctor@test.com");
        doctor.setPassword("secret");
        doctor.setRole(UserRole.DENTIST);
        doctor.setDocumentType(com.iclinic.iclinicbackend.shared.enums.DocumentType.CEDULA_EC);
        doctor.setDocumentNumber("1711111111");
        doctor.setCompany(company);
        doctor.setBranch(branch);

        schedule = BranchSchedule.builder()
                .id(1L)
                .branch(branch)
                .doctor(doctor)
                .dayOfWeek(testDate.getDayOfWeek())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .slotDurationMinutes(30)
                .active(true)
                .build();

        appointment = Appointment.builder()
                .id(1L)
                .company(company)
                .branch(branch)
                .contact(contact)
                .doctor(doctor)
                .scheduledStart(LocalDateTime.of(testDate, LocalTime.of(10, 0)))
                .scheduledEnd(LocalDateTime.of(testDate, LocalTime.of(10, 30)))
                .status(AppointmentStatus.SCHEDULED)
                .build();
    }

    @Test
    @DisplayName("getAvailableSlots - Retorna slots cuando no hay citas ni bloques")
    void testGetAvailableSlotsSuccess() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(userRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(branchScheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(2L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(schedule));
        when(branchBlockedSlotRepository.findByBranchIdAndActiveTrueAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                anyLong(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findByDoctorIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThan(
                anyLong(), anyList(), any(), any())).thenReturn(List.of());

        List<AvailableSlotDto> slots = appointmentService.getAvailableSlots(1L, 2L, testDate);

        assertNotNull(slots);
        assertFalse(slots.isEmpty());
        assertFalse(slots.isEmpty());
    }

    @Test
    @DisplayName("getAvailableSlots - Retorna lista vacía cuando no hay horario")
    void testGetAvailableSlotsNoSchedule() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(userRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(branchScheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(2L, testDate.getDayOfWeek()))
                .thenReturn(Optional.empty());

        List<AvailableSlotDto> slots = appointmentService.getAvailableSlots(1L, 2L, testDate);

        assertNotNull(slots);
        assertTrue(slots.isEmpty());
    }

    @Test
    @DisplayName("getAvailableSlots - Lanza excepción cuando sucursal no existe")
    void testGetAvailableSlotsNoBranch() {
        when(branchRepository.findById(1L)).thenThrow(new BranchNotFoundException(1L));

        assertThrows(BranchNotFoundException.class, () -> appointmentService.getAvailableSlots(1L, 2L, testDate));
    }

    @Test
    @DisplayName("createAppointment - Crea cita exitosamente")
    void testCreateAppointmentSuccess() {
        CreateAppointmentRequestDto dto = CreateAppointmentRequestDto.builder()
                .companyId(1L)
                .branchId(1L)
                .contactId(1L)
                .doctorId(2L)
                .scheduledStart(LocalDateTime.of(testDate, LocalTime.of(10, 0)))
                .scheduledEnd(LocalDateTime.of(testDate, LocalTime.of(10, 30)))
                .notes("Test appointment")
                .build();

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(crmContactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(userRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(branchScheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(2L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(schedule));
        when(branchBlockedSlotRepository.findByBranchIdAndActiveTrueAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                anyLong(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findByDoctorIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThan(
                anyLong(), anyList(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.save(any())).thenReturn(appointment);
        when(appointmentMapper.toResponseDto(any())).thenReturn(AppointmentResponseDto.builder().id(1L).build());

        AppointmentResponseDto response = appointmentService.createAppointment(dto);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    @DisplayName("createAppointment - Falla cuando empresa no existe")
    void testCreateAppointmentCompanyNotFound() {
        CreateAppointmentRequestDto dto = CreateAppointmentRequestDto.builder()
                .companyId(999L)
                .branchId(1L)
                .contactId(1L)
                .doctorId(2L)
                .scheduledStart(LocalDateTime.of(testDate, LocalTime.of(10, 0)))
                .scheduledEnd(LocalDateTime.of(testDate, LocalTime.of(10, 30)))
                .build();

        when(companyRepository.findById(999L)).thenThrow(new CompanyNotFoundException(999L));

        assertThrows(CompanyNotFoundException.class, () -> appointmentService.createAppointment(dto));
    }

    @Test
    @DisplayName("createAppointment - Falla cuando fechas son inválidas")
    void testCreateAppointmentInvalidDateRange() {
        CreateAppointmentRequestDto dto = CreateAppointmentRequestDto.builder()
                .companyId(1L)
                .branchId(1L)
                .contactId(1L)
                .doctorId(2L)
                .scheduledStart(LocalDateTime.of(testDate, LocalTime.of(10, 30)))
                .scheduledEnd(LocalDateTime.of(testDate, LocalTime.of(10, 0)))
                .build();


        assertThrows(IllegalArgumentException.class, () -> appointmentService.createAppointment(dto));
    }

    @Test
    @DisplayName("createAppointment - Falla cuando hay conflicto con otra cita")
    void testCreateAppointmentConflict() {
        CreateAppointmentRequestDto dto = CreateAppointmentRequestDto.builder()
                .companyId(1L)
                .branchId(1L)
                .contactId(1L)
                .doctorId(2L)
                .scheduledStart(LocalDateTime.of(testDate, LocalTime.of(10, 0)))
                .scheduledEnd(LocalDateTime.of(testDate, LocalTime.of(10, 30)))
                .build();

        Appointment conflictingAppointment = Appointment.builder()
                .id(2L)
                .status(AppointmentStatus.CONFIRMED)
                .scheduledStart(LocalDateTime.of(testDate, LocalTime.of(10, 15)))
                .scheduledEnd(LocalDateTime.of(testDate, LocalTime.of(10, 45)))
                .build();

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(crmContactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(userRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(branchScheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(2L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(schedule));
        when(branchBlockedSlotRepository.findByBranchIdAndActiveTrueAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                anyLong(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findByDoctorIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThan(
                anyLong(), anyList(), any(), any())).thenReturn(List.of(conflictingAppointment));

        assertThrows(IllegalArgumentException.class, () -> appointmentService.createAppointment(dto));
    }

    @Test
    @DisplayName("rescheduleAppointment - Reagenda cita exitosamente")
    void testRescheduleAppointmentSuccess() {
        LocalDateTime newStart = LocalDateTime.of(testDate, LocalTime.of(11, 0));
        LocalDateTime newEnd = LocalDateTime.of(testDate, LocalTime.of(11, 30));

        RescheduleAppointmentRequestDto dto = RescheduleAppointmentRequestDto.builder()
                .scheduledStart(newStart)
                .scheduledEnd(newEnd)
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(branchScheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(2L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(schedule));
        when(branchBlockedSlotRepository.findByBranchIdAndActiveTrueAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                anyLong(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findByDoctorIdAndStatusInAndScheduledStartLessThanAndScheduledEndGreaterThanAndIdNot(
                anyLong(), anyList(), any(), any(), anyLong())).thenReturn(List.of());
        when(appointmentRepository.save(any())).thenReturn(appointment);
        when(appointmentMapper.toResponseDto(any())).thenReturn(AppointmentResponseDto.builder().id(1L).build());

        AppointmentResponseDto response = appointmentService.rescheduleAppointment(1L, dto);

        assertNotNull(response);
    }

    @Test
    @DisplayName("rescheduleAppointment - Falla cuando cita está cancelada")
    void testRescheduleAppointmentCancelled() {
        Appointment cancelledAppointment = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.CANCELLED)
                .build();

        RescheduleAppointmentRequestDto dto = RescheduleAppointmentRequestDto.builder()
                .scheduledStart(LocalDateTime.of(testDate, LocalTime.of(11, 0)))
                .scheduledEnd(LocalDateTime.of(testDate, LocalTime.of(11, 30)))
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(cancelledAppointment));

        assertThrows(IllegalArgumentException.class, () -> appointmentService.rescheduleAppointment(1L, dto));
    }

    @Test
    @DisplayName("cancelAppointment - Cancela cita exitosamente")
    void testCancelAppointmentSuccess() {
        CancelAppointmentRequestDto dto = CancelAppointmentRequestDto.builder()
                .reason("Cambio de horario")
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenReturn(appointment);
        when(appointmentMapper.toResponseDto(any())).thenReturn(AppointmentResponseDto.builder().id(1L).build());

        AppointmentResponseDto response = appointmentService.cancelAppointment(1L, dto);

        assertNotNull(response);
        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
    }

    @Test
    @DisplayName("cancelAppointment - Falla cuando cita ya está cancelada")
    void testCancelAppointmentAlreadyCancelled() {
        Appointment cancelledAppointment = Appointment.builder()
                .id(1L)
                .status(AppointmentStatus.CANCELLED)
                .build();

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(cancelledAppointment));

        assertThrows(IllegalArgumentException.class, () -> appointmentService.cancelAppointment(1L, null));
    }

    @Test
    @DisplayName("findByBranch - Retorna citas de sucursal")
    void testFindByBranchSuccess() {
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(appointmentRepository.findByBranchIdOrderByScheduledStartAsc(1L))
                .thenReturn(List.of(appointment));
        when(appointmentMapper.toResponseDto(any())).thenReturn(AppointmentResponseDto.builder().id(1L).build());

        List<AppointmentResponseDto> responses = appointmentService.findByBranch(1L);

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
    }

    @Test
    @DisplayName("findByContact - Retorna citas de contacto")
    void testFindByContactSuccess() {
        when(crmContactRepository.findById(1L)).thenReturn(Optional.of(contact));
        when(appointmentRepository.findByContactIdOrderByScheduledStartDesc(1L))
                .thenReturn(List.of(appointment));
        when(appointmentMapper.toResponseDto(any())).thenReturn(AppointmentResponseDto.builder().id(1L).build());

        List<AppointmentResponseDto> responses = appointmentService.findByContact(1L);

        assertNotNull(responses);
        assertFalse(responses.isEmpty());
    }

    @Test
    @DisplayName("findEntityById - Retorna cita por ID")
    void testFindEntityByIdSuccess() {
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        Appointment result = appointmentService.findEntityById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("findEntityById - Falla cuando cita no existe")
    void testFindEntityByIdNotFound() {
        when(appointmentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> appointmentService.findEntityById(999L));
    }
}

