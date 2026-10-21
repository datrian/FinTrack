package com.fintrack.app.data.remote

import com.fintrack.app.ui.screens.humanizeCode
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Respuestas reales (recortadas) de los endpoints de Analítica FinTrack V1.1.
class AnaliticaResponsesTest {
    private val gson = Gson()

    @Test
    fun `prediccion sin historial suficiente llega sin monto`() {
        val json = """{"estado":"HISTORIAL_INSUFICIENTE","explicacion_ux":"Hay 0 de las 8 semanas","gasto_estimado":null,"rango_min_ux":null,"rango_max_ux":null,"nivel_confiabilidad_ux":null,"periodo_inicio_objetivo":"2026-10-19","periodo_fin_objetivo":"2026-10-25","semanas_completas_utilizadas":0,"semanas_requeridas":8,"ficha_tecnica":{"mae_modelo":null}}"""
        val response = gson.fromJson(json, PrediccionSemanalResponse::class.java)
        assertEquals("HISTORIAL_INSUFICIENTE", response.estado)
        assertNull(response.gasto_estimado)
        assertEquals(8, response.semanas_requeridas)
    }

    @Test
    fun `salud trae puntaje y proyeccion con montos como texto`() {
        val json = """{"estado":"OK","etiqueta_salud":"FAVORABLE","score_salud_difuso":83.4,"explicacion_ux":"Salud orientativa","proyeccion_periodo":{"aplicado_estimado_cierre":"9316.67","dias_observados":3,"dias_periodo":13,"diferencia_vs_presupuesto":"-21183.33","ritmo_diario_aplicado":"716.67"}}"""
        val response = gson.fromJson(json, SaludPeriodoResponse::class.java)
        assertEquals(83.4, response.score_salud_difuso!!, 0.001)
        assertEquals(-21183.33, response.proyeccion_periodo!!.diferencia_vs_presupuesto!!.toDouble(), 0.001)
        assertEquals(13, response.proyeccion_periodo!!.dias_periodo)
    }

    @Test
    fun `clustering sin datos suficientes trae lista vacia`() {
        val json = """{"clusters":[],"estado":"DATOS_INSUFICIENTES","explicacion_ux":"Hay 2 gastos","total_gastos_analizados":2}"""
        val response = gson.fromJson(json, ClusteringGastosResponse::class.java)
        assertTrue(response.clusters!!.isEmpty())
        assertEquals(2, response.total_gastos_analizados)
    }

    @Test
    fun `sugerencia sin resultado`() {
        val json = """{"categoria_id":null,"confianza":null,"estado":"SIN_SUGERENCIA","explicacion_ux":"Todavía falta variedad de ejemplos","sugerencia_encontrada":false}"""
        val response = gson.fromJson(json, SugerenciaClasificacionResponse::class.java)
        assertFalse(response.sugerencia_encontrada!!)
        assertNull(response.categoria_id)
    }

    @Test
    fun `codigos del backend se vuelven texto legible`() {
        assertEquals("Historial insuficiente", humanizeCode("HISTORIAL_INSUFICIENTE"))
        assertEquals("En riesgo", humanizeCode("EN_RIESGO"))
    }
}
