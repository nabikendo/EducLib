# Preparação do EducLib para uma futura versão iOS

Este guia registra os contratos de produto e de dados que devem ser preservados quando começar o desenvolvimento para iPhone e iPad. O EducLib disponível neste repositório é um aplicativo Android nativo em Kotlin/Jetpack Compose; ainda não existe uma versão iOS nem uma camada Kotlin Multiplatform compartilhada.

## Princípios de produto

- **Sem conta obrigatória:** o acervo e a circulação funcionam localmente, sem login, Firebase ou sincronização em nuvem.
- **Backup explícito:** a cópia automática do Android está desativada; exportar ou importar o JSON é uma ação consciente da pessoa usuária.
- **Dados sob controle da biblioteca:** livros, leitores, empréstimos, reservas e estantes pertencem à escola que usa o aparelho. Toda exportação ou compartilhamento deve ser iniciado explicitamente pela pessoa usuária.
- **Privacidade por padrão:** não enviar nomes, turmas, telefones ou histórico de empréstimos a serviços remotos. Notificações e widgets exibem contagens, não nomes de leitores.
- **Acessibilidade equivalente:** respeitar Dynamic Type, VoiceOver, contraste, áreas de toque confortáveis, foco de teclado e a preferência por movimento reduzido.
- **Regras de circulação testáveis:** prazos, quantidades, devoluções parciais, atrasos e bloqueios não devem depender da interface.
- **Português brasileiro:** manter linguagem simples e consistente; a localização não deve ser codificada dentro das regras de domínio.

## Contrato portátil de backup

O backup é um JSON UTF-8 com `formatVersion: 2` e `createdAt` em milissegundos desde o Unix epoch. As coleções de v2 são `books`, `loans`, `borrowers`, `reservations`, `shelves`, `shelfPositions` e `weeklySuggestions`. Um campo opcional ou uma lista vazia não é a mesma coisa que uma seção obrigatória ausente: v2 exige todas as coleções acima.

A versão 1 não declara `formatVersion` (ou a declara como 1) e contém somente `books` e `loans`. A restauração v1 substitui livros e empréstimos, mas preserva leitores, reservas, estantes, sugestões e configurações já presentes no aparelho. A restauração v2 substitui os dados portáteis das sete coleções em uma única transação. Configurações locais, credenciais e preferências de lembrete não fazem parte do backup e não devem ser apagadas por uma restauração.

Antes de gravar, a implementação deve validar a versão, IDs repetidos, quantidades e referências entre coleções. Um arquivo inválido ou de versão futura deve ser recusado sem alterar os dados atuais. A prévia deve apresentar o conteúdo que será substituído e pedir confirmação explícita.

### Campos do domínio

Os nomes abaixo são os nomes JSON atuais. Não alterar capitalização, tipos ou significado dos campos sem incrementar `formatVersion` e implementar migração compatível.

| Coleção | Campos principais | Semântica |
|---|---|---|
| `books` | `id`, `title`, `author`, `isbn`, `genre`, `year`, `location`, `totalQuantity`, `availableQuantity`, `lastLoanDate`, `totalLoans`, `useFrequency`, `category`, `tipoObra`, `publicoAlvo`, `classificacaoJTA`, `textoJustificativa`, `posicaoEstanteId` | Quantidade disponível deve estar entre zero e a quantidade total. `photoPath` é local e serializado como `null`. |
| `loans` | `id`, `bookId`, `bookTitle`, `bookAuthor`, `borrowerName`, `borrowerClass`, `loanDate`, `dueDate`, `returnDate`, `quantity` | Datas são epoch em milissegundos; `returnDate: null` representa empréstimo ativo. O backup mantém o retrato de nome/título registrado no histórico. |
| `borrowers` | `id`, `name`, `type`, `identifier`, `phone`, `classRoom`, `blockedUntil` | `blockedUntil` é nullable e usa epoch em milissegundos. `photoUri` é local e serializado como `null`. |
| `reservations` | `id`, `bookId`, `bookTitle`, `borrowerName`, `borrowerClass`, `requestDate` | `bookId` precisa referenciar um livro incluído no mesmo backup. |
| `shelves` | `id`, `name` | IDs são usados pelas posições de estante. |
| `shelfPositions` | `id`, `shelfId`, `accessibilityLevel`, `coordinate` | `shelfId` precisa referenciar uma estante; nível de acessibilidade atual: 1 baixo, 2 médio, 3 alto. |
| `weeklySuggestions` | `id`, `bookId`, `bookTitle`, `bookAuthor`, `professorName`, `classRoom`, `recommendation`, `createdAt` | Mantém um retrato do livro e do texto da recomendação; o ID pode ficar desatualizado se o livro for removido. Não dependa dele para exibir o registro histórico. |

