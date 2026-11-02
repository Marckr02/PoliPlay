# Changelog

Todas las versiones siguen [SemVer](https://semver.org/lang/es/) en fase `0.x`:
- `0.MENOR.PARCHE` - `MENOR` sube con `feat:`, `PARCHE` con `fix:`.

Las entradas marcadas **no publicada** nunca llegaron a salir: no hay etiqueta
de Git ni `VERSION_NAME` que las respalde. Su trabajo se recoge dentro de la
versión publicada que sí lo incorporó.

## [0.15.2] - 2026-11-02

Etiqueta `v0.15.2` · commit `23e9a3a` ("fix: texto repetido en cierre estimado")

### Corregido
- Medidor de presupuesto: el porcentaje se mostraba multiplicado por 100 (430,0 % en vez de 4,3 %) en Finanzas e Inicio.
- Barra de estado: los íconos siguen el tema elegido en la app, no solo el del sistema.
- Nuevo gasto: la etiqueta de transferencia ya no se corta.
- Montos negativos se muestran como -$20,00.
- El total del día ya no cuenta las transferencias como gasto; la transferencia muestra origen y destino.
- Cierre estimado: se evita repetir "Aún faltan datos".

### Pruebas
- 227 pruebas unitarias en verde (`./gradlew :app:testDebugUnitTest`); instrumentadas: 65 en verde (emulador `CampusTest`, API 36).

---

## [0.15.1] - 2026-11-01

Etiqueta `v0.15.1` · commit `8e1b58d` ("fix: re-bloqueo biométrico al volver a primer plano")

### Corregido
- **Dock (T1)**: ocupaba toda la pantalla al iniciar; ahora mide solo su franja. Prueba de regresión dentro de `Scaffold`.
- **Presupuestos (T2)**: un mes nuevo lista todas las categorías de gasto para asignar montos; "Copiar del mes anterior" copia también el presupuesto total, busca hasta 12 meses atrás y no pisa lo existente.
- **Migración 5→6 (T3)**: la tabla `grades` coincide con la entidad (`tipo`, `score`, `maxScore`, `date`); evita el fallo de validación al actualizar desde v5.
- **Respaldo (T4)**: incluye calificaciones, subtareas, plantillas de movimientos y repetición de tareas; `dbVersion` 6; se siguen aceptando respaldos 2 a 5.
- **Bloqueo (T5)**: se vuelve a pedir autenticación al regresar a la app; sin `runBlocking` en el hilo principal.

### Pruebas
- 222 pruebas unitarias en verde (`./gradlew :app:testDebugUnitTest`); instrumentadas: 65 en verde (emulador `CampusTest`, API 36).

---

## [0.15.0] - 2026-11-01

Fases 3 y 4 del plan de auditoría v10 (C1–C7, D1–D8). Sin cambios de lógica de negocio, de base de datos ni de rutas.

### Corregido
- **Colores fuera de paleta (C2)**: eliminación de colores literales (`Color.White`, `Color(0xFF...)`) en componentes y centralización en `CampusExtendedColors` con tokens para bordes pastel (`noteLavenderBorder`, `noteLimeSoftBorder`, `notePeachBorder`, `noteSkyBorder`), acento (`accentBorder`, `buttonPrimaryBorder`) y marcas sobre tinta (`onInkTrack`, `onInkTick`).
- **Control de tema oscuro (C4)**: exposición de `isDark` en `CampusExtendedColors` para eliminar llamadas sueltas a `isSystemInDarkTheme()` fuera de `Theme.kt`.
- **Animaciones respetan accesibilidad (C5)**: todas las animaciones `tween` en componentes y rutas de navegación usan `motion.ms(...)` para detenerse con "Reducir animaciones".
- **Calendario embebido (D3)**: eliminación de la altura fija de 520.dp en `CalendarScreen` dentro de la vista de pagos para evitar problemas de scroll anidado y fuentes grandes.
- **Terminología y glosario (D2)**: eliminación total de "Borrar" en `strings.xml` (0 ocurrencias) en favor de "Eliminar", "Quitar" y "Limpiar".

### Añadido
- **Formato unificado (`CampusFormats`) (D5)**: objeto centralizado en `core/util` para formateo de fechas, horas, moneda y porcentajes bajo el locale español de Ecuador (`es-EC`).
- **Migración de menús (C7)**: adopción de `CampusMenu` / `CampusMenuItem` y `CampusSelectField` en reemplazo de los menús nativos de Material.
- Pruebas de contraste para todos los tokens de borde C2 en `CampusPaletteContrastTest`.

### Cambiado
- Marcado `PillSegmentedControl` con `@Deprecated` (C6).
- Estandarización de porcentajes con `CampusFormats` en `PaceGauge`.

---

## [0.14.4] - 2026-11-01

Fases 1 y 2 del plan de auditoría v10 (A1–A3, E1, B1–B3). Sin cambios de
lógica de negocio, de base de datos ni de rutas.
- **Contraste del tema oscuro (A1–A3)**: el contenido sobre lima o ámbar usa el
  token `onLime` (#103C3B, el mismo valor que `onAccent`) en los dos temas; el
  texto activo de pestañas, chips y controles pasa de tinta a `inkContent`, y
  los bordes e indicadores activos pasan a `inkIndicator` (tinta en claro, lima
  en oscuro). El interruptor encendido y las teclas del teclado numérico
  siguen el mismo criterio.
- **Medidor de ritmo (A2)**: el arco en alerta tiene su propio color (#FF969B)
  en claro y en oscuro. Antes reutilizaba el relleno pálido de las alertas,
  casi invisible sobre la tarjeta oscura.
- Se migraron 23 de los 36 usos del token de tinta; los 13 que quedan son
  rellenos decorativos, bordes de superficie o comparaciones semánticas y se
  mantienen a propósito.

### Añadido
- **Menú del sistema de diseño**: superficies con borde de 1 dp, sin sombra y
  filas de 48 dp, con variantes normal y destructiva y separador. Se usa en
  Pagos programados y queda disponible para el resto de menús.
- Pruebas de contraste de la paleta para los estados activos (claro y oscuro).
- Pruebas instrumentadas que verifican el tamaño mínimo de los objetivos
  táctiles y la estructura de la tarjeta de pago programado.

### Cambiado
- **Pagos programados (B3)**: cada pago tiene una sola acción principal,
  "Marcar como pagado", y un menú de "Más acciones" con Activar/Pausar, Editar
  y Eliminar. El estado Activo/Pausado deja de ser un botón y pasa a ser un
  indicador. La eliminación se puede seguir deshaciendo desde el aviso
  inferior.
- **Objetivos táctiles y roles (B1)**: todo elemento interactivo mide al menos
  48 × 48 dp de área táctil y expone su rol. La barra de formato del editor de
  notas pasa de 44 a 48 dp. Los 46 tamaños declarados por debajo de 48 dp que
  quedaban son, revisados uno a uno, decorativos o están dentro de un
  contenedor interactivo que ya cumple el mínimo; ninguno es un control por sí
  mismo. Los 10 `clickable` que no declaraban rol ahora lo hacen, salvo el
  velo y la tarjeta del diálogo, que solo absorben toques y quedan justificados
  en el código.
- **Lectores de pantalla (B2)**: los iconos decorativos llevan un comentario
  que justifica por qué no se anuncian y las iniciales de los avatares ya no
  se leen dos veces. Antes 52 de los 55 `contentDescription = null` tenían la
  justificación; ahora los 55 la tienen.
- La versión de la app pasa a 0.14.4 y este archivo vuelve a cubrir 0.14.1,
  0.14.2, 0.13.0 y 0.12.0, con el estado real de cada publicación.

### Pruebas
- 215 pruebas unitarias (1 de ellas ignorada por ser inestable en CI) y 64
  instrumentadas; `lintDebug` en verde.

---

## [0.14.3] - no publicada

No llegó a publicarse: no hay commit ni etiqueta `v0.14.3`. El hotfix de
contraste del modo oscuro que tenía asignados se entrega dentro de 0.14.4.

---

## [0.14.2] - 2026-10-31

Etiqueta `v0.14.2` · commit `61855bd` ("fix: 5 critical UX/UI audit fixes")

### Corregido
- La etiqueta de la próxima clase en Inicio usaba tinta sobre lima sin cumplir
  el contraste mínimo; ahora usa el color de contenido correcto.
- Materias vuelve a tener cabecera con botón "Volver": se había quedado una
  trampa de navegación al entrar a una materia.
- El formulario de movimiento ya no espera dos segundos antes de cerrar al
  guardar.
- El editor de notas ya no bloquea la interfaz al guardar: el vaciado del
  estado es asíncrono.
- El retiro en cajero ya no pide confirmar con un diálogo bloqueante: se cierra
  al instante y muestra el aviso.

### Pruebas
- 211 pruebas unitarias (1 ignorada por ser inestable en CI) y 56
  instrumentadas; `lintDebug` en verde.

---

## [0.14.1] - 2026-10-31

Etiqueta `v0.14.1` · commit `a59c313` ("fix: v9 corrections")

### Corregido
- **Tema oscuro**: se añadieron los tokens de contenido y de indicador activo
  para que los estados encendidos se distingan en oscuro, con pruebas de
  contraste.
- **Medidor de ritmo**: el relleno en alerta pasó a un color propio en lugar de
  reutilizar el de alertas.
- **Pestañas**: se retiraron los selectores en píldora que quedaban en Finanzas,
  Académico, Ajustes y el editor de notas; en su lugar hay fichas y pestañas
  subrayadas.
- **Animaciones**: todas las transiciones de color y escala pasan por el
  movimiento de la app, y el medidor ya no se reanima al recomponer.
- **Accesibilidad**: los iconos decorativos quedan documentados, los botones de
  texto del sistema se unifican y el título del medidor se anuncia como
  encabezado.
- **Editor de notas**: tokens propios para el papel y el texto, y estilos de
  cuerpo de nota.

### Añadido
- Pruebas del ritmo de presupuesto: caso de aceptación y casos límite (meses
  de 28 a 31 días, gasto igual o mayor al presupuesto).

### Pruebas
- 211 pruebas unitarias (1 ignorada por ser inestable en CI) y 56
  instrumentadas; `lintDebug` en verde.

---

## [0.14.0] - 2026-10-31

### Añadido
- **Rediseño editorial v8 (CampusPocket 2.0)**: nueva identidad visual coherente en 32 pantallas.
- Tokens editoriales: paleta `Ink`/`Lime`/`Mint`/`Peach`/`Sky`/`Lavender`, tipografía Manrope (500/600/700/800), radios 4/5/6/7/8/10/12/16/18/28/50, elevación solo en dock/snackbar/hojas.
- Componentes base: `CampusButton` (5 estilos), `CampusTextField` (borde `ControlLine`, foco `Lime`+`Ink`, error `Danger`+`liveRegion`), `CampusChip` (40/48 dp, `Role.RadioButton`/`Tab`, `stateDescription`), `UnderlinedTabRow` (45 dp, subrayado 3 dp `Ink`), `BentoCard` (`Surface`+`Line` 1 dp, héroe `Ink`+borde blanco 14%), `CampusSwitch`, `CampusDivider`, `CampusBottomSheet`/`CampusSheetOption`, `KeypadAmountInput` (244 dp, 7 dp gap, tecla `Lime`/`Ink`, escala 0.94 con `LocalMotion`), `Snackbar` (`Ink`+borde `Lime`, 54 dp).
- Navegación: `FloatingDock` (superficie sólida, borde `Ink`, radio 18, sombra suave, subrayado activo `Ink`), `ContextualActionButton` (`Lime`+`Ink`, sombra dura 5/5→2/2 al presionar con `LocalMotion`), `CampusTopBar` (título Display, botón 44 dp con borde, línea inferior 1 dp `Line`), `RoundIconButton` (44 dp, radio 8, transparente con borde `Line`).
- Inicio: saludo Display, `CampusDivider`, hero `Ink` con `PaceGauge` compacto, tarjetas `Surface`+`Line`, casilla `ShapeTaskCheckbox` (radio 5), botón "Añadir" abre hoja.
- Académico: `AcademicSectionNav` (Hoy/Semana/Materias/Tareas, `UnderlinedTabRow`), `DayStrip` v8 (contenedor `Surface`+`Line`, celdas con divisor, activo `Ink`), `TimelineRow` (actual `Ink`+barra `Lime`, futuras `Surface`+`Line`+borde izquierdo 4 dp), `TaskItemCard` (alto 72, `CampusCheckbox` radio 5).
- Finanzas: `PaceGauge` semicircular (248 dp, trazo 14 dp, arco 180°, animación 600 ms `LocalMotion`, estados Por debajo/encima/agotado/sin presupuesto, descripción accesible única, marca esperada, región viva `Polite`), insights `Mint`/`Peach`, pestañas `UnderlinedTabRow`, formulario movimiento con chips recientes, `TransactionImpactCard` (`LimeSoft`+borde `Ink` 4 dp), retiro en cajero validación en vivo, botón "Revisa el monto".
- Notas: `PastelNoteCard` (`Surface`+`Line`+borde superior 5 dp color nota, 154 dp), `SearchPill` (radio 28, `ControlLine`, foco `Lime`+`Ink`), `MarkdownToolbar` (44 dp, `ControlLine`), `EditorModeTab` (`UnderlinedTabRow`), editor papel `#FFFAF0`/`#E1D6BC`, líneas 23 dp + interlineado 23 sp.
- Ajustes: `UnderlinedTabRow` (Tema, tiempo bloqueo), botones cabecera sin relleno, `CampusSwitch` mantenido.

### Cambiado
- `PillSegmentedControl` → `UnderlinedTabRow` en Finanzas, Académico (sub-nav), Ajustes, editor.
- `RoundIconButton`: sin relleno, borde `Line` (antes `surface`/`accent`).
- Tarjetas: `Card` → `Surface`/`BentoCard` con borde `Line` 1 dp, sin sombra; héroe `Ink` con borde blanco 14%.
- Fila de tarea: alto 72, `CampusCheckbox` radio 5 (antes `Checkbox` circular).
- Botón contextual: sombra dura única en la app, etiquetas por pantalla ("Añadir", "Nueva tarea", "Movimiento", "Nueva nota").
- Formularios: chips (`CampusChip`/`SelectableModuleCard`) en lugar de `SegmentedControl`/`PillSegmentedControl`.

### Pruebas
- 207 pruebas unitarias (1 de ellas ignorada por ser inestable en CI) y 56
  instrumentadas; `lintDebug` en verde.

> **Aclaración (añadida en 0.14.4)**: esta entrada decía antes "207 pruebas
> unitarias (7 preexistentes no relacionadas con diseño)". En la etiqueta
> `v0.14.0` solo hay **1** prueba ignorada
> (`UserPreferencesRepositoryTest`, marcada por ser inestable en CI); no hay
> ninguna prueba fallando. Las 207 se ejecutaron y pasaron.

---

## [0.13.0] - no publicada

No llegó a publicarse: no hay etiqueta `v0.13.0`. El commit `3d70ea9`
(2026-10-28) añadió la activación opcional de Crashlytics con marcadores de
versión; ese trabajo quedó incorporado en 0.14.0.

---

## [0.12.0] - 2026-10-27

Etiqueta `v0.12.0` · commit `3b29e17` ("feat: calificaciones EPN y color por
materia coherente")

### Añadido
- **Calificaciones EPN por materia**: registra Nota 1 y Nota 2 (sobre 20) y la
  app calcula el estado (aprobado directo, supletorio o reprobado) y cuánto
  necesitas en el supletorio. Incluye el simulador del examen final.
- **Color de materia coherente**: Hoy, Semana, Tareas y Notas usan el mismo
  color de materia en todas las pantallas.
- Nueva tabla de calificaciones en la base de datos (migración v5 → v6) con su
  esquema exportado para las pruebas.

### Pruebas
- 206 pruebas unitarias (1 ignorada por ser inestable en CI) y 56
  instrumentadas; `lintDebug` en verde.

---

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