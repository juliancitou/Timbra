# Plan de trabajo — Sintonía

## Objetivo

Obtener un preset de Poweramp basado en **cómo escuchas tú** cada frecuencia con tus FreeBuds Pro 4,
en lugar de estimaciones. Lo más importante: encontrar con precisión dónde se te disparan la "S"
y el "tss" de los platillos, y dónde se acumula el bajo que tapa las voces.

## Qué puede y qué no puede medir la app

| Puede | No puede |
|---|---|
| Encontrar picos y valles angostos de tu combinación audífono + oído (sibilancia, resonancias) | Medir el nivel absoluto en tu oído (haría falta un micrófono de medición) |
| Comparar bandas entre sí con tu percepción | Saber con exactitud el equilibrio global graves/agudos |
| Generar y exportar el preset `.json` | Cambiar Poweramp por ti (tú importas el archivo) |

Por eso el resultado combina dos cosas: **correcciones medidas** (picos/valles) + **tu gusto**
(cuánto grave, voz y brillo quieres), con controles simples.

## Cómo funciona la medición

1. **Preparación:** misma configuración que usas para escuchar (mismo modo de ANC, HUAWEI SOUND
   en Predeterminado, mismo códec). El ecualizador de Poweramp debe estar **apagado** durante la medición.
2. **Nivel cómodo:** suena un ruido de referencia en 1 kHz. Ajustas el volumen del teléfono hasta
   que sea claro pero cómodo. La app recuerda ese volumen y te avisa si cambia.
3. **Igualación por bandas:** para cada una de las 25 bandas de Poweramp, la app alterna la
   referencia (1 kHz) y la banda a probar (~0.8 s cada una). Mueves un deslizador hasta que ambas
   suenen igual de fuertes y tocas "Suenan igual". Puedes repetir o saltar.
4. **Doble pasada opcional:** repite las bandas en otro orden; si una difiere más de 3 dB entre
   pasadas, la app te pide repetirla. Esto mejora mucho la confiabilidad.
5. **Duración aproximada:** 8–12 minutos una pasada.

### Señal de prueba

- Ruido rosa filtrado a 1/3 de octava (dos biquads pasa-banda en cascada), normalizado al mismo RMS.
- Alternativa: tono "warble" (seno con modulación ±1/6 de octava a 5 Hz).
- Las bandas de 20–36 Hz son poco confiables en percepción: se pueden saltar y en ese caso se
  conserva tu ajuste de gusto.

### Matemática (resumen)

- `g_i` = ganancia que pusiste en la banda *i* para igualarla a 1 kHz.
- Contorno de igual sonoridad ISO 226:2023 a ~60 fonios: `C_i` (relativo a 1 kHz).
- Desviación estimada del audífono: `D_i = C_i − g_i` (positivo = esa banda te llega más fuerte de lo ideal).
- Se separa `D` en **tendencia** (suavizado ancho, ~1 octava) y **detalle** (picos/valles angostos).
- Preset = `−detalle` (corrección medida, limitada a ±6 dB) + curva de gusto + tendencia atenuada.
- Ganancias a 0.5 dB, rango −8 a +3 dB, preamp según `AGENTS.md`.
- La interfaz muestra cuáles bandas son "confiables" y cuáles "estimadas".

## Pantallas

1. **Inicio:** "Crear mi ecualización", "Mis mediciones", "Ajustes".
2. **Preparación:** lista de verificación con casillas (audífonos conectados, ANC en tu modo
   habitual, Poweramp EQ apagado, lugar silencioso).
3. **Nivel cómodo:** botón reproducir/pausar, aviso de volumen, "Listo".
4. **Medición:** progreso (banda 7 de 25), frecuencia con explicación corta ("aquí vive la S"),
   deslizador grande, botones "Repetir", "Saltar", "Suenan igual".
5. **Resultados:** gráfica de vidrio con tu curva, picos detectados señalados en lenguaje claro
   ("Tienes un pico en 7 kHz: aquí está la S").
6. **Ajuste de gusto:** tres controles simples (Graves, Presencia de voz, Brillo) + vista previa de la curva.
7. **Exportar:** nombre del preset, "Guardar archivo" / "Compartir", e instrucciones para importarlo en Poweramp.
8. **Historial:** mediciones anteriores, comparar dos curvas, volver a exportar.

## Fases

### Fase 0 — Base del proyecto
- Proyecto en Android Studio (Empty Activity, Compose), repositorio en GitHub, `AGENTS.md` y `PLAN.md`.
- `libs.versions.toml` con dependencias base. Opcional: GitHub Actions que compile el APK de
  depuración en cada push, para descargarlo e instalarlo directo en el teléfono.
- **Listo cuando:** la app vacía se instala en el POCO y el CI compila.

### Fase 1 — Diseño y navegación
- Tema, `GlassBackground` (aurora animada), `GlassCard`, `GlassButton`, `GlassSlider`, `GlassChip`.
- Navegación entre todas las pantallas con contenido de ejemplo.
- **Listo cuando:** se ve elegante en el teléfono, fluido, con respaldo sin desenfoque y buen contraste.

### Fase 2 — Motor de audio seguro
- Generador de ruido rosa, filtros biquad, tono warble, alternador referencia/prueba, fades, limitador,
  foco de audio, pausa al desconectar audífonos, paro automático.
- Pruebas unitarias: frecuencia central y ancho de banda de los filtros, RMS normalizado,
  el limitador nunca deja pasar picos por encima del máximo.
- **Listo cuando:** suena limpio (sin clics) en los FreeBuds y las pruebas pasan.

### Fase 3 — Flujo de medición
- Preparación, nivel cómodo, 25 bandas, repetir/saltar, doble pasada, guardado de progreso.
- **Listo cuando:** puedes completar una medición de principio a fin y retomarla si sales.

### Fase 4 — Análisis y exportación
- ISO 226, separación tendencia/detalle, detección de picos, controles de gusto, exportación JSON.
- Prueba golden del formato Poweramp.
- **Listo cuando:** el archivo se importa en Poweramp y suena como se espera.

### Fase 5 — Historial y pulido
- Historial, comparación de curvas, animaciones, textos finales, ícono de la app.

### Fase 6 (opcional) — Analizador de espectro
- Captura del audio de otras apps con `AudioPlaybackCapture` (requiere permiso de grabación,
  consentimiento de proyección y un servicio en primer plano mientras corre). Algunas apps no
  permiten ser capturadas; hay que probar con Spotify.

### Fase 7 (opcional) — Datos objetivos
- Importar una curva medida (CSV de squig.link) y combinarla con tu medición personal.
- Varios perfiles (por modo de ANC o por códec).

## Riesgos conocidos

- La percepción cambia con el cansancio: mejor medir descansado y en silencio, y hacer doble pasada.
- El ajuste de la punta en el oído cambia los agudos: medir con las mismas puntas que usas.
- La estimación por debajo de ~60 Hz y el equilibrio global son aproximados; los picos angostos son lo confiable.
