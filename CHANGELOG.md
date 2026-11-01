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

### 2026-11-01 · `4fec879` · feat: escuchar la vista previa con la fuerza de Poweramp
- Con Spotify y el micrófono, en la bocina: un preset plano baja el nivel y sube ~1.4 dB arriba de 5 kHz; uno de
  mesetas de −6 dB da unos −2.5 dB relativos en Poweramp y −5.6 dB en Timbra. Poweramp aplica ~45 %.
- Ajustes → Calibrar importa un preset de medición con esas mesetas, lo activa, mide y guarda esa fracción. La vista
  previa suena con esa fuerza; el preset exportado no cambia. Se puede volver a oír el preset completo.
- La subida de agudos no se compensa, y la causa de la falta de profundidad sigue sin conocerse. Se midió en un solo
  teléfono, con AIDL 1. Como Poweramp no dice cuál era el preset activo, el usuario debe volver a elegirlo.
- `Importar en Poweramp` usa la acción oficial `IMPORT_PRESETS`: el `ACTION_VIEW` anterior abría Poweramp sin importar.
  Comprobado en el teléfono: aparece el aviso, se importa y Poweramp regresa a Timbra.

### 2026-11-01 · `f10008a` · docs: registrar medición con audífonos
- FreeBuds Pro 4 pegados al micrófono, con Spotify y el preset real de esos audífonos. Debajo de ~1.7 kHz la señal
  supera poco al ruido y repetir la pasada varió hasta 8.7 dB, así que esa zona no vale.
- Arriba de 2 kHz, Timbra sigue el preset con menos de 1 dB (salvo en 20 kHz). En el corte de −5.7 dB, Poweramp da −2.4 dB en AIDL 1
  y como mucho −4.3 dB en AIDL 2, sin seguir la curva.
- Es el mismo patrón que en la bocina. Aún no se compensa: con estos datos no se separa cuánto es falta de profundidad y
  cuánto subida de agudos. Falta medir un preset plano y uno de corte ancho.

### 2026-11-01 · `c870047` · docs: registrar mediciones de Poweramp
- Mediciones con micrófono en la bocina, con "Creep" en Spotify y el mismo tramo de 60 s en cada pasada. Repetir la
  pasada sin ecualizar dio 0.0 dB en casi todas las bandas (máximo −0.4), así que la medición es estable.
- Preset de cortes angostos de −8 dB: Timbra corta cerca de −7 dB y Poweramp (AIDL 1) cerca de −2 a −3 dB.
- Preset real "Timbra - Bocina": Timbra sigue el preset con ±1 dB. En Poweramp, con el nivel alineado a los medios, los
  −4 dB de graves salen cerca de −2 dB y los agudos suben unos 3.5 dB arriba de 3 kHz, aunque el preset los deja en 0.
- La causa no se conoce y solo se midió en la bocina. Aún no se compensa: falta medir con audífonos.

### 2026-10-31 · `657bc0a` · fix: mostrar cuánto corta Poweramp y no recomendar pipeline
- Mediciones con micrófono en la bocina del POCO X8 Pro Max, con un preset de cortes de −8 dB y ruido rosa:
  - Timbra: cortes de cerca de −7 dB, como se esperaba.
  - Poweramp en "Por defecto": sin cortes arriba de 1.7 kHz y de cerca de −2 dB debajo.
  - Poweramp en AIDL 1: los cinco cortes en su frecuencia, pero de cerca de −2 dB, con un ancho parecido al de Timbra.
  - Apagar DVC o pasar a Cascada no recupera la profundidad. Falta saber si ocurre también con Spotify.
- Se quitó el consejo de usar "Por defecto" en Modo Pipeline: en este teléfono empeora el resultado. Ya no se avisa
  sobre ese ajuste.
- La medición ya no dice que Poweramp no aplicó el preset cuando corta poco: muestra cuánto corta cada uno.

### 2026-10-31 · `9baa356` · fix: medir Poweramp con el micrófono
- En el teléfono del usuario, `Visualizer(0)` no notó ni el ecualizador de Timbra, así que se quitó.
- Ahora se graba el micrófono: silencio, ruido rosa solo, con Timbra y con Poweramp. Solo se compara la diferencia
  entre grabaciones, con lo que se cancelan el micrófono y la bocina. Las bandas que superan al ruido del cuarto por
  menos de 12 dB se descartan. Falta comprobar en el teléfono que funciona.
- El audio queda solo en memoria. Con audífonos, un auricular debe ir pegado al micrófono: es menos preciso.

### 2026-10-31 · `2f3cbed` · fix: comprobar el medidor antes de comparar Poweramp
- Las primeras mediciones dieron una línea casi plana y una "diferencia de 8.4 dB" engañosa: comparaban con la curva
  esperada sin saber si el medidor había visto algún efecto.
- Ahora suena ruido solo, luego con el ecualizador de Timbra y luego con el de Poweramp, y se comparan las dos
  mediciones. Si no se ve el recorte del ecualizador de Timbra, se avisa que el medidor no sirve en ese teléfono.
- Comprobado en el teléfono con sesiones en silencio: Poweramp se une a la sesión que Timbra le anuncia; el comando de
  apagar quita su efecto, el de encender lo recupera y al cerrar la sesión la suelta.
- La medición ya no apaga Poweramp, pero sí lo enciende si estaba apagado.

### 2026-10-31 · `3d18eb7` · feat: agregar prueba a ciegas y medición de Poweramp
- Prueba a ciegas desde el resultado: 5 intentos con dos opciones sin nombre. Con tan pocos intentos es una señal,
  no una prueba estadística.
- El resultado permite probar con otro género, o avisa que se puede cambiar de canción en Spotify.
- Ajustes → Calibrar puede medir Poweramp: ruido rosa con Poweramp encendido y apagado, comparando la salida con
  `Visualizer(0)`. Es tosco (8 bits, ~47 Hz por punto, solo arriba de ~100 Hz) y Android lo marca como obsoleto.
  Falta comprobar en el teléfono que funciona. Solo compara la forma, y Poweramp queda encendido al terminar.
