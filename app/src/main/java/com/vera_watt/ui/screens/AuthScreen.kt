package com.vera_watt.ui.screens

import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vera_watt.viewmodel.VeraWattViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun AuthScreen(vm: VeraWattViewModel, onLoginSucesso: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var aCarregar by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf("") }
    var showRegisterWeb by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    if (showRegisterWeb) {
        Dialog(
            onDismissRequest = { showRegisterWeb = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.Black
            ) {
                Box(Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = WebViewClient()
                                loadUrl("https://gustavosanz.taila8ef3d.ts.net/2%C2%BA_Ano/VeraWatt/login.php")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { showRegisterWeb = false },
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                    ) {
                        Text("X", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp), 
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo / Title
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Text(
                text = "⚡ Vera Watt", 
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF10B981),
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
            )
        }

        Text("Acesso Digital", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp))

        OutlinedTextField(
            value = email, 
            onValueChange = { email = it }, 
            label = { Text("NIF / E-mail do Contrato", color = Color.Gray) }, 
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedBorderColor = Color(0xFF10B981)
            )
        )
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password, 
            onValueChange = { password = it }, 
            label = { Text("Password", color = Color.Gray) }, 
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = Color.White,
                focusedTextColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedBorderColor = Color(0xFF10B981)
            )
        )
        
        AnimatedVisibility(visible = mensagemErro.isNotEmpty()) {
            Text(
                text = mensagemErro, 
                color = Color(0xFFEF4444), 
                modifier = Modifier.padding(top = 16.dp),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    mensagemErro = "Preencha todos os campos!"
                } else {
                    mensagemErro = ""
                    aCarregar = true
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val cliente = okhttp3.OkHttpClient()
                            val dadosJson = """{"email":"$email","password":"$password"}"""
                            val corpo = dadosJson.toRequestBody("application/json; charset=utf-8".toMediaType())
                            val pedido = okhttp3.Request.Builder()
                                .url("https://gustavosanz.taila8ef3d.ts.net/2%C2%BA_Ano/VeraWatt/api_login.php")
                                .post(corpo)
                                .build()
                                
                            val resposta = cliente.newCall(pedido).execute()
                            val respostaPhp = resposta.body?.string()
                            
                            withContext(Dispatchers.Main) {
                                aCarregar = false
                                if (respostaPhp != null) {
                                    val json = org.json.JSONObject(respostaPhp)
                                    val sucesso = json.optBoolean("sucesso", false)
                                    if (sucesso) {
                                        val userObj = json.optJSONObject("utilizador")
                                        val nome = userObj?.optString("nome", "Utilizador") ?: "Utilizador"
                                        val tipo = userObj?.optString("tipo_utilizador", "cliente") ?: "cliente"
                                        val pontos = userObj?.optInt("eco_pontos", 0) ?: 0

                                        vm.saveUserData(nome = nome, email = email, role = tipo, pontos = pontos)
                                        vm.saveLoginSession(true)
                                        Toast.makeText(ctx, "Bem-vindo, $nome!", Toast.LENGTH_SHORT).show()
                                        onLoginSucesso()
                                    } else {
                                        mensagemErro = json.optString("erro", "Email ou password incorretos")
                                    }
                                } else {
                                    mensagemErro = "Erro a comunicar com o servidor."
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                aCarregar = false
                                mensagemErro = "Servidor offline ou inacessivel. Inicie o XAMPP e o Tailscale."
                            }
                        }
                    }
                }
            }, 
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            enabled = !aCarregar
        ) { 
            if (aCarregar) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Entrar na Area de Cliente", fontSize = 18.sp, fontWeight = FontWeight.Bold) 
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                vm.saveUserData(
                    nome = "Utilizador Demonstracao",
                    email = "demo@verawatt.pt",
                    role = "demo",
                    pontos = 100
                )
                vm.saveLoginSession(true)
                Toast.makeText(ctx, "Acesso em Modo Demonstracao (Offline)", Toast.LENGTH_SHORT).show()
                onLoginSucesso()
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
        ) {
            Text("Entrar em Modo Demonstracao (Offline)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        
        Spacer(Modifier.height(12.dp))
        
        TextButton(
            onClick = { showRegisterWeb = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ainda nao e cliente? Aderir a Vera Watt", color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
        }
    }
}
