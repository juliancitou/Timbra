# Timbra

App para Android que te ayuda a encontrar **la ecualización que mejor te suena** con
**cualquier audífono**, y la guarda como preset para **Poweramp Equalizer**.

La idea es sencilla y se siente como un juego: suena música y en pantalla ves varias esferas.
Cada esfera es una ecualización distinta y, al tocarla, el sonido cambia al instante. Ronda por
ronda (graves, voces, la "S", el brillo…) eliges la que más te gusta, y al final tienes tu
ecualización lista para exportar.

Para quien quiera más precisión hay un **modo avanzado** que mide cómo escuchas cada una de las
25 bandas y encuentra picos que molestan, como una "S" que pica.

- Repositorio: <https://github.com/juliancitou/Timbra>
- Pendientes y avance: [`TODO.md`](TODO.md)
- Historial de cambios: [`CHANGELOG.md`](CHANGELOG.md)

---

## Estado del proyecto

🚧 **En desarrollo.** Todavía no hay una versión que se pueda usar. El avance por etapas está en
[`TODO.md`](TODO.md). Cuando exista la primera versión publicada aparecerá en la sección
[Releases](https://github.com/juliancitou/Timbra/releases).

---

## Qué hace y qué no

| Sí hace | No hace |
|---|---|
| Te deja elegir de oído, con esferas, cuánto grave, voz y brillo quieres | Medir el volumen real dentro de tu oído (para eso hace falta un micrófono de medición) |
| Funciona con audífonos Bluetooth, de cable y con la bocina del teléfono | Ecualizar Spotify u otras apps fuera del test: eso lo hace Poweramp con el preset |
| En modo avanzado, encuentra picos angostos (sibilancia, resonancias) | Cambiar ajustes de tu teléfono, de tus audífonos o de Poweramp |
| Genera un archivo `.json` con la misma cantidad de bandas que usas en Poweramp | Conectarse a internet ni pedir cuentas |

**Términos que verás en la app:**

- **Hz (hercios):** qué tan grave o agudo es un sonido. 20 Hz es un grave muy profundo;
  1 000 Hz (1 kHz) está en medio de la voz; 8 000 Hz (8 kHz) es donde suele vivir la "S".
- **dB (decibeles):** cuánto se sube o se baja una banda. +3 dB se nota claramente;
  −6 dB es un recorte fuerte.
- **Preset:** un conjunto de ajustes del ecualizador guardado en un archivo.

---

## Requisitos para usar la app

| Necesitas | Detalle |
|---|---|
| Teléfono Android | **Android 13 o más reciente** |
| Audífonos | Cualquiera, Bluetooth o de cable. Haz el test con los mismos con los que escuchas música |
| Poweramp Equalizer | Disponible en Google Play. Es donde se importa el preset que genera Timbra |
| Tiempo y silencio | Unos 5–8 minutos para el test (10–15 para el modo avanzado) en un lugar callado |

La app **no usa internet**. Solo pide el micrófono si decides medir el ruido del lugar.

### Dispositivos probados

| Tipo | Modelo | Sistema / detalle |
|---|---|---|
| Teléfono | POCO X8 Pro Max | HyperOS 3.0, Android 16 |
| Audífonos | HUAWEI FreeBuds Pro 4 | Bluetooth, códec AAC |

Debería funcionar en cualquier teléfono con Android 13 o superior y con cualquier audífono, pero
solo se ha probado con el equipo de la tabla. El resultado es personal: vale para **tu** oído con
**esos** audífonos. Si cambias de audífonos, haz el test otra vez; la app guarda una ecualización
por cada par de audífonos.

---

## Instalar la app

Hay dos caminos. El primero es el más fácil.

### Opción A: descargar el APK (recomendada)

> Disponible cuando se publique la primera versión.

1. Desde el teléfono, abre la sección
   [Releases](https://github.com/juliancitou/Timbra/releases) del repositorio.
2. En la versión más reciente, toca el archivo `timbra-vX.Y.Z.apk` para descargarlo.
3. Abre el archivo descargado. Android te pedirá permiso para instalar apps de esa fuente
   (el navegador o el gestor de archivos): actívalo solo para esta instalación.
4. Si aparece un aviso de Google Play Protect, es porque la app no viene de Play Store.
   Elige **Instalar de todos modos**.
5. Abre **Timbra** desde tu lista de apps.

### Opción B: compilarla desde el código

Para quien quiera construir la app en su propia computadora.

#### Computadora compatible

Requisitos de Android Studio, la herramienta oficial para crear apps de Android:

| Sistema | Versión mínima | Notas |
|---|---|---|
| Windows | Windows 10 de 64 bits | No funciona en equipos con procesador ARM |
| macOS | macOS 12 | Se recomienda Apple Silicon (M1 o posterior); el soporte para Intel se está retirando |
| Linux | Cualquier distribución de 64 bits con GNOME, KDE o Unity y glibc 2.31 o superior | No funciona en equipos con procesador ARM. El proyecto se desarrolla en Ubuntu 24.04 |

| Recurso | Mínimo | Recomendado |
|---|---|---|
| Memoria RAM | 8 GB | 16 GB o más |
| Espacio libre | 8 GB | 16 GB o más en disco SSD |
| Pantalla | 1280 × 800 | 1920 × 1080 |

No necesitas el emulador: la app se prueba directo en el teléfono, porque el sonido tiene que
salir por tus audífonos reales.

#### Programas necesarios

1. **Android Studio**, versión estable más reciente (Rabbit 2 · 2026.2.2 o posterior):
   <https://developer.android.com/studio>. Ya incluye el JDK (Java) que necesita el proyecto.
2. Dentro de Android Studio, en **Settings → Languages & Frameworks → Android SDK**, instala:
   - **SDK Platforms:** Android 17.0 (API 37).
   - **SDK Tools:** Android SDK Build-Tools 37 y Android SDK Platform-Tools.
3. **Git**: <https://git-scm.com>.

#### Pasos

1. **Descarga el código.** En una terminal:
   ```bash
   git clone https://github.com/juliancitou/Timbra.git
   ```
2. **Ábrelo en Android Studio:** *Open* → elige la carpeta `Timbra`. Espera a que termine la
   sincronización de Gradle (barra de progreso abajo a la derecha). La primera vez descarga
   bastantes archivos y puede tardar varios minutos.
3. **Prepara el teléfono** (solo una vez):
   1. **Ajustes → Acerca del teléfono** → toca 7 veces el número de versión
      (en Xiaomi/POCO es "Versión de HyperOS") hasta que diga que ya eres desarrollador.
   2. **Ajustes → Ajustes adicionales → Opciones de desarrollador** → activa
      **Depuración USB**. En Xiaomi/POCO activa también **Instalar vía USB**
      (puede pedir tarjeta SIM y haber iniciado sesión en tu cuenta Xiaomi).
   3. Conecta el teléfono con un cable USB **de datos**. Si pregunta para qué usar el USB,
      elige **Transferencia de archivos**.
   4. Acepta el aviso **"¿Permitir depuración USB?"** y marca *Permitir siempre desde esta
      computadora*.
4. **Instala la app:** en Android Studio elige tu teléfono en la lista de dispositivos (arriba)
   y pulsa **Run ▶**. La app se instala y se abre sola.

#### Comandos útiles

Desde la carpeta del proyecto (en Windows usa `gradlew.bat` en lugar de `./gradlew`):

```bash
./gradlew assembleDebug        # crea el APK en app/build/outputs/apk/debug/
./gradlew installDebug         # lo instala en el teléfono conectado
./gradlew testDebugUnitTest    # corre las pruebas
./gradlew lintDebug            # revisa problemas comunes del código
adb devices                    # muestra los teléfonos conectados
```

#### Si algo falla

| Problema | Solución |
|---|---|
| `adb devices` no muestra nada | Revisa que el cable sea de datos, que esté en *Transferencia de archivos* y que la Depuración USB siga activa |
| `adb devices` dice `unauthorized` | Mira la pantalla del teléfono y acepta el aviso de depuración |
| `adb devices` dice `no permissions` (Linux) | En Ubuntu/Debian: `sudo apt install android-sdk-platform-tools-common`, luego desconecta y vuelve a conectar |
| `adb: command not found` | Agrega `platform-tools` del SDK a tu `PATH` (en Linux suele estar en `~/Android/Sdk/platform-tools`) |
| Error `INSTALL_FAILED_USER_RESTRICTED` en Xiaomi/POCO | Activa **Instalar vía USB** en Opciones de desarrollador |

---

## Cómo se usa

> Flujo previsto; puede cambiar durante el desarrollo.

### Encuentra tu ecualización (modo principal)

1. **Prepara:** elige la salida (audífonos Bluetooth, de cable o la bocina del teléfono), cuántas
   bandas usas en Poweramp y **apaga el ecualizador de Poweramp** mientras haces el test.
2. **Música y nivel cómodo:** elige un género de la app o una canción guardada en tu teléfono (se
   usan sus primeros 2 minutos). También puedes dejar Spotify sonando y oír cada opción sobre tu
   música, en vivo (requiere apagar antes Poweramp Equalizer). Ajusta el volumen del teléfono hasta que se oiga
   claro pero cómodo, nunca fuerte.
3. **Rondas de esferas:** cada ronda se enfoca en algo: balance general, graves, claridad, voces,
   la "S" y los platillos, y el aire de los agudos. Toca las esferas para escuchar cada opción;
   todas suenan igual de fuertes, así que eliges solo por cómo suenan. Cuando una te guste, elígela;
   si ninguna te convence, pulsa **No me gustó ninguno** y esa parte se queda como estaba. Mantén
   presionado **Mantén para oír como estaba** para comparar. Al final se repiten graves y voz sin
   marcas, para comprobar tu elección.
4. **Tu resultado:** escúchalo completo y compáralo con "sin ecualizar".
5. **Exporta:** descarga o comparte el archivo `.json`.
6. **Importa en Poweramp Equalizer:** abre la lista de presets, usa la opción de importar y elige
   el archivo. Después **asígnalo a su salida** (⋮ → Guardar/Asignar), para que Poweramp lo use
   solo con esos audífonos o con la bocina. La app muestra los pasos exactos al exportar.

### Calibrar con Poweramp (opcional)

Desde Ajustes. Importa un preset de prueba en Poweramp, escúchalo con Spotify y elige en Timbra la
forma que suene igual. Así lo que oyes durante el test se parece más a lo que oirás en Poweramp.

### Modo avanzado (opcional)

Desde Ajustes. Para cada una de las 25 bandas oirás alternarse un sonido de referencia y uno de
prueba, y moverás un control hasta que suenen igual de fuertes. Con eso la app encuentra picos
angostos (por ejemplo, dónde está exactamente tu "S") y los corrige sobre el resultado del test.

---

## Uso seguro

Tu oído es lo más importante:

- La app **nunca reproduce sonido sola** al abrirse; siempre empieza cuando tú lo pides.
- Todo el sonido pasa por un limitador que impide niveles altos, y entra y sale de forma suave
  para evitar golpes.
- Se detiene sola tras 20 minutos de sonido continuo, al salir de la pantalla de medición o si
  se desconectan los audífonos.
- Antes de medir, **baja el volumen del teléfono** y súbelo poco a poco.
- Si algo suena fuerte o molesto, pausa: el botón siempre está a la vista.

## Privacidad

- Sin internet, sin cuentas, sin anuncios y sin analítica.
- Tus mediciones se guardan solo en el almacenamiento interno de la app.
- Para ecualizar Spotify en vivo, la app pide el permiso `MODIFY_AUDIO_SETTINGS`, que Android concede
  al instalar. No copia ni graba su audio, y lo suelta al salir del test.
- El micrófono es opcional: mide unos 2 segundos el nivel de ruido en "Antes de empezar". El audio no se
  guarda ni sale del teléfono.
- El botón "Apagar Poweramp" apaga Poweramp Equalizer con su API pública, y la app lo vuelve a encender
  al terminar el test. Si la app se cierra de golpe, puede quedarse apagado.
- Si eliges una canción tuya, solo se lee en memoria: no se copia, no se modifica ni sale del teléfono.
- Los presets se guardan donde tú elijas con el selector de archivos del sistema.
- Al desinstalar la app se borran todas sus mediciones.

---

## Estructura del repositorio

```
Timbra/
├── README.md            Esta guía
├── TODO.md              Pendientes por etapa
├── CHANGELOG.md         Historial detallado de cambios
├── .github/workflows/   Compilación automática: pruebas, revisión y APK
├── art/                 Logo original en SVG (fuente del ícono de la app)
└── app/                 Código de la app (Kotlin + Jetpack Compose)
```

## Versiones

- Se usa versionado semántico: `0.1.0`, `0.2.0`, … Cada etapa terminada es una versión nueva;
  la `1.0.0` será la primera versión completa.
- Cada versión publicada incluye el APK listo para instalar en
  [Releases](https://github.com/juliancitou/Timbra/releases).
- El detalle de cada cambio está en [`CHANGELOG.md`](CHANGELOG.md).

## Agradecimientos

Timbra existe gracias a herramientas, bibliotecas y conocimiento que otras personas compartieron.
Gracias a todas ellas.

También puedes verlos dentro de la app: **Ajustes → Más información**.

### Bibliotecas incluidas en la app

| Proyecto | Para qué se usa | Autor | Licencia |
|---|---|---|---|
| [Jetpack Compose](https://developer.android.com/compose) y Material 3 | Toda la interfaz | Google / AndroidX | Apache 2.0 |
| [Navigation Compose](https://developer.android.com/guide/navigation) | Navegación entre pantallas | Google / AndroidX | Apache 2.0 |
| [AndroidX Core, Activity y Lifecycle](https://developer.android.com/jetpack/androidx) | Base de la app, ViewModel y ciclo de vida | Google / AndroidX | Apache 2.0 |
| [Kotlin](https://kotlinlang.org), Coroutines y kotlinx.serialization | Lenguaje, tareas en segundo plano y rutas | JetBrains | Apache 2.0 |
| [Cascadia Mono](https://github.com/microsoft/cascadia-code) | Tipografía de toda la app (obtenida de [Google Fonts](https://github.com/google/fonts)) | Microsoft | SIL OFL 1.1 |

La licencia de Cascadia Mono va incluida dentro de la app, en `assets/licenses/`.

### Herramientas de desarrollo y pruebas

| Proyecto | Para qué se usa | Licencia |
|---|---|---|
| [Android Studio](https://developer.android.com/studio), Android SDK y Android Gradle Plugin | Crear, compilar y probar la app | Apache 2.0 y términos del SDK de Android |
| [Gradle](https://gradle.org) y [Foojay Toolchains](https://github.com/gradle/foojay-toolchains) | Compilación y descarga del JDK correcto | Apache 2.0 |
| [JUnit 4](https://junit.org/junit4/) | Pruebas unitarias | EPL 1.0 |
| [AndroidX Test y Espresso](https://developer.android.com/training/testing) | Pruebas en el dispositivo | Apache 2.0 |
| [GitHub Actions](https://github.com/features/actions): `checkout`, `setup-java`, `upload-artifact` y [`gradle/actions`](https://github.com/gradle/actions) | Compilación automática y APK de cada versión | MIT |
| [Eclipse Temurin](https://adoptium.net) | JDK de la compilación automática | GPLv2 con excepción de classpath |

### Conocimiento en el que se basa el audio

- **Audio EQ Cookbook** de Robert Bristow-Johnson: fórmulas de los filtros del ecualizador.
- **Digital Audio Resampling** de Julius O. Smith III: la interpolación sinc con la que una canción
  tuya se adapta a la frecuencia de tu teléfono (<https://ccrma.stanford.edu/~jos/resample/>).
- **Método de Welch** de Peter D. Welch (1967): cómo se mide el espectro para comparar el audio con
  y sin ecualizar (<https://doi.org/10.1109/TAU.1967.1161901>).
- **Poweramp API** de Maksim Petrov (licencia propia para software que se comunica con Poweramp): los comandos para apagar y volver a encender Poweramp
  Equalizer durante el test (<https://github.com/maxmpz/powerampapi>).
- **AutoEq** de Jaakko Pasanen (MIT): las mediciones de audífonos que Poweramp Equalizer incluye y que la
  app puede usar como punto de partida (<https://github.com/jaakkopasanen/AutoEq>).
- **ITU-R BS.1770**: la ponderación K con la que se iguala el volumen entre ecualizaciones.
- **Paul Kellet**: el filtro que genera el ruido rosa (publicado en musicdsp.org).
- **Manfred Schroeder** y **Freeverb** de Jezar at Dreampoint (dominio público): la estructura de
  la reverberación de la música del test.

## Desinstalar

Como cualquier app: mantén presionado el ícono de **Timbra** → **Desinstalar**.
No deja cambios en el sistema.
