# Timbra

App para Android que mide **cómo escuchas tú** cada frecuencia con tus audífonos y genera un
preset de ecualizador para **Poweramp Equalizer** hecho a tu medida.

La idea es sencilla: la app te pone dos sonidos, uno de referencia y otro de prueba, y tú mueves
un control hasta que los dos suenen igual de fuertes. Repite esto en 25 bandas, desde los graves
más profundos hasta los agudos más altos, y con tus respuestas calcula una ecualización que
corrige lo que a ti te sobra o te falta (por ejemplo, una "S" que pica o un bajo que tapa las voces).

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
| Encuentra picos y huecos angostos en cómo te llegan las frecuencias (sibilancia, resonancias) | Medir el volumen real dentro de tu oído (para eso hace falta un micrófono de medición) |
| Compara las bandas entre sí según tu percepción | Cambiar ajustes de tu teléfono, de tus audífonos o de Poweramp |
| Genera un archivo `.json` que importas en Poweramp | Conectarse a internet ni pedir cuentas |
| Te deja ajustar a tu gusto cuánto grave, voz y brillo quieres | Reproducir sonido fuera de la pantalla de medición |

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
| Audífonos | Bluetooth o de cable. Mide siempre con los mismos con los que escuchas música |
| Poweramp Equalizer | Disponible en Google Play. Es donde se importa el preset que genera Timbra |
| Tiempo y silencio | Unos 10–15 minutos en un lugar callado, de preferencia descansado |

La app **no pide permisos especiales** y **no usa internet**.

### Dispositivos probados

| Tipo | Modelo | Sistema / detalle |
|---|---|---|
| Teléfono | POCO X8 Pro Max | HyperOS 3.0, Android 16 |
| Audífonos | HUAWEI FreeBuds Pro 4 | Bluetooth, códec AAC |

Debería funcionar en cualquier teléfono con Android 13 o superior, pero solo se ha probado en el
equipo de la tabla. La medición es personal: el resultado vale para **tu** oído con **esos**
audífonos. Si cambias de audífonos, mide de nuevo.

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

1. **Prepara:** conecta tus audífonos, usa el mismo modo de cancelación de ruido con el que
   escuchas música y **apaga el ecualizador de Poweramp** mientras mides.
2. **Nivel cómodo:** suena un sonido de referencia. Ajusta el volumen del teléfono hasta que se
   oiga claro pero cómodo, nunca fuerte.
3. **Igualar bandas:** para cada una de las 25 bandas oirás alternarse la referencia y el sonido
   de prueba. Mueve el control hasta que los dos suenen igual de fuertes y toca **Suenan igual**.
   Puedes repetir o saltar cualquier banda, y pausar cuando quieras.
4. **Resultados:** verás tu curva y, en palabras simples, dónde están tus picos
   ("aquí está la S").
5. **Ajusta a tu gusto:** tres controles: Graves, Presencia de voz y Brillo.
6. **Exporta:** guarda o comparte el archivo `.json`.
7. **Importa en Poweramp Equalizer:** abre la lista de presets de Poweramp Equalizer, usa la
   opción de importar y elige el archivo. La app muestra los pasos exactos al exportar.

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
└── app/                 Código de la app (Kotlin + Jetpack Compose)
```

## Versiones

- Se usa versionado semántico: `0.1.0`, `0.2.0`, … Cada etapa terminada es una versión nueva;
  la `1.0.0` será la primera versión completa.
- Cada versión publicada incluye el APK listo para instalar en
  [Releases](https://github.com/juliancitou/Timbra/releases).
- El detalle de cada cambio está en [`CHANGELOG.md`](CHANGELOG.md).

## Desinstalar

Como cualquier app: mantén presionado el ícono de **Timbra** → **Desinstalar**.
No deja cambios en el sistema.
