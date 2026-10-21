package com.fintrack.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionExpiredInterceptorTest {
    private lateinit var server: MockWebServer
    private val expiredTokens = mutableListOf<String>()
    private val client = OkHttpClient.Builder()
        .addInterceptor(SessionExpiredInterceptor { expiredTokens.add(it) })
        .build()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun call(code: Int, authorization: String?): Int {
        server.enqueue(MockResponse().setResponseCode(code).setBody("""{"detail":"x"}"""))
        val request = Request.Builder().url(server.url("/api/v1/cuentas")).apply {
            if (authorization != null) header("Authorization", authorization)
        }.build()
        return client.newCall(request).execute().use { it.code }
    }

    @Test
    fun `401 con token avisa que la sesion vencio con ese token`() {
        val code = call(401, "Bearer token-viejo")
        assertEquals(401, code) // la respuesta sigue llegando a la pantalla
        assertEquals(listOf("token-viejo"), expiredTokens)
    }

    @Test
    fun `401 sin token (login fallido) no avisa`() {
        call(401, null)
        assertTrue(expiredTokens.isEmpty())
    }

    @Test
    fun `respuestas que no son 401 no avisan`() {
        call(200, "Bearer token")
        call(403, "Bearer token")
        call(500, "Bearer token")
        assertTrue(expiredTokens.isEmpty())
    }
}
