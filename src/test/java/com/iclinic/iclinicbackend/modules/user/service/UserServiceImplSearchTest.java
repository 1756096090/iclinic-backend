package com.iclinic.iclinicbackend.modules.user.service;
import com.iclinic.iclinicbackend.modules.user.dto.UserResponseDto;
import com.iclinic.iclinicbackend.modules.user.entity.EcuadorianUser;
import com.iclinic.iclinicbackend.modules.user.mapper.UserMapper;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Search Tests")
class UserServiceImplSearchTest {
    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private com.iclinic.iclinicbackend.modules.auth.service.CurrentUserService currentUserService;
    @InjectMocks
    private UserServiceImpl userService;
    @Test
    @DisplayName("shouldSearchUsersByBranchAndTextUsingTrimmedQueryAndLimit")
    void shouldSearchUsersByBranchAndTextUsingTrimmedQueryAndLimit() {
        when(currentUserService.isSuperAdmin()).thenReturn(true);
        EcuadorianUser user1 = new EcuadorianUser();
        user1.setId(1L);
        user1.setFirstName("Veronica");
        user1.setLastName("Salazar");
        EcuadorianUser user2 = new EcuadorianUser();
        user2.setId(2L);
        user2.setFirstName("Victor");
        user2.setLastName("Gomez");
        UserResponseDto dto1 = UserResponseDto.builder().id(1L).fullName("Veronica Salazar").build();
        UserResponseDto dto2 = UserResponseDto.builder().id(2L).fullName("Victor Gomez").build();
        when(userRepository.searchByBranchIdAndText(eq(10L), eq("ver"), any())).thenReturn(new PageImpl<>(List.of(user1, user2)));
        when(userMapper.toResponseDto(user1)).thenReturn(dto1);
        when(userMapper.toResponseDto(user2)).thenReturn(dto2);
        List<UserResponseDto> result = userService.searchByBranchIdAndText(10L, "  ver  ", 5);
        assertEquals(2, result.size());
        assertEquals("Veronica Salazar", result.get(0).getFullName());
        assertEquals("Victor Gomez", result.get(1).getFullName());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).searchByBranchIdAndText(eq(10L), eq("ver"), pageableCaptor.capture());
        assertEquals(5, pageableCaptor.getValue().getPageSize());
    }
    @Test
    @DisplayName("shouldUseDefaultLimitWhenLimitIsNull")
    void shouldUseDefaultLimitWhenLimitIsNull() {
        when(currentUserService.isSuperAdmin()).thenReturn(true);
        when(userRepository.searchByBranchIdAndText(eq(11L), eq(""), any())).thenReturn(Page.empty());
        List<UserResponseDto> result = userService.searchByBranchIdAndText(11L, null, null);
        assertEquals(0, result.size());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).searchByBranchIdAndText(eq(11L), eq(""), pageableCaptor.capture());
        assertEquals(20, pageableCaptor.getValue().getPageSize());
    }

    @Test
    @DisplayName("shouldFindDoctorsByBranchIdOnly")
    void shouldFindDoctorsByBranchIdOnly() {
        when(currentUserService.isSuperAdmin()).thenReturn(true);
        EcuadorianUser doctor = new EcuadorianUser();
        doctor.setId(3L);
        doctor.setFirstName("Ana");
        doctor.setLastName("Lopez");

        UserResponseDto dto = UserResponseDto.builder().id(3L).fullName("Ana Lopez").build();

        when(userRepository.findByBranchIdAndRoleAndActiveTrueOrderByFirstNameAsc(eq(5L), any())).thenReturn(List.of(doctor));
        when(userMapper.toResponseDto(doctor)).thenReturn(dto);

        List<UserResponseDto> result = userService.findDoctorsByBranchId(5L);

        assertEquals(1, result.size());
        assertEquals("Ana Lopez", result.get(0).getFullName());

        verify(userRepository).findByBranchIdAndRoleAndActiveTrueOrderByFirstNameAsc(5L, com.iclinic.iclinicbackend.shared.enums.UserRole.DENTIST);
    }
}