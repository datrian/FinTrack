package com.fintrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fintrack.app.data.FakeData
import com.fintrack.app.data.local.SessionManager
import com.fintrack.app.data.remote.RetrofitClient
import com.fintrack.app.data.remote.parseApiErrorMessage
import com.fintrack.app.ui.components.FinTrackDetailTopBar
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackRed
import java.io.IOException
import kotlinx.coroutines.launch
import retrofit2.HttpException

// Pantalla de inicio de sesión. Valida contra el usuario de ejemplo (Carlos
// Mendoza / FakeData.DEMO_PASSWORD) para pruebas rápidas de la interfaz, y
// contra el backend (POST /api/v1/auth/login, respaldado por Neon) para
// cualquier otra cuenta.
@Composable
fun LoginScreen(onBack: () -> Unit, onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isValid = email.isNotBlank() && password.isNotBlank()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { FinTrackDetailTopBar(title = "Iniciar Sesión", onBackClick = onBack) }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionCard {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Correo") },
                        placeholder = { Text("Ej. carlos.mendoza@email.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = FinTrackRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        val trimmedEmail = email.trim()
                        val isDemoUser = trimmedEmail.equals(FakeData.userProfile.email, ignoreCase = true) &&
                            password == FakeData.DEMO_PASSWORD
                        if (isDemoUser) {
                            onLoginSuccess()
                        } else {
                            isChecking = true
                            scope.launch {
                                try {
                                    val token = RetrofitClient.authApi.login(trimmedEmail, password)
                                    SessionManager.saveSession(
                                        context = context,
                                        accessToken = token.access_token,
                                        userId = token.usuario.id_usuario,
                                        name = token.usuario.nombre_usuario,
                                        email = token.usuario.correo_usuario
                                    )
                                    isChecking = false
                                    onLoginSuccess()
                                } catch (e: HttpException) {
                                    isChecking = false
                                    errorMessage = if (e.code() == 401) {
                                        "Correo o contraseña incorrectos"
                                    } else {
                                        e.parseApiErrorMessage()
                                    }
                                } catch (e: IOException) {
                                    isChecking = false
                                    errorMessage = "No se pudo conectar con el servidor. Revisa tu conexión."
                                }
                            }
                        }
                    },
                    enabled = isValid && !isChecking,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinTrackNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 20.dp)
                ) {
                    Text(text = if (isChecking) "Verificando..." else "Ingresar")
                }
            }
        }
    }
}
