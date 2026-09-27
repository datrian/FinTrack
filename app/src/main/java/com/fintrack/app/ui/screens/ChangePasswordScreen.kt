package com.fintrack.app.ui.screens

import android.widget.Toast
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
import com.fintrack.app.data.local.SessionManager
import com.fintrack.app.data.remote.CambiarPasswordRequest
import com.fintrack.app.data.remote.RetrofitClient
import com.fintrack.app.data.remote.parseApiErrorMessage
import com.fintrack.app.ui.components.FinTrackDetailTopBar
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackRed
import java.io.IOException
import kotlinx.coroutines.launch
import retrofit2.HttpException

// Contraseña alfanumérica (sin caracteres especiales) de 8 a 128 caracteres,
// con al menos una mayúscula, una minúscula y un número.
private val PASSWORD_REGEX = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z0-9]{8,128}\$")
private const val PASSWORD_REQUIREMENTS_HINT =
    "Debe ser alfanumérica (sin símbolos), de 8 a 128 caracteres, con al menos una mayúscula, una minúscula y un número."

// Pantalla para cambiar la contraseña de la cuenta activa
// (PUT /api/v1/usuarios/me/password, requiere sesión iniciada).
@Composable
fun ChangePasswordScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isNewPasswordValid = PASSWORD_REGEX.matches(newPassword)
    val isValid = currentPassword.isNotBlank() &&
        isNewPasswordValid &&
        confirmPassword == newPassword

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { FinTrackDetailTopBar(title = "Cambiar Contraseña", onBackClick = onBack) }

        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionCard {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it; errorMessage = null },
                        label = { Text("Contraseña actual") },
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; errorMessage = null },
                        label = { Text("Contraseña nueva") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = PASSWORD_REQUIREMENTS_HINT,
                        color = if (newPassword.isNotEmpty() && !isNewPasswordValid) FinTrackRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
                        label = { Text("Confirmar contraseña nueva") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                        Text(
                            text = "Las contraseñas no coinciden",
                            color = FinTrackRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
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
                        val token = SessionManager.getAccessToken(context)
                        if (token.isNullOrBlank()) {
                            errorMessage = "Tu sesión expiró. Vuelve a iniciar sesión."
                            return@Button
                        }
                        isSaving = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val response = RetrofitClient.usuarioApi.cambiarPassword(
                                    authorization = "Bearer $token",
                                    request = CambiarPasswordRequest(
                                        password_actual = currentPassword,
                                        password_nuevo = newPassword
                                    )
                                )
                                isSaving = false
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "Contraseña actualizada correctamente", Toast.LENGTH_SHORT).show()
                                    onBack()
                                } else {
                                    throw HttpException(response)
                                }
                            } catch (e: HttpException) {
                                isSaving = false
                                errorMessage = when (e.code()) {
                                    401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
                                    else -> e.parseApiErrorMessage()
                                }
                            } catch (e: IOException) {
                                isSaving = false
                                errorMessage = "No se pudo conectar con el servidor. Revisa tu conexión."
                            }
                        }
                    },
                    enabled = isValid && !isSaving,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinTrackNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 20.dp)
                ) {
                    Text(text = if (isSaving) "Guardando..." else "Guardar cambios")
                }
            }
        }
    }
}
