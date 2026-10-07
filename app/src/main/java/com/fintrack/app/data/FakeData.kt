package com.fintrack.app.data

import com.fintrack.app.data.model.FuturePrediction
import com.fintrack.app.data.model.MonthProjection

/**
 * Sample / preview data mirroring the values shown in the FinTrack Figma design.
 * Replace this with a real repository (network or local database) when wiring up
 * the app to an actual backend.
 */
object FakeData {

    // Resumen mostrado en la pantalla de Inicio (Home).
    const val TOTAL_BALANCE = 45280.50
    const val BALANCE_CHANGE_PERCENT = 8.4
    const val MONTHLY_INCOME = 5400.00
    const val MONTHLY_EXPENSES = 2120.00

    // Texto fijo de análisis de tendencia mostrado en la pantalla de Predicción.
    const val TREND_ANALYSIS_TEXT =
        "Basado en tus últimos 3 meses, se prevé una tendencia de ahorro ascendente de un 12%. " +
            "Recomendamos mantener los límites actuales."

    // Puntos del gráfico de gasto mensual (los últimos 3 son reales, "Nov*" es proyectado).
    val monthProjections = listOf(
        MonthProjection("Ago", 2100.0, "\$2.1k", isProjected = false),
        MonthProjection("Sep", 2300.0, "\$2.3k", isProjected = false),
        MonthProjection("Oct", 2100.0, "\$2.1k", isProjected = false, isSelected = true),
        MonthProjection("Nov*", 2240.0, "\$2.24k", isProjected = true)
    )

    // Predicciones de gasto para los próximos meses, con su nivel de confianza.
    val futurePredictions = listOf(
        FuturePrediction("Noviembre (Próximo)", 2240.00, "Alta confianza"),
        FuturePrediction("Diciembre", 2610.00, "Confianza Media"),
        FuturePrediction("Enero", 2080.00, "Confianza Media")
    )

    // Credenciales del usuario de ejemplo (Carlos Mendoza), usadas como atajo de
    // prueba en la pantalla de inicio de sesión (no pasan por el backend, por lo
    // que Mi Perfil no tendrá datos reales que mostrar con esta cuenta).
    const val DEMO_EMAIL = "carlos.mendoza@email.com"
    const val DEMO_PASSWORD = "1234"
}
