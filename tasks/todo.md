# EducLib — iPad-inspired tablet UI

- [x] Design tablet landscape mockups in Figma.
- [x] Add editorial pastel palette and motion tokens.
- [x] Adapt library welcome header for wide tablets without breaking phone layout.
- [x] Replace tablet sidebar placeholder icons with consistent outlined Material vector icons.
- [ ] Audit and unify iconography across the remaining screens.
- [x] Implement responsive dark sidebar for tablets (>= 600 dp width).
- [x] Implement animated master/detail split view for wide landscape tablets; full history remains available in existing dialog.
- [ ] Add true shared-element transitions between book covers and details (panel slide/fade implemented).
- [x] Add CI triggers for feature branch and pull requests.
- [x] Android CI build and focused unit tests succeeded at commit 4bdd28b3.
- [ ] Re-run CI for the latest iconography commit and capture tablet screenshots.
- [ ] Review dark mode, accessibility, reduced motion and keyboard navigation.

Implementation is isolated on branch `design/ipados-tablet-editorial`; not yet merged.

## Audit findings

- [x] Loan creation, return and deletion previously used multiple DB writes without an enclosing transaction; wrapped in Room transactions.
- [x] Loan creation previously accepted quantity <= 0; rejected.
- [ ] Check restore of backup images: portable backup explicitly drops book and borrower photo paths.
- [ ] Check dialog overflow and screen-reader navigation on landscape tablets.
- [ ] Confirm Android 36.1 SDK availability in CI environment.

## Update distribution

- [x] In-app APK download from stable GitHub Release, Android installer handoff and next-day retry.
- [x] Validate APK package name before opening installer.
- [ ] Verify installer flow on physical tablet, APK signing continuity and stable release secrets.

## Release status

- Stable release is **not** published. Main is unchanged.
- Publishing requires a passing build for the final commit, signing continuity, and verification of the update installer on a real Android tablet.

## Escopo de 06/11 — capas, ícones e backups

- [x] Adicionar transição compartilhada da capa no Split View com SharedTransitionLayout, duração de 320 ms.
- [x] Animar a capa no Figma (frame 22:78) com translação e escala de 320 ms.
- [x] Converter os ícones das telas principais, diálogos e configurações para Material Outlined.
- [x] Criar backup portátil ZIP com manifesto JSON v2 e fotos locais de livros/leitores.
- [x] Criar exportação e restauração ZIP com confirmação explícita antes da substituição dos dados.
- [x] Limitar tamanho de entradas e validar caminhos/IDs para reduzir riscos de ZIP malformado.
- [ ] Confirmar compilação e testes do commit final no GitHub Actions.
- [ ] Testar animação de capa, restauração e exportação em tablet físico.
- [ ] Criar testes automatizados de ida e volta do ZIP e interrupções de restauração.

Observações: o backup automático e a exportação JSON continuam sem imagens para manter compatibilidade; a opção ZIP é o backup multimídia completo. Fotos remotas por URL não são incorporadas ao ZIP. O arquivo de restauração substitui os cadastros atuais após confirmação. A versão estável permanece não publicada.
