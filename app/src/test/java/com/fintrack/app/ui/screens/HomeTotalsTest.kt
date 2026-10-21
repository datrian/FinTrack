package com.fintrack.app.ui.screens

import com.fintrack.app.data.model.TipoTransaccion
import com.fintrack.app.data.remote.TransaccionListaSalida
import com.fintrack.app.data.remote.TransaccionResponse
import com.google.gson.Gson
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeTotalsTest {
    private fun tx(tipo: TipoTransaccion?, monto: String, fecha: String, fechaOperacion: String? = null) = TransaccionResponse(
        id_transaccion = "id",
        cuenta_id = "cuenta",
        tipo_transaccion = tipo,
        cuenta_destino_id = null,
        categoria_id = null,
        subcategoria_id = null,
        monto_transaccion = monto,
        fecha_registro_transaccion = fecha,
        comentario_transaccion = null,
        fecha_operacion_transaccion = fechaOperacion
    )

    private val octubre = YearMonth.of(2026, 10)

    @Test
    fun `resumen mensual solo cuenta el mes actual`() {
        val totals = listOf(
            tx(TipoTransaccion.INGRESO, "1000.00", "2026-09-30T12:00:00"),
            tx(TipoTransaccion.INGRESO, "500.00", "2026-10-02T12:00:00"),
            tx(TipoTransaccion.GASTO, "200.00", "2026-10-31T12:00:00"),
            tx(TipoTransaccion.GASTO, "50.00", "2026-11-01T12:00:00")
        ).toHomeTotals(octubre)
        assertEquals(500.0, totals.monthIncome, 0.001)
        assertEquals(200.0, totals.monthExpenses, 0.001)
    }

    @Test
    fun `las transferencias no cambian el resumen`() {
        val totals = listOf(
            tx(TipoTransaccion.INGRESO, "100.00", "2026-10-01T12:00:00"),
            tx(TipoTransaccion.TRANSFERENCIA, "80.00", "2026-10-01T12:00:00")
        ).toHomeTotals(octubre)
        assertEquals(HomeTotals(monthIncome = 100.0, monthExpenses = 0.0), totals)
    }

    @Test
    fun `fecha con zona se ubica en el mes segun la hora local`() {
        val t = tx(TipoTransaccion.GASTO, "10.00", "2026-10-15T10:00:00+00:00")
        assertEquals(octubre, t.yearMonth())
        assertEquals(null, tx(TipoTransaccion.GASTO, "10.00", "no-es-fecha").yearMonth())
    }

    // Antes, un tipo que la app no conocía llegaba como null y el "when"
    // lanzaba NullPointerException, cerrando Inicio y Transacciones.
    @Test
    fun `tipos nuevos o desconocidos no cuentan ni cierran la app`() {
        val totals = listOf(
            tx(TipoTransaccion.GASTO, "100.00", "2026-10-02T12:00:00"),
            tx(TipoTransaccion.PAGO_DEUDA, "500.00", "2026-10-02T12:00:00"),
            tx(TipoTransaccion.PRESTAMO_RECIBIDO, "900.00", "2026-10-02T12:00:00"),
            tx(null, "70.00", "2026-10-02T12:00:00")
        ).toHomeTotals(octubre)
        assertEquals(HomeTotals(monthIncome = 0.0, monthExpenses = 100.0), totals)
    }

    @Test
    fun `gson lee los tipos nuevos y deja null los desconocidos`() {
        val json = """{"transacciones":[
            {"id_transaccion":"a","cuenta_id":"c","tipo_transaccion":"PAGO_DEUDA","monto_transaccion":"10.00","fecha_registro_transaccion":"2026-10-02T12:00:00"},
            {"id_transaccion":"b","cuenta_id":"c","tipo_transaccion":"TIPO_DEL_FUTURO","monto_transaccion":"10.00","fecha_registro_transaccion":"2026-10-02T12:00:00"}
        ],"total":2,"limite":20,"offset":0}"""
        val lista = Gson().fromJson(json, TransaccionListaSalida::class.java).transacciones
        assertEquals(TipoTransaccion.PAGO_DEUDA, lista[0].tipo_transaccion)
        assertNull(lista[1].tipo_transaccion)
    }

    // Un gasto de agosto capturado en octubre pertenece a agosto: manda la
    // fecha de operación, no la de registro.
    @Test
    fun `el mes sale de la fecha de operacion y no de la de registro`() {
        val capturadoTarde = tx(TipoTransaccion.GASTO, "350.00", "2026-10-21T07:13:48Z", fechaOperacion = "2026-08-10")
        assertEquals(YearMonth.of(2026, 8), capturadoTarde.yearMonth())
        assertEquals(HomeTotals(monthIncome = 0.0, monthExpenses = 0.0), listOf(capturadoTarde).toHomeTotals(octubre))
    }
}
