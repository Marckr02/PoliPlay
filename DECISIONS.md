# DECISIONS.md — CampusPocket

Registro de decisiones de diseño y arquitectura. Fecha del registro: octubre 2026.

## Estado del proyecto (real, verificado con `clean assembleDebug testDebugUnitTest`)

- **Fase 0 (cimientos): ✅ completada.** Tema, barra inferior con 4 destinos, Room/Hilt/Navigation, strings.xml, LogUtil, Money (centavos) con pruebas.
- **Fase 1 (académico manual): ✅ completada.** CRUD de materias con sesiones (día lunes–sábado, horas con `TimePicker` 24 h, aula), detalle de materia (observada con Flow, editar/eliminar), pantallas **Hoy** y **Semana** con `ViewModel` + `StateFlow`. Semana: lunes–viernes, sábado solo si tiene clases, domingo nunca.
- **Fase 2 (importador PDF): ✅ completada.** Extractor PdfBox + parser puro + pantalla de revisión obligatoria + guardado en una transacción + `import_hints`.
- **Fase 3 (tareas y recordatorios): ✅ completada.** Lista de tareas (pendientes/completadas, filtro por materia, orden por entrega), formulario (fecha/hora 24 h, prioridad, varios recordatorios), permisos con explicación previa, deep link desde la notificación, detalle de materia con sus tareas. Verificada con 73 pruebas JVM + 9 instrumentadas en dispositivo más prueba manual del usuario.
- **Fase 4 (finanzas base: cuentas, categorías, transacciones, historial): ✅ completada.**
- **Fase 5A (presupuestos y resumen): ✅ completada.** Mes/semestre (sin semestre fechas = últimos 6), barra azul/ámbar(80%)/roja(>100%), "excedido" en texto, copiar mes anterior sin pisar, dona con Canvas propio, resumen con disponible + balance actual + top categorías. Sin "Pago programado ni Balance/Gasto proyectado".
- **Fase 5B (pagos programados, proyección, calendario, alertas, revisión diaria): ✅ completada.**
- **Fase 6 (notas): ✅ completada.** Lista con carpetas anidadas (círculo con inicial, no iconos de librería), búsqueda con LIKE escapado, editor Markdown propio (título, negrita, cursiva, código, listas, casillas tocables; sin Markwon), autoguardado con debounce + al salir (nota vacía no se guarda; updatedAt solo si cambia), vínculos a materia/tarea mostrados en sus detalles (FK SET_NULL conserva la nota). Respaldo JSON ya las incluía desde la Fase 4.5. Esquema v3 sin tocar. 119 JVM + 21 instrumentadas en emulador. Nueva columna `scheduled_payments.anchorDayOfMonth` con `version=3` + `MIGRATION_2_3` y test real. WorkManager a las 8:00. Pestañas de Finanzas: Resumen/Cuentas/Presupuestos/Calendario/Historial. Hoja "+" con 5 tipos (Pago abre su formulario propio). Alertas 80/100 guardadas en DataStore con clave `budget_alert:{yearMonth}:{categoryId}`.
- **Regla 5A**: instrumentadas en emulador; el teléfono NO se toca salvo confirmación explícita del usuario (se instala con `adb -s emulator-5554 install`). Cuentas agrupadas por tipo con saldo calculado (Flow), categorías con círculo de color + inicial, transacciones de cuatro tipos con validaciones puras, historial con filtros. Verificada con 94 JVM + 14 instrumentadas en dispositivo. Esquema Room sin tocar (v2 congelado).
- **Fase 4.5 (respaldo JSON, adelanto de la 7): ✅ completada.**
- **Fase 7 (ajustes, seguridad, pulido): ✅ completada.** Ajustes reales en DataStore (tema sistema/claro/oscuro, canales de notificación, permiso de notificaciones y alarmas exactas, selector de semestre, acerca de), banner de 30 días/descartable 7, bloqueo biométrico opcional con credencial del dispositivo y tiempo configurable (si el dispositivo no tiene credencial, no activa), tema oscuro ya profundo (dona/barras/calendar/negativos usan colores del tema), accesibilidad con contentDescription, estados vacíos con acción, lintDebug en verde, README. `Modo de bloqueo` probado en el emulador (rostro simulado no se puede probar automáticamente; el toggle y política sí). Exportar/importar en Ajustes, formato versionado con las 13 tablas de entonces (hoy 14), validación antes de tocar nada, transacción única todo-o-nada. 101 JVM + 16 instrumentadas.
- **Fase 8 (retiro en cajero y ritmo del presupuesto total): ✅ completada.** Retiro = transferencia atómica `TRANSFER`/`RETIRO_CAJERO` desde una cuenta de ahorros activa hacia Efectivo; monto > 0 múltiplo de 5 USD, sin sobregiro, sin gasto ni comisión. Historial con etiqueta “Retiro en cajero”, origen → destino y filtro propio. Presupuesto total mensual en tabla independiente `monthly_budgets` (único por `yearMonth`, sin arrastre); tarjeta en Presupuestos y tarjeta de ritmo en Resumen con esperado vs. real, marca del día, barra topada al 100 % y sugerencia si falta. Esquema v4 con `MIGRATION_3_4`; respaldo v4 de 14 tablas compatible con v2/v3. 158 JVM + 41 instrumentadas.

