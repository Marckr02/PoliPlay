# Changelog

Todas las versiones siguen [SemVer](https://semver.org/lang/es/) en fase `0.x`:
- `0.MENOR.PARCHE` — `MENOR` sube con `feat:`, `PARCHE` con `fix:`.

## [0.8.1] — 2026-10-26
Commit: ver etiqueta `v0.8.1`

### Añadido
- "Seguir añadiendo" en el formulario de gasto: tras guardar, el formulario sigue abierto, limpia el monto y conserva categoría, cuenta y fecha; permite deshacer el último guardado.
- Aviso de novedades al arrancar la app, una sola vez tras actualizar; en una instalación nueva no se muestra.
- Acerca de muestra la versión, el código, el commit y la fecha de compilación reales (`BuildConfig.GIT_SHA`/`BUILD_DATE`).
- La bienvenida guarda las preferencias de recordatorios y alarmas exactas, y continúa a la importación del PDF elegido al terminar.

### Cambiado
- Versión en un único archivo versionado, `version.properties` (0.8.1 → versionCode 801), leído por `app/build.gradle.kts`.
- Notas de versión en texto plano mediante `release_notes_current`; el historial completo queda en este archivo.
- El aviso automático de novedades vive en Inicio, no en Ajustes; la fila "Novedades" solo abre el diálogo.
- Se oculta la fila "Licencias de código abierto" hasta que exista la pantalla real.

### Corregido
- El domingo ya se puede elegir y guardar en tareas, movimientos y pagos con `CampusDatePickerDialog` (ancho fijo y conversión UTC segura).
- Se eliminaron de este CHANGELOG las funciones del formulario v6 que no están implementadas.

---

## [0.8.0] — 2026-10-25
Commit: `221e2da`

### Añadido
- Mejoras en Inicio: hero con próxima clase.
- Animaciones unificadas bajo `LocalMotion`/`MotionTokens`.
- Vibración táctil (`Haptics.confirm`/`reject`/`tick`).

### Corregido
- Zona horaria en `DatePicker` (conversión UTC consistente).

---

## [0.7.0] — 2026-10-24
Commit: `b6c4716`

### Añadido
- v5 Fase 3: Insights del ritmo (por día, cierre estimado) con tests unitarios
- Modo privado global (`hideAmounts` + `LocalPrivacyMode` + `MoneyText`)
- `SummaryScreen` con gráfico Canvas (esperado, gastado, proyección, marca "Hoy")
- Accesibilidad: `contentDescription` textual del gráfico, `liveRegion` en ritmo

### Cambiado
- `FinanceHomeScreen` usa `LazyColumn` único con pestañas fijas y grupos por día
- Scroll por pestaña guardado/restaurado (`rememberSaveable`)

---

## [0.6.0] — 2026-10-24
Commit: `6a6bbf1`

### Añadido
- v5 Fases 1 y 2: `LocalMotion`, `SnackbarHost`, `ShimmerBox`, `EmptyState`, vibración
- Finanzas lazy: `LazyListScope` por pestaña, grupos por día, ventana de mes
- Pestañas fijas (`stickyHeader`), posición de scroll por pestaña

---

## [0.5.0] — 2026-10-21
Commit: `10ab6f2`

### Añadido
- v4: Dock flotante, `ContextualActionButton`, hero del ritmo
- `PillSegmentedControl`, `BentoCard`, sistema de diseño extendido

---

## [0.4.1] — 2026-10-21
Commit: `b29e1d4`

### Corregido
- Presupuesto mensual en Inicio y ritmo en Finanzas (cálculo días restantes)

---

## [0.4.0] — 2026-10-21
Commit: `0d5fc73`

### Añadido
- Rediseño UI v2: sistema de diseño, Finanzas y accesibilidad
- Componentes base: `CampusButton`, `CampusChip`, `CampusDialog`, `CampusTextField`, etc.

---

## [0.3.0] — 2026-10-20
Commit: `42aaf1c`

### Añadido
- Rediseño bento con dock flotante e ícono CampusPocket

---

## [0.2.0] — 2026-10-20
Commit: `d0e1d46`

### Añadido
- Fases 7 y 8: rediseño, seguridad (biometría), retiro y presupuesto

---

## [0.1.0] — 2026-10-04
Commit: `b6bce2d`

### Añadido
- Primera versión PoliPlay (Campus Pocket): módulos académico, finanzas y notas
- Room, DataStore, Hilt, Compose, Navigation, WorkManager
- Migraciones v1→v2→v3→v4→v5 con tests