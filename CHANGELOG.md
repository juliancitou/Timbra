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

### 2026-10-30 · `5f75358` · ui: mejorar lectura con TalkBack y letra grande
- TalkBack ya no lee los adornos de terminal: `//` de las secciones, números `01` y viñetas
  `›` de las listas y los corchetes de `[x]` (la casilla anuncia "marcada / sin marcar").
- Letra grande del sistema: los chips pueden usar dos líneas y en Resultados pasan a la fila
  siguiente; los márgenes de la gráfica se calculan midiendo sus etiquetas; los valores grandes
  ("+12.0 dB", "11.2 kHz") se reducen para caber en una línea (`FitText`); los títulos largos
  bajan de línea sin sacar el cursor de la pantalla.
- Revisado en código: todo lo tocable mide al menos 48 dp y los íconos tienen descripción.
  Falta confirmarlo en el teléfono con el tamaño de letra al máximo.

### 2026-10-30 · `9902a0c` · test: verificar contraste AA de la paleta
- `ContrastTest` calcula el contraste WCAG 2.1 de cada pareja texto/fondo, con el vidrio sin
  desenfoque sobre el fondo más claro como peor caso: 4.5:1 para texto y 3:1 para gráficos.
- Ajustes para cumplir: gris secundario `#A1A1AA` → `#C2C2CA` (estaba en 3.67:1 sobre vidrio),
  rojo de aviso `#FF6B6B` → `#FF8A8A`, relleno de chips seleccionados del 22 % al 14 % y texto
  de chips seleccionados en blanco (el borde y el relleno de color marcan la selección).

### 2026-10-30 · `69982c3` · ui: más aire en pantalla y títulos estilo terminal
- Pedido del usuario: que el contenido no llene la pantalla. Márgenes laterales de 24 a 32 dp,
  tipografía un 10–15 % más chica (título 23 sp, indicación principal 18 sp, cuerpo 14 sp,
  valor del deslizador 36 sp) y botones de 52 dp de alto (siguen sobre el mínimo de 48 dp).
- `TerminalTitle`: los títulos se ven como una línea de terminal, `> Título`, con el `>` en
  ámbar. En Inicio, un cursor `_` parpadea cada 1.1 s (fijo si se reduce el movimiento). El
  `>` y el cursor no se leen en voz alta con lectores de pantalla.

### 2026-10-30 · `53481e9` · ui: rediseñar pantallas con estilo minimalista
- Pedido del usuario: menos tarjetas de vidrio y más jerarquía tipográfica. El texto va
  directo sobre el fondo; el vidrio queda solo en la gráfica, botones y chips.
- `GlassScreen`: barra superior con volver y una acción opcional, título grande y la
  indicación principal (`lead`) en letra grande y blanca, lo primero que se lee.
- `TerminalText.kt`: `LeadText`, `SectionLabel` (`// sección` en ámbar), `StepList`
  (`01 02 03`), `BulletList` (`›`), `CheckRow` (casillas `[x]` / `[ ]` con vibración al
  marcar), `DetailText` y `Hairline`.
- Inicio con pasos y recomendaciones antes de empezar; Ajustes pasa a un ícono de engrane
  arriba a la derecha. Historial como lista con separadores en lugar de tarjetas.
- `GlassSlider` gana el tamaño `Compact` (etiqueta y valor en una línea) para los tres
  controles de gusto.

### 2026-10-30 · `3e050fc` · ui: fondo negro y gris más sutil
- Se quitan las brasas ámbar y naranja: quedan dos nubes grises (7 % y 10 %) sobre el
  degradado casi negro, con un ciclo más lento de 40 s.

### 2026-10-30 · `14e0958` · ui: animar navegación con zoom y atrás predictivo
- Al entrar, la pantalla nueva crece desde 92 % mientras aparece y la anterior se agranda a
  106 % al desvanecerse; al regresar, la actual se encoge y se esconde y la anterior vuelve
  desde 106 %. Entradas de 350 ms con curva enfatizada de frenado y salidas de 200 ms.
- `enableOnBackInvokedCallback`: el gesto de atrás predictivo de Android mueve la animación
  con el dedo y se puede cancelar a la mitad.
- Sin animación si el sistema pide reducir movimiento.

