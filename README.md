# FinTrack — Frontend Android (Kotlin + Jetpack Compose)

Este proyecto implementa, en Kotlin puro con Jetpack Compose y Material 3, el frontend de la
app **FinTrack** a partir de la plantilla de Figma proporcionada. Incluye las 8 pantallas del
prototipo, navegación inferior y datos de ejemplo que reflejan los valores mostrados en el diseño.

## Pantallas incluidas

1. **Inicio** (`HomeScreen`) — saludo, balance total, resumen mensual, accesos rápidos.
2. **Mis Cuentas** (`AccountsScreen`) — tarjetas de cuentas (Principal, Inversión, Crédito, Efectivo).
3. **Transacciones** (`TransactionsScreen`) — lista filtrable (Todas / Gastos / Ingresos).
4. **Presupuestos** (`BudgetScreen`) — gasto total presupuestado y límites por categoría.
5. **Predicción** (`PredictionScreen`) — análisis de tendencia, gráfico de proyección a 3 meses, predicciones futuras.
6. **Categorías** (`CategoriesScreen`) — lista de categorías con conteo de subcategorías.
7. **Subcategorías** (`SubcategoriesScreen`) — subcategorías de la categoría seleccionada, con alta/baja.
8. **Mi Perfil** (`ProfileScreen`) — datos del usuario, preferencias, cerrar sesión.

La navegación inferior (Inicio, Cuenta, Transacciones, Presupuesto, Predicción) usa
`androidx.navigation:navigation-compose`. "Mi Perfil" se abre desde el ícono de menú (☰) en la
barra superior de cualquier pantalla principal, y desde ahí se llega a "Categorías" → "Subcategorías",
igual que en el prototipo de Figma.

## Estructura del proyecto

```
FinTrackApp/
├── app/
│   └── src/main/java/com/fintrack/app/
│       ├── MainActivity.kt
│       ├── data/
│       │   ├── FakeData.kt          # Datos de ejemplo (reemplazar por una API/BD real)
│       │   └── model/Models.kt      # Modelos: Account, Transaction, BudgetCategoryLimit, etc.
│       ├── navigation/
│       │   ├── FinTrackDestination.kt
│       │   └── FinTrackNavGraph.kt
│       └── ui/
│           ├── components/CommonComponents.kt   # TopBar, SectionCard, barras de progreso, etc.
│           ├── screens/                          # Las 8 pantallas
│           └── theme/                             # Color.kt, Type.kt, Theme.kt
└── app/src/main/res/                              # Strings, ícono adaptativo, tema base XML
```

## Cómo abrir el proyecto

1. Instala **Android Studio** (versión Koala/2024.1 o más reciente recomendada).
2. Abre Android Studio → **Open** → selecciona la carpeta `FinTrackApp`.
3. Deja que Android Studio sincronice Gradle. Este proyecto **no incluye el binario
   `gradle-wrapper.jar`** (no se pudo descargar desde este entorno sin acceso a internet
   completo). Android Studio debería detectarlo automáticamente y ofrecer regenerarlo
   ("Gradle wrapper is missing... " → *OK* / *Sync Now*). Si no aparece ese diálogo, puedes
   generarlo tú mismo una vez con Gradle instalado localmente:
   ```bash
   gradle wrapper --gradle-version 8.7
   ```
4. Corre la app en un emulador o dispositivo (mínimo Android 8.0 / API 26).

## Próximos pasos sugeridos

- Reemplazar `FakeData` por un repositorio real (Retrofit/Ktor + Room, o Firebase).
- Agregar un `ViewModel` por pantalla (actualmente los datos son estáticos/composables simples).
- Conectar el formulario de "Nueva subcategoría" y "Crear nueva categoría" a persistencia real.
- Revisar las 3 últimas pantallas (Categorías, Subcategorías, Mi Perfil) contra el Figma para
  ajustar colores exactos si tienes acceso a los tokens de diseño (variables de Figma).
