/**
 * Caso de uso de matricula/conclusao: publica CursoConcluidoEvent e concede recompensas (orquestracao).
 */
@org.springframework.modulith.ApplicationModule(allowedDependencies = { "shared", "aluno", "voucher", "progressao" })
package com.example.gamificacao.grupo_7.matricula;
