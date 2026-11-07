# EducLib — Biblioteca Escolar

O **EducLib** é um aplicativo Android para organizar o acervo e os empréstimos de uma biblioteca escolar. Foi desenvolvido em Kotlin, Jetpack Compose e Room. Abre sem conta ou senha obrigatória e mantém os registros no aparelho.

## O que oferece

- Cadastro e consulta local de livros, leitores e empréstimos.
- Busca e filtros rápidos para encontrar livros e acompanhar devoluções.
- Backup JSON com prévia antes da restauração e leitura de backups legados v1.
- Backup portátil ZIP (`.edulib.zip`) com manifesto, índice e fotos locais de livros e leitores; restauração substitui os cadastros somente após confirmação.
- Lembretes locais de devolução, opcionais e configuráveis pela pessoa usuária.
- Widget de tela inicial com contagens de devoluções atrasadas e próximas.
- Interface em português brasileiro, com navegação lateral para tablets, Split View no acervo e transição compartilhada de capas; respeita o tamanho de fonte do sistema.
- Verificação de novas releases públicas do GitHub; a instalação sempre depende da confirmação do Android.

Lembretes e widget são opcionais. O EducLib continua útil sem rede, sem notificações e sem criar conta.

## Privacidade e backup

Os registros do acervo ficam no armazenamento local do aplicativo. O projeto não exige conta, não sincroniza o acervo em nuvem e não envia dados pessoais da biblioteca a serviços de IA. Há duas funções opcionais que usam internet: o verificador consulta a API pública do GitHub para procurar releases e, quando a pessoa inicia a busca de capa, o app consulta provedores de livros usando o ISBN informado. Nomes de leitores, turmas, telefones e histórico de empréstimos não são enviados por essas consultas.

A exportação de backup é uma ação explícita; o backup automático do Android está desativado. O JSON pode conter dados pessoais, como nomes, turmas e telefones, e não inclui fotos locais; sua tela de restauração mostra uma prévia das contagens. O ZIP portátil inclui as fotografias locais de livros e leitores, além dos registros; ambos os formatos devem ser guardados/compartilhados somente em local privado. Antes da restauração ZIP, o app pede confirmação e informa que o arquivo pode conter dados pessoais e fotos, sem criptografia. O ZIP valida manifesto, índice de mídia, caminhos e limites de tamanho; exportação falha explicitamente se uma foto local cadastrada estiver inacessível, em vez de omiti-la silenciosamente. Consulte [`IOS_PORTING_GUIDE.md`](IOS_PORTING_GUIDE.md) para o contrato versionado de dados e os cuidados de interoperabilidade futura.

## Requisitos de desenvolvimento

- Android Studio com suporte a JDK 17.
- Android SDK 36.1 para compilar a configuração atual.
- Android 7.0 (API 24) ou superior para instalar o app.

Abra a pasta do repositório no Android Studio, aguarde a sincronização do Gradle e execute em um emulador ou dispositivo. Pela linha de comando, com Gradle instalado, use `gradle assembleDebug`.

## Releases e atualização pelo app

A versão preparada para publicação é **16.5.0** (`versionCode` 21); a versão pública só muda após a release ser criada e o APK validado. Consulte a página [Releases](https://github.com/nabikendo/EducLib/releases) para identificar o artefato efetivamente publicado.

Ao abrir, o app consulta a release pública mais recente no máximo uma vez a cada 12 horas. Se encontrar um APK com versão superior à instalada, mostra um aviso dentro do app. **Baixar atualização** baixa o APK para o cache e valida pacote, nome/código de versão e certificado SHA-256 contra o app instalado antes de encaminhá-lo ao instalador Android. No Android 8 ou superior pode ser necessário autorizar a instalação para esta origem. A pessoa ainda precisa confirmar a instalação; ela nunca é silenciosa.

### Assinatura de releases

As atualizações precisam preservar o mesmo `applicationId` e ser assinadas com a mesma chave privada. Não perca nem substitua a keystore de release: sem ela, o Android não instala uma versão nova por cima da anterior. Guarde uma cópia segura fora deste repositório. Keystores (`*.jks` e `*.keystore`) estão ignoradas pelo Git.

#### Migração única 16.4.0 → 16.5.0

A keystore que assinou a 16.4.0 não foi localizada. Portanto, a 16.5.0 será assinada com uma nova chave e **não pode ser instalada por cima** da 16.4.0. O verificador interno também não instalará um APK com assinatura diferente. Antes de remover a versão antiga, faça a migração manual abaixo:

1. Na 16.4.0, exporte um backup portátil completo (`.edulib.zip`) e confirme que o arquivo foi salvo fora do armazenamento do app.
2. Baixe o APK 16.5.0 na página GitHub Releases antes de desinstalar a versão antiga.
3. Desinstale o EducLib 16.4.0 e instale o APK 16.5.0 baixado. O Android pode pedir autorização para instalar APKs desta origem.
4. Abra o app novo, restaure o ZIP e confira livros, leitores e fotografias. Guarde o ZIP até terminar a verificação.

Sem exportar o ZIP antes da desinstalação, os dados locais podem ser perdidos. Depois de instalada a 16.5.0, as versões futuras deverão usar a nova keystore guardada nos secrets do GitHub.

Para publicar pelo GitHub Actions, configure em **Settings → Secrets and variables → Actions → Repository secrets**:

- `ANDROID_KEYSTORE_BASE64`: conteúdo Base64 do arquivo `.jks`.
- `ANDROID_KEYSTORE_PASSWORD`: senha da keystore.
- `ANDROID_KEY_PASSWORD`: senha da chave `upload`.

Nunca grave esses valores no código, em issues ou em arquivos versionados.

### Publicar uma nova versão

1. Incremente `versionCode` e atualize `versionName` em `app/build.gradle.kts`.
2. Envie as alterações verificadas para `main`.
3. Crie e envie uma tag igual à versão, com prefixo `v` (exemplo: `v16.5.0`).
4. Para versões normais, o workflow `Release Android APK` exige que o commit da tag esteja em `main`, executa os testes, compila e assina o APK e compara package, `versionName`, `versionCode` crescente e SHA-256 do certificado com a última release publicada antes de criar/anexar o artefato. A tag precisa corresponder a `versionName`; qualquer falha interrompe a publicação.
5. A migração já aprovada da 16.5.0 é excepcional: após integrar a alteração do workflow e guardar a nova keystore, execute **Run workflow** na branch `main` com `release_tag=v16.5.0`, `allow_signing_migration=true` e `confirm_manual_reinstall=true`. Esse caminho especial só permite a mudança de certificado para `v16.5.0` e inclui as instruções de backup/reinstalação nas notas da release. Não use essa opção para releases futuras.

O verificador de atualização procura um APK anexado à release mais recente. Releases sem arquivo `.apk` não geram aviso de atualização.

> **Importante:** a migração de assinatura 16.4.0 → 16.5.0 requer exportar o backup ZIP, baixar o APK, desinstalar a versão antiga e reinstalar/restaurar manualmente. Não publique uma versão com certificado diferente sem aviso explícito e backup dos usuários.

## Licença e créditos

Este projeto é distribuído sob a licença MIT. Consulte [`LICENSE`](LICENSE). Criado por [@nabikendo](https://github.com/nabikendo).
