# AGENTS.md — Sintonía

Instrucciones para cualquier agente de IA (o persona) que trabaje en este repositorio.
Léelo completo antes de escribir código. El plan de trabajo está en `PLAN.md`.

## Qué es este proyecto

**Sintonía** es una app Android personal (un solo usuario, un solo teléfono) que mide cómo
percibe el usuario cada banda de frecuencia a través de sus audífonos Bluetooth, y genera un
preset de ecualizador para **Poweramp Equalizer** (archivo `.json` importable).

- Teléfono: POCO X8 Pro Max (HyperOS / Android reciente).
- Audífonos: HUAWEI FreeBuds Pro 4 (códec AAC 44.1 kHz habitual, LDAC opcional).
- Destino del resultado: Poweramp Equalizer, modo gráfico, 25 bandas personalizadas.
- No se publica en Play Store. Se instala por USB o desde el APK de GitHub Actions.

## Principios (en orden de prioridad)

1. **Seguridad auditiva.** Ningún sonido puede salir sin pasar por el limitador de nivel de la app.
   Nunca subir el nivel máximo, quitar el fade-in/out ni el paro automático sin que el usuario lo pida.
2. **No tocar el teléfono.** La app no modifica ajustes del sistema, volumen, Bluetooth, Poweramp
   ni otras apps. Solo lee (por ejemplo, el volumen actual) y escribe en su propio almacenamiento
   o donde el usuario elija con el selector de archivos del sistema.
3. **Permisos mínimos.** Fase 1 no requiere permisos peligrosos. No agregues permisos al
   `AndroidManifest.xml` sin que el plan lo indique y el usuario lo apruebe.
4. **Fácil de entender.** Cualquier persona debe poder completar una medición sin saber de audio.
   Textos cortos, un paso por pantalla, un solo botón principal por pantalla.
5. **Elegante.** Estética "liquid glass": vidrio translúcido con desenfoque, profundidad, movimiento sutil.

## Stack

- **Kotlin** + **Jetpack Compose** + **Material 3**.
- Un solo módulo `:app` mientras el proyecto sea pequeño.
- Arquitectura MVVM: `ViewModel` + `StateFlow` + UI declarativa. Navegación con Navigation Compose.
- Persistencia: archivos JSON en almacenamiento interno con `kotlinx.serialization`
  (sin base de datos en Fase 1).
- Desenfoque de fondo: biblioteca **Haze** (`dev.chrisbanes.haze`) para el efecto de vidrio sobre
  contenido. Si se evalúa otra biblioteca de "liquid glass", proponerla primero.
- Audio: `AudioTrack` en modo stream, PCM float, frecuencia de muestreo nativa del dispositivo
  (`AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE`, normalmente 48 000 Hz).
- `minSdk = 33` (Android 13; permite `RuntimeShader`/AGSL y `RenderEffect`). `targetSdk` y
  `compileSdk` = la versión estable más reciente disponible.
- Versiones de dependencias: siempre la **última estable**, declaradas en
  `gradle/libs.versions.toml`. No inventes números de versión: consúltalos.

## Estructura de paquetes

```
app/src/main/java/<paquete>/
  audio/        Generación de señal: ruido por bandas, tonos warble, filtros biquad, limitador, fades
  measure/      Lógica de la sesión de medición (orden de bandas, repeticiones, estado)
  analysis/     Matemática: curva percibida, ISO 226, suavizado, detección de picos, preset final
  export/       Serialización al formato Poweramp y guardado/compartir del archivo
  data/         Modelos persistidos (sesiones, presets) y repositorio de archivos
  ui/theme/     Colores, tipografía, formas, componentes de vidrio (GlassCard, GlassButton, etc.)
  ui/screens/   Una carpeta por pantalla (Inicio, Preparación, Nivel, Medición, Resultados, Exportar, Historial)
```

La lógica de `audio/`, `analysis/` y `export/` debe ser **Kotlin puro** (sin dependencias de Android)
siempre que sea posible, para poder probarla con pruebas unitarias en la JVM.

## Comandos

```bash
./gradlew assembleDebug        # compila el APK de depuración
./gradlew installDebug         # instala en el teléfono conectado por USB
./gradlew testDebugUnitTest    # pruebas unitarias (obligatorias antes de cada commit)
./gradlew lintDebug            # lint de Android
```

Antes de dar una tarea por terminada: compila, corre las pruebas unitarias y el lint.
Si algo no se pudo verificar (por ejemplo, el sonido real en el teléfono), dilo explícitamente.

## Contrato del archivo de Poweramp (no romper)

El preset exportado debe tener exactamente esta forma. Las dos primeras bandas son las de tono
(siempre `gain: 0.0`); siguen 25 bandas gráficas `type: 2` en estas frecuencias exactas:
20, 27, 36, 47, 63, 84, 112, 150, 200, 267, 356, 474, 632, 843, 1125, 1500, 2000, 2667, 3557,
4743, 6325, 8434, 11247, 14998, 20000 Hz.

