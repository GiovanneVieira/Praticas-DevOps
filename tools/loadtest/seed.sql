-- ============================================================================
-- Seed de carga do Desafio 1 (baseline vs pos-modulith)
-- Gera 10.000 alunos com distribuicao enviesada (maioria com pouco progresso,
-- cauda com maratonistas/premium), espelhando as regras de negocio da AC1:
--   - moedas = 3 a cada 12 cursos concluidos (PoliticaProgressaoPadrao)
--   - plano PREMIUM a partir de 12 cursos concluidos
-- IDs deterministicos (0000a000-0000-4000-8000-<seq em 12 digitos>) para que
-- os scripts k6 enderecem alunos pelo indice sem consulta previa.
-- OBS: password e placeholder (carga nao faz login; endpoints sao permitAll).
-- ============================================================================

-- Idempotente: re-execucoes (accidentais ou manuais) recomecam do zero.
TRUNCATE alunos, matriculas, cursos, vouchers, ranking_entries, conquistas_concedidas, event_publication;

SELECT setseed(0.42);

WITH progresso AS (
    SELECT
        s,
        floor(power(random(), 2) * 40)::int AS cursos
    FROM generate_series(1, 10000) AS s
)
INSERT INTO alunos (id, name, email, password, ra, plano, moedas, cursos_concluidos, created_at, updated_at)
SELECT
    ('0000a000-0000-4000-8000-' || lpad(s::text, 12, '0'))::uuid,
    'Aluno Carga ' || s::text,
    'aluno.carga.' || s::text || '@gamificacao.local',
    'senha-nao-usada-em-carga',
    lpad(s::text, 6, '0'),
    CASE WHEN cursos >= 12 THEN 'PREMIUM' ELSE 'BASICO' END,
    3 * (cursos / 12),
    cursos,
    now() - (s % 365) * interval '1 day',
    now()
FROM progresso;

-- Read model do ranking: apenas alunos que concluíram ao menos 1 curso
-- (mesma semantica do listener: entra no ranking quem tem evento de conclusao).
INSERT INTO ranking_entries (id, aluno_id, nome_aluno, moedas, cursos_concluidos, atualizado_em)
SELECT gen_random_uuid(), id, name, moedas, cursos_concluidos, now()
FROM alunos
WHERE cursos_concluidos > 0;

ANALYZE alunos;
ANALYZE ranking_entries;
