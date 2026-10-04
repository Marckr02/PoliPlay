# CampusPocket

App personal (uso del usuario, EPN) que reúne: horario académico importado desde PDF, tareas con recordatorios, finanzas (cuentas, presupuestos, pagos programados, calendario), notas en Markdown y respaldo JSON. 100 % local, sin backend.

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

- **Unitarias (JVM)**: `.\gradlew.bat testDebugUnitTest` → ~134 pruebas cubriendo dominio puro (Money, parser PDF, reloj, tareas, transacciones, presupuestos, pagos recurrentes, bloqueo, notas, respaldo).
- **Instrumentadas (emulador AVD `CampusTest` o similar)**: `.\gradlew.bat connectedDebugAndroidTest` → migraciones reales (1→2, 2→3), respaldo ida/vuelta, Room DAOs, Compose UI, humo de arranque.
- **Release**: `.\gradlew.bat assembleRelease` + `.\gradlew.bat lintDebug` en verde.

## Respaldo (exportar / restaurar)

- Ajustes → Respaldo → **Exportar datos** abre el selector de archivos y escribe `campuspocket-AAAA-MM-DD.json`.
- **Importar datos** lee el JSON (versionado: se acepta v2 y se migra; superior se rechaza).
- El archivo **no está cifrado**: guárdalo en lugar seguro.

Hecho por y para un solo usuario. No se sube nada a la nube.