## Cómo verificar

1. `./gradlew clean assembleDebug` en verde.
2. `./gradlew testDebugUnitTest` en verde (Money, parser del PDF con el fixture 8.5, ViewModels de la Fase 1, utilidades).
3. App: barra inferior con 4 destinos; Académico muestra Hoy/Semana/Materias funcionales.

## Decisiones

### Build (AGP 9.4.1, sin red)
- AGP 9 trae **Kotlin integrado**: no se aplica `org.jetbrains.kotlin.android`. Kotlin 2.2.21; el plugin de Compose lleva la misma versión. KSP 2.3.10, Hilt 2.59, Room 2.8.4.
- `compileSdk`/`targetSdk` = 37, `minSdk` = 26, Java 17. Sin *core library desugaring*.
- **Smart App Control** bloqueaba el `aapt2.exe` extraído de Maven en la caché de Gradle (CreateProcess error 4551, proceso de Java). Solución local en `gradle.properties`: `android.aapt2FromMavenOverride` apuntando al aapt2 del SDK (que la directiva sí permite). Borrar esa línea si se permite el de Maven.
- Dependencias: PdfBox-Android (`com.tom-roush:pdfbox-android:2.0.27.0`), **sin Gson ni kotlinx.serialization por ahora** (el respaldo JSON de la Fase 7 se escribirá a mano o con kotlinx.serialization al llegar).

### Tema y navegación
- Tema `CampusTheme` (Material 3, color propio, sin color dinámico; oscuro en Fase 7). Iconos vectoriales propios + `material-icons-core` (viene transitiva con material3).
- **Un solo `NavHost`** con un grafo anidado `navigation(...)` por pestaña (manda la barra inferior). Rutas en constantes (`TopLevelDestination`, `AcademicDestinations`). Los ViewModels reciben argumentos de navegación vía `SavedStateHandle` inyectado por Hilt.
- Grafo académico: `academic/hoy`, `academic/semana`, `academic/materias`, `academic/course/{courseId}`, `academic/materia_form[?courseId=]`, `academic/import`, `academic/tasks`, `academic/task_form[?taskId=]`. La lista de tareas se abre desde los botones "Tareas" de Hoy y Semana.

### Dominio y la capa domain
- `domain` no importa `android.*` (verificado). Modelos con `java.time` (`LocalDate`, `LocalTime`, `Instant`); mapeadores entidad↔dominio viven en la capa `data`.
- **Día de clase**: se guarda `DayOfWeek.value` (1=Lunes…7=Domingo) y la hora como minutos desde medianoche. La universidad no tiene clases en domingo: `SchoolDays` (lunes–viernes + sábado) manda en UI y vista semanal.
- **Semestre activo**: uno solo. El CRUD manual crea uno automático si falta (`SemesterTerm`: ene–jun → "AAAA-1", jul–dic → "AAAA-2"). El importador crea el del PDF ("2026-B") y lo marca activo; si ya existe uno con ese nombre, **fusiona** (ver "Reimportación (fusión por semestre)"; ya no se borra en cascada). No se crean semestres paralelos activos.
- Al borrar una materia (confirmada como ausente), sus tareas quedan con `courseId = null` (FK `SET_NULL`, decisión deliberada: no borramos tareas del usuario).

