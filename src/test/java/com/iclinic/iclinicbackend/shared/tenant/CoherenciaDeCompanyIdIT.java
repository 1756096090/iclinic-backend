package com.iclinic.iclinicbackend.shared.tenant;

import com.iclinic.iclinicbackend.modules.crm.contact.entity.CrmContact;
import com.iclinic.iclinicbackend.modules.crm.conversation.entity.Conversation;
import com.iclinic.iclinicbackend.support.AbstractPostgresIT;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El {@code company_id} denormalizado se deriva del padre en {@code @PrePersist},
 * y esa derivación <strong>no se repite en {@code @PreUpdate}</strong>. Es
 * deliberado, y este fichero existe para que siga siéndolo.
 * <p>
 * Si alguien reasigna una conversación a un contacto de otra empresa, el
 * {@code company_id} de la fila queda obsoleto. La tentación es añadir un
 * {@code @PreUpdate} que lo vuelva a derivar — y eso <em>oculta</em> el problema:
 * reasignaría el tenant de la fila en silencio, que es justo lo que no se quiere.
 * Lo correcto es que el cambio sea <em>rechazado</em>, y de eso se encargan dos
 * cosas a la vez:
 * <ul>
 *   <li>la FK compuesta {@code (company_id, contact_id) → crm_contacts(company_id, id)},
 *       porque el par no existe;</li>
 *   <li>el {@code WITH CHECK} de la política de RLS, porque la fila resultante no
 *       pertenece al tenant activo.</li>
 * </ul>
 * <p>
 * Un comportamiento que solo garantiza una FK compuesta necesita una prueba que lo
 * demuestre. Sin ella, dentro de seis meses alguien "arregla" el hueco añadiendo la
 * re-derivación y nadie nota que ha relajado la garantía.
 */
@ActiveProfiles("it")
@SpringBootTest
class CoherenciaDeCompanyIdIT extends AbstractPostgresIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    @Autowired
    private org.springframework.transaction.support.TransactionTemplate tx;

    @Test
    @DisplayName("mover una conversacion a un contacto de otra empresa se RECHAZA, no se re-deriva")
    void moverUnaConversacionDeEmpresaSeRechaza() {
        // POR JPA, no con SQL directo. Es la diferencia que hace que este test
        // sirva: @PreUpdate solo corre por el camino de Hibernate, asi que un
        // UPDATE por jdbc probaria la FK pero NO detectaria que alguien anadio la
        // re-derivacion. Se comprobo con una mutacion: con jdbc el test pasaba
        // igual con y sin ella.
        Long idConversacionDeLa1 = jdbc.queryForObject(
                "SELECT id FROM crm_conversations WHERE company_id = 1 LIMIT 1", Long.class);
        Long idContactoDeLa2 = jdbc.queryForObject(
                "SELECT id FROM crm_contacts WHERE company_id = 2 LIMIT 1", Long.class);

        assertThatThrownBy(() -> tx.executeWithoutResult(estado -> {
            Conversation conversacion = em.find(Conversation.class, idConversacionDeLa1);
            CrmContact ajeno = em.find(CrmContact.class, idContactoDeLa2);
            conversacion.setContact(ajeno);
            em.flush();
        }))
                .as("""
                        Si esto NO lanza, alguien ha anadido la re-derivacion de company_id
                        en @PreUpdate: la fila se habra movido de clinica en silencio en vez
                        de ser rechazada. Ver el javadoc de esta clase antes de "arreglarlo".""")
                // Se comprueba el nombre del constraint y no el tipo de excepcion:
                // por el EntityManager llega la de Hibernate y por JdbcTemplate la
                // traducida de Spring. Lo que importa es QUE constraint lo rechaza.
                .hasMessageContaining("fk_cv_contact");

        Long empresaDespues = jdbc.queryForObject(
                "SELECT company_id FROM crm_conversations WHERE id = ?", Long.class, idConversacionDeLa1);
        assertThat(empresaDespues)
                .as("la fila no se movio de empresa a escondidas")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("cambiar a mano el company_id de una fila tambien se RECHAZA")
    void cambiarElCompanyIdAManoSeRechaza() {
        Long conversacionDeLa1 = jdbc.queryForObject(
                "SELECT id FROM crm_conversations WHERE company_id = 1 LIMIT 1", Long.class);

        // Sin FK compuesta esto pasaria y la fila quedaria en la otra clinica.
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE crm_conversations SET company_id = 2 WHERE id = ?", conversacionDeLa1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("al insertar, el company_id se deriva del padre y coincide con el")
    void alInsertarSeDerivaDelPadre() {
        // Lo que SI hace @PrePersist. Se comprueba sobre los datos del seed, que
        // pasaron por el relleno de V11.
        Integer incoherentes = jdbc.queryForObject("""
                SELECT count(*) FROM crm_conversations cv
                  JOIN crm_contacts c ON c.id = cv.contact_id
                 WHERE cv.company_id <> c.company_id
                """, Integer.class);
        assertThat(incoherentes).isZero();

        Integer mensajesIncoherentes = jdbc.queryForObject("""
                SELECT count(*) FROM crm_messages m
                  JOIN crm_conversations cv ON cv.id = m.conversation_id
                 WHERE m.company_id <> cv.company_id
                """, Integer.class);
        assertThat(mensajesIncoherentes).isZero();
    }

    /**
     * {@code CrmMessage} es la excepción al comentario general sobre padres
     * perezosos: deriva de {@code conversation.getCompanyId()}, que <em>no</em> es
     * el identificador, así que la llamada inicializa el proxy con una consulta
     * extra. En los demás se usa {@code padre.getCompany().getId()}, y llamar al
     * getter del identificador sobre un proxy no lo inicializa.
     * <p>
     * No es un defecto —una consulta más al insertar un mensaje— pero conviene
     * saberlo antes de meter esto en un bucle de importación masiva.
     */
    @Test
    @DisplayName("un mensaje hereda la empresa de su conversacion")
    void unMensajeHeredaLaEmpresaDeSuConversacion() {
        Integer sinHeredar = jdbc.queryForObject("""
                SELECT count(*) FROM crm_messages m
                  JOIN crm_conversations cv ON cv.id = m.conversation_id
                 WHERE m.company_id IS DISTINCT FROM cv.company_id
                """, Integer.class);
        assertThat(sinHeredar).isZero();
    }
}
