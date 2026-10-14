package com.vera_watt.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vera_watt.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel

class VeraWattViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("vera_watt_prefs", Context.MODE_PRIVATE)

    var loggedIn by mutableStateOf(prefs.getBoolean("is_logged_in", false))
    var autoBiometric by mutableStateOf(prefs.getBoolean("auto_biometric", true))
    var hasCompletedOnboarding by mutableStateOf(prefs.getBoolean("has_completed_onboarding", false))
    var paymentMethod by mutableStateOf(prefs.getString("payment_method", "") ?: "")

    fun updatePaymentMethod(method: String) {
        paymentMethod = method
        prefs.edit().putString("payment_method", method).apply()
    }
    
    var isServerOffline by mutableStateOf(false)
    var isAppLocked by mutableStateOf(loggedIn && autoBiometric)

    var userId by mutableIntStateOf(prefs.getInt("user_id", 1))
    var userName by mutableStateOf(prefs.getString("user_name", "Utilizador") ?: "Utilizador")
    var userEmail by mutableStateOf(prefs.getString("user_email", "") ?: "")
    var userRole by mutableStateOf(prefs.getString("user_role", "cliente") ?: "cliente")
    var ecoPontos by mutableIntStateOf(prefs.getInt("eco_pontos", 0))

    fun saveUserData(id: Int = 1, nome: String, email: String, role: String, pontos: Int = 0) {
        userId = id
        userName = nome
        userEmail = email
        userRole = role
        ecoPontos = pontos
        prefs.edit()
            .putInt("user_id", id)
            .putString("user_name", nome)
            .putString("user_email", email)
            .putString("user_role", role)
            .putInt("eco_pontos", pontos)
            .apply()
        fetchInvoices()
    }

    fun clearUserData() {
        userId = 1
        userName = "Utilizador"
        userEmail = ""
        userRole = "cliente"
        ecoPontos = 0
        prefs.edit()
            .remove("user_id")
            .remove("user_name")
            .remove("user_email")
            .remove("user_role")
            .remove("eco_pontos")
            .apply()
    }

    fun saveLoginSession(login: Boolean, completeOnboarding: Boolean? = null) {
        loggedIn = login
        prefs.edit().putBoolean("is_logged_in", login).apply()
        if (!login) {
            clearUserData()
        }
        if (completeOnboarding != null) {
            hasCompletedOnboarding = completeOnboarding
            prefs.edit().putBoolean("has_completed_onboarding", completeOnboarding).apply()
        }
    }

    fun toggleAutoBiometric(enabled: Boolean) {
        autoBiometric = enabled
        prefs.edit().putBoolean("auto_biometric", enabled).apply()
    }

    fun checkServerStatus() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val req = okhttp3.Request.Builder().url("https://gustavosanz.taila8ef3d.ts.net/2%C2%BA_Ano/VeraWatt/api_login.php").build()
                val resp = okhttp3.OkHttpClient().newCall(req).execute()
                isServerOffline = !resp.isSuccessful
            } catch(e: Exception) {
                isServerOffline = true
            }
        }
    }

    var hasUpdateAvailable by mutableStateOf(false)
    var latestVersionName by mutableStateOf("")
    var updateMessage by mutableStateOf("")
    var updateDownloadUrl by mutableStateOf("")
    var isCheckingUpdate by mutableStateOf(false)
    var updateCheckedFeedback by mutableStateOf<String?>(null)

    fun dismissUpdateDialog() {
        hasUpdateAvailable = false
    }

    fun checkForUpdates(manual: Boolean = false) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isCheckingUpdate = true
            try {
                val req = okhttp3.Request.Builder()
                    .url("https://gustavosanz.taila8ef3d.ts.net/2%C2%BA_Ano/VeraWatt/api_version.php")
                    .build()
                val resp = okhttp3.OkHttpClient().newCall(req).execute()
                val body = resp.body?.string()
                if (resp.isSuccessful && body != null) {
                    val json = org.json.JSONObject(body)
                    val serverCode = json.optInt("version_code", com.vera_watt.AppVersion.CODE)
                    val serverName = json.optString("version_name", com.vera_watt.AppVersion.NAME)
                    val msg = json.optString("message", "")
                    val url = json.optString("store_url", json.optString("download_url", "https://gustavosanz.github.io/Stopp-Store/verawatt.html"))

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (serverCode > com.vera_watt.AppVersion.CODE) {
                            hasUpdateAvailable = true
                            latestVersionName = serverName
                            updateMessage = msg
                            updateDownloadUrl = url
                        } else if (manual) {
                            updateCheckedFeedback = "A aplicacao esta atualizada (v${com.vera_watt.AppVersion.NAME})."
                        }
                    }
                }
            } catch (e: Exception) {
                if (manual) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        updateCheckedFeedback = "Nao foi possivel verificar atualizacoes."
                    }
                }
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isCheckingUpdate = false
                }
            }
        }
    }

    var watts by mutableStateOf(850)
    var solarWatts by mutableStateOf(1200)
    var tariff by mutableStateOf(Tariff.CHEIAS)
    var period by mutableStateOf(0)        // 0 semanal, 1 mensal, 2 anual
    var showCost by mutableStateOf(false)
    var isNothingMode by mutableStateOf(false) // Easter Egg  // false = kWh, true = €

    val invoices = mutableStateListOf(
        Invoice(3, "Setembro 2026", 34.2, 8.9, 9.6, InvoiceStatus.PENDING),
        Invoice(2, "Agosto 2026", 32.4, 8.9, 9.1, InvoiceStatus.PAID),
        Invoice(1, "Julho 2026", 29.8, 8.9, 8.5, InvoiceStatus.OVERDUE)
    )
    val ecoActions = mutableStateListOf(
        EcoAction(1, "Programar a máquina de lavar para as 13:30 (pico solar)", 10, 4.5),
        EcoAction(2, "Desligar aparelhos em standby à noite", 15, 9.0),
        EcoAction(3, "Usar forno e loiça fora da ponta (após as 22:00)", 8, 3.2)
    )

    init {
        fetchInvoices()
        viewModelScope.launch {
            while (true) {
                watts = (watts + Random.nextInt(-200, 201)).coerceIn(150, 3500)
                solarWatts = (solarWatts + Random.nextInt(-100, 101)).coerceIn(0, 2500)
                val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                tariff = when { h >= 22 || h < 8 -> Tariff.VAZIO; h in 18..20 -> Tariff.PONTA; else -> Tariff.CHEIAS }
                delay(1500)
            }
        }
    }

    val ecoScore: Int get() = (40 + ecoActions.filter { it.done }.sumOf { it.points }).coerceAtMost(100)
    val savedMonthly: Double get() = ecoActions.filter { it.done }.sumOf { it.monthlySaving }
    val co2Avoided: Double get() = 12.0 + ecoActions.filter { it.done }.sumOf { it.points } * 0.8

    fun toggleAction(id: Int) {
        val i = ecoActions.indexOfFirst { it.id == id }
        if (i >= 0) ecoActions[i] = ecoActions[i].copy(done = !ecoActions[i].done)
    }

    fun pay(id: Int) {
        val i = invoices.indexOfFirst { it.id == id }
        if (i >= 0) {
            invoices[i] = invoices[i].copy(status = InvoiceStatus.PAID)
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val url = "https://gustavosanz.taila8ef3d.ts.net/2%C2%BA_Ano/VeraWatt/api_faturas.php?action=pagar&id_fatura=$id"
                    val req = okhttp3.Request.Builder().url(url).build()
                    okhttp3.OkHttpClient().newCall(req).execute()
                } catch (e: Exception) {
                    // Fallback
                }
            }
        }
    }

    fun fetchInvoices() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val url = "https://gustavosanz.taila8ef3d.ts.net/2%C2%BA_Ano/VeraWatt/api_faturas.php?user_id=$userId"
                val req = okhttp3.Request.Builder().url(url).build()
                val resp = okhttp3.OkHttpClient().newCall(req).execute()
                val body = resp.body?.string()
                if (resp.isSuccessful && body != null) {
                    val json = org.json.JSONObject(body)
                    if (json.optBoolean("sucesso", false)) {
                        val arr = json.optJSONArray("faturas")
                        if (arr != null && arr.length() > 0) {
                            val list = mutableListOf<Invoice>()
                            for (idx in 0 until arr.length()) {
                                val item = arr.getJSONObject(idx)
                                val id = item.optInt("id")
                                val mes = item.optString("mes")
                                val energia = item.optDouble("energia", 30.0)
                                val potencia = item.optDouble("potencia", 8.9)
                                val taxas = item.optDouble("taxas", 8.0)
                                val estadoStr = item.optString("estado", "pendente")
                                val status = when (estadoStr.lowercase()) {
                                    "pago" -> InvoiceStatus.PAID
                                    "atrasado" -> InvoiceStatus.OVERDUE
                                    else -> InvoiceStatus.PENDING
                                }
                                list.add(Invoice(id, mes, energia, potencia, taxas, status))
                            }
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                invoices.clear()
                                invoices.addAll(list)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Manter estado local se offline
            }
        }
    }

    fun bars(): List<Bar> {
        val (labels, cons, sol) = when (period) {
            0 -> Triple(listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"),
                listOf(9f, 11f, 8f, 10f, 12f, 15f, 14f), listOf(6f, 7f, 5f, 8f, 9f, 10f, 11f))
            1 -> Triple(listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"),
                listOf(320f, 290f, 280f, 250f, 230f, 210f, 220f, 240f, 240f, 270f, 300f, 330f),
                listOf(90f, 120f, 180f, 240f, 300f, 340f, 350f, 320f, 250f, 170f, 100f, 80f))
            else -> Triple(listOf("2023", "2024", "2025", "2026"),
                listOf(3300f, 3250f, 3180f, 3100f), listOf(1800f, 2100f, 2400f, 2300f))
        }
        return labels.indices.map { Bar(labels[it], cons[it], sol[it]) }
    }
}
