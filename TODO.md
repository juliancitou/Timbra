# TODO — Timbra

Pendientes y metas, organizados por etapas.
Marca `[x]` al terminar. Lo relevante se documenta en `CHANGELOG.md` con fecha y commit.

**Meta final:** un preset de Poweramp basado en tu propia escucha, con la "S" y los platillos
controlados, bajos presentes sin tapar voces y medios claros, obtenido en una medición de menos de
15 minutos desde una app elegante y fácil de usar.

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
- [ ] Tag y release `v0.1.0` (el workflow adjunta el APK)

## Fase 1 — Diseño y navegación · `v0.2.0`

**Meta:** se ve elegante y fluido en el teléfono, con estilo liquid glass y buen contraste.

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
- [ ] Tag y release `v0.2.0`

## Fase 2 — Motor de audio seguro · `v0.3.0`

**Meta:** sonidos limpios, sin clics, siempre dentro de un nivel seguro.

- [ ] `PinkNoise`: generador de ruido rosa
- [ ] `Biquad`: filtro pasa-banda; dos en cascada para tercio de octava
- [ ] `BandNoise`: ruido por banda normalizado al mismo RMS
- [ ] `WarbleTone`: seno modulado ±1/6 de octava a 5 Hz (alternativa)
- [ ] `Fade`: entradas y salidas de 50 ms
- [ ] `SafetyLimiter`: −18 dBFS RMS, picos a −6 dBFS
- [ ] `ToneAlternator`: alterna referencia 1 kHz y banda de prueba (~0.8 s cada una)
- [ ] `AudioEngine` con `AudioTrack` (stream, PCM float, frecuencia nativa)
- [ ] Foco de audio y pausa al perderlo
- [ ] Pausa al desconectar audífonos (`ACTION_AUDIO_BECOMING_NOISY`)
- [ ] Paro automático a los 20 minutos y al salir de la pantalla
- [ ] Pruebas: frecuencia central y ancho de banda de filtros
- [ ] Pruebas: RMS normalizado entre bandas
- [ ] Pruebas: el limitador nunca supera el pico máximo
- [ ] Pruebas: fades sin discontinuidades
- [ ] Escuchar en los FreeBuds: sin clics, sin distorsión, volumen cómodo
- [ ] Tag y release `v0.3.0`

## Fase 3 — Flujo de medición · `v0.4.0`

**Meta:** completar una medición de principio a fin y poder retomarla.

- [ ] Lista de verificación de preparación (audífonos, ANC, Poweramp EQ apagado, silencio)
- [ ] Pantalla de nivel cómodo con aviso si cambia el volumen del teléfono
- [ ] Recorrido por las 25 bandas con progreso visible
- [ ] Explicación corta por banda ("aquí vive la S", "aquí está el golpe del bombo")
- [ ] Botones Repetir, Saltar y Suenan igual
- [ ] Opción de saltar bandas de 20–36 Hz
- [ ] Doble pasada opcional en orden distinto
- [ ] Pedir repetir bandas con diferencia > 3 dB entre pasadas
- [ ] Guardar progreso y retomar al volver
- [ ] Persistir sesiones con `kotlinx.serialization`
- [ ] Probar una medición completa en el teléfono (8–12 min)
- [ ] Tag y release `v0.4.0`

## Fase 4 — Análisis y exportación · `v0.5.0`

**Meta:** el preset se importa en Poweramp y suena mejor que los presets hechos a mano.

- [ ] Tabla ISO 226:2023 a ~60 fonios interpolada en las 25 frecuencias
- [ ] Curva de desviación `D = C − g`
- [ ] Separar tendencia (suavizado ~1 octava) y detalle (picos/valles)
- [ ] Detección de picos y su descripción en lenguaje claro
- [ ] Marcar bandas confiables y estimadas
- [ ] Controles de gusto: Graves, Presencia de voz, Brillo
- [ ] Construir preset: corrección ±6 dB + gusto + tendencia atenuada
- [ ] Redondeo a 0.5 dB, rango −8 a +3 dB, cálculo de preamp
- [ ] `PowerampExporter`: 2 bandas de tono en 0 dB + 25 bandas gráficas
- [ ] Prueba golden del JSON en `app/src/test/resources/`
- [ ] Guardar con el selector de archivos y compartir
- [ ] Instrucciones de importación en la app
- [ ] Importar en Poweramp y comparar contra el preset v5
- [ ] Tag y release `v0.5.0`

## Fase 5 — Historial y pulido · `v1.0.0`

**Meta:** app completa, estable y agradable de usar a diario.

- [ ] Historial de mediciones
- [ ] Comparar dos curvas en la misma gráfica
- [ ] Volver a exportar presets anteriores
- [ ] Renombrar y borrar mediciones
- [ ] Animaciones y microinteracciones finales
- [ ] Revisión completa de textos
- [ ] Ícono de la app
- [ ] Pantalla Acerca de con versión
- [ ] Tag y release `v1.0.0`

## Fase 6 (opcional) — Analizador de espectro

- [ ] Investigar si Spotify permite `AudioPlaybackCapture`
- [ ] Servicio en primer plano solo mientras el analizador corre
- [ ] FFT y visualización en tiempo real con estilo de vidrio
- [ ] Permisos explicados antes de pedirlos

## Fase 7 (opcional) — Datos objetivos

- [ ] Importar curva medida (CSV de squig.link)
- [ ] Combinar curva objetiva con la medición personal
- [ ] Perfiles por modo de ANC y por códec (AAC / LDAC)

---

## Ideas y deuda técnica

- [ ] Decidir `android:allowBackup` (la plantilla lo deja en `true`: respaldo en la nube de Android)
- [ ] Evaluar bibliotecas de "liquid glass" adicionales a Haze
- [ ] Probar el efecto Glass de Haze 2 como alternativa al desenfoque clásico
- [ ] Widget o acceso rápido para exportar el último preset
- [ ] Modo claro opcional
