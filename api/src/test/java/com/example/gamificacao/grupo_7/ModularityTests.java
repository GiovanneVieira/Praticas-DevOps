package com.example.gamificacao.grupo_7;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Evidência do mini-lab do Desafio 1: as fronteiras declaradas em cada
 * package-info (@ApplicationModule.allowedDependencies) sao verificadas em
 * build - uma dependência não declarada entre módulos falha o CI antes do
 * code review. O Documenter gera o diagrama PlantUml dos módulos
 * (antes/depois da modularização) em target/spring-modulith-docs.
 */
class ModularityTests {

    static final ApplicationModules modules = ApplicationModules.of(Application.class);

    @Test
    void fronteirasDeModulosDevemSerRespeitadas(){
        modules.verify();
    }

    @Test
    void documentarModulosComoPlantUml(){
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }

}