### Importador de PDF (formato único)

- **Único formato soportado: el horario del SAEw impreso desde el navegador con
  "Guardar como PDF"** (una página, palabras completas por línea). El formato viejo del
  sistema (3 páginas, texto partido por el ancho de columna) se descartó por completo;
  se detecta y se rechaza con un mensaje que indica cómo generar el bueno.
  **No se usa OCR**: un PDF impreso con "Microsoft Print to PDF" es una foto y sale con
  el mensaje de "sin texto seleccionable" (medido: `textPositions = 0`).
- Detección de formato por el encabezado: tiene que contener las palabras enteras
  `Nro. Código Materia Paral Aula Creds N.Mat Lunes…Sabado FechaI FechaF Carrera Obs Coreq
  Profesor` (normalizadas, sin tildes); el formato viejo las parte ("Códig"+"o") y no cuela.
- **Columnas por borde izquierdo** (nunca por centro): el texto justificado deja a la
  última palabra cerca del borde derecho ("Y", "DE", "PROFESOR") y el centro cambiaba de
  columna.
- **Filas** centradas verticalmente: bandas entre puntos medios de las marcas de `Nro.`,
  la última simétrica; el pie ("Unidad Titulación Especial" o cualquier otro) queda fuera
  por geometría y no se lee.
- Celdas: se unen con un espacio; se retira el sufijo "(CÓDIGO)" de la materia; nombres y
  profesores se guardan en formato título con de/del/la/el/en/los/las/y en minúscula.
- Todo lo referido a la línea `Estudiante:` se descarta *antes* de procesar y jamás se
  escribe en logs ni en el TSV de pruebas (la línea se purga del fixture).
- **import_hints se queda sin usar de momento** (la tabla y el DAO siguen; el parser ya no
  produce nombres crudos que necesiten corrección automática). Queda para una futura
  migración/refactor del respaldo.
- Fixture de pruebas: `app/src/test/resources/fixtures/saew-horario-2026b.tsv`
  (extracción real del `HORARIO_2026B.pdf`, sanitizado: sin la línea Estudiante).
  Columnas: página, x, y, ancho, texto (y crece hacia abajo).
- Arnés de diagnóstico en `PdfDebugHarnessTest`: SOLO corre con la propiedad de sistema
  `campuspocket.pdf.fixture=<ruta>` (sin rutas fijas); redacta Estudiante; escribe en
  `tools/pdf-dumps/` (git-ignorado).
- Dependencia extra de tests: `org.apache.pdfbox:pdfbox` de escritorio (clasificación
  `testImplementation`): el de Android no corre en JVM.

### Base de datos
- **Migraciones explícitas desde ya.** Se quitó `fallbackToDestructiveMigration`.
  `Migrations.MIGRATION_1_2` retira la tabla `settings` y crea los dos índices que se
  habían añadido sin subir versión. Prueba instrumentada `MigrationTest` (necesita
  dispositivo/emulador; requiere `room-testing` y los esquemas como assets de androidTest).
  OJO: `room-testing` arrastra `kotlinx-serialization-json 1.8.1` contra `core 1.7.3`
  (fijado por savedstate) y esa discordancia rompe con `AbstractMethodError`; se fuerza
  json-jvm/json a 1.7.3 solo en el classpath de androidTest.
- **Esquema actual v4.** Los snapshots `app/schemas/1..4.json` no se regeneran ni se editan a
  mano. Cualquier cambio de entidad exige: `version = 5` en `AppDatabase`, una `Migration`
  en `Migrations` y su prueba instrumentada (como `MigrationTest`).
