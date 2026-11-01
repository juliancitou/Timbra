# TODO — Timbra

Pendientes y metas, organizados por etapas.
Marca `[x]` al terminar. Lo relevante se documenta en `CHANGELOG.md` con fecha y commit.

**Meta final:** que cualquier persona, con cualquier audífono, encuentre jugando la ecualización
que más le gusta y la exporte a Poweramp; y que quien quiera más precisión tenga un modo avanzado
que mida banda por banda. Todo desde una app elegante y fácil de usar.

---

## Fase 0 — Base del proyecto · `v0.1.0`

**Meta:** la app vacía se instala en el POCO y el repositorio compila solo en GitHub.

- [x] Crear repositorio `juliancitou/Timbra` en GitHub
- [x] Agregar `README.md`, `CHANGELOG.md`, `TODO.md`
- [x] Plan de trabajo y preset de referencia
- [x] `.gitignore`, `.gitattributes`, `.editorconfig`
- [x] Workflow `android.yml`: pruebas, lint y APK en cada push; APK adjunto al publicar release
- [x] Instalar Android Studio, SDK más reciente y Platform-Tools
- [x] Instalar GitHub CLI e iniciar sesión
- [x] Verificar identidad de Git (`user.name`, `user.email`) de tu cuenta
- [x] Crear proyecto Empty Activity (Compose) dentro de la carpeta `timbra`
  - [x] Paquete `com.juliancitou.timbra`, `minSdk 33`
  - [x] `versionName = "0.1.0"`, `versionCode = 1`
  - [x] Conservar el `.gitignore` del repositorio (si Android Studio crea otro en la raíz, fusionarlos)
- [x] Ordenar dependencias en `gradle/libs.versions.toml` con versiones estables actuales
- [x] Acciones de `android.yml` en su versión mayor más reciente
- [x] JDK 25 igual en Android Studio, terminal y `android.yml`
- [x] Verificar la autoría de los commits
- [x] Activar Depuración USB e Instalar vía USB en el teléfono
- [x] Instalar y abrir la app vacía en el POCO
- [x] Confirmar que el workflow pasa en verde en GitHub
- [x] Tag y release `v0.1.0` (el workflow adjunta el APK)

## Fase 1 — Diseño y navegación · `v0.2.0`

**Meta:** se ve elegante y fluido en el teléfono, con buen contraste (empezó como liquid glass y
terminó en estilo terminal/ASCII).

### Tema
- [x] Paleta: negro y grises, acento ámbar, secundario naranja, rojo solo para avisos
- [x] Tipografía monoespaciada Cascadia Mono con números tabulares para dB y Hz
- [x] Formas: esquinas de 28 dp, espaciados consistentes
- [x] Tema oscuro como principal
- [x] Quitar restos de la plantilla (colores sin usar, etiqueta repetida, chequeo de SDK innecesario)

### Componentes de vidrio (`ui/theme/`)
- [x] `GlassBackground`: degradado + 2–3 manchas aurora con movimiento lento
- [x] Desactivar animación si el sistema pide reducir movimiento
- [x] Integrar Haze para desenfoque de fondo
- [x] `GlassCard`: relleno 8–14 %, borde con degradado, sombra suave
- [x] `GlassButton` (principal y secundario) con respuesta háptica
- [x] `GlassSlider`: grande, cómodo con el pulgar, valor en dB visible
- [x] `GlassChip` para opciones y estados
- [x] `FrequencyChart`: curva sobre las 25 bandas, ejes en Hz y dB
- [x] Respaldo sin desenfoque (relleno al 20 %)

### Navegación y pantallas de ejemplo
- [x] Navigation Compose con transiciones de zoom de 200–350 ms y atrás predictivo
- [x] Inicio
- [x] Preparación
- [x] Nivel cómodo
- [x] Medición
- [x] Resultados
- [x] Ajuste de gusto
- [x] Exportar
- [x] Historial
- [x] Ajustes
- [x] Todos los textos en `strings.xml` (español de México)

### Revisión
- [x] Contraste AA en todos los textos (verificado con `ContrastTest`)
- [x] Objetivos táctiles ≥ 48 dp y `contentDescription` en iconos
- [x] Probar con texto grande del sistema
- [x] Probar fluidez en el POCO (sin tirones al desenfocar)
- [x] Tag y release `v0.2.0`

## Fase 2 — Motor de audio seguro y ecualizador en vivo · `v0.3.0`

**Meta:** sonido limpio, siempre dentro de un nivel seguro, con ecualización en vivo sin clics.

