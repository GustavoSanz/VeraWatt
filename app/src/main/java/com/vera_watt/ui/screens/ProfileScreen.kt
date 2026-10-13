package com.vera_watt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vera_watt.ui.theme.DangerRed
import com.vera_watt.ui.theme.PrimaryGreen

import com.vera_watt.viewmodel.VeraWattViewModel

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.clickable

@Composable
fun ProfileScreen(vm: VeraWattViewModel, onLogout: () -> Unit) {
    val context = LocalContext.current
    var alerts by remember { mutableStateOf(true) }
    var debit by remember { mutableStateOf(true) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            text = "O Meu Perfil",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 32.dp, start = 24.dp, bottom = 24.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Avatar",
                        tint = PrimaryGreen,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (vm.userName.isNotEmpty()) vm.userName else "Utilizador",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (vm.userEmail.isNotEmpty()) vm.userEmail else "Sem e-mail configurado",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val isAdmin = vm.userRole.equals("admin", ignoreCase = true)
                        val isDemo = vm.userRole.equals("demo", ignoreCase = true)
                        val badgeColor = when {
                            isAdmin -> Color(0xFF10B981)
                            isDemo -> Color(0xFF8B5CF6)
                            else -> Color(0xFF0EA5E9)
                        }
                        val badgeText = when {
                            isAdmin -> "Conta Admin"
                            isDemo -> "Modo Demonstracao"
                            else -> "Conta Cliente"
                        }
                        Badge(containerColor = badgeColor) { 
                            Text(badgeText, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) 
                        }
                    }
                }
            }

                        var showPaymentDialog by remember { mutableStateOf(false) }

            if (showPaymentDialog) {
                Dialog(onDismissRequest = { showPaymentDialog = false }) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(550.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent
                    ) {
                        AndroidView(factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                setBackgroundColor(0)
                                addJavascriptInterface(WebAppInterface { card ->
                                    post {
                                        vm.updatePaymentMethod(card)
                                        showPaymentDialog = false
                                    }
                                }, "Android")
                                loadUrl("file:///android_asset/payment.html")
                            }
                        })
                    }
                }
            }

            ProfileSection(title = "Contrato & Pagamentos") {
                ProfileItemRow(icon = Icons.Default.ElectricBolt, label = "Potencia contratada", value = "6.9 kVA")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showPaymentDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileItemRow(
                            icon = Icons.Default.CreditCard, 
                            label = "Metodo de Pagamento", 
                            value = if (vm.paymentMethod.isEmpty()) "Nao configurado" else vm.paymentMethod
                        )
                    }
                    IconButton(onClick = { showPaymentDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Editar Pagamento", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            ProfileSection(title = "Seguranca & Preferencias") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Text("Login com Biometria", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Switch(checked = vm.autoBiometric, onCheckedChange = { vm.toggleAutoBiometric(it) }, colors = SwitchDefaults.colors(checkedTrackColor = PrimaryGreen))
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Text("Alertas de Consumo", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Switch(checked = alerts, onCheckedChange = { alerts = it }, colors = SwitchDefaults.colors(checkedTrackColor = PrimaryGreen))
                }
            }

            LaunchedEffect(vm.updateCheckedFeedback) {
                vm.updateCheckedFeedback?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    vm.updateCheckedFeedback = null
                }
            }

            ProfileSection(title = "Sistema & Versao") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Versao da App", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text("v${com.vera_watt.AppVersion.NAME} (Build ${com.vera_watt.AppVersion.CODE})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (vm.isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryGreen, strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = { vm.checkForUpdates(manual = true) }) {
                            Text("Verificar Atualizacoes", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed.copy(alpha = 0.15f), contentColor = DangerRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair")
                Spacer(Modifier.width(8.dp))
                Text("Terminar Sessao", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(Modifier.height(120.dp)) // Espaço para compensar a barra flutuante de vidro
        }
    }
}

@Composable
fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun ProfileItemRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

class WebAppInterface(private val onPaymentSuccess: (String) -> Unit) {
    @JavascriptInterface
    fun paymentComplete(cardNumber: String) {
        onPaymentSuccess(cardNumber)
    }
}