Caminhos locais de fotos/capas não são transferíveis entre aparelhos e são omitidos. Uma eventual exportação de imagens exige um pacote portátil separado, com consentimento explícito e política de tamanho/privacidade definida. O backup contém dados pessoais da comunidade escolar (por exemplo, nomes e telefones); a tela de compartilhamento deve avisar para guardá-lo e enviá-lo somente em local privado.

## Mapeamento recomendado

| Responsabilidade | Android atual | Opção iOS futura |
|---|---|---|
| Interface | Jetpack Compose | SwiftUI |
| Persistência | Room/SQLite | SwiftData ou SQLite, com migrações explícitas |
| Lembretes | WorkManager + notificações locais, opt-in | UserNotifications, pedindo autorização ao ativar |
| Widget | `AppWidgetProvider`, contagens de hoje e próximos sete dias | WidgetKit, as mesmas contagens agregadas via App Group |
| Importação/exportação | JSON v1/v2 e CSV do acervo | Ler e escrever o mesmo contrato JSON; CSV deve manter cabeçalho UTF-8 documentado |

A primeira versão iOS pode ser nativa. Se surgir uma necessidade real de compartilhar regras, extrair funções puras de domínio (por exemplo, resumo de vencimentos e validação de empréstimos) e criar testes de contrato multiplataforma antes de introduzir Kotlin Multiplatform. Não compartilhe código de interface nem APIs Android com o domínio.

## Regras e verificações de domínio

- Um empréstimo ativo tem `returnDate == null`; um empréstimo devolvido conserva a data real de devolução.
- Atraso não deve ser contado para empréstimos já devolvidos. A classificação deve considerar a data/hora atual e a zona local de forma consistente.
- Quantidade emprestada deve ser positiva e não pode superar os exemplares disponíveis.
- Devoluções parciais mantêm a quantidade ainda emprestada e registram a parte devolvida no histórico.
- As datas persistidas/transportadas são timestamps epoch em milissegundos; cálculos de “hoje” devem usar calendário e zona local, não divisões fixas por 24 horas.
- Notificações são opcionais, discretas e não incluem nome do leitor ou título do livro na tela bloqueada.

## Critérios de paridade antes do lançamento iOS

1. Importar backups v1 e v2 com prévia, validação e confirmação; arquivos inválidos, duplicados, com referências quebradas ou de versão futura não podem corromper a base.
2. Testar a restauração em transação com dados existentes e confirmar que configurações/credenciais locais não são substituídas.
3. Cobrir busca, filtros, empréstimo, quantidades, renovação, devolução total/parcial, vencimentos e bloqueios com testes de domínio.
4. Validar VoiceOver, Dynamic Type, contraste, áreas de toque, movimento reduzido, teclado e layouts para iPhone/iPad.
5. Implementar notificações e widget como recursos opcionais; solicitar permissões somente quando a pessoa ativar a função.
6. Testar exportação Android → iOS e iOS → Android com um conjunto de dados sintético, sem informações reais de estudantes.
7. Documentar migrações antes de mudar o esquema e manter compatibilidade de leitura com os formatos que já foram publicados.
8. Não prometer sincronização entre plataformas até existir um desenho de sincronização seguro, consentido e separado do modo local.