- Sigue pendiente el modo paramétrico para resonancias: cambia el contrato del JSON y necesita antes una forma de
  encontrarlas.

### 2026-10-31 · `4ebd9f4` · feat: partir de AutoEq e importar desde Timbra
- Poweramp incluye mediciones de AutoEq, también de los FreeBuds Pro 4 con y sin cancelación de ruido. Timbra las
  lee de `eq_presets`; el formato binario se dedujo de sus presets y una prueba lo fija con un dato real.
- Si existe para los audífonos conectados, el test parte de esa curva, y se puede elegir partir sin ella. AutoEq
  sube hasta +6 dB y el preset no pasa de +3, así que la curva se baja entera y se conserva su forma.
- Preparación pide el control de ruido en uso, para elegir la medición y nombrar el preset.
- Exportar abre el archivo directamente en Poweramp y detecta cuando el preset ya está importado.

### 2026-10-31 · `7547049` · ui: mostrar la ecualización con una gráfica de líneas
- Las columnas de celdas resultaron confusas. `EqualizerDisplay` ahora dibuja una línea en 0 dB (sonido original)
  y una curva ámbar (lo activo), con relleno entre ambas. En "Sin ecualizar" la curva baja a 0 y el preset queda
  punteado.
- La línea muestra el preset o 0 dB según lo elegido. Lo medido o leído del efecto sigue en la lectura numérica y
  en el texto de comprobación.

### 2026-10-31 · `1b3ca27` · ui: mostrar la ecualización como pantalla de estudio
- `EqualizerDisplay`: columnas de celdas de 0.5 dB entre −8 y +3 dB, el rango de Poweramp. Las columnas muestran
  lo que está activo y una marca blanca, el preset. En "Sin ecualizar" las columnas se vacían y la marca se queda.
- Tocar o arrastrar elige una banda y muestra su frecuencia, el valor del preset y el valor actual.
- Debajo se indican las frecuencias y las zonas (graves, claridad, voz, "S", aire), con las que cambiaron en ámbar.
- El resultado dice en palabras qué se añadió, qué se quitó y qué quedó igual, respecto al sonido original.
  Sustituye la lista de bandas y el conteo de bandas cambiadas.
- "Antes de empezar" muestra los ajustes de Poweramp que conviene cambiar y los vuelve a leer al regresar a Timbra.
- Comillas de "S" y de los ajustes de Poweramp corregidas: Android las quitaba por no estar escapadas.

### 2026-10-31 · `fb629c6` · feat: copiar bandas y ajustes de Poweramp
- La llamada `preference` de la API de Poweramp Equalizer entrega sus ajustes sin pedir permisos (comprobado
  con build 1033). La propia API la marca como experimental: si faltan las claves, Timbra sigue como antes.
- Preparación y Calibrar copian las bandas personalizadas y la superposición. Con bandas fijas no se copian,
  porque sus frecuencias no están confirmadas, y se siguen eligiendo a mano.
- Exportar muestra solo lo que hay que cambiar; el tono y el compresor no se pueden leer y siempre se recuerdan.
- "Antes de empezar" da Poweramp por apagado si `peq_enable` está en falso.
- Solo lee: no escribe nada en Poweramp. Que `dsp_border_gain` sea "Ecualizador suave" y `peq_enable` sea
  "Habilitar procesamiento de audio" se dedujo por el nombre y los valores; falta confirmarlo.

### 2026-10-31 · `21d9f64` · audio: escuchar con la curva Envolvente de Poweramp
- Poweramp tiene "Superposición de bandas": en Envolvente la curva pasa por el valor de cada banda y en
  Cascada las bandas se suman. Timbra sumaba, así que con música de la app o una canción propia una zona
  ancha sonaba más del doble de fuerte (+3 dB daba ~+7 dB) que el preset en Envolvente.
- `GraphicEqualizer` resuelve ahora qué ganancia debe tener cada filtro para pasar por el valor de cada banda,
  con un sistema de ecuaciones y dos correcciones. Es el modo por defecto. La forma exacta entre bandas de
  Poweramp no está publicada: Timbra coincide en los centros de banda, no necesariamente entre ellos.
- Spotify usa también esa curva, en bandas de 1/6 de octava. Antes usaba escalones por banda.
- Calibrar ofrece Envolvente y Cascada en lugar de las cuatro formas anteriores.
- Exportar muestra los ajustes recomendados de Poweramp. El formato del preset no cambia.

### 2026-10-31 · `cdb1252` · audio: hacer audibles los cambios entre opciones
- Al calcular el preset ya redondeado, dos opciones vecinas de una ronda de afinado podían diferir 0.5 dB en
  solo 2 a 5 bandas, y eso casi no se oye.
- Pasos de la primera pasada: Balance 3 dB; Graves, Claridad, Voz y "S" 2 dB; Aire 2.5 dB. Afinar usa la mitad.
- Rangos ampliados para que quepan cinco opciones: Balance ±6, Claridad −6 a +2, Voz −5 a +3, Aire −7 a +3.
- Aire empieza en 9.5 kHz en vez de 11 kHz, porque casi no se oye arriba de 14 kHz.
- En bocina, Graves actúa desde 300 Hz y Claridad en 500 Hz, porque la bocina casi no reproduce lo que está
  más abajo. Son valores de referencia, no medidos en este teléfono.
- Una prueba fija cuánto debe cambiar el preset entre opciones vecinas, en cada ronda y salida. No sustituye
  la prueba de oído. El formato del preset no cambia.

### 2026-10-31 · `da41740` · feat: comprobar Poweramp y ruido antes de empezar
- **Poweramp:** su API pública puede encenderlo y apagarlo, pero no dice su estado. Para saberlo se intenta
  tomar `DynamicsProcessing` en la sesión de Spotify con prioridad 0: Android lo rechaza si otra app lo
  controla. Sin Spotify abierto queda como desconocido. Falta comprobar en el teléfono que apagarlo suelta
  el efecto; si no lo suelta, aparecería como encendido.
