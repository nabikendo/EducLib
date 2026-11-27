# EducLib — Biblioteca Escolar

O **EducLib** é um aplicativo livre (licença [GPL-3.0](LICENSE)) para organizar o acervo e os empréstimos de uma biblioteca escolar e localizar cada livro nas estantes com um mapa 3D. Existe para **Android** e para **computadores com Windows ou Linux** (inclusive Zorin OS). Foi desenvolvido em Kotlin, Jetpack Compose/Compose Multiplatform e Room. Abre sem conta ou senha obrigatória e mantém os registros no próprio aparelho.

## O que oferece

- Cadastro e consulta local de livros, leitores e empréstimos.
- Busca e filtros rápidos para encontrar livros e acompanhar devoluções.
- Backup JSON com prévia antes da restauração e leitura de backups legados v1.
- Backup portátil ZIP (`.edulib.zip`) com manifesto, índice e fotos locais de livros e leitores; restauração substitui os cadastros somente após confirmação.
- Lembretes locais de devolução, opcionais e configuráveis pela pessoa usuária.
- Widget de tela inicial com contagens de devoluções atrasadas e próximas.
- Interface em português brasileiro, com navegação lateral para tablets, Split View no acervo e transição compartilhada de capas; respeita o tamanho de fonte do sistema.
- Verificação de novas releases públicas do GitHub; a instalação sempre depende da confirmação do Android.
- **Mapa 3D da biblioteca:** mostra as estantes na planta, as prateleiras ocupadas e, ao buscar um livro, destaca a estante e a prateleira onde ele está (aba ROOF → *Mapa 3D*, ou o ícone de RA nos detalhes do livro). Arraste para girar, use pinça para aproximar e alterne entre vista 3D e planta.
- **Varredura com realidade aumentada (opcional):** em aparelhos com ARCore, como o Galaxy S24 FE, *Escanear (RA)* mede cada estante com a câmera e grava posição, largura, altura e orientação no mapa.

### Como escanear a biblioteca

1. Instale o “Google Play Services para RA” se o app pedir. Celulares sem ARCore continuam usando o mapa, com estantes em posição estimada ou medidas digitadas em *Medidas*.
2. Marque **dois pontos de referência fixos no piso** (por exemplo, os dois lados da porta). Eles definem a origem do mapa. Use sempre os mesmos pontos para que estantes medidas em dias diferentes fiquem no mesmo mapa.
3. Para cada estante: escolha-a (ou crie uma), fique de frente para ela e marque o **canto inferior esquerdo** e o **canto inferior direito**, rentes ao piso. Depois mire a **borda superior** para medir a altura, ou pule e digite. Confira profundidade, número de prateleiras e colunas e salve.
4. Para o livro aparecer na prateleira certa, cadastre as posições com *Prateleira nº* e *Coluna nº* (ou textos como “Fileira 2, Prateleira 3”) e vincule o livro à posição.

O S24 FE não tem sensor LiDAR/ToF. A profundidade vem do movimento da câmera, então a precisão costuma ficar em alguns centímetros com boa iluminação. Por isso o app mede estantes em vez de gerar uma malha 3D bruta da sala. A câmera é usada apenas no aparelho: nenhuma imagem é gravada ou enviada.

Lembretes e widget são opcionais. O EducLib continua útil sem rede, sem notificações e sem criar conta.

## Versão para computador (Windows e Linux)

A pasta [`desktop/`](desktop) contém o EducLib para computador, com acervo, leitores, empréstimos e devoluções (com as mesmas regras de prazo, devolução parcial e bloqueio por atraso do Android), estantes e o mesmo mapa 3D. A medição com realidade aumentada continua só no Android; no computador, as medidas das estantes são digitadas em *Mapa 3D → Medidas*.

**Instalação:** baixe os instaladores gerados pelo workflow *Desktop (Windows e Linux)* em Actions (artefatos `EducLib-windows` e `EducLib-linux-deb`).

- **Windows:** execute o `.msi` (ou o `.exe`). A instalação é só para o usuário atual e não pede senha de administrador.
- **Zorin OS / Ubuntu / Debian:** abra o `.deb` com o instalador de programas ou rode `sudo apt install ./educlib_1.0.0_amd64.deb`. O EducLib aparece no menu, em *Educação*.

Os instaladores trazem o próprio Java, então não é preciso instalar mais nada.

**Dados:** cada computador guarda a biblioteca em `%APPDATA%\EducLib` (Windows) ou `~/.local/share/educlib` (Linux), no arquivo `biblioteca.json`. Esse arquivo é um backup EducLib v2 válido, e a versão anterior fica em `biblioteca.json.bak`. Não há sincronização automática: para levar os dados do celular ao computador, exporte o backup JSON no Android e use *Backup → Importar backup* no computador; para o caminho inverso, use *Exportar backup* e restaure no celular. A importação substitui todos os dados do destino. Fotos de capas e leitores não vão pelo JSON.

**Desenvolvimento:** com JDK 17+ e Gradle 9, dentro de `desktop/`:

```bash
gradle run
```

```bash
gradle test
```

`gradle packageDeb` gera o `.deb` no Linux; `gradle packageMsi packageExe` gera os instaladores no Windows. O código sem dependência de Android (entidades, formato de backup, regras de empréstimo, busca e mapa 3D) fica em [`shared/`](shared) e é compilado pelos dois apps.

## Privacidade e backup

