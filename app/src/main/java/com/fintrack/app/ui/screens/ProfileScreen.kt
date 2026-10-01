package com.fintrack.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fintrack.app.data.local.UserProfileStore
import com.fintrack.app.data.remote.parseApiErrorMessage
import com.fintrack.app.ui.components.FinTrackDetailTopBar
import com.fintrack.app.ui.components.NavigationRowItem
import com.fintrack.app.ui.components.ProfileAvatar
import com.fintrack.app.ui.components.SectionCard
import com.fintrack.app.ui.components.formatRegistrationDate
import com.fintrack.app.ui.components.initialsOf
import com.fintrack.app.ui.theme.FinTrackGreen
import com.fintrack.app.ui.theme.FinTrackNavy
import com.fintrack.app.ui.theme.FinTrackRed
import java.io.IOException
import kotlinx.coroutines.launch
import retrofit2.HttpException

// Pantalla de Perfil: datos del usuario, preferencias generales (moneda,
// notificaciones, accesos a Categorías/Subcategorías) y cerrar sesión.
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onChangePassword: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentProfile = UserProfileStore.profile.value
    var isLoading by remember { mutableStateOf(currentProfile == null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val loadProfile: () -> Unit = {
        scope.launch {
            isLoading = true
            loadError = null
            try {
                UserProfileStore.refresh(context)
            } catch (e: HttpException) {
                loadError = if (e.code() == 401) "Tu sesión expiró. Vuelve a iniciar sesión." else e.parseApiErrorMessage()
            } catch (e: IOException) {
                loadError = "No se pudo conectar con el servidor. Revisa tu conexión."
            } catch (e: IllegalStateException) {
                loadError = "Tu sesión expiró. Vuelve a iniciar sesión."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (currentProfile == null) loadProfile()
    }

    // Estado local del switch de notificaciones, inicializado con el valor guardado en el perfil.
    var notificationsEnabled by remember(currentProfile) { mutableStateOf(currentProfile?.budgetNotificationsEnabled ?: false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item { FinTrackDetailTopBar(title = "Mi Perfil", onBackClick = onBack) }

        if (currentProfile == null) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = FinTrackNavy)
                    } else if (loadError != null) {
                        Text(
                            text = loadError.orEmpty(),
                            color = FinTrackRed,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, FinTrackNavy, RoundedCornerShape(16.dp))
                                .clickable { loadProfile() }
                                .padding(vertical = 16.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                        ) {
                            Text(text = "Reintentar", color = FinTrackNavy, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    // Tarjeta con avatar (iniciales), nombre y correo del usuario.
                    SectionCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ProfileAvatar(initials = initialsOf(currentProfile.name), modifier = Modifier.size(72.dp))
                            Text(
                                text = currentProfile.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                            Text(
                                text = currentProfile.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Miembro desde ${formatRegistrationDate(currentProfile.registrationDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "PREFERENCIAS GENERALES",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )

                    // Tarjeta con las opciones de preferencias, separadas por divisores.
                    SectionCard {
                        NavigationRowItem(title = "Moneda principal", trailingText = currentProfile.currency, onClick = {})
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Notificaciones de presupuesto", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = FinTrackGreen)
                            )
                        }
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        NavigationRowItem(title = "Administrar Categorías", onClick = onNavigateToCategories)
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        NavigationRowItem(title = "Administrar Subcategorías", onClick = onNavigateToCategories)
                    }

                    // Botón "Cambiar contraseña" con borde azul, arriba de cerrar sesión.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                            .border(1.dp, FinTrackNavy, RoundedCornerShape(16.dp))
                            .clickable { onChangePassword() }
                            .padding(vertical = 16.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = FinTrackNavy)
                        Text(
                            text = "Cambiar contraseña",
                            color = FinTrackNavy,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Botón "Cerrar sesión" con borde rojo, al final de la pantalla.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 20.dp)
                            .border(1.dp, FinTrackRed, RoundedCornerShape(16.dp))
                            .clickable { onLogout() }
                            .padding(vertical = 16.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        Text(text = "Cerrar sesión", color = FinTrackRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