- **"Apagar Poweramp":** solo al tocarlo. Vuelve a encenderlo al llegar a Exportar o a Inicio, y solo si
  Timbra lo apagó. Si la app se cierra de golpe, puede quedarse apagado.
- **Ruido:** `RECORD_AUDIO` opcional, unos 2 segundos, solo el nivel. El umbral de −45 dBFS no está
  calibrado con un micrófono real.
- **No detectable, sigue a mano:** cancelación de ruido de los audífonos, ecualizador del teléfono, Dolby y
  posición de la bocina. Ni el fabricante ni Android dan una forma pública de leerlos.
- El nombre del dispositivo Bluetooth ya se detectaba; el sistema lo reporta como "HUAWEI FreeBuds Pro 4".

### 2026-10-31 · `7e5b38b` · feat: mostrar y comprobar la diferencia al ecualizar
- **Graves en Spotify:** `DynamicsProcessing` usaba su bloque por defecto de 10 ms (512 muestras a
  48 kHz, ~94 Hz por punto de la FFT, sin interpolar). Casi todas las bandas graves caían en el mismo
  punto. Ahora pide ~85 ms (4096 muestras, ~12 Hz por punto), a cambio de ~85 ms de retraso.
- **"Sin ecualizar" con menos o más de 25 bandas:** usaba una lista fija de 25 y obligaba a recrear el
  efecto en cada cambio. Ahora usa las bandas del preset.
- **Al salir del test** el efecto se deja plano antes de soltarlo: Poweramp comparte ese mismo efecto y
  podía quedarse con los últimos ajustes de Timbra. Falta comprobar en el teléfono que recupera los suyos.
- **Resultado:** la gráfica suma en naranja lo que suena ahora, sobre el preset en ámbar. Con audio de la
  app se procesa el mismo fragmento con y sin ecualizar por la cadena real y se mide cada banda con el
  método de Welch, descontando el preamp. En Spotify no se pueden leer sus muestras sin más permisos: se
  lee del efecto la ganancia de cada banda y se confirma que coincide, o se avisa en rojo. Eso prueba lo
  que el efecto tiene aplicado, no lo que sale por los audífonos.
- Resumen de bandas que cambian, el mayor cambio, aviso si es pequeño (menos de 1.5 dB) y lista por banda.
- **Ecualizador del teléfono:** una app no puede leerlo ni cambiarlo. "Antes de empezar" pide dejarlo en
  0 y abre los ajustes de sonido; no se comprueba que se haya hecho.

### 2026-10-31 · `e68a6be` · feat: calibrar con Poweramp y comprobar elecciones
- En el teléfono se vio que Poweramp Equalizer también usa `DynamicsProcessing` (prioridad 1337), y
  Android no deja leer sus bandas sin privilegios ni cambiar ajustes del sistema. Por eso se calibra
  de oído: Ajustes → Calibrar compara el mismo preset en Poweramp y en Timbra.
- Formas de vista previa: directa (una banda por banda del preset, la que se usa por defecto) y
  angosta, media y ancha, con la curva de campanas en bandas de 1/8 de octava.
- Solo se envían a `DynamicsProcessing` las bandas que cambian, para reducir llamadas entre procesos.
- "Mantén para oír como estaba" en las rondas. Al final se repiten graves y voz sin marcar la opción
  actual: queda el punto medio de las dos elecciones y, si difieren más de un paso, se avisa en el
  resultado. Las rondas pasan de 12 a 14, y de 10 a 12 en bocina.
- Preparación se divide en dos pantallas y Nivel cómodo muestra solo la fuente de música elegida.
- Limitaciones: la calibración es subjetiva, y falta probar todo esto en el teléfono y con audífonos.
- Riesgo: si Poweramp comparte el efecto con Timbra, sus ajustes podrían no volver solos después del
  test; apagarlo y encenderlo debería restaurarlos. Falta comprobarlo.

### 2026-10-31 · `a7e0082` · ui: restaurar botones ASCII y dar más espacio
- Pedido del usuario: volver a los botones de marco ASCII y a las opciones entre corchetes. A lo que
  se refería con más espacio era a ordenar los textos, no a cambiar los botones.
- `Spacing.section` entre secciones, `Spacing.inSection` dentro de ellas y `TerminalSection` para que
  la separación sea igual en toda la app.

### 2026-10-31 · `dd68a6d` · feat: ecualizar Spotify en vivo durante el test
- Pedido del usuario: dejar Spotify en segundo plano y oír cada opción sobre su música. Spotify
  anuncia su sesión de audio al reproducir y pausar; se comprobó en el teléfono que lo hace, y Timbra
  se registra para recibir esos anuncios. El efecto se aplica con `DynamicsProcessing`, sin copiar audio.
- Nuevo permiso `MODIFY_AUDIO_SETTINGS`, aprobado por el usuario; es de tipo normal y no pide nada.
- El nivel de `SafetyLimiter` no aplica a otra app. La regla ahí es no sonar más fuerte que sin
  ecualizar: ganancia de entrada ≤ 0 dB y limitador en −1 dB.
- Al no poder medir la música de Spotify, el volumen entre opciones se iguala con ruido rosa: es una
  aproximación. `DynamicsProcessing` tampoco filtra igual que Poweramp, así que lo que se oye puede
  diferir un poco del preset. Sus cambios no tienen fundido.
- Limitaciones: falta comprobar en el teléfono que el anuncio llegue a Timbra y que el cambio se oiga.
  Poweramp Equalizer debe estar apagado.

### 2026-10-31 · `999ada0` · ui: dar bordes claros a botones y opciones
- Pedido del usuario: que se note dónde empieza y dónde termina cada opción. El botón principal va
  relleno de ámbar con letra oscura, como video inverso; el secundario y las opciones llevan línea
  continua gris, y la opción elegida se rellena. Esquinas de 2 dp, casi rectas.