- [x] `SafetyLimiter`: −18 dBFS RMS, picos a −6 dBFS
- [x] `Fade`: entradas y salidas de 50 ms
- [x] Fundido cruzado ≤ 50 ms al cambiar de ecualización
- [x] `Biquad`: pico, pasa-banda y estantes
- [x] `GraphicEqualizer`: 25 bandas que imitan a Poweramp + preamp
- [x] `LoudnessMatcher`: igualar volumen entre ecualizaciones (ponderación K)
- [x] `AudioOutput` con `AudioTrack` (stream, PCM float, frecuencia nativa)
- [x] Foco de audio y pausa al perderlo
- [x] Pausa al desconectar audífonos (`ACTION_AUDIO_BECOMING_NOISY`)
- [x] Paro automático a los 20 minutos y al salir de la pantalla
- [x] Pruebas: respuesta en frecuencia de los filtros y del ecualizador
- [x] Pruebas: el limitador nunca supera el pico máximo
- [x] Pruebas: fades y fundidos sin discontinuidades
- [x] Pruebas: igualación de volumen entre ecualizaciones
- [x] Pantalla temporal "Prueba de audio" en Ajustes
- [x] Escuchar en los audífonos: sin clics, sin distorsión, volumen cómodo
- [x] Tag y release `v0.3.0`

## Fase 3 — Música generada · `v0.4.0`

**Meta:** un bucle musical sintetizado por la app, agradable y que cubra graves, medios y agudos.

- [x] Bombo y bajo
- [x] Acordes / pad
- [x] "Voz" sintética con formantes
- [x] Hi-hats, platillos y sonido de "s"
- [x] Bucle de 8–16 s sin cortes
- [x] Variantes por ronda que resaltan cada zona
- [x] Pruebas: cubre las 25 bandas y no satura
- [x] Bajo pulsado con sub-grave (más definición)
- [x] Seis géneros: pop, rock, clásica, electrónica, hip-hop y jazz
- [x] 14 ecualizaciones de prueba hechas con curvas suaves (`EqualizerShapes`)
- [x] Escuchar en los audífonos: pop suena musical
- [x] Escuchar los seis géneros y ajustar lo que no convenza
- [ ] Más géneros a futuro (mariachi, blues, balada acústica…)
- [ ] Tag y release `v0.4.0`

## Fase 4 — Test de esferas y exportación · `v0.5.0`

**Meta:** encontrar tu ecualización jugando con esferas y exportarla a Poweramp.

- [x] Rondas: balance, graves, claridad, voz, "S" y platillos, aire
- [x] Pasada de afinado con pasos más chicos alrededor de lo elegido
- [x] Pantalla de ronda con esferas y cambio de sonido instantáneo
- [x] Repetir ronda y volver a la anterior
- [x] Resultado: gráfica y comparar con/sin ecualización
- [x] Redondeo a 0.5 dB, rango −8 a +3 dB, cálculo de preamp
- [x] `PowerampExporter`: 2 bandas de tono en 0 dB + 25 bandas gráficas
- [x] Prueba golden del JSON en `app/src/test/resources/`
- [x] Guardar con el selector de archivos y compartir
- [x] Instrucciones de importación en la app
- [x] Perfil de audífono básico (nombre)

### Salida de audio
- [x] Elegir la salida: Bluetooth, cable o bocina del teléfono
- [x] Detectar la salida conectada (sin permisos) y avisar si no coincide con la elegida
- [x] Bluetooth: punto de partida pensado en los HUAWEI FreeBuds Pro 4
- [x] Cable: punto de partida plano, válido para cualquier audífono
- [x] Bocina: recorte de graves, rangos propios, sin ronda de aire y preparación distinta
- [x] Nombre del preset con la salida: `Timbra - FreeBuds Pro 4 (Bluetooth)`
- [x] Instrucciones para asignar el preset a la salida en Poweramp

### Bandas de Poweramp
- [x] `BandLayout`: personalizada (N de 5 a 32, inicio y fin)
- [ ] Opciones fijas (5, 10, 12, 15, 16, 24, 31, 32): falta confirmar sus frecuencias en Poweramp
- [x] Elegir la configuración de bandas en la app (por defecto: 25 bandas, 20 Hz–20 kHz)
- [x] Exportar y previsualizar con la configuración elegida (Q según la separación de bandas)
- [x] Gráfica y textos con cualquier cantidad de bandas
- [x] Prueba golden de una configuración personalizada (10 bandas)

