# Changelog

Todas las versiones siguen [SemVer](https://semver.org/lang/es/) en fase `0.x`:
- `0.MENOR.PARCHE` — `MENOR` sube con `feat:`, `PARCHE` con `fix:`.

## [Sin publicar]
### Añadido
- Formulario de gasto v6: chips de categoría/cuenta, date chips, línea de impacto, teclado fijo
- Chips de categoría (FlowRow, 5 + "Más"), chips de cuenta (2-3 con saldo)
- Date chips (Hoy/Ayer/Elegir), línea de impacto presupuesto, extras (+ Nota / Seguir añadiendo)
- Teclado fijo abajo con botón Guardar (ocupa 3 filas), haptics en teclas

### Cambiado
- Versión leída desde `gradle.properties` (`VERSION_NAME=0.9.0` → `versionCode=900`)

### Eliminado
- Lecturas duplicadas de `ANIMATOR_DURATION_SCALE` (unificado en `LocalMotion`)

---

## [0.9.0] — 2026-10-26
Commit: `b12781d` (base)

### Añadido
- Formulario de gasto v6: chips de categoría (FlowRow, 5 + "Más"), chips de cuenta (2-3 con saldo)
- Date chips (Hoy/Ayer/Elegir), línea de impacto presupuesto, extras (+ Nota / Seguir añadiendo)
- Teclado fijo abajo con botón Guardar (ocupa 3 filas), haptics en teclas

### Cambiado
- Versión leída desde `gradle.properties` (`VERSION_NAME=0.9.0` → `versionCode=900`)

---

## [0.8.0] — 2026-10-25
Commit: `221e2da`

### Añadido
- Mejoras en Inicio: hero con próxima clase, corrección de zona horaria en selector de fechas
- Animaciones unificadas bajo `LocalMotion`/`MotionTokens`
- Transiciones por tipo de pantalla (raíz: fundido; formularios: slide; detalles: eje horizontal)
- Dock con indicador deslizante, hojas de creación escalonadas, teclado de monto con escala 0.94
- Vibración táctil (`Haptics.confirm`/`reject`/`tick`)

### Corregido
- Zona horaria en `DatePicker` (conversión UTC consistente)

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