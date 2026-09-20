package com.iclinic.iclinicbackend;

import com.iclinic.iclinicbackend.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("it")
@SpringBootTest
class IclinicBackendApplicationIT extends AbstractPostgresIT {

    @Test
    void contextLoads() {
    }

}
