package com.fintrack.app.data

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

    // Credenciales del usuario de ejemplo (Carlos Mendoza), usadas como atajo de
    // prueba en la pantalla de inicio de sesión (no pasan por el backend, por lo
    // que Mi Perfil no tendrá datos reales que mostrar con esta cuenta).
    const val DEMO_EMAIL = "carlos.mendoza@email.com"
    const val DEMO_PASSWORD = "1234"
}