### Test para quien no sabe de audio
- [x] "No me gustó ninguno": la zona se queda como estaba y se pasa a la siguiente
- [x] Probar con una canción guardada en el teléfono, en estéreo
- [x] Nodos sin dB, con la opción "actual" marcada, y resumen en palabras del resultado
- [x] Botones y opciones con bordes claros; Inicio con el flujo actual
- [ ] Probar una canción real en el teléfono: MP3, FLAC y un archivo que no se pueda leer
- [x] Ecualizar Spotify en vivo durante el test
- [ ] Comprobar en el teléfono que Spotify conecta y que el cambio se oye, con Poweramp Equalizer apagado
- [x] Mostrar en el resultado lo que suena ahora: medido en el audio de la app o leído del efecto en Spotify
- [x] Afinar los graves del efecto en Spotify (bloque de ~85 ms en vez de 10 ms)
- [x] Recordar el ecualizador del teléfono y abrir los ajustes de sonido en "Antes de empezar"
- [ ] Comprobar en el teléfono que "Sin ecualizar" y "Con ecualización" se oyen distintos y que el resultado confirma las bandas
- [x] "Antes de empezar" marca solo Poweramp y el ruido; botón para apagar Poweramp sin salir de la app
- [ ] Comprobar en el teléfono que Timbra distingue Poweramp encendido y apagado con Spotify sonando
- [ ] Comprobar que "Apagar Poweramp" lo apaga y que vuelve a encenderse al exportar o volver a Inicio
- [ ] Ajustar el umbral de ruido (`QUIET_ROOM_MAX_DBFS`) con el micrófono real
- [ ] Comprobar que Poweramp vuelve a sus ajustes después del test
- [x] Pasos más grandes entre opciones y graves de bocina donde sí se oyen
- [ ] Comprobar en el teléfono que cada ronda se distingue, sobre todo Aire, Claridad y las de afinar
- [x] Vista previa en modo Envolvente, como Poweramp, y elegible en Calibrar
- [x] Ajustes recomendados de Poweramp en Exportar
- [ ] Comprobar de oído que Timbra y Poweramp suenan igual con los ajustes recomendados
- [x] Leer los ajustes de Poweramp desde Timbra y copiar bandas y superposición
- [x] Pantalla de ecualizador por celdas y resumen de lo que se añadió o quitó
- [x] Avisos de ajustes de Poweramp también en "Antes de empezar"
- [ ] Revisar en el teléfono la pantalla corregida, el modo A y el final del resultado
- [ ] Confirmar qué interruptor representan `dsp_border_gain` y `peq_enable` cambiándolos en Poweramp
- [ ] Comparar lo que se oye en vivo con el preset importado en Poweramp (Ajustes → Calibrar)
- [x] Vista previa con la curva completa del preset y forma elegible al calibrar
- [x] Mantener presionado para oír como estaba y comprobación final de graves y voz
- [ ] Probar con FreeBuds y con audífonos de cable
- [ ] Revisar en el teléfono las pantallas nuevas: Nivel cómodo, rondas, resultado y Calibrar

### Verificación
- [ ] Importar en Poweramp y comparar con la vista previa
- [ ] Calibrar el ancho de banda (`POWERAMP_BAND_Q`) si la vista previa no suena igual que Poweramp
- [ ] Probar el test completo en el teléfono (12 rondas, 10 en bocina; resultado y exportación)
- [ ] Comprobar en el teléfono la detección de salida y el nombre de los audífonos
- [ ] Importar en Poweramp un preset con otra cantidad de bandas
- [ ] Tag y release `v0.5.0`

## Fase 5 — Modo avanzado: medición por bandas · `v0.6.0`

**Meta:** encontrar picos angostos (como la "S") y combinarlos con el resultado del test.

- [x] `PinkNoise` (adelantado en la Fase 2)
- [ ] `BandNoise` (tercio de octava, mismo RMS) y `WarbleTone`
- [ ] `ToneAlternator`: referencia 1 kHz y banda de prueba (~0.8 s cada una)
- [ ] Preparación y nivel cómodo con aviso si cambia el volumen del teléfono
- [ ] Recorrido por las 25 bandas con progreso y explicación por zona
- [ ] Repetir, saltar y doble pasada; repetir bandas con diferencia > 3 dB
- [ ] Guardar progreso y retomar (`kotlinx.serialization`)
- [ ] Tabla ISO 226:2023 a ~60 fonios en las 25 frecuencias
- [ ] Curva de desviación, tendencia y detalle, detección de picos
- [ ] Marcar bandas confiables y estimadas
- [ ] Combinar la corrección con el resultado del test de esferas
- [ ] Probar una medición completa en el teléfono
- [ ] Tag y release `v0.6.0`

## Fase 6 — Mis ecualizaciones, perfiles y pulido · `v1.0.0`

**Meta:** app completa, estable y agradable de usar a diario con cualquier audífono.

- [ ] Perfiles por audífono (nombre detectado o escrito, códec opcional)
- [ ] Historial por perfil
- [ ] Comparar dos curvas en la misma gráfica
- [ ] Volver a exportar presets anteriores
- [ ] Renombrar y borrar
- [ ] Animaciones y microinteracciones finales
- [ ] Revisión completa de textos
- [x] Agradecimientos, licencias y contacto dentro de la app (Ajustes → Más información)
- [x] Ícono de la app (adelantado: logo del usuario en `art/timbra_logo.svg`)
- [ ] Tag y release `v1.0.0`

## Fase 7 (opcional) — Analizador de espectro

- [ ] Investigar si Spotify permite `AudioPlaybackCapture`
- [ ] Servicio en primer plano solo mientras el analizador corre
- [ ] FFT y visualización en tiempo real
- [ ] Permisos explicados antes de pedirlos

## Fase 8 (opcional) — Datos objetivos

- [ ] Importar curva medida (CSV de squig.link) por modelo de audífono
- [ ] Combinar curva objetiva con la preferencia personal

---

## Ideas y deuda técnica

- [ ] Widget o acceso rápido para exportar el último preset
- [ ] Modo claro opcional
