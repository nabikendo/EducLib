# Lessons learned

- Inventory and circulation mutations must be in a single Room `withTransaction` block; a sequence of suspend DAO writes is not atomic.
- Validate loan quantity as strictly positive before touching stock.
- For tablet-first UI, keep navigation persistent and use width breakpoints instead of treating the tablet as a scaled phone.
- Do not claim build success without an actual Gradle/CI result.
