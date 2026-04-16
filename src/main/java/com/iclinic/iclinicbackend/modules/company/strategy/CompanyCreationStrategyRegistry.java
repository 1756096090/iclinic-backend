package com.iclinic.iclinicbackend.modules.company.strategy;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registro de estrategias para crear empresas.
 * Inicializa el mapa una sola vez al arrancar el contexto (thread-safe via @PostConstruct).
 */
@Component
@RequiredArgsConstructor
public class CompanyCreationStrategyRegistry {

    private final List<CompanyCreationStrategy> strategies;
    private Map<String, CompanyCreationStrategy> strategyMap;

    @PostConstruct
    void init() {
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        CompanyCreationStrategy::getCompanyType,
                        Function.identity()
                ));
    }

    /**
     * Obtiene la estrategia correspondiente al tipo de empresa.
     *
     * @param companyType nombre del tipo en mayúsculas (e.g. "ECUADORIAN")
     */
    public CompanyCreationStrategy getStrategy(String companyType) {
        CompanyCreationStrategy strategy = strategyMap.get(companyType.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "Tipo de empresa no soportado: " + companyType +
                    ". Tipos válidos: " + strategyMap.keySet()
            );
        }
        return strategy;
    }
}
