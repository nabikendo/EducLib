# Lessons learned

- Inventory and circulation mutations must be in a single Room `withTransaction` block; a sequence of suspend DAO writes is not atomic.
- Validate loan quantity as strictly positive before touching stock.
- For tablet-first UI, keep navigation persistent and use width breakpoints instead of treating the tablet as a scaled phone.
- Do not claim build success without an actual Gradle/CI result.

- Backup de mídia não deve omitir uma foto local inacessível e ainda relatar sucesso: falhar claramente, registrar IDs esperados num índice e exigir que índice e entradas ZIP coincidam na importação.
- ZIP e extração precisam compartilhar os mesmos limites de entrada, contagem e tamanho total; validar um índice ajuda a detectar arquivo truncado/foto ausente antes de iniciar a restauração.
- Para fotos escolhidas via seletor, copiar imediatamente para armazenamento privado evita URIs temporárias que quebram depois de reiniciar o app; preservar tokens de avatar e validar o round-trip também na Room.
- Os arquivos não fazem parte da transação Room: copiar mídia antes da atualização do DB, apagar cópias parciais em erro e não apagar os arquivos antigos antes de confirmar o commit.
- Não basta confiar no instalador para recusar assinatura incompatível: comparar o certificado SHA-256 do APK candidato com a instalação e bloquear pacote/versão incompatíveis antes de encaminhar ao sistema.
- Um APK de release só deve ser publicado depois de verificar o artefato final (assinatura, package, `versionName` e `versionCode`) contra a release anterior; a configuração da keystore ou o nome da tag, isoladamente, não comprovam continuidade.
- Breakpoints calculados pela configuração de tela podem ficar obsoletos quando a janela é redimensionada ou dividida; decisões de Split View e grade devem usar a largura Compose efetivamente disponível.
- Weekly suggestions are denormalized history with no Room foreign key or delete cascade; deleting a catalog record must not erase those recommendations or make a valid backup unrestorable. Validate persisted IDs and true Room relations, not an assumed relationship the schema does not enforce.
- Pin Robolectric tests to an API level shipped by the pinned Robolectric dependency; a device target SDK being 36 does not mean that Robolectric 4.16.1 can emulate API 36.
- Um APK assinado validamente ainda pode não ser instalável como atualização: compare o certificado do APK novo com o APK público anterior e pare a publicação em caso de divergência.
- Se a keystore antiga for irrecuperável e a pessoa aprovar migração manual, não remover o gate geral: permitir a troca somente para a tag/version explicitamente aprovada, exigir confirmação de reinstalação, manter os testes e as validações de pacote/versão/assinatura, e publicar instruções para exportar ZIP, baixar o APK, desinstalar, reinstalar e restaurar.
- Tratar a nova keystore como dado crítico de continuidade: nunca versioná-la, guardar cópia segura fora do repositório e preservar os secrets para todas as releases futuras.