- **v3 → v4 (Fase 8).** `transactions.subtype` nullable (sin tocar filas viejas) y tabla
  `monthly_budgets` (`id`, `yearMonth` TEXT, `amountCents` INTEGER) con índice único por
  `yearMonth`. `MigrationTest` cubre la migración real 3→4.
- Nota histórica del desarrollo: `app/schemas/1.json` se regeneró durante la limpieza y
  ya no refleja el primer v1 instalado en el dispositivo; por eso el usuario desinstala una
  vez antes de probar. El `MigrationTest` NO usa ese `1.json`: crea la "v1 antigua" con DDL
  a mano (con `settings` y sin los dos índices nuevos) para que la migración sea real.
- Inserciones `ABORT` por defecto; `REPLACE` solo en `import_hints` (sin claves foráneas hijas).
- Inserciones compuestas (materia+sesiones, tarea+recordatorios, importación completa) en
  **transacciones** (`@Transaction`/ `withTransaction`).

### Recordatorios (aplicado en Fase 3, con UI)
- Un recordatorio = una alarma (`requestCode` = id). Exacta si se puede; si no, inexacta (degradación registrada en log de debug y aviso en pantalla con botón a los ajustes del sistema).
- `TaskRepositoryImpl` programa recordatorios al crear/editar y los cancela al borrar o completar. Editar reemplaza los recordatorios: se cancelan los viejos y se programan los nuevos.
- `BootReceiver` reprograma los recordatorios futuros tras reiniciar.
- El formulario ofrece antelaciones fijas (a la hora, 10 min, 1 h, 1 día) elegibles en múltiple.
- La notificación abre la tarea: `ReminderReceiver` manda `EXTRA_TASK_ID` a `MainActivity`, que navega al formulario de esa tarea.
- Infra de pruebas: `kotlinx-coroutines-test` va también como `debugImplementation` y se fusionan sus service files (sin esto, las pruebas Compose en dispositivo fallan con "Exception handler was not found via a ServiceLoader").

### Reimportación (fusión por semestre)

- La reimportación NO borra el semestre: fusiona por (semestre, código, paralelo).
  Materia conocida: se actualiza (mismo id, conserva las tareas vinculadas) y se reemplazan
  sus sesiones. Materia nueva: se inserta. Materia guardada que ya no sale en el PDF: se
  marca como ausente y **solo se borra si el usuario confirma** el diálogo con su nombre.
  Todo en una sola transacción de Room.
- El semestre importado queda activo (nunca hay dos activos); si no existe, se crea.

### Finanzas (Fase 4)
- Pestaña: solo **Cuentas** e **Historial**. Resumen/Presupuestos/Calendario/Pagos programados: Fase 5.
- Saldo = inicial + ingresos + reembolsos − gastos − transferencias salientes + entrantes. Tarjeta de crédito: campo "Deuda inicial" (positivo en pantalla, se guarda negativo).
- Cuentas y categorías se **archivan**, no se borran (FKs RESTRICT en cuentas con movimientos).
- Validaciones puras en `FinanceValidators`: monto > 0, transferencia origen ≠ destino, categoría del tipo correcto. Reembolso: categoría obligatoria, gasto original opcional.
- `ComputeAccountBalances` (dominio) combina `observeAccountDeltas` + `observeTransferInDeltas`.
- `ColorPalette` vive en `core/designsystem` (lo comparten academic y finance).
- **Montos: entrada estilo cajero en TODA la app** (transacciones y saldo/deuda inicial de cuenta): teclado numérico, el usuario solo escribe dígitos y cada dígito entra por la derecha (0,05 → 0,54 → 5,40). Se guardan centavos directamente; `Money.toLocalDecimalString` formatea la vista (es_EC, coma decimal).
- El FAB "+" de Finanzas abre siempre la hoja de los 4 tipos (en ambas pestañas); "Nueva cuenta" vive en la cabecera de Cuentas.

### Finanzas (Fase 8)

