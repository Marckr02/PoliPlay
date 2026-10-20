# CampusPocket

App personal (uso del usuario, EPN) que reúne: horario académico importado desde PDF, tareas con recordatorios, finanzas (cuentas, presupuestos, pagos programados, calendario), notas en Markdown y respaldo JSON. 100 % local, sin backend.

Desde el rediseño híbrido profesional:
- **Inicio** es un lanzador de módulos (Académico, Finanzas, Notas) con saludo personalizado opcional.
- **Onboarding** de primer uso en dos pasos: nombre local opcional y selección de módulos (persistidos en DataStore).
- **Ajustes** agrupa Mi perfil (nombre local, no es cuenta en línea), Mis módulos (activar/desactivar sin borrar datos) y Apariencia (Seguir sistema / Claro / Oscuro).
- La barra inferior se construye con los módulos activos; si un módulo se desactiva mientras está abierto, la app vuelve a Inicio sin tocar los datos.
- Tema claro cálido y tema oscuro azul carbón con esquemas Material 3 completos (tokens en `core/designsystem/Color.kt`).
- **Finanzas**: retiro en cajero (ahorros → Efectivo) y ritmo del presupuesto total del mes en el resumen.

## Cómo compilar

Requisito: Android Studio con SDK 36 (la directiva de seguridad local requiere el `aapt2.exe` del SDK; `gradle.properties` ya lo apunta con ruta local).

```
./gradlew.bat clean assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/`.

## Instalar en otro dispositivo

El APK release firmado vive en:
```
app\build\outputs\apk\release\app-release.apk
```
(o el debug: `app\build\outputs\apk\debug\app-debug.apk`)

Pasarlo a otro Android: copiar el `.apk` al dispositivo y abrirlo (Android pedirá permitir instalar de fuentes desconocidas). O por ADB: `adb install app-release.apk`.

### Clave de firma (IMPORTANTE, guardar aparte)
El release está firmado con el keystore local `keystore.jks` (alias `campuspocket`, contraseñas en `keystore.properties`). **Ese archivo NO se sube a ningún repositorio**: solo existe en la máquina del propietario. Si se pierde, actualizaciones futuras de la app tendrían que instalarse como app nueva (Android exige la misma firma para actualizar).

## Cómo probar

- **Unitarias (JVM)**: `.\gradlew.bat testDebugUnitTest` → 158 pruebas cubriendo dominio puro (Money, parser PDF, reloj, tareas, transacciones, presupuestos, ritmo del presupuesto, retiro en cajero, pagos recurrentes, bloqueo, notas, respaldo) y preferencias (UserPreferencesRepository con DataStore real en archivo temporal).
- **Instrumentadas (emulador AVD `CampusTest` o similar)**: `.\gradlew.bat connectedDebugAndroidTest` → 41 pruebas: migraciones reales (1→2, 2→3, 3→4), retiro atómico y validaciones, presupuesto total mensual, respaldo ida/vuelta, Room DAOs, Compose UI y humo de arranque.
- **Release**: `.\gradlew.bat assembleRelease` + `.\gradlew.bat lintDebug` en verde.

## Respaldo (exportar / restaurar)

- Ajustes → Respaldo → **Exportar datos** abre el selector de archivos y escribe `campuspocket-AAAA-MM-DD.json`.
- **Importar datos** lee el JSON (versionado: `formatVersion` 1 y `dbVersion` 4; se aceptan respaldos v2/v3 y se normalizan al esquema actual; una versión superior se rechaza). El respaldo incluye las 14 tablas, entre ellas `monthly_budgets` y el subtipo de retiro.
- El archivo **no está cifrado**: guárdalo en lugar seguro.

Hecho por y para un solo usuario. No se sube nada a la nube.
