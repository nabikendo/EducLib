# EducLib 16.5.0 — auditoria e migração de assinatura

## Estado confirmado

- PR #4 foi integrado à `main` pelo commit `56a3f7b`.
- PR #5, com as correções da auditoria, foi integrado por squash em `2748e43b38482b961223a7c755cf853064812451`.
- PR #6 registrou a falha da assinatura e foi integrado em `6ff028897c40ec63be46e3264d287c7defd2220e`.
- A `main` mantém `versionName = "16.5.0"`, `versionCode = 21` e Room versão 9, sem mudança de esquema.
- Os checks Android CI do código do PR #5 passaram (testes focados e build de depuração); runs [42325319715](https://github.com/nabikendo/EducLib/actions/runs/42325319715) e [42325324797](https://github.com/nabikendo/EducLib/actions/runs/42325324797).
- A tag anotada `v16.5.0` existe e aponta para `2748e43`; a página de Releases ainda não contém 16.5.0.
- O run de release [42325636987](https://github.com/nabikendo/EducLib/actions/runs/42325636987) compilou um APK assinado válido, mas detectou certificado diferente do APK 16.4.0 e barrou a publicação.
- O workflow da tag atual não contém o despacho manual que será adicionado nesta branch; não mover a tag. O fluxo de migração deve ser executado a partir da `main` e compilar explicitamente a fonte da tag existente.
- Histórico da tag 16.4.0 mostra o mesmo nome dos secrets (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_PASSWORD`) e alias `upload`. Nenhuma keystore foi encontrada no repositório, histórico Git ou sandbox.
- Em 2026-11-07, a pessoa usuária aprovou explicitamente uma migração manual para uma chave nova, ciente de que instalações 16.4.0 não poderão atualizar por cima.

## Correções auditadas no PR #5

- [x] Breakpoints principais seguem a largura Compose efetiva; navegação tablet respeita safe insets; a transição compartilhada de capa mantém 320 ms.
- [x] Ícones residuais passaram para Material Outlined e variantes direcionais AutoMirrored quando disponíveis. A seta Voltar permanece AutoMirrored Filled porque o artefato instalado não oferece sua variante Outlined.
- [x] Fotos selecionadas de leitores são copiadas ao armazenamento privado e cópias parciais são removidas em falha; avatares predefinidos permanecem no backup JSON/ZIP.
- [x] ZIP usa índice de mídia, limites simétricos, falha se foto local cadastrada não puder ser incluída, valida entrada antes de restaurar e remove cópias locais se Room falhar.
- [x] Testes Robolectric cobrem round-trip ZIP de capa/retrato/bytes, avatar, índice incompleto, mídia ausente e restauração em Room em memória; IDs inválidos e preservação de sugestões históricas são testados.
- [x] Backup automático JSON mantém a última cópia válida; atualizador valida pacote, versão/código e certificado antes do instalador.

## Migração manual de assinatura preparada nesta branch

- [ ] Validar o workflow de despacho manual e integrar a branch/PR atual. A exceção de certificado deve ser única, limitada ao tag `v16.5.0` e exigir `allow_signing_migration=true` e `confirm_manual_reinstall=true`; todos os gates de assinatura APK válida, pacote, versão e código continuam obrigatórios.
- [ ] Gerar uma keystore `JKS` nova com alias `upload`; substituir os três repository secrets pelo novo material sem expor valores no chat; conservar uma cópia de recuperação fora do repositório.
- [ ] Depois do merge, abrir **Actions → Release Android APK → Run workflow** na branch `main`, com `release_tag=v16.5.0`, `allow_signing_migration=true` e `confirm_manual_reinstall=true`. O workflow deve construir o código do tag já existente, não mover o tag.
- [ ] Confirmar testes, assinatura APK válida, diferença de certificado intencional, mesmo `applicationId`, `versionName=16.5.0`, `versionCode=21`, `versionCode` crescente e APK anexado a Release.
- [ ] Conferir que as notas da Release explicam a migração e não prometem atualização automática da 16.4.0.

## Aviso obrigatório aos usuários da 16.4.0

Uma chave nova não atualiza uma instalação da 16.4.0 por cima. O verificador interno não deve contornar a assinatura. Antes de desinstalar: exportar o backup ZIP completo e guardá-lo fora do app; baixar o APK 16.5.0; desinstalar 16.4.0; instalar o APK; abrir o app novo; restaurar o ZIP; conferir livros, leitores e fotos. A desinstalação sem backup pode apagar os dados locais. Depois desta migração, preservar a keystore nova para todas as versões futuras.

## Testes ainda não feitos

- [ ] A compilação/testes Gradle da nova branch dependem do Android CI; o sandbox não tem Gradle/SDK para uma compilação local reproduzível.
- [ ] Instalação real em aparelho, incluindo os passos de exportar/desinstalar/reinstalar/restaurar; inspeção visual/funcional em tablet Android ou emulador.

## Fontes verificadas

- [Repositório EducLib](https://github.com/nabikendo/EducLib)
- [PR #5 — correções da auditoria](https://github.com/nabikendo/EducLib/pull/5)
- [PR #6 — documentação do bloqueio](https://github.com/nabikendo/EducLib/pull/6)
- [Release 16.4.0 e APK de referência](https://github.com/nabikendo/EducLib/releases/tag/v16.4.0)
- [Workflow bloqueado por divergência — 42325636987](https://github.com/nabikendo/EducLib/actions/runs/42325636987)