Os registros do acervo ficam no armazenamento local do aplicativo. O projeto não exige conta, não sincroniza o acervo em nuvem e não envia dados pessoais da biblioteca a serviços de IA. Há duas funções opcionais que usam internet: o verificador consulta a API pública do GitHub para procurar releases e, quando a pessoa inicia a busca de capa, o app consulta provedores de livros usando o ISBN informado. Nomes de leitores, turmas, telefones e histórico de empréstimos não são enviados por essas consultas.

A exportação de backup é uma ação explícita; o backup automático do Android está desativado. O JSON pode conter dados pessoais, como nomes, turmas e telefones, e não inclui fotos locais; sua tela de restauração mostra uma prévia das contagens. O ZIP portátil inclui as fotografias locais de livros e leitores, além dos registros; ambos os formatos devem ser guardados/compartilhados somente em local privado. Antes da restauração ZIP, o app pede confirmação e informa que o arquivo pode conter dados pessoais e fotos, sem criptografia. O ZIP valida manifesto, índice de mídia, caminhos e limites de tamanho; exportação falha explicitamente se uma foto local cadastrada estiver inacessível, em vez de omiti-la silenciosamente. Consulte [`IOS_PORTING_GUIDE.md`](IOS_PORTING_GUIDE.md) para o contrato versionado de dados e os cuidados de interoperabilidade futura.

## Requisitos de desenvolvimento

- Android Studio com suporte a JDK 17.
- Android SDK 36.1 para compilar a configuração atual.
- Android 7.0 (API 24) ou superior para instalar o app.

Abra a pasta do repositório no Android Studio, aguarde a sincronização do Gradle e execute em um emulador ou dispositivo. Pela linha de comando, com Gradle instalado, use `gradle assembleDebug`.

Para rodar os testes de unidade (backup, integridade das estantes, geometria do mapa 3D e lembretes):

```bash
gradle testDebugUnitTest
```

A varredura com RA só pode ser testada em um aparelho físico compatível com ARCore; emuladores não reproduzem a câmera e a profundidade reais.

## Releases e atualização pelo app

A versão preparada para publicação é **16.5.0** (`versionCode` 21); a versão pública só muda após a release ser criada e o APK validado. Consulte a página [Releases](https://github.com/nabikendo/EducLib/releases) para identificar o artefato efetivamente publicado.

Ao abrir, o app consulta a release pública mais recente no máximo uma vez a cada 12 horas. Se encontrar um APK com versão superior à instalada, mostra um aviso dentro do app. **Baixar atualização** baixa o APK para o cache e valida pacote, nome/código de versão e certificado SHA-256 contra o app instalado antes de encaminhá-lo ao instalador Android. No Android 8 ou superior pode ser necessário autorizar a instalação para esta origem. A pessoa ainda precisa confirmar a instalação; ela nunca é silenciosa.

### Assinatura de releases

As atualizações precisam preservar o mesmo `applicationId` e ser assinadas com a mesma chave privada. Não perca nem substitua a keystore de release: sem ela, o Android não instala uma versão nova por cima da anterior. Guarde uma cópia segura fora deste repositório. Keystores (`*.jks` e `*.keystore`) estão ignoradas pelo Git.

Para publicar pelo GitHub Actions, configure em **Settings → Secrets and variables → Actions → Repository secrets**:

- `ANDROID_KEYSTORE_BASE64`: conteúdo Base64 do arquivo `.jks`.
- `ANDROID_KEYSTORE_PASSWORD`: senha da keystore.
- `ANDROID_KEY_PASSWORD`: senha da chave `upload`.

Nunca grave esses valores no código, em issues ou em arquivos versionados.

### Publicar uma nova versão

1. Incremente `versionCode` e atualize `versionName` em `app/build.gradle.kts`.
2. Envie as alterações verificadas para `main`.
3. Crie e envie uma tag igual à versão, com prefixo `v` (exemplo: `v16.5.0`).
4. O workflow `Release Android APK` exige que o commit da tag esteja em `main`, executa os testes, compila e assina o APK e compara package, `versionName`, `versionCode` crescente e SHA-256 do certificado com a última release publicada antes de criar/anexar o artefato. A tag precisa corresponder a `versionName`; o workflow falha se os segredos de assinatura não estiverem configurados ou se qualquer verificação falhar.

O verificador de atualização procura um APK anexado à release mais recente. Releases sem arquivo `.apk` não geram aviso de atualização.

> **Migração da chave:** instalações de APKs de debug ou builds assinados com outra keystore podem não aceitar uma primeira versão assinada com uma chave diferente. Talvez seja necessário fazer backup dos dados e reinstalar o app. Depois da primeira release assinada, mantenha a mesma keystore.

## Licença e créditos

O EducLib é software livre, distribuído sob a **GNU General Public License, versão 3 ou posterior** (`GPL-3.0-or-later`). Consulte o texto completo em [`LICENSE`](LICENSE).

Em resumo, e sem substituir o texto da licença:

- você pode usar, estudar, modificar e redistribuir o EducLib, inclusive em outras escolas;
- quem distribuir o app ou uma versão modificada precisa entregar também o código-fonte correspondente, sob a mesma licença;
- o programa é fornecido sem garantia.

Versões publicadas até a 16.5.0 foram distribuídas sob a licença MIT, e quem as recebeu continua podendo usá-las nos termos da MIT. A GPL vale para o código a partir desta mudança.

As bibliotecas de terceiros mantêm as próprias licenças. AndroidX, Jetpack Compose, Room, ZXing Android Embedded, Retrofit, OkHttp, Moshi e Coil usam Apache 2.0. O SDK do ARCore e o “Google Play Services para RA” são distribuídos pelo Google sob os próprios termos.

Criado por [@nabikendo](https://github.com/nabikendo).