- Las etiquetas de estado conservan los corchetes, sin caja, para no parecer botones.
- Inicio describe el flujo actual, en vez de la medición por bandas, y deja de mostrar "Mis
  mediciones", que aún no guarda nada. La pantalla sigue en el código para la Fase 6.

### 2026-10-31 · `6f997fa` · feat: usar una canción propia y rechazar opciones
- Nivel cómodo permite elegir un archivo con el selector del sistema, sin permisos. Se usan sus
  primeros 2 minutos, en memoria; no se copia ni sale del teléfono. Una canción no se reinicia
  entre rondas: solo cambia la ecualización. No funciona con Spotify u otras apps.
- Botón "No me gustó ninguno". Los nodos muestran A–E y "actual" en vez de dB, y al entrar a una
  ronda se escucha de inicio lo que ya hay, también cerca de los límites, donde no es el centro.
- La gráfica sale de las rondas, para simplificarlas, y queda en el resultado junto a un resumen
  en palabras de lo que cambió.
- Nota: este commit no compila por sí solo porque `HomeScreen` se actualizó en el siguiente.

### 2026-10-31 · `13e9257` · audio: preparar canciones del usuario en estéreo
- La cadena de audio pasa a estéreo: un ecualizador por canal y limitador con ganancia común, para
  que limitar no mueva el sonido hacia un lado. Los límites de nivel no cambian.
- Las canciones se igualan a −24 LUFS en su parte más fuerte, porque suelen venir mucho más altas y
  el limitador las aplastaría. La compensación de volumen también se mide en esa parte.
- Remuestreo sinc con ventana de Blackman, porque uno lineal apagaría los agudos que se van a juzgar.
- Limitación: falta probar el decodificador con archivos reales en el teléfono.

### 2026-10-31 · `e47a371` · feat: dejar una zona como estaba si no gusta ninguna
- `keepCurrent()` conserva el valor de la zona. En la pasada gruesa también salta el afinado, porque
  no tiene sentido afinar algo que no gustó; en el afinado conserva lo elegido en la pasada gruesa.
- Con la música de la app, cada ronda resalta instrumentos distintos, así que la mezcla cambia
  aunque el ajuste no cambie. Con una canción propia eso no pasa.

### 2026-10-31 · `cde4081` · ui: elegir salida y bandas en la preparación
- Preparación con `[ Bluetooth ] [ Cable ] [ Bocina ]`, preseleccionada según lo detectado hasta
  que el usuario elija otra. Si no coincide, solo avisa y no bloquea, porque Android podría no
  reportar bien algún accesorio.
- La lista de preparación cambia con la salida: "audífonos conectados" desaparece porque ya se
  detecta, cable no pregunta por cancelación de ruido y bocina pide posición y bocina destapada.
- Editor plegable de bandas con los límites de Poweramp (5–32, 5–200 Hz, 14–20 kHz en pasos de
  100 Hz); se recuerda en `SharedPreferences`.
- Nombre sugerido con la salida: `Timbra - FreeBuds Pro 4 (Bluetooth)`. Exportar indica qué bandas
  usar y cómo asignar el preset a la salida (⋮ → Guardar/Asignar).
- Limitación: hay que comprobar en el teléfono la detección y el nombre del accesorio.

### 2026-10-31 · `afcd4f0` · audio: detectar la salida de audio activa
- `OutputDetector` pregunta a qué dispositivo iría la música (`getAudioDevicesForAttributes`) y
  vigila los cambios con `AudioDeviceCallback`. No requiere permisos y no cambia la ruta de audio.
- Si el sistema no responde, prefiere Bluetooth sobre cable y cable sobre bocina.

### 2026-10-31 · `02ec322` · feat: adaptar las rondas a la salida y a las bandas
- `OutputKind`: Bluetooth parte de la tendencia de los FreeBuds Pro 4 (graves −1, claridad −2,
  voz +1, "S" −3, aire −1 dB); cable parte de plano; bocina parte de graves −4 y voz −1 dB, con
  rangos propios y 10 rondas en vez de 12, sin la de aire.
- En bocina, la ronda de graves nunca los sube, pero un balance muy cálido aún puede levantarlos
  hasta unos 2 dB.
- Los puntos de partida son una estimación inicial; hay que confirmarlos escuchando.

### 2026-10-31 · `6649c6e` · audio: ecualizar y exportar con bandas configurables
- `BandLayout`: de 5 a 32 bandas, repartidas de forma logarítmica entre la primera y la última
  frecuencia. La configuración por defecto reproduce las 25 bandas de siempre.
- Vista previa con Q según la separación entre bandas, `Q ∝ √r/(r−1)`, para conservar el solape que
  tiene Q = 2 con 25 bandas. Es una aproximación: falta compararla con Poweramp.
- El golden de 25 bandas no cambió. El nuevo golden de 10 bandas fija el formato, pero no viene de
  un archivo de Poweramp: falta comprobar que las importe.
- Las opciones fijas (10, 15, 31…) quedan fuera hasta confirmar sus frecuencias; la de 8 bandas se
  quitó de las notas porque Poweramp no la ofrece.

### 2026-10-30 · `5a9315d` · docs: planear salidas de audio y bandas de Poweramp
- Pedido del usuario, agregado a la Fase 4 antes de la `v0.5.0`:
  - **Salida de audio:** Bluetooth (pensada en los HUAWEI FreeBuds Pro 4), cable (cualquier
    audífono) y bocina del teléfono, que se ecualiza distinto: recortar graves que no puede dar y
    suavizar 2–4 kHz. La app preselecciona la salida conectada y explica cómo asignar el preset a
    esa salida en Poweramp (⋮ → Guardar/Asignar).
  - **Bandas de Poweramp:** Poweramp deja elegir 5, 8, 10, 12, 15, 16, 24, 31 o 32 bandas, o una
    configuración personalizada de 5 a 32 con inicio y fin. Las 25 bandas actuales del usuario son
    la personalizada de 20 Hz a 20 kHz con reparto logarítmico, así que las personalizadas se pueden
    calcular. Las fijas se ofrecerán cuando se confirmen sus frecuencias en el Poweramp del usuario.
