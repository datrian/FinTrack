package com.fintrack.app.ui.screens

import com.fintrack.app.data.model.EstadoPresupuesto
import com.fintrack.app.data.remote.PeriodoDetalleResponse
import com.fintrack.app.data.remote.PeriodoListaSalida
import com.fintrack.app.data.remote.TipoPeriodo
import com.google.gson.Gson
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetPeriodTest {
    private val inicio = LocalDate.of(2026, 10, 19)

    @Test
    fun `fecha fin automatica segun el tipo de periodo`() {
        assertEquals(LocalDate.of(2026, 10, 25), TipoPeriodo.SEMANAL.endDateFor(inicio))
        assertEquals(LocalDate.of(2026, 11, 2), TipoPeriodo.QUINCENAL.endDateFor(inicio))
        assertEquals(LocalDate.of(2026, 11, 18), TipoPeriodo.MENSUAL.endDateFor(inicio))
        assertNull(TipoPeriodo.PERSONALIZADO.endDateFor(inicio))
    }

    @Test
    fun `codigo sugerido sigue el formato del backend`() {
        assertEquals("OCT-2026", suggestedPeriodCode(TipoPeriodo.MENSUAL, inicio))
        assertEquals("19-OCT-2026", suggestedPeriodCode(TipoPeriodo.SEMANAL, inicio))
    }

    // Respuestas reales del backend (recortadas), con los montos como number.
    @Test
    fun `lista de presupuestos se lee con el formato nuevo`() {
        val json = """{"presupuestos":[{"id_periodo":"d8f7","codigo_periodo":"OCT-2026","fecha_inicio_periodo":"2026-10-19","fecha_fin_periodo":"2026-10-31","estado_periodo":"ACTIVO","monto_total_presupuesto":5000.0,"aplicado_total_periodo":150.0,"periodo_vencido":false,"monto_presupuesto_periodo":500.0,"monto_restante_periodo":350.0,"monto_excedido_periodo":0,"porcentaje_consumido_periodo":30.0,"estado_consumo_periodo":"OK"}]}"""
        val periodo = Gson().fromJson(json, PeriodoListaSalida::class.java).presupuestos!!.single()
        assertEquals("d8f7", periodo.id_periodo)
        assertEquals(EstadoPresupuesto.ACTIVO, periodo.estado_periodo)
        assertEquals(500.0, periodo.monto_presupuesto_periodo!!.toDouble(), 0.001)
        assertEquals(150.0, periodo.aplicado_total_periodo!!.toDouble(), 0.001)
    }

    @Test
    fun `detalle trae periodo y grupos`() {
        val json = """{"periodo":{"id_periodo":"d8f7","estado_periodo":"ACTIVO"},"grupos":[{"grupo_id":"g1","nombre_grupo":"Gastos fijos","porcentaje_asignado_grupo":45,"monto_presupuesto_grupo":225.0,"aplicado_dinero_grupo":150.0,"porcentaje_utilizado_grupo":66.66666666666667,"estado_consumo_grupo":"OK"},{"grupo_id":"g2","nombre_grupo":"Ahorro","porcentaje_asignado_grupo":0,"porcentaje_utilizado_grupo":null}]}"""
        val detalle = Gson().fromJson(json, PeriodoDetalleResponse::class.java)
        assertEquals("d8f7", detalle.periodo!!.id_periodo)
        assertEquals(listOf("Gastos fijos", "Ahorro"), detalle.grupos!!.map { it.nombre_grupo })
        assertEquals(45, detalle.grupos!![0].porcentaje_asignado_grupo)
        assertNull(detalle.grupos!![1].porcentaje_utilizado_grupo)
    }
}
