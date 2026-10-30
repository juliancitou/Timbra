# Sintonía

App Android personal para medir cómo escuchas cada frecuencia con tus audífonos y generar un
preset de **Poweramp Equalizer** a tu medida.

- Plan de trabajo: [`PLAN.md`](PLAN.md)
- Reglas para agentes y colaboradores: [`AGENTS.md`](AGENTS.md)

## Qué instalar en tu computadora

1. **Android Studio** (versión estable más reciente): <https://developer.android.com/studio>.
   Incluye Kotlin, el JDK y el gestor del SDK.
2. En Android Studio → **SDK Manager**: instala la plataforma de Android más reciente y las
   *Android SDK Build-Tools* y *Platform-Tools*.
3. **Git**: <https://git-scm.com> (si usas Windows, incluye Git Bash).
4. Espacio libre: unos 10 GB. Internet en la primera compilación.
5. Opcional: un agente de código (por ejemplo Claude Code) para trabajar sobre el repositorio;
   leerá `AGENTS.md` automáticamente.

## Preparar el teléfono (una sola vez)

1. Ajustes → Acerca del teléfono → toca 7 veces "Versión de HyperOS" (ya lo hiciste).
2. Opciones de desarrollador → activa **Depuración USB** e **Instalar vía USB**.
3. Conecta el teléfono con un cable USB de datos y acepta el aviso "¿Permitir depuración USB?".

## Crear el proyecto (Fase 0)

1. Crea en GitHub un repositorio vacío llamado `sintonia` (privado si prefieres).
2. En Android Studio: **New Project → Empty Activity** (la plantilla de Compose).
   - Name: `Sintonía` · Package: `com.<tuusuario>.sintonia` · Language: Kotlin
   - Minimum SDK: **API 33**
3. Copia `AGENTS.md`, `PLAN.md` y este `README.md` a la raíz del proyecto.
4. En la terminal del proyecto:
   ```bash
   git init
   git add .
   git commit -m "chore: proyecto base"
   git branch -M main
   git remote add origin https://github.com/<tuusuario>/sintonia.git
   git push -u origin main
   ```
5. Con el teléfono conectado, pulsa **Run ▶** en Android Studio. Si la app vacía abre en el
   POCO, la Fase 0 está lista.

## Uso seguro

- Antes de medir, baja el volumen del teléfono. La app nunca reproduce sonido sola.
- Si algo suena fuerte o molesto, pausa: siempre hay un botón visible.
- Durante la medición, apaga el ecualizador de Poweramp y usa el mismo modo de cancelación de ruido
  con el que escuchas música.

## Desinstalar

Como cualquier app: mantener presionado el ícono → Desinstalar. No deja cambios en el sistema.