- El contrato del JSON deja de ser "siempre 25 bandas"; habrá una prueba golden por configuración.

### 2026-10-30 · `20c8d1d` · feat: agregar el test de esferas y la exportación
- Flujo real: Inicio → Preparación (lista y nombre de los audífonos) → Nivel cómodo (elegir
  género y volumen, con música real) → 12 rondas → Tu ecualización → Exportar.
- Ronda: pregunta clara ("¿Cuánto bajo te gusta?"), instrucciones como líneas de código, cinco
  nodos grandes con su valor y el sentido de cada extremo, la curva de la opción que suena y
  "Reproducir/Pausar" + "Elegir". Atrás vuelve a la ronda anterior.
- Cada opción suena con el volumen igualado (ponderación K sobre los primeros 4 s de la música) y
  es exactamente el preset que se exportaría; la música resalta la zona de la ronda.
- Resultado: curva final, comparar `[ Con ecualización ]` / `[ Sin ecualizar ]` con el volumen
  igualado, nombre del preset (por defecto, el de los audífonos) y "Repetir el test".
- Exportar: "Guardar archivo" con el selector del sistema (sin permisos de almacenamiento) y
  "Compartir" con una copia temporal vía `FileProvider` (solo la carpeta `cache/presets`).
- `EqualizerFinderViewModel` vive con la actividad, genera música y opciones en un hilo de baja
  prioridad y crea la salida de audio solo cuando hace falta. El sonido del test se detiene al
  salir de sus pantallas o al mandar la app al fondo.
- La medición por bandas de ejemplo queda en Ajustes → Modo avanzado (vista previa, sin sonido)
  hasta la Fase 5.

### 2026-10-30 · `ab958c1` · ui: agregar nodos de opción y campo de texto
- `OptionNodes`: cinco círculos de 60 dp en fila; el que suena se ilumina en ámbar con un punto
  que crece con rebote y un anillo que "respira" mientras hay sonido. TalkBack los trata como
  botones de opción con su valor en dB.
- `TerminalTextField`: campo `$ texto` con cursor ámbar y subrayado punteado.

### 2026-10-30 · `92d7a12` · feat: exportar presets de Poweramp con prueba golden
- `PowerampExporter.toJson()` genera el formato exacto de Poweramp: un objeto con dos bandas de
  tono en 0 dB y 25 gráficas, ganancias con un decimal, sin `-0.0` y nombre `Timbra - …` escapado.
- Prueba golden contra `app/src/test/resources/golden/poweramp-voces-v5.json`: es el preset v5
  del usuario, que ya se sabe que Poweramp importa, con el nombre cambiado. El exportador lo
  reproduce carácter por carácter.
- `fileName()` sugiere `timbra-mi-ecualizacion.json` (sin acentos ni espacios).

### 2026-10-30 · `ee017d6` · feat: agregar el motor de rondas del test
- `EqualizerFinder` (inmutable): 6 zonas (balance, graves, claridad, voz, "S" y aire), cada una
  con pasada gruesa y de afinado (paso a la mitad): 12 rondas. Cinco opciones centradas en la
  elección actual que se recorren hacia dentro cerca de los límites. Rangos conservadores (graves
  hasta +3 dB para evitar el "bombeo"). `result()` aplica redondeo, rango y preamp de Poweramp.

## [0.4.0] · 2026-10-30 · Fase 3: música generada y estilo terminal

Música del test generada por la app en seis géneros (pop, rock, clásica, electrónica, hip-hop y
jazz) con bajo pulsado más definido, 14 ecualizaciones de prueba hechas con curvas suaves y el
preamp de Poweramp. La interfaz pasa del liquid glass a un estilo de terminal/ASCII: botones con
marco ASCII, chips con corchetes, pasos como líneas de código, títulos que se escriben y engrane
que se expande en Ajustes. Además: ícono con el logo del usuario, pantalla "Acerca de" con
créditos y contacto, y la prueba de audio fluida a 120 Hz. El usuario escuchó los seis géneros
sin quejas.

### 2026-10-30 · `df4afc3` · ui: pasos como líneas de código y subtítulos más grandes
- Pedido del usuario: que pasos e indicaciones se vean como código. `CodeLines` muestra cada
  paso con número de línea tenue, una barra `│` que acompaña al texto aunque ocupe varios
  renglones y resaltado sencillo: números en ámbar (literales) y texto entre comillas en naranja
  (cadenas). El texto no cambia, para que cualquiera lo entienda. En Inicio, las recomendaciones
  continúan la numeración de los pasos (1–3, 4–6), como un solo archivo.
- `codeTokens()` separa la línea en partes y tiene 5 pruebas (números, comillas rectas y
  tipográficas, números dentro de comillas y que unir las partes da la línea original).
- Subtítulos `// sección` de 13 a 16 sp: más grandes que el texto normal (14 sp).
- Reemplaza a `StepList`, `BulletList` y la viñeta `›`.

### 2026-10-30 · `502edad` · refactor: renombrar componentes Glass a Terminal
- `GlassBackground`, `GlassScreen`, `GlassCard`, `GlassButton`, `GlassIconButton`, `GlassChip` y
  `GlassSlider` pasan a `TerminalBackground`, `TerminalScreen`, `TerminalPanel`, `TerminalButton`,
  `TerminalIconButton`, `TerminalChip` y `TerminalSlider`: el nombre debe decir qué son, y ya no
  son de vidrio. Cambio mecánico, sin efecto visible.