### 2026-10-30 · `a00a09a` · ui: usar tipografía Cascadia Mono
- Estilo "app de programador" pedido por el usuario. Consolas no se puede incluir por
  licencia; se usa Cascadia Mono (Microsoft, OFL), su sucesora libre, como fuente variable
  (un archivo de 704 KB con grosores de 200 a 700) desde Google Fonts.
- Licencia OFL incluida en `assets/licenses/`, como exige la OFL al redistribuir la fuente.
- Tamaños ajustados para monoespaciada (más ancha) y espaciado entre letras en 0.

### 2026-10-30 · `8e4d2f0` · ui: cambiar paleta a negro, gris y ámbar
- Paleta pedida por el usuario: base casi negra (`#050506` → `#18181B`), texto blanco y gris,
  acento ámbar `#FFC23D` y secundario naranja `#FF8A3D`. Sustituye a la aqua/violeta.
- Aurora más oscura: brasas ámbar (20 %), naranja (15 %) y gris humo (9 %). Velo del vidrio
  al 8 %.
- Como el ámbar pasó a ser el acento, los avisos de seguridad cambian a **rojo** `#FF6B6B`.

### 2026-10-30 · `8714975` · ui: agregar navegación y pantallas de ejemplo
- Las 9 pantallas del plan con contenido de ejemplo: Inicio, Preparación, Nivel cómodo,
  Medición, Resultados, Ajuste de gusto, Exportar, Historial y Ajustes. Todavía no suena
  nada ni se guarda nada; cada pantalla ya responde qué hacer, por qué y qué sigue.
- `GlassScreen`: estructura común con título, botón de volver, contenido desplazable y
  acciones fijas abajo. `GlassIconButton` para el botón redondo de volver (48 dp).
- Navegación con rutas tipadas: deslizamiento corto más desvanecido de 300 ms, sin animación
  si el sistema pide reducir movimiento. Un solo `GlassBackground` para toda la app, así la
  aurora no se reinicia al cambiar de pantalla.
- Ajustes incluye un interruptor del desenfoque para probar el respaldo al 20 % y muestra la
  versión de la app (se activó `BuildConfig`). El estado aún no se guarda al cerrar la app.
- Medición muestra una explicación por zona ("aquí vive la S"); Ajuste de gusto mueve una
  vista previa aproximada de la curva (la fórmula real llega en la Fase 4).
- Textos en `strings.xml` marcados como español (`tools:locale="es"`) para que lint no los
  revise con el diccionario de inglés.

### 2026-10-30 · `2fbe84f` · chore: agregar Navigation Compose
- Navigation Compose 2.10.2 y el plugin de serialización de Kotlin (2.4.21), que las rutas
  tipadas necesitan y que la Fase 3 usará para guardar sesiones en JSON.

### 2026-10-30 · `e13bd4c` · ui: crear FrequencyChart
- Gráfica de las 25 bandas de Poweramp: eje de frecuencia logarítmico (20 Hz–20 kHz), eje de
  −12 a +12 dB con la línea de 0 dB resaltada, curva suave (Catmull-Rom → Bézier) con relleno
  degradado, puntos huecos para bandas estimadas y una banda resaltada.
- `POWERAMP_BAND_FREQUENCIES_HZ` en `export/`, y la escala en funciones puras con 6 pruebas
  (incluida la que confirma que las 25 bandas quedan espaciadas de forma pareja).

### 2026-10-30 · `af09bcd` · ui: mostrar componentes en pantalla de prueba
- Pantalla temporal con tarjeta, deslizador, chips y botones para revisarlos en el teléfono;
  se reemplaza por la navegación real más adelante en la Fase 1.

### 2026-10-30 · `0809fca` · ui: crear GlassSlider y formato de dB y Hz
- `GlassSlider`: rango −12 a +12 dB en pasos de 0.5 dB, perilla de 32 dp, 56 dp de alto,
  valor grande con dígitos tabulares y vibración ligera en cada paso. Usa el `thumb`
  personalizado de Material 3, que aún es API experimental (`@OptIn`).
- `formatDecibels` ("+1.5 dB", "−3.0 dB", sin "−0.0") y `formatHertz` ("843 Hz", "8.4 kHz",
  "20 kHz"), independientes del idioma del teléfono, con 7 pruebas unitarias.

