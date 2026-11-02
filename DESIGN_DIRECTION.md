# EducLib — direção de redesign

## Direção visual

**Biblioteca viva:** uma identidade editorial, acolhedora e funcional. Papel claro, verde-floresta, terracota em pequenos acentos e títulos serifados criam personalidade sem sacrificar contraste ou leitura. A interface deve priorizar tarefas reais de balcão e descoberta, não dashboards decorativos.

## O que melhorar pelos cinco critérios

1. **Inovação:** transformar sinais que o app já possui — circulação, disponibilidade, reservas e recomendações docentes — em próximas ações compreensíveis. Indicadores como JTA/ROOF precisam explicar o que medem, sua origem e seus limites; não devem ser tratados como uma nota automática sobre estudantes ou livros.
2. **Engenhosidade e polimento:** unificar a marca como EducLib, reduzir ruído no catálogo, colocar busca e leitura de código antes de métricas secundárias e usar uma escala consistente de cores, tipografia, espaçamento e movimento.
3. **Inclusividade:** testar TalkBack, escala de fonte, contraste, foco, navegação por teclado e alvos de toque. Trocar rótulos ambíguos e textos minúsculos, dar nome acessível a ações e preparar as strings para tradução. Recomendações devem poder representar leitores, níveis de leitura e repertórios diversos sem inferir dados sensíveis.
4. **Diversão e prazer:** dar destaque a descobertas e recomendações com contexto — quem sugeriu, para qual turma e por quê — e celebrar ações úteis com feedback discreto. Não gamificar atrasos nem expor rankings individuais de estudantes.
5. **Impacto social:** preservar o funcionamento offline e o controle local dos dados, tornar a circulação acolhedora e não punitiva, proteger informações de menores e favorecer acesso equitativo ao acervo com métricas agregadas e explicáveis.

## Etapas recomendadas

- **Etapa 1 — fundação visual Android:** tokens e tipografia coerentes, marca única, catálogo com hierarquia mais clara, busca/scan em primeiro plano e revisão de textos de métricas. Sem mudanças em Room, migrações ou regras de circulação.
- **Etapa 2 — polimento por fluxo:** redesenhar empréstimos, leitores, reservas e configurações um a um; acrescentar estados vazio, carregando e erro; validar aparelhos pequenos, tablets, texto ampliado e TalkBack.
- **Etapa 3 — confiabilidade:** criar testes de caracterização para empréstimo, renovação, devolução parcial, reserva, backup e migrações antes de alterar domínio ou armazenamento. Documentar precisamente o conteúdo restaurado por backup.
- **Etapa 4 — iOS:** manter a experiência nativa em SwiftUI e extrair primeiro as regras puras do domínio (empréstimo, prazos, busca e indicadores), com contratos e testes compartilhados. O app Android Jetpack Compose não é um APK que possa ser executado no iOS; uma camada compartilhada pode ser decidida depois de estabilizar o domínio.

Uma reformulação pode elevar a qualidade e aproximar o produto dos critérios citados, mas não garante indicação ou premiação do Apple Design Awards.
