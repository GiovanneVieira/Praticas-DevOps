package com.example.gamificacao.grupo_7.conquistas;


import com.example.gamificacao.grupo_7.shared.Plano;
/**
 * Catalogo de conquistas do RF02 (Desafio 1): versao em codigo, nao em banco,
 * porque a definicao de cada conquista e uma regra de negocio que muda com
 * releases; apenas a concessao por aluno e estado persistido.
 *
 * Cada constante declara o criterio (cursos concluidos com nota superior a
 * 7,0, moedas acumuladas ou plano Premium) e o alvo que desbloqueia.
 */
public enum Conquista {

    PRIMEIRA_CONCLUSAO(
            "Primeira Conclusao",
            "Conclua 1 curso com nota superior a 7,0",
            Criterio.CURSOS_CONCLUIDOS, 1),

    ESTUDANTE_DEDICADO(
            "Estudante Dedicado",
            "Conclua 5 cursos com nota superior a 7,0",
            Criterio.CURSOS_CONCLUIDOS, 5),

    MARATONISTA(
            "Maratonista",
            "Conclua 12 cursos com nota superior a 7,0",
            Criterio.CURSOS_CONCLUIDOS, 12),

    COLECIONADOR(
            "Colecionador",
            "Acumule 10 moedas na plataforma",
            Criterio.MOEDAS, 10),

    MAGNATA(
            "Magnata",
            "Acumule 25 moedas na plataforma",
            Criterio.MOEDAS, 25),

    ELITE(
            "Elite",
            "Alcance o plano Premium",
            Criterio.PLANO_PREMIUM, 1);

    public enum Criterio {
        CURSOS_CONCLUIDOS, MOEDAS, PLANO_PREMIUM
    }

    private final String titulo;
    private final String descricao;
    private final Criterio criterio;
    private final int alvo;

    Conquista(String titulo, String descricao, Criterio criterio, int alvo){
        this.titulo = titulo;
        this.descricao = descricao;
        this.criterio = criterio;
        this.alvo = alvo;
    }

    public String getTitulo(){
        return this.titulo;
    }

    public String getDescricao(){
        return this.descricao;
    }

    public int getAlvo(){
        return this.alvo;
    }

    /**
     * Avalia se o marco desta conquista foi atingido pelo estado do aluno,
     * por limite fechado (exatamente o alvo ou mais desbloqueia).
     */
    public boolean atingidaPor(int cursosConcluidos, int moedas, Plano plano){
        return switch (this.criterio) {
            case CURSOS_CONCLUIDOS -> cursosConcluidos >= this.alvo;
            case MOEDAS -> moedas >= this.alvo;
            case PLANO_PREMIUM -> plano == Plano.PREMIUM;
        };
    }

    /**
     * Progresso atual rumo ao alvo, limitado ao proprio alvo (para exibir
     * "7/12" e nao "30/12"); conquistas de plano nao tem progresso numerico.
     */
    public Integer progressoDe(int cursosConcluidos, int moedas){
        return switch (this.criterio) {
            case CURSOS_CONCLUIDOS -> Math.min(cursosConcluidos, this.alvo);
            case MOEDAS -> Math.min(moedas, this.alvo);
            case PLANO_PREMIUM -> null;
        };
    }

}