### 2026-10-30 · `17b5319` · ui: crear GlassChip
- Chip de opción o, sin `onClick`, de estado. Tono ámbar solo para avisos de seguridad.
  Se ve de 40 dp pero el área táctil es de 48 dp; el texto nunca se parte en dos líneas.

### 2026-10-30 · `70ea842` · ui: crear GlassButton con respuesta háptica
- Principal sólido en aqua (uno por pantalla) y secundario de vidrio. 56 dp de alto,
  vibración de confirmación al tocar y leve reducción de tamaño al presionar (200 ms),
  desactivada si el sistema pide reducir movimiento.

### 2026-10-30 · `dba0042` · ui: crear GlassCard con Haze
- Haze 2.0.1 (`haze` + `haze-blur`). La aurora es la fuente (`hazeSource`) y los
  componentes la desenfocan con `hazeBlur(HazeInput.Backdrop(...))`.
- `glassSurface`: desenfoque de 28 dp, velo blanco al 10 %, ruido 0.06, borde de 1 dp con
  degradado blanco, esquinas de 28 dp y sombra suave. Sin fuente o con
  `LocalGlassBlurEnabled = false` usa relleno blanco al 20 %.
- Haze 2 también trae un efecto "Glass"; queda como idea para probar más adelante.

### 2026-10-30 · `252be21` · ui: agregar fondo aurora animado
- Degradado azul noche → índigo → violeta con tres manchas (aqua, violeta, azul) que dan una
  vuelta cada 28 s. La animación se lee solo al dibujar, sin recomponer la pantalla.
- Si la escala de animación del sistema está en 0, la aurora queda fija.

### 2026-10-30 · `9f2adb7` · ui: crear tema oscuro de Timbra
- Paleta: fondo `#0B1026`, acento aqua `#6FE7DD`, secundario violeta `#B79CFF`, ámbar y rojo
  solo para avisos. Tema siempre oscuro, sin colores dinámicos del sistema.
- Fuente del sistema con `withTabularNumbers()` para dB y Hz. Formas con esquinas de 28 dp y
  espaciados de 4/8/16/24/32 dp.
- Ventana con fondo azul noche (sin destello blanco al abrir) y barras del sistema con iconos
  claros. Se quitaron los restos de la plantilla que marcaba lint.

### 2026-10-29 · `7e46e84` · chore: crear proyecto Android base
- Proyecto creado con la plantilla *Empty Activity* (Compose) de Android Studio Rabbit 2
  (2026.2.2): paquete `com.juliancitou.timbra`, `minSdk 33`, `compileSdk`/`targetSdk 37`
  (Android 17), `versionName 0.1.0`, `versionCode 1`.
- Versiones actualizadas a la última estable: Gradle 9.8.2 (con SHA-256 verificado),
  AGP 9.5.0, Kotlin 2.4.21, core-ktx 1.19.1, lifecycle 2.11.0, activity-compose 1.13.0,
  Compose BOM 2026.09.00, androidx.test junit 1.3.0, espresso 3.7.0.
- AGP 9 trae Kotlin 2.2.10 integrado; se declara `org.jetbrains.kotlin.android` con
  `apply false` en la raíz para subirlo a 2.4.21 (debe coincidir con el plugin de Compose).
- Código compilado para Java 17; Gradle corre con JDK 25 (`gradle-daemon-jvm.properties`).
- Verificado: `testDebugUnitTest`, `lintDebug` y `assembleDebug` pasan. Lint solo reporta
  avisos de la plantilla, que se limpian en la Fase 1.
- Instalada y abierta en el POCO X8 Pro Max (HyperOS 3.0, Android 16). En HyperOS hay que
  activar **Instalar vía USB** y **Depuración USB (ajustes de seguridad)**, y aceptar el aviso
  en pantalla; si no, la instalación falla con `INSTALL_FAILED_USER_RESTRICTED`.
- Pendiente de decidir: `android:allowBackup="true"` de la plantilla.

### 2026-10-29 · `63deedb` · ci: usar JDK 25 como Android Studio
- `android.yml` pasa de Temurin 21 a 25 para compilar con el mismo JDK que Android Studio
  Rabbit 2 y la terminal.

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
