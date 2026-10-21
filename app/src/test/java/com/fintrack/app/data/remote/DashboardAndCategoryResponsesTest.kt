package com.fintrack.app.data.remote

import com.fintrack.app.data.model.EstadoPresupuesto
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Respuestas reales (recortadas) de resumen-dashboard y de categorías.
class DashboardAndCategoryResponsesTest {
    private val gson = Gson()

    @Test
    fun `resumen del dashboard trae saldos y presupuesto activo`() {
        val json = """{"id_usuario":"u1","saldo_total_usuario":"55350.00","reserva_total_usuario":"0","dinero_libre_total_usuario":"55350.00","limite_credito_total_usuario":"0","credito_utilizado_total_usuario":"0","credito_disponible_total_usuario":"0","porcentaje_utilizacion_credito_usuario":null,"periodo_activo":{"id_periodo":"d8f7","codigo_periodo":"OCT-2026","estado_periodo":"ACTIVO","monto_presupuesto_periodo":"30500.00000000000000000000","aplicado_total_periodo":"2150.00","porcentaje_consumido_periodo":"7.04918032786885245902"}}"""
        val response = gson.fromJson(json, ResumenDashboardResponse::class.java)
        assertEquals(55350.0, response.saldo_total_usuario!!.toDouble(), 0.001)
        assertNull(response.porcentaje_utilizacion_credito_usuario)
        assertEquals(EstadoPresupuesto.ACTIVO, response.periodo_activo!!.estado_periodo)
        assertEquals(30500.0, response.periodo_activo!!.monto_presupuesto_periodo!!.toDouble(), 0.001)
    }

    @Test
    fun `resumen sin presupuesto activo`() {
        val json = """{"id_usuario":"u1","saldo_total_usuario":"0","periodo_activo":null}"""
        assertNull(gson.fromJson(json, ResumenDashboardResponse::class.java).periodo_activo)
    }

    @Test
    fun `categoria trae su grupo`() {
        val json = """{"categorias":[{"id_categoria":"c1","grupo_id":"g2","nombre_categoria":"Alimentación","es_activa_categoria":true}],"total":1}"""
        val categoria = gson.fromJson(json, CategoriaListaSalida::class.java).categorias.single()
        assertEquals("g2", categoria.grupo_id)
    }
}