### 2026-10-30 · `649b8d0` · ui: quitar el vidrio y usar paneles ASCII
- Pedido del usuario: el liquid glass casi no se notaba y prefiere la dirección ASCII. Se quitan
  Haze (dependencia, superficie de vidrio y fuente de desenfoque) y el interruptor "Efecto de
  vidrio" de Ajustes. El fondo negro con su difuminado gris se conserva.
- Las tarjetas pasan a ser recuadros ASCII sin fondo (`asciiFrame` gris al 35 %).
- `ContrastTest` se simplifica: todo va directo sobre el fondo. Haze sale de los
  agradecimientos del README y de la app porque ya no se usa.

### 2026-10-30 · `cb03326` · ui: chips con corchetes estilo terminal
- Pedido del usuario: los chips quedan solo como la palabra entre corchetes, `[ Confiable ]`,
  sin fondo ni contorno. Gris sin seleccionar, ámbar seleccionado y rojo para avisos, con el
  color deslizándose al cambiar. TalkBack lee solo la palabra.
- Sin fondo propio, el texto del chip depende de lo que tenga detrás: el rojo de aviso sube de
  `#FF8A8A` a `#FF9A9A` para cumplir 4.5:1 también sobre el vidrio sin desenfoque (estaba en
  4.14:1). `ContrastTest` ahora verifica los chips sobre fondo y sobre vidrio.

### 2026-10-30 · `61b8a9b` · ui: botones con marco ASCII e íconos sin fondo
- Pedido del usuario: botones con aspecto de recuadro ASCII y sin fondo. `asciiFrame` dibuja
  esquinas ┌ ┐ └ ┘, laterales continuos y guiones repartidos de forma pareja arriba y abajo, que
  siempre cierran en las esquinas sin importar el ancho ni el tamaño de letra. Se dibuja en vez de
  escribirse con caracteres porque el texto se descuadraría con la letra grande del sistema.
- Principal: contorno y texto ámbar. Secundario: contorno gris y texto blanco. Al presionar, los
  guiones se alargan hasta formar una línea continua en 200 ms.
- Engrane y flecha de volver: solo el ícono, sin vidrio, borde ni escuadras (onda circular al
  tocar, área táctil de 48 dp). Se elimina `bracketCorners`, que ya no se usa.

### 2026-10-30 · `de27119` · ui: animar títulos, listas, casillas y controles
- Títulos `> Título` que se escriben letra por letra en ~0.3 s; las letras pendientes ya ocupan
  su lugar (transparentes), así que nada se mueve. TalkBack lee el título completo.
- Listas `01 02 03`, `›` y la lista de preparación aparecen escalonadas (45 ms entre elementos).
- Casillas `[ ]` → `[x]` con la "x" que aparece con rebote y colores que se deslizan.
- Perilla del deslizador con forma de cursor `▌` que crece al arrastrarla.
- Barra de progreso de Medición que avanza suave y gráfica que se transforma de una curva a otra
  en 200 ms al mover los controles de gusto.
- `PlayingIndicator`: medidor de barras decorativo junto al estado "Sonando".

### 2026-10-30 · `f8511f1` · ui: girar el engrane y expandir Ajustes desde él
- Al tocar el engrane gira media vuelta y Ajustes se expande desde él hasta llenar la pantalla
  (transformación de contenedor con `SharedTransitionLayout`, 550 ms); al regresar se encoge de
  vuelta. Entre Inicio y Ajustes el resto solo se desvanece para no competir con la animación.
- El botón de volver se impulsa hacia la izquierda y regresa con rebote al tocarlo.
- Con movimiento reducido no hay giro, impulso ni transformación.

### 2026-10-30 · `eacea7e` · ui: botones estilo terminal con escuadras
- Pedido del usuario: los botones redondos le quitaban el toque de programación. Ahora tienen
  esquinas casi rectas (8 dp) y escuadras ⌜ ⌝ ⌞ ⌟ que se cierran hacia el centro al presionar,
  con rebote. El secundario sigue siendo de vidrio, con borde fino.
- Esquinas más rectas en todo: tarjetas de 28 a 14 dp, chips de 20 a 6 dp. Los chips cambian de
  color con una transición en lugar de golpe.

### 2026-10-30 · `20d3c5e` · ui: agregar Acerca de con créditos y contacto
- Pantalla "Acerca de", discreta: al final de Ajustes solo hay una línea "Más información ›".
- Indica que Timbra es un proyecto personal creado por juliancitou, con enlaces a su perfil de
  GitHub, al repositorio y a los issues para reportar problemas o sugerir ideas, y una
  invitación abierta a colaborar.
- Agradecimientos (bibliotecas y referencias de audio) con autor, licencia y enlace, iguales a
  los del README. El texto completo de la licencia OFL de Cascadia Mono se puede leer ahí.
- Los enlaces se abren en el navegador: la app sigue sin permiso de internet. No se incluye
  correo electrónico para no publicarlo; el contacto es por GitHub.
- `LinkRow`: componente de enlace con estilo de terminal (`→ github.com/...`).

### 2026-10-30 · `d3ed656` · docs: agregar agradecimientos
- Sección "Agradecimientos" en el README, pedida por el usuario: bibliotecas incluidas (Haze,
  Jetpack Compose y Material 3, Navigation, AndroidX, Kotlin, Cascadia Mono), herramientas de
  desarrollo y pruebas (Android Studio, Gradle, Foojay, JUnit, AndroidX Test, GitHub Actions,
  Temurin) y el conocimiento en que se basa el audio (Audio EQ Cookbook, ITU-R BS.1770, Paul
  Kellet, Schroeder/Freeverb), cada uno con enlace y licencia verificada.
- A partir de ahora, cada dependencia o recurso nuevo se agrega ahí en el mismo commit.

### 2026-10-30 · `c29f5bd` · fix: hacer fluida la pantalla de prueba de audio
- Síntoma: la prueba de audio se sentía con tirones. Medido con `dumpsys gfxinfo`: mediana de
  9 ms por cuadro con pantalla de 120 Hz (presupuesto de 8.3 ms) y 1 % de cuadros de 61 ms.
