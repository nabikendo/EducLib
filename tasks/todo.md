# EducLib 16.5.0 — auditoria, correções e release

## Estado remoto confirmado

- PR #4 está integrado à `main` pelo commit `56a3f7b04862e7c93765c9a1d13a9152128b4161`.
- `main` está no commit `66c283a028ad861f1c786f10f426d16cce330aff`, com `versionName = "16.5.0"` e `versionCode = 21`.
- O run `42322171208` terminou com sucesso para o commit de versão; ele ocorreu antes das correções desta auditoria, então não aprova este diff.
- A release de referência `v16.4.0` está publicada com `EducLib-16.4.0.apk` (15.017.813 bytes). Nenhuma publicação 16.5.0 está confirmada neste checklist.
- Room permanece na versão 9, igual à 16.4.0; as correções não alteram esquema nem exigem migração. Não usar fallback destrutivo.

## Correções preparadas na branch de auditoria

- [x] Tornar breakpoints das telas principais dependentes da largura Compose disponível; manter navegação tablet e safe insets; centralizar o token da transição de capas em 320 ms.
- [x] Converter os ícones residuais para Material Outlined; a seta de voltar permanece `AutoMirrored.Filled` porque o artefato Compose instalado não fornece a variante Outlined autoespelhada.
- [x] Persistir fotos selecionadas de leitores no armazenamento privado; limpar arquivos parciais se a cópia falhar; preservar avatares predefinidos no backup JSON/ZIP.
- [x] ZIP inclui índice de mídia; exportação e importação aplicam limites simétricos e falham quando fotos locais cadastradas faltam ou estão inacessíveis; restauração limpa cópias de mídia se a transação Room falhar.
- [x] Adicionar testes Robolectric para round-trip ZIP de capa/retrato, bytes, avatar, índice incompleto, foto ausente e restauração em Room em memória; adicionar testes para IDs inválidos e preservação de sugestões históricas. O primeiro CI revelou que Robolectric 4.16.1 não suporta API 36; o teste foi fixado em API 35 e aguarda novo CI.
- [x] Fortalecer validação de IDs do backup e impedir que fallback de cópia parcial sobrescreva o último backup automático JSON válido.
- [x] Atualizador valida pacote, versão/código crescente e SHA-256 do certificado contra o app instalado antes de encaminhar o APK ao instalador.
- [x] Workflow de release exige commit contido em `main`, executa testes, confere metadados do APK e continuidade do certificado em relação à release estável anterior.
- [x] README e este checklist atualizados; correções aprendidas registradas em `tasks/lessons.md`.

## Gates obrigatórios antes de anunciar publicação

- [x] PR #5 da branch `audit/16.5.0` aberto; revisar diff final e integrar somente depois de todos os checks aprovados.
- [ ] CI da branch/PR precisa compilar e passar os testes focados, inclusive `PortableMediaBackupTest`. O sandbox não tem Gradle nem Android SDK configurados; `git diff --check` e `bash -n` nos blocos dos workflows não substituem a compilação.
- [ ] Só após merge e CI verde, criar/enviar a tag `v16.5.0`.
- [ ] Acompanhar o workflow de release e confirmar em GitHub Releases o APK `EducLib-16.5.0.apk`, `versionName=16.5.0`, `versionCode=21`, assinatura APK válida e certificado SHA-256 idêntico ao APK 16.4.0.
- [ ] Teste de instalação sobre 16.4.0 com dados representativos e teste visual/funcional em tablet Android real ou emulador ainda não foram executados; a responsividade/transição foram revisadas estaticamente.

## Bloqueios e proteção de dados

- As credenciais e a verificação criptográfica da keystore são reservadas ao workflow de release; não ler nem copiar segredos para o ambiente local.
- Se o CI falhar, corrigir e repetir CI antes do tag. Se o workflow de release falhar na comparação do certificado, confirmar que `ANDROID_KEYSTORE_BASE64` corresponde à chave da release 16.4.0; não substituir a assinatura nem desinstalar o app como workaround.
- O ZIP contém registros e fotografias pessoais e não é criptografado; manter a confirmação de restauração e orientar armazenamento privado.
- Sugestões semanais são denormalizadas e não têm foreign key/cascade para livros; sua referência pode continuar após a exclusão do livro. Não apagá-las automaticamente nem rejeitar esse histórico durante o backup.

## Fontes remotas verificadas

- [Repositório EducLib](https://github.com/nabikendo/EducLib)
- [PR #4 integrado](https://github.com/nabikendo/EducLib/pull/4)
- [GitHub Actions — run 42322171208](https://github.com/nabikendo/EducLib/actions/runs/42322171208)
- [Release publicada 16.4.0 e APK de referência](https://github.com/nabikendo/EducLib/releases/tag/v16.4.0)