- **Retiro en cajero = transferencia especial.** Se guarda UNA fila `TRANSFER` con
  `subtype = RETIRO_CAJERO`, sin categoría y sin comisión. Como los saldos se derivan de
  las transacciones, la misma fila descuenta de ahorros y acredita a Efectivo; el
  repositorio valida y escribe dentro de `db.withTransaction` (sin sobregiro, sin rastro
  si falla). Origen: solo cuentas `SAVINGS` activas. Destino: la cuenta activa llamada
  “Efectivo” (sin distinguir mayúsculas) o, si no existe, la primera `CASH` activa.
- **Múltiplo de 5 = 500 centavos.** Coincide con los chips $5/$10/$15; “Otro” acepta solo
  dígitos. Los errores se muestran junto al control y desaparecen al editar.
- **Presupuesto total mensual independiente.** `monthly_budgets` tiene un monto por
  `yearMonth` (índice único), no se suma a los presupuestos por categoría y no se copia
  al mes siguiente. Se define/edita desde la tarjeta de Presupuestos en modo mes; en modo
  semestre se oculta.
- **Ritmo (`BudgetPace`, dominio puro).** `esperadoPct = día/díasDelMes`,
  `realPct = gastado/presupuesto`; el gasto es el neto de reembolsos del resumen mensual
  (excluye transferencias/retiros y pagos no ejecutados). Las comparaciones usan valores
  sin redondear; la UI muestra porcentajes a 1 decimal half-up y montos a 2. La barra
  topa el relleno al 100 %, muestra el porcentaje real completo y una marca vertical del
  esperado. Sin presupuesto (ausente o ≤ 0) se oculta la tarjeta y se sugiere definirlo.
- **Recálculo reactivo.** `SummaryViewModel` combina transacciones, presupuestos,
  presupuesto total y un flujo de fecha que revisa cada minuto: se actualiza al abrir,
  editar un gasto, cambiar el presupuesto y cambiar de día/mes.
- **Cambio de UI mínimo:** como la app no tiene un selector de tipo dentro de
  Transferencias (el tipo se elige en la hoja “+” del FAB), “Retiro en cajero” se agregó
  como quinta acción de esa hoja. Historial gana etiqueta y chip de filtro. No se añadió
  notificación de ritmo: el estado vive solo en la tarjeta del Resumen.

### Otras

### Otras
- `allowBackup = false` (el respaldo propio en JSON llega en la Fase 7).
### Respaldo (Fase 4.5, adelanto de la 7)
- **Ajustes real** (ya no placeholder): "Exportar datos" (SAF `CreateDocument`, aviso de "no cifrado" antes) e "Importar datos" (SAF `OpenDocument`). Fecha del último respaldo en DataStore (`last_backup_at`).
- **Formato JSON versionado** (`core/backup`): `formatVersion` 1, `dbVersion` 4, `exportedAt` (ISO) y las **14 tablas con sus ids**, incluido `monthly_budgets` y `transactions.subtype`. `kotlinx-serialization` (plugin con la misma versión de Kotlin, `json 1.8.1` — se fuerza 1.8.1 también en release).
- **Importar = todo-o-nada**: valida formato y versión ANTES de tocar nada; confirmación del usuario; borrado e inserción en UNA transacción de Room con `PRAGMA defer_foreign_keys` (las FKs se verifican al commit; si falla, se revierte). Un archivo inválido deja todo intacto. Se aceptan `dbVersion` 2 y 3: v2 deriva `anchorDayOfMonth`; v3 deja `subtype = null` y `monthly_budgets` vacío. Una versión superior se rechaza.
- Pruebas: JVM ida/vuelta (`BackupJsonTest`, `BackupVersioningTest`) + instrumentada `BackupManagerTest` (exportar→borrar→importar idéntico, incluido retiro y presupuesto total; inválido no cambia nada).
- **Regla operativa nueva**: las instrumentadas se ejecutan en el teléfono SOLO con confirmación explícita del usuario (pueden sobreescribir sus datos al reinstalar).

### Otras
- `local.properties` no se sube al repositorio.
- Logs de release sin datos personales: `LogUtil` solo imprime en debug y nunca montos/nombres/notas.
