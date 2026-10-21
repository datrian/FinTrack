package com.fintrack.app.ui.screens

import com.fintrack.app.data.model.TipoTransaccion
import com.fintrack.app.data.remote.TransaccionResponse
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeTotalsTest {
    private fun tx(tipo: TipoTransaccion, monto: String, fecha: String) = TransaccionResponse(
        id_transaccion = "id",
        cuenta_id = "cuenta",
        tipo_transaccion = tipo,
        cuenta_destino_id = null,
        categoria_id = null,
        subcategoria_id = null,
        monto_transaccion = monto,
        fecha_registro_transaccion = fecha,
        comentario_transaccion = null
    )

    private val octubre = YearMonth.of(2026, 10)

    @Test
    fun `balance suma ingresos y resta gastos de todos los meses`() {
        val totals = listOf(
            tx(TipoTransaccion.INGRESO, "1000.00", "2026-09-15T12:00:00"),
            tx(TipoTransaccion.INGRESO, "500.50", "2026-10-02T12:00:00"),
            tx(TipoTransaccion.GASTO, "300.25", "2026-10-03T12:00:00"),
            tx(TipoTransaccion.GASTO, "100.00", "2026-08-01T12:00:00")
        ).toHomeTotals(octubre)
        assertEquals(1100.25, totals.balance, 0.001)
    }

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
    fun `las transferencias no cambian balance ni resumen`() {
        val totals = listOf(
            tx(TipoTransaccion.INGRESO, "100.00", "2026-10-01T12:00:00"),
            tx(TipoTransaccion.TRANSFERENCIA, "80.00", "2026-10-01T12:00:00")
        ).toHomeTotals(octubre)
        assertEquals(HomeTotals(balance = 100.0, monthIncome = 100.0, monthExpenses = 0.0), totals)
    }

    @Test
    fun `fecha con zona se ubica en el mes segun la hora local`() {
        val t = tx(TipoTransaccion.GASTO, "10.00", "2026-10-15T10:00:00+00:00")
        assertEquals(octubre, t.yearMonth())
        assertEquals(null, tx(TipoTransaccion.GASTO, "10.00", "no-es-fecha").yearMonth())
    }
}
