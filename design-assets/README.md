# Design assets

Archivos de diseño originales (SVG, PNG, mockups, etc.) que todavía no están
procesados como recursos de Android. Gradle no toca nada de esta carpeta.

Cuando un archivo esté listo para usarse de verdad en la app, se convierte
y se copia al lugar correspondiente dentro de `app/src/main/res/` (por
ejemplo, un SVG se importa como Vector Drawable en `res/drawable`, o una
imagen rasterizada se coloca en `res/mipmap-*` si es el ícono de la app).