```json
[{
  "name": "Sintonía - <nombre>",
  "preamp": -4.5,
  "parametric": false,
  "bands": [
    {"type": 0, "channels": 0, "frequency": 90,    "q": 0.8, "gain": 0.0, "color": 0},
    {"type": 1, "channels": 0, "frequency": 10000, "q": 0.6, "gain": 0.0, "color": 0},
    {"type": 2, "channels": 0, "frequency": 20,    "q": 0.0, "gain": 0.0, "color": 0}
  ]
}]
```

- Es un **arreglo** con un objeto.
- Ganancias redondeadas a 0.5 dB. Rango permitido: −8.0 a +3.0 dB.
- `preamp` = −(ganancia positiva máxima + 3.0), y nunca mayor que −4.5.
- Debe existir una prueba "golden" que compare la salida con un archivo de referencia en
  `app/src/test/resources/`.

## Reglas de audio y seguridad

- Todo buffer pasa por `SafetyLimiter` antes de escribirse en `AudioTrack`.
- Nivel máximo de salida por defecto: −18 dBFS RMS. Picos limitados a −6 dBFS.
- Fade-in y fade-out de 50 ms en cada inicio, parada y cambio de estímulo (sin clics).
- Paro automático tras 20 minutos de sonido continuo o al salir de la pantalla de medición.
- Pausar el sonido si se desconectan los audífonos (`ACTION_AUDIO_BECOMING_NOISY`).
- Usar `AudioAttributes.USAGE_MEDIA` y solicitar foco de audio; respetar su pérdida.
- Nunca reproducir sonido automáticamente al abrir la app.

## Diseño: "liquid glass"

- **Fondo:** degradado oscuro profundo (azul noche → violeta) con 2–3 manchas de color difusas
  que se desplazan muy lento ("aurora"). Animación desactivable y desactivada si el sistema pide
  reducir el movimiento.
- **Superficies de vidrio:** relleno blanco 8–14 % de opacidad, desenfoque de fondo 24–32 dp
  (Haze), borde de 1 dp con degradado blanco (más claro arriba-izquierda), esquinas de 28 dp,
  sombra suave. Ningún texto pequeño directamente sobre un fondo animado sin una capa de vidrio.
- **Color de acento:** un solo color principal (aqua/cian suave) y uno secundario (violeta).
  Rojo/ámbar solo para avisos de volumen y seguridad.
- **Tipografía:** sans-serif limpia (Inter o la del sistema), números tabulares para dB y Hz.
- **Movimiento:** transiciones de 200–350 ms con easing suave; vibración háptica ligera al confirmar.
- **Accesibilidad:** contraste AA en todo texto, objetivos táctiles ≥ 48 dp, `contentDescription`
  en iconos, soporte de texto grande.
- **Respaldo:** si el desenfoque no está disponible, usar relleno translúcido más opaco (20 %).
  La app debe verse bien también así.
- Componentes reutilizables en `ui/theme/`: `GlassBackground`, `GlassCard`, `GlassButton`,
  `GlassSlider`, `GlassChip`, `FrequencyChart`. Las pantallas solo usan estos componentes.

## Textos y experiencia de uso

- Idioma de la interfaz: **español (México)**. Todos los textos en `res/values/strings.xml`.
- Tono: claro, amable, sin jerga. Si se usa un término técnico (dB, Hz), acompañarlo de una
  explicación corta la primera vez.
- Cada pantalla responde a: ¿qué hago aquí?, ¿por qué?, ¿qué sigue?
- Siempre debe haber forma de pausar, repetir o saltar una banda, y de salir sin perder el progreso.

## Estilo de código y Git

- Formato estándar de Kotlin (ktlint o el formateador de Android Studio).
- Funciones pequeñas, nombres descriptivos en inglés para código; comentarios en español cuando ayuden.
- Commits pequeños y descriptivos en español, formato: `tipo: descripción`
  (`feat`, `fix`, `ui`, `audio`, `test`, `docs`, `chore`).
- Una rama por fase (`fase-1-diseno`, `fase-2-audio`, ...), fusionar a `main` cuando cumpla
  sus criterios de aceptación.
- No subir APKs, claves ni archivos `local.properties` al repositorio.

## Qué NO hacer

- No agregar analítica, anuncios, inicio de sesión ni acceso a internet (Fase 1 no pide el permiso `INTERNET`).
- No usar root ni comandos ADB que cambien ajustes del sistema.
- No reproducir audio fuera de la pantalla de medición.
- No cambiar el contrato del JSON de Poweramp sin actualizar la prueba golden y este archivo.
- No dar por buena una funcionalidad de audio sin indicar que requiere prueba en el teléfono real.