- Causa: ~30 chips, cada uno con su propio desenfoque de Haze, recalculados en cada cuadro porque
  la aurora se movía a 120 cuadros por segundo; además, la generación de música competía con la
  interfaz por el procesador.
- Arreglo: los chips sin seleccionar usan relleno translúcido al 8 % con borde (sin desenfoque);
  la aurora se actualiza a 15 cuadros por segundo (se mueve tan lento que no se nota); la música
  se genera en un hilo propio de baja prioridad, lo que además quita la necesidad de un `Mutex`.

### 2026-10-30 · `9166176` · ui: usar el logo de Timbra como ícono
- Ícono adaptable a partir de `art/timbra_logo.svg` (logo del usuario): fondo con el degradado
  oscuro y, al frente, los cinco canales del ecualizador escalados dentro de la zona segura de
  66 dp con margen. Versión monocroma para los íconos con tema de Android.
- Los íconos vectoriales de Android no tienen desenfoque: el brillo del canal amarillo se imita
  con dos trazos anchos y translúcidos. Se borran los `.webp` de la plantilla (minSdk 33 siempre
  usa el ícono adaptable).

### 2026-10-30 · `85d4dd1` · ui: agregar géneros y 14 ecualizaciones a la prueba
- En "Probar el sonido": fila de géneros y 14 ecualizaciones (sin ecualizar, preset de ejemplo,
  más/menos graves, menos lodo, más voz, menos "S", más brillo, menos agudos, en V, cálido,
  brillante, rock, concierto). Cada género se genera la primera vez que se elige.
- Al cambiar de zona se conserva el punto de la pieza; al cambiar de género empieza de nuevo.

### 2026-10-30 · `59461c4` · audio: agregar curvas de ecualización y preamp
- `EqualizerShapes`: campana (gaussiana en octavas) y estantes bajo y alto que se suman; serán
  las "dimensiones" de las esferas en la Fase 4.
- `toPowerampGains()` redondea a 0.5 dB y limita a −8…+3 dB; `powerampPreampDb()` aplica la regla
  −(máximo positivo + 3), nunca mayor que −4.5. Una prueba atrapó que −0.24 redondeaba a `-0.0`,
  que habría salido así en el JSON; se corrigió.

### 2026-10-30 · `b3394bc` · audio: agregar seis géneros a la música del test
- `Song` por género con tempo, progresión, patrones y mezcla propios, sobre pistas comunes
  (`MusicStem`), así las rondas del test funcionan con cualquier género. Melodías originales.
- Pop 96 BPM; rock 120 BPM con el bombo como pista más fuerte y guitarras distorsionadas;
  clásica 72 BPM con cuerdas, timbales y sala amplia; electrónica 124 BPM con bombo en cada
  pulso; hip-hop 88 BPM con 808 y voz rítmica con muchas "s"; jazz 120 BPM con swing.
- 75 pruebas: duración, costura, nivel seguro y cobertura de bandas en los seis géneros.

### 2026-10-30 · `953de62` · audio: agregar instrumentos para más géneros
- Bajo pulsado (armónicos agudos que caen antes + sub-grave): más definición, pedido por el
  usuario. Bombo con tres estilos (suave, con pegada, largo), caja con más o menos chasquido,
  aplauso, escobillas, platillo suspendido, timbales, contrabajo, 808, piano con
  inarmonicidad, guitarra distorsionada con mano apagada, sección de cuerdas con vibrato,
  sintetizador y la vocal "u".

### 2026-10-30 · `01b7c15` · audio: agregar paso bajo y parciales con caída
- `BiquadCoefficients.lowPass` y `addPartials`: osciladores por rotación con caída propia por
  parcial (piano, contrabajo, timbales, bajo). Incluye por error el borrado de
  `Arrangement.kt`, que en realidad pertenece al commit `b3394bc`.

### 2026-10-30 · `78945de` · ui: agregar la música a la prueba de audio
- "Música del test" como señal predeterminada y fila "zona resaltada"; "Igualar volumen" mide
  con la propia música.

### 2026-10-30 · `63e75e4` · audio: generar el bucle musical del test
- `MusicLoop`: pistas por separado enrolladas en un bucle sin costura (las colas del final se
  suman al inicio), reverberación en régimen continuo y mezcla normalizada a −24 LUFS con picos
  bajo −8 dBFS. `MusicFocus` resalta graves, claridad, voz, "S" o aire por ronda.

### 2026-10-30 · `e3194f4` · audio: agregar instrumentos sintetizados y reverberación
- Tablas de onda sin aliasing, bombo, caja, hi-hat, ride, crash, bajo, pad, voz con formantes
  (vocales a, o, e) y consonante "s"; reverberación tipo Schroeder/Freeverb.

## [0.3.0] · 2026-10-30 · Fase 2: motor de audio seguro y ecualizador en vivo

La app suena por primera vez. Cadena fija fuente → ecualizador de 25 bandas (con cambio sin
clics) → entrada/salida de 50 ms → limitador de seguridad (−18 dBFS RMS, picos a −6 dBFS),
igualación de volumen con ponderación K, foco de audio, pausa al desconectar audífonos y paro
automático a los 20 minutos. Pantalla temporal "Prueba de audio" en Ajustes. Probado en el
POCO X8 Pro Max con los FreeBuds Pro 4: sin clics, cambios suaves, pausas correctas y volumen
cómodo. También incluye la reorganización del plan (test de esferas como modo principal) y la
desactivación del respaldo de datos.

### 2026-10-30 · `4b8a5e1` · ui: agregar pantalla de prueba de audio
- Pantalla temporal en Ajustes → "Probar el sonido": ruido rosa o tono de 1 kHz, cuatro
  ecualizaciones para cambiar mientras suena (sin ecualizar, preset de ejemplo, más graves,
  menos agudos), igualar volumen sí/no y el estado de reproducción.
