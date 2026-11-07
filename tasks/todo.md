# EducLib 16.5.0 — auditoria e estado da publicação

## Estado confirmado em 2026-11-06 22:49 -03:00

- PR #4 permanece integrado à `main` pelo commit `56a3f7b`.
- PR #5, com correções desta auditoria, foi integrado por squash em `2748e43b38482b961223a7c755cf853064812451`.
- `main` define `versionName = "16.5.0"` e `versionCode = 21`; Room continua na versão 9, sem mudança de esquema.
- Os checks Android CI de `push` e `pull_request` para o commit `8134abd` passaram; ambos concluíram `Run focused unit tests` e `Build Debug APK`.
- A tag anotada `v16.5.0` foi enviada e aponta para o commit de merge `2748e43`.
- O run de release `42325636987` passou testes, validação de secrets e build do APK assinado; validou criptograficamente os APKs 16.4.0 e 16.5.0, mas falhou porque os certificados não coincidem.
- O gate falhou antes da verificação `aapt` dos metadados do APK e antes de criar a release. A API confirma `release not found` para `v16.5.0`; o único APK público continua sendo `EducLib-16.4.0.apk`.

## Correções realizadas e verificadas no CI

- [x] Layouts principais adaptam-se à largura Compose disponível; navegação de tablet respeita safe insets e a transição compartilhada das capas conserva 320 ms.
- [x] Ícones residuais passaram para Material Outlined e as variantes direcionais disponíveis foram trocadas por AutoMirrored. A seta Voltar permanece AutoMirrored Filled porque não há variante Outlined autoespelhada no artefato instalado.
- [x] Fotos selecionadas de leitores são copiadas ao armazenamento privado; falhas de cópia removem arquivos parciais. Avatares predefinidos persistem em backup JSON/ZIP.
- [x] ZIP contém índice de mídia; exportação/importação usam limites simétricos, detectam fotos ausentes e a restauração remove cópias locais se Room falhar.
- [x] Testes Robolectric cobrem round-trip ZIP de capa/retrato e bytes, avatar, índice incompleto, mídia local ausente e restauração transacional em Room em memória; IDs inválidos são rejeitados e sugestões históricas sem FK são preservadas.
- [x] O backup automático JSON não sobrescreve a última cópia válida por meio de fallback não atômico.
- [x] O atualizador interno valida pacote/versão/código e compara o certificado SHA-256 antes de abrir o instalador.
- [x] Workflow de release exige commit em `main`, testes, secrets, assinatura válida, metadados e continuidade do certificado antes de publicar.
- [x] README e lições técnicas atualizados.

## Bloqueio atual e retomada segura

A keystore usada pelo workflow gera um APK válido, mas o certificado de assinatura é diferente daquele do APK publicado 16.4.0. O passo `Create GitHub release and attach APK` foi ignorado. **Não publicar o artefato atual, não ignorar esse gate e não trocar o `applicationId` como workaround.**

1. Em GitHub → **Settings → Secrets and variables → Actions → Repository secrets**, substitua `ANDROID_KEYSTORE_BASE64` pela keystore privada exata que corresponde ao certificado do APK 16.4.0.
2. Atualize `ANDROID_KEYSTORE_PASSWORD` e `ANDROID_KEY_PASSWORD` para as credenciais dessa keystore/chave (alias `upload`). Não compartilhe a keystore nem senhas em issues ou no chat.
3. Reexecute o workflow falho `42325636987` para a tag existente `v16.5.0`; não é necessário criar outra tag.
4. Só considerar publicada quando o rerun passar a comparação de assinatura e metadados, criar a release, e a página de Releases mostrar `EducLib-16.5.0.apk`. Se a keystore 16.4.0 não estiver disponível, a atualização sobre instalações existentes está bloqueada e requer recuperar a chave ou aprovar um plano explícito de migração; não há correção segura por código.

## Ainda pendente depois de resolver a chave

- [ ] Rerun verde do release; confirmar `versionName=16.5.0`, `versionCode=21`, pacote esperado e certificado SHA-256 idêntico ao APK 16.4.0.
- [ ] Verificar APK disponível na release pública `v16.5.0` e confirmar que o instalador aceita atualização sobre 16.4.0.
- [ ] Teste de instalação real e inspeção visual/funcional em tablet Android/emulador ainda não foram executados; a responsividade e a transição foram revisadas estaticamente.

## Fontes verificadas

- [Repositório EducLib](https://github.com/nabikendo/EducLib)
- [PR #4 integrado](https://github.com/nabikendo/EducLib/pull/4)
- [PR #5 e correções integradas](https://github.com/nabikendo/EducLib/pull/5)
- [CI verde por push — 42325319715](https://github.com/nabikendo/EducLib/actions/runs/42325319715)
- [CI verde por PR — 42325324797](https://github.com/nabikendo/EducLib/actions/runs/42325324797)
- [Release 16.4.0 e APK de referência](https://github.com/nabikendo/EducLib/releases/tag/v16.4.0)
- [Workflow de release bloqueado — 42325636987](https://github.com/nabikendo/EducLib/actions/runs/42325636987)
