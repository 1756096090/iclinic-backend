package com.iclinic.iclinicbackend.modules.access;

import com.iclinic.iclinicbackend.modules.access.repository.CompanyMembershipRepository;
import com.iclinic.iclinicbackend.modules.access.service.MembershipService;
import com.iclinic.iclinicbackend.modules.company.entity.Company;
import com.iclinic.iclinicbackend.modules.company.repository.CompanyRepository;
import com.iclinic.iclinicbackend.modules.user.dto.CreateUserRequestDto;
import com.iclinic.iclinicbackend.modules.user.repository.UserRepository;
import com.iclinic.iclinicbackend.modules.user.service.UserService;
import com.iclinic.iclinicbackend.shared.enums.DocumentType;
import com.iclinic.iclinicbackend.shared.enums.UserRole;
import com.iclinic.iclinicbackend.shared.enums.UserType;
import com.iclinic.iclinicbackend.support.AbstractPostgresIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Lo que este paso tiene que demostrar: que a partir de ahora <strong>no se puede
 * crear un usuario sin pertenencia</strong>.
 * <p>
 * Hasta este cambio, {@code POST /api/v1/users} creaba el usuario y nada más: la
 * membresía solo aparecía si alguien reiniciaba la aplicación y corría el
 * backfill. Funcionaba por accidente, porque la autorización todavía miraba
 * {@code users.company_id}.
 */
@ActiveProfiles("it")
@SpringBootTest
class MembresiaEnElAltaIT extends AbstractPostgresIT {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyMembershipRepository membershipRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private TransactionTemplate tx;

    @MockitoSpyBean
    private MembershipService membershipService;

    /**
     * {@code UserServiceImpl.create} comprueba que el usuario actual pueda crear
     * ese usuario, asi que sin contexto de seguridad responde 401 y el test no
     * llega a lo que quiere probar. Se autentica como el ADMIN sembrado de la
     * empresa 1.
     */
    @BeforeEach
    void autenticarComoAdmin() {
        var admin = userRepository.findByEmail("juan.garcia@clinica.ec").orElseThrow();
        var auth = new UsernamePasswordAuthenticationToken(
                admin.getKeycloakUserId().toString(), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        auth.setDetails(admin);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("crear un usuario le da membresia EN LA MISMA peticion, sin reiniciar nada")
    @Transactional
    void crearUsuarioCreaLaMembresia() {
        Company empresa = companyRepository.findById(1L).orElseThrow();

        var creado = userService.create(dto("nuevo.miembro@clinica.ec", empresa.getId()));

        var membresias = membershipRepository.findByUserIdAndActiveTrue(creado.getId());
        assertThat(membresias)
                .as("antes de este cambio esta lista estaba vacia hasta el siguiente reinicio")
                .hasSize(1);
        assertThat(membresias.get(0).getCompany().getId()).isEqualTo(empresa.getId());
        assertThat(membresias.get(0).getRole()).isEqualTo(UserRole.ASSISTANT);
    }

    @Test
    @DisplayName("si la membresia falla, el usuario TAMPOCO queda")
    void siLaMembresiaFallaElUsuarioNoQueda() {
        // Es la razon de que el alta sea atomica: un usuario a medias es
        // exactamente el huerfano que este cambio elimina.
        // La configuracion del espia se envuelve en una transaccion a proposito:
        // @MockitoSpyBean envuelve el proxy transaccional, asi que la llamada del
        // `when(...)` pasa por el interceptor y `Propagation.MANDATORY` la
        // rechazaria. Que MANDATORY salte incluso aqui es buena senal: significa
        // que de verdad impide llamar a conceder() fuera de una transaccion.
        tx.executeWithoutResult(estado ->
                doThrow(new IllegalStateException("fallo simulado de la membresia"))
                        .when(membershipService).conceder(any(), any(), any(), any()));

        String correo = "huerfano@clinica.ec";

        assertThatThrownBy(() -> tx.executeWithoutResult(estado ->
                userService.create(dto(correo, 1L))))
                .isInstanceOf(IllegalStateException.class);

        assertThat(userRepository.findByEmail(correo))
                .as("la transaccion debe revertir tambien el usuario")
                .isEmpty();
    }

    @Test
    @DisplayName("un usuario sin empresa y sin ser admin de plataforma se rechaza")
    void usuarioSinEmpresaNiPlataformaSeRechaza() {
        // Antes se creaba sin mas y quedaba sin pertenecer a nada.
        assertThatThrownBy(() -> tx.executeWithoutResult(estado ->
                userService.create(dto("sin.empresa@clinica.ec", null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("necesita empresa");
    }

    /**
     * El rol vive ahora en la membresía; {@code users.role} es un espejo que se
     * retira en el bloque D. Mientras existan los dos, tienen que coincidir: una
     * divergencia silenciosa entre ambos sería peor que cualquiera de los dos.
     */
    @Test
    @DisplayName("users.role y company_memberships.role no divergen")
    @Transactional
    void elRolEspejoNoDiverge() {
        var creado = userService.create(dto("espejo@clinica.ec", 1L));

        var usuario = userRepository.findById(creado.getId()).orElseThrow();
        var membresia = membershipRepository.findByUserIdAndActiveTrue(creado.getId()).get(0);

        assertThat(usuario.getRole()).isEqualTo(membresia.getRole());
    }

    private CreateUserRequestDto dto(String email, Long companyId) {
        CreateUserRequestDto dto = new CreateUserRequestDto();
        dto.setFirstName("Nuevo");
        dto.setLastName("Miembro");
        dto.setEmail(email);
        dto.setRole(UserRole.ASSISTANT);
        dto.setUserType(UserType.ECUADORIAN);
        dto.setDocumentType(DocumentType.CEDULA_EC);
        dto.setDocumentNumber("1712345678");
        dto.setCompanyId(companyId);
        return dto;
    }
}
