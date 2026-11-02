# EducLib — Biblioteca Escolar

Aplicativo Android para apoiar a gestão de uma biblioteca escolar, desenvolvido em Kotlin e Jetpack Compose. O app abre sem conta ou senha e guarda os registros localmente com Room, com recursos de consulta de livros e controle de empréstimos.

## Requisitos

- Android Studio com suporte a JDK 17.
- Android SDK 36.1 para compilar a configuração atual.
- Android 7.0 (API 24) ou superior para instalar o app.

Abra a pasta do repositório no Android Studio, aguarde a sincronização do Gradle e execute o app em um emulador ou dispositivo. Pela linha de comando, com Gradle instalado, use `gradle assembleDebug`.

## Uso sem conta e dados locais

Não é necessário criar conta nem lembrar uma senha para usar o EducLib. Livros, leitores e empréstimos ficam armazenados no aparelho; antes de trocar de dispositivo, use a opção de backup nas configurações para exportar os dados.

## Chave Gemini e segurança

O arquivo `.env.example` contém apenas um valor fictício. Para testes locais, crie um `.env` ignorado pelo Git e configure sua chave de desenvolvimento. Não distribua APKs compilados com uma chave real: chaves incluídas em um aplicativo Android podem ser extraídas do binário. Para disponibilizar os recursos Gemini em produção, encaminhe as chamadas por um backend que mantenha a chave fora do app. Os builds públicos deste repositório não recebem uma chave Gemini real.

## Releases e atualização pelo app

Ao abrir o app, ele consulta a release pública mais recente do GitHub no máximo uma vez a cada 12 horas. Se encontrar um APK com versão superior à instalada, mostra um aviso dentro do app. **Baixar atualização** abre o APK da release no navegador; o usuário ainda precisa confirmar o download e a instalação no Android. A instalação não é silenciosa.

As atualizações precisam preservar o mesmo `applicationId` e ser assinadas com a mesma chave privada. Não perca nem substitua a keystore de release: sem ela, o Android não instala uma versão nova por cima da anterior. Guarde uma cópia segura fora deste repositório. Keystores (`*.jks` e `*.keystore`) estão ignoradas pelo Git.

### Configurar a assinatura no GitHub Actions

Antes da primeira publicação, crie ou escolha uma keystore permanente com o alias `upload`. O projeto não contém uma keystore de release. Se já existe uma keystore usada para distribuir esta aplicação, use exatamente essa; uma chave diferente impede a atualização por cima de instalações assinadas com a chave anterior.

Cadastre os valores abaixo em **Settings → Secrets and variables → Actions → Repository secrets**. Nunca os grave no código:

- `ANDROID_KEYSTORE_BASE64`: conteúdo Base64 do arquivo `.jks`.
- `ANDROID_KEYSTORE_PASSWORD`: senha da keystore.
- `ANDROID_KEY_PASSWORD`: senha da chave `upload`.

Para gerar o Base64 sem quebras de linha com Python, execute localmente:

```bash
python3 -c "import base64; print(base64.b64encode(open('my-upload-key.jks','rb').read()).decode())"
```

### Publicar uma versão

1. Incremente `versionCode` e atualize `versionName` em `app/build.gradle.kts`.
2. Envie as alterações para `main`.
3. Crie uma tag igual à versão, com o prefixo `v`. Por exemplo, para `versionName = "16.1.0"`:

   ```bash
   git tag v16.1.0
   git push origin v16.1.0
   ```

4. O workflow `Release Android APK` compila e assina o APK e cria uma release pública no GitHub com o arquivo anexado. A tag precisa corresponder a `versionName`; o workflow falha se os segredos de assinatura não estiverem configurados.

O verificador dentro do app procura um APK anexado à release mais recente. Releases sem um arquivo `.apk` não geram aviso de atualização.

> **Migração da versão anterior:** instalações de APKs de debug ou de builds assinados com outra keystore podem não aceitar a primeira versão assinada pela nova chave. Talvez seja necessário fazer backup dos dados e reinstalar o app. Depois da primeira release assinada, mantenha a mesma keystore para todas as versões.

## Licença

Este projeto é distribuído sob a licença MIT. Consulte [`LICENSE`](LICENSE).