- `AudioTestViewModel` (MVVM con `StateFlow`); el sonido se detiene al salir de la pantalla, al
  mandar la app al fondo o al apagar la pantalla. Nunca suena al abrir la app.

### 2026-10-30 · `3acf2ff` · audio: agregar salida con foco y pausa al desconectar
- `AudioOutput`: `AudioTrack` en stream, PCM float estéreo a la frecuencia nativa del teléfono
  (48 kHz si no la informa), bloques de ~10 ms y un hilo con prioridad de audio que solo vive
  mientras hay sonido.
- Pide el foco de audio (`USAGE_MEDIA`); si otra app o una llamada lo toma, pausa y no reanuda
  sola. Pausa al recibir `ACTION_AUDIO_BECOMING_NOISY` (audífonos desconectados).
- Estados: detenido, sonando, en pausa por otra app, en pausa por audífonos y paro por tiempo.

### 2026-10-30 · `2558c18` · audio: unir la cadena de audio con paro automático
- `AudioRenderer`: fuente → ecualizador con fundido → entrada/salida de 50 ms →
  `SafetyLimiter`, en ese orden fijo. Mono duplicado en los dos canales.
- Paro automático a los 20 minutos de sonido continuo. Cambiar de fuente baja, cambia y sube.
- Los controles solo dejan indicaciones; el hilo de audio es el único que modifica el estado.

### 2026-10-30 · `cd46c6e` · audio: agregar ruido rosa e igualación de volumen
- `PinkNoise` (filtro de Paul Kellet, misma energía por octava) y `SineTone`.
- `loudnessLufs` con ponderación K de ITU-R BS.1770 derivada para cualquier frecuencia de
  muestreo; coincide con los coeficientes oficiales a 48 kHz con error menor a 0.001 dB.
- `LoudnessMatcher`: ajusta el preamp de vista previa para que cada ecualización suene igual de
  fuerte que sin ecualizar (el preset exportado no cambia).

### 2026-10-30 · `e677805` · audio: cambiar de ecualización sin clics
- Durante 50 ms se procesan la ecualización vieja y la nueva y se mezclan con curva de coseno;
  los pesos suman 1 porque ambas salen de la misma señal (potencia constante daría +3 dB).

### 2026-10-30 · `74dad4a` · audio: agregar fades de 50 ms
- `GainRamp` con forma de coseno: sin esquinas, sin clic al empezar o parar.

### 2026-10-30 · `c54d47f` · audio: agregar limitador de seguridad
- `SafetyLimiter`: RMS en ventana de 400 ms limitado a −18 dBFS (ataque 20 ms, liberación
  500 ms) y techo de pico de −6 dBFS con ataque instantáneo más recorte duro final. NaN e
  infinitos salen como silencio. Probado con ruido 50 veces sobre escala completa.

### 2026-10-30 · `ff08f93` · audio: agregar ecualizador gráfico de 25 bandas
- `GraphicEqualizer`: una campana por banda de Poweramp en serie más el preamp. Q = 2, el valor
  con la curva más pareja (1.4 dB de ondulación con todas las bandas iguales); las bandas
  vecinas se suman, así que las gráficas deben usar la respuesta real. Por calibrar contra
  Poweramp en la Fase 4.

### 2026-10-30 · `d730af0` · audio: agregar filtros biquad
- Pico, estantes, paso alto y pasa-banda (Audio EQ Cookbook), estado en `Double` y respuesta en
  frecuencia exacta para las pruebas.

### 2026-10-30 · `894f922` · docs: reorganizar plan con test de esferas
- Decisión del usuario: el modo principal pasa a ser **"Encuentra tu ecualización"**, un test
  tipo juego. Suena música y el usuario toca esferas, cada una con una ecualización distinta que
  se escucha al instante; por rondas (balance, graves, claridad, voz, "S" y platillos, aire)
  elige la favorita y al final exporta el preset a Poweramp.
- La medición banda por banda queda como **modo avanzado** opcional, combinable con el test.
- La app deja de ser solo para los FreeBuds Pro 4: funciona con **cualquier audífono** y
  guardará una ecualización por cada par (perfiles).
- La música del test la **genera la app** (bombo, bajo, pad, voz sintética, platillos, "s"):
  sin archivos, sin derechos de autor y siempre disponible.
- Claves técnicas: volumen igualado entre esferas (si no, gana la más fuerte), cambio sin clics
  con fundido cruzado ≤ 50 ms y vista previa que imita el ecualizador de 25 bandas de Poweramp.
- Nuevas fases: 2 audio seguro y ecualizador en vivo, 3 música generada, 4 test de esferas y
  exportación, 5 modo avanzado, 6 perfiles e historial (`v1.0.0`); analizador y datos objetivos
  quedan como 7 y 8 opcionales.

### 2026-10-30 · `4273571` · chore: desactivar respaldo y transferencia de datos
- `android:allowBackup="false"` y reglas de extracción que excluyen todo, tanto del respaldo
  en la nube como de la copia a un teléfono nuevo (Android 12+ ignora `allowBackup` para esta
  última). Las mediciones nunca salen del teléfono, como dice el README.
- Se elimina `backup_rules.xml`: solo aplica a Android 11 o anterior y la app pide Android 13.

## [0.2.0] · 2026-10-30 · Fase 1: diseño y navegación

Tema oscuro con estética de programador (negro, gris, blanco y ámbar; tipografía Cascadia Mono),
componentes de vidrio con Haze, las 9 pantallas con contenido de ejemplo, navegación con zoom y
atrás predictivo, y revisión de accesibilidad (contraste AA verificado con pruebas, TalkBack y
letra grande). Probado en el POCO X8 Pro Max. Todavía no reproduce sonido.

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

## [0.1.0] · 2026-10-29 · Fase 0: base del proyecto

Repositorio, documentación, CI y proyecto Android vacío que compila y se instala en el POCO.

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
