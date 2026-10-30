# CHANGELOG — Timbra

Historial detallado del proyecto. Los mensajes de commit son breves; aquí va el *qué*, el *por qué*
y las decisiones técnicas de cada cambio relevante.

Formato de cada entrada:

```
### AAAA-MM-DD · `hash-corto` · tipo: título del commit
- Qué se implementó.
- Por qué / decisión tomada.
- Valores elegidos, limitaciones o pendientes derivados.
```

Las versiones siguen [versionado semántico](https://semver.org/lang/es/). Lo que aún no tiene
versión publicada va en **Sin publicar**.

---

## [Sin publicar]

### 2026-10-29 · `fa7949c` · docs: reescribir README
- Guía completa para cualquier persona: qué hace la app y qué no, términos básicos (Hz, dB,
  preset), requisitos del teléfono (Android 13 o superior) y dispositivos probados
  (POCO X8 Pro Max con HyperOS 3.0 / Android 16 y HUAWEI FreeBuds Pro 4).
- Dos formas de instalar: APK desde Releases o compilar desde el código, con los requisitos
  oficiales de Android Studio por sistema operativo (Windows, macOS, Linux), preparación del
  teléfono, comandos útiles y tabla de problemas comunes.
- Secciones de uso previsto, uso seguro, privacidad, estructura, versiones y desinstalación.

### 2026-10-29 · `cb53d0f` · chore: configurar repositorio
- `.gitignore` para Android/Gradle/Kotlin, IDEs, salidas de compilación y firmas. Las notas de
  trabajo locales quedan fuera del repositorio; solo se publican `README.md`, `TODO.md` y
  `CHANGELOG.md` como documentación.
- `AGENTS.md` y `PLAN.md` salen de la raíz y pasan a ser notas locales. Siguen en el historial
  del commit `d75d794`; no se reescribe la historia de `main` porque requeriría `push --force`.
- `.gitattributes` con fin de línea LF (CRLF solo en `.bat`) y binarios marcados;
  `.editorconfig` con 4 espacios, 2 en JSON/YAML/TOML y 120 columnas en Kotlin.
- Workflow `.github/workflows/android.yml`: si existe `gradlew`, corre pruebas, lint y
  `assembleDebug`, y sube el APK como artefacto. Cuando el dueño publica un release, adjunta
  `timbra-<tag>.apk` con `gh release upload`; nunca crea releases por su cuenta.
- Versiones: `actions/checkout@v7`, `actions/setup-java@v6` (Temurin 21),
  `gradle/actions/setup-gradle@v6`, `actions/upload-artifact@v7`. Pendiente confirmar el JDK
  cuando exista el proyecto Android (Android Studio Rabbit 2 trae JDK 25).
- Entorno de desarrollo preparado: Android Studio Rabbit 2 (2026.2.2), SDK Platform 37
  (Android 17), Build-Tools 37.0.0, Platform-Tools 37.0.1 y GitHub CLI.

### 2026-10-29 · `d75d794` · chore: inicializando proyecto
- Documentación inicial: reglas del proyecto, plan de trabajo y `README.md`.
- Nombre del proyecto: **Timbra** (de *timbre*, el color propio de cada sonido).
  Paquete: `com.juliancitou.timbra`. Repositorio: `github.com/juliancitou/Timbra`.
- Enfoque elegido: medición por **igualación de sonoridad** por bandas (ruido rosa en tercios de
  octava contra una referencia de 1 kHz) en las mismas 25 frecuencias del ecualizador gráfico de
  Poweramp. Se descartó medir con el micrófono del teléfono porque no da datos confiables de lo que
  llega al oído.
- Limitación asumida: la app localiza bien picos y valles angostos (sibilancia, resonancias),
  pero el equilibrio global graves/agudos es aproximado y se complementa con controles de gusto.
- Stack: Kotlin, Jetpack Compose, Material 3, Haze para el desenfoque, `AudioTrack` para el audio,
  `minSdk 33`.
- Seguridad auditiva definida desde el inicio: limitador obligatorio (−18 dBFS RMS, picos a
  −6 dBFS), fades de 50 ms, paro automático a los 20 min y pausa al desconectar audífonos.
