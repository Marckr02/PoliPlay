# Changelog

Todas las versiones siguen [SemVer](https://semver.org/lang/es/) en fase `0.x`:
- `0.MENOR.PARCHE` - `MENOR` sube con `feat:`, `PARCHE` con `fix:`.

## [Sin publicar]

Ola 3 del Bloque D.

### Añadido
- Calificaciones por materia (sistema EPN): Nota 1 y Nota 2 (sobre 20), suma, promedio, estado (Aprobado directo ≥28, Supletorio 18-27, Reprobado <18), examen supletorio (sobre 40) y simulador "¿cuánto necesito?" con fórmula exacta.
- Color por materia coherente en toda la app: Hoy, Semana, Tareas (barra lateral) y Notas (si vinculadas a materia).

### Pruebas
- 206 pruebas unitarias, 56 instrumentadas y `lintDebug` en verde.

## [0.11.0] - 2026-10-27

### Añadido
- Línea de tiempo "Tu día" en Inicio: clases de hoy, tareas pendientes que vencen hoy y pagos programados activos con vencimiento hoy. Los pagos sin hora se agrupan como "Todo el día".
- Atajo "Repetir último gasto" en el icono de la app: abre el formulario de gasto con monto, nota, cuenta y categoría del último gasto guardado. Solo aparece cuando existe al menos un gasto.
- Atajo "Nota rápida" en el icono de la app.
- Exportación del calendario a `.ics` desde Ajustes: horario semanal hasta el fin del semestre activo y entregas pendientes de hoy en adelante como eventos de 30 minutos.

### Corregido
- El atajo "Nueva nota" y el texto compartido abren el editor de nota en lugar de una ruta inválida.

### Pruebas
- 197 pruebas unitarias, 56 instrumentadas y `lintDebug` en verde.

## [0.10.0] - 2026-10-26
Commit: ver etiqueta `v0.10.0`

Ola 1 del Bloque D.

### Añadido
- Confirmación "¿Descartar cambios?" al volver de los formularios de cuenta, categoría, materia, tarea, pago, movimiento y retiro cuando hay datos sin guardar.
- "Deshacer" al eliminar notas, movimientos y pagos programados desde el aviso inferior; ya no hay diálogo de confirmación para esos tres borrados.
- Estados vacíos ilustrados en Movimientos, Notas, Pagos y Materias, con acción directa cuando corresponde.

### Cambiado
- Los textos usan cifras tabulares: montos, fechas y contadores conservan su ancho al cambiar de valor.
- La tarjeta de próxima clase de Inicio lleva borde `inkLine` para distinguirse del fondo en tema oscuro.

### Pruebas
- 189 pruebas unitarias, 54 instrumentadas y `lintDebug` en verde.

## [0.9.2] - 2026-10-26
Commit: ver etiqueta `v0.9.2`

### Añadido
- Búsqueda global como pantalla secundaria: campo enfocado, filtros Todo / Tareas / Notas / Movimientos / Materias, resultados agrupados con coincidencias resaltadas, búsquedas recientes y navegación directa al elemento.
- Transiciones por tipo de pantalla: fundido entre pestañas, formularios y modales desde abajo, detalles con eje horizontal; respetan la preferencia de movimiento reducido.
- El selector de fecha se muestra en español aunque el dispositivo esté configurado en otro idioma.

### Cambiado
- Release con R8 activo (minificación y reducción de recursos) y reglas para kotlinx.serialization y PDFBox; verificado en emulador.
- El dock vuelve a ocupar solo su franja inferior; la altura mínima introducida en 0.9.1 lo expandía hasta la pantalla completa.
- El onboarding permite omitir la importación del horario con "Lo haré después" sin tener que buscar el botón final.
- README documenta los atajos del icono, el icono monocromático, R8 y las cifras actuales de pruebas.
- `.opencode/` deja de versionarse (las habilidades siguen en disco).
- Versión 0.9.2 (versionCode 902).

### Corregido
- El selector de fecha usa la configuración local de Material3 en lugar de cambiar el idioma global del proceso.
- Las pruebas de tareas, la tarjeta de presupuesto de Inicio y el selector de fecha reflejan los textos y la semántica actuales.

---

## [0.9.1] — 2026-10-26
Commit: ver etiqueta `v0.9.1`

### Cambiado
- Accesibilidad: los títulos de pantalla y de sección se anuncian como encabezados; las listas, pestañas, filas y enlaces exponen su rol y etiqueta de acción.
- Los selectores de color de materias, cuentas y categorías se anuncian con el nombre del color, su estado de selección y rol de opción.
- El editor de notas y los formularios anuncian sus cambios de estado y errores; la búsqueda de Inicio anuncia el número de resultados y los iconos decorativos quedan documentados.
- El sistema de diseño incorpora tokens de tipografía para cifras, sobrelíneas y teclado, y un token de fondo atenuado compartido.
- Las tarjetas de bienvenida y de módulos usan `BentoCard`; los enlaces usan `CampusTextButton`.
- Los textos con altura fija pasan a altura mínima para que crezcan con el tamaño de fuente del sistema.
- Los textos literales de Inicio se mueven a `strings.xml`, incluidos los plurales de resultados de búsqueda.
- Versión 0.9.1 (versionCode 901).

### Corregido
- Los botones, chips y teclados conservan su área táctil mínima aunque el texto crezca.
- Los nombres de color y los contadores de búsqueda respetan el idioma y el ajuste de fuente del sistema.

---

## [0.9.0] — 2026-10-26
Commit: ver etiqueta `v0.9.0`

### Añadido
- Formulario de movimiento rediseñado en una sola pantalla: monto grande, teclado fijo con "Guardar" integrado y sin desplazamiento para guardar.
- Chips de categoría (5 visibles + "Más") con búsqueda y creación de categorías sin salir del formulario.
- Chips de cuenta con saldo y hoja de selección; chips de fecha Hoy / Ayer / Elegir fecha.
- Línea de impacto para gastos: compara el gasto del mes, incluido el monto en curso, con el presupuesto. En movimientos nuevos, el resto de tipos muestra cómo quedará el saldo de la cuenta.
- Categoría sugerida según la franja horaria y el historial de los últimos 60 días.
- Campo de nota opcional ("+ Nota") con sugerencias de descripciones recientes.

### Cambiado
- Las plantillas de movimientos recientes, "Guardar como plantilla" y "Eliminar" siguen disponibles en el nuevo diseño; el reembolso conserva el desplegable del gasto original.
- "Seguir añadiendo" ahora vive en el propio formulario y permite deshacer el último guardado.
- Versión 0.9.0 (versionCode 900).

### Corregido
- La línea de impacto no duplica el movimiento al editar ni cuenta gastos de otros meses; solo se muestra para gastos del mes en curso.

---

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