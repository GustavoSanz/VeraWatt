package com.vera_watt.ui

import android.content.ComponentName
import android.widget.Toast
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphFrame
import com.nothing.ketchum.GlyphManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vera_watt.R
import com.vera_watt.ui.screens.*
import com.vera_watt.viewmodel.VeraWattViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VeraWattApp(vm: VeraWattViewModel) {
    val ctx = LocalContext.current
    var sessionOpen by remember { mutableStateOf(false) }
    val gm = remember { GlyphManager.getInstance(ctx.applicationContext) }

    DisposableEffect(Unit) {
        val callback = object : GlyphManager.Callback {
            override fun onServiceConnected(p0: ComponentName?) {
                try {
                    val registered = when {
                        Common.is20111() -> gm.register(Glyph.DEVICE_20111)
                        Common.is22111() -> gm.register(Glyph.DEVICE_22111)
                        Common.is23111() -> gm.register(Glyph.DEVICE_23111)
                        Common.is23113() -> gm.register(Glyph.DEVICE_23113)
                        Common.is24111() -> gm.register(Glyph.DEVICE_24111)
                        Common.is25111() -> gm.register(Glyph.DEVICE_25111)
                        Common.is25131() -> gm.register(Glyph.DEVICE_25131)
                        else -> gm.register(Glyph.DEVICE_23111)
                    }
                    if (registered) {
                        gm.openSession()
                        sessionOpen = true
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            override fun onServiceDisconnected(p0: ComponentName?) {
                sessionOpen = false
            }
        }
        gm.init(callback)
        onDispose {
            try {
                gm.turnOff()
                if (sessionOpen) gm.closeSession()
                gm.unInit()
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(vm.watts, vm.solarWatts, sessionOpen, vm.isNothingMode) {
        if (sessionOpen) {
            try {
                if (vm.isNothingMode) {
                    val builder = gm.glyphFrameBuilder
                    if (builder != null) {
                        val pct = (vm.watts / 3500f).coerceIn(0f, 1f)
                        if (pct > 0.66f) {
                            builder.buildChannelA().buildChannelB().buildChannelC()
                        } else if (pct > 0.33f) {
                            builder.buildChannelA().buildChannelB()
                        } else {
                            builder.buildChannelA()
                        }
                        gm.toggle(builder.build())
                    }
                } else {
                    gm.turnOff()
                }
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        vm.checkForUpdates()
    }

    LaunchedEffect(vm.loggedIn) {
        if (vm.loggedIn) {
            vm.checkServerStatus()
            vm.checkForUpdates()
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (vm.isAppLocked) {
        LockScreen(vm)
    } else if (!vm.loggedIn) {
        AuthScreen(vm) { vm.saveLoginSession(true) }
    } else if (!vm.hasCompletedOnboarding) {
        OnboardingScreen { vm.saveLoginSession(true, true) }
    } else {
        val tabs = listOf(
            Icons.Default.ElectricBolt to "Live",
            Icons.Default.Analytics to "Graficos",
            Icons.Default.Eco to "Eco",
            Icons.Default.Receipt to "Faturas",
            Icons.Default.Calculate to "Simular",
            Icons.Default.Person to "Perfil"
        )
        val pagerState = rememberPagerState(pageCount = { tabs.size })
        val coroutineScope = rememberCoroutineScope()

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column {
                    var taps by remember { mutableStateOf(0) }
                    val ctx = LocalContext.current
                    TopAppBar(
                        title = { 
                            Text(
                                text = if (vm.isNothingMode) "V E R A  W A T T" else "Vera Watt",
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = if (vm.isNothingMode) FontFamily(Font(R.font.dot)) else FontFamily.Default,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    taps++
                                    if (taps == 5) {
                                        vm.isNothingMode = !vm.isNothingMode
                                        if (vm.isNothingMode) {
                                            Toast.makeText(ctx, "Glyph Interface Connected. Nothing OS Detected.", Toast.LENGTH_SHORT).show()
                                        }
                                        taps = 0
                                    }
                                }
                            ) 
                        },
                        actions = { 
                            Text("${vm.watts} W", Modifier.padding(end = 16.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) 
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                    // Offline Status Banner
                    if (vm.isServerOffline) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Servidor em Manutencao (Modo Offline)", 
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                // Bolha Flutuante (Bubble Dock) Centralizada e Animada
                Box(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(32.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        shadowElevation = 16.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            tabs.forEachIndexed { i, t ->
                                val isSelected = pagerState.currentPage == i
                                val width by animateDpAsState(
                                    targetValue = if (isSelected) 96.dp else 40.dp,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .width(width)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent)
                                        .clickable { coroutineScope.launch { pagerState.animateScrollToPage(i) } },
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = t.first, 
                                            contentDescription = t.second,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        if (isSelected) {
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = t.second,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                // Marca d'água gigante no fundo
                Icon(
                    imageVector = tabs[pagerState.currentPage].first,
                    contentDescription = null,
                    modifier = Modifier
                        .size(350.dp)
                        .align(Alignment.Center)
                        .offset(y = (-40).dp)
                        .alpha(0.04f),
                    tint = MaterialTheme.colorScheme.onBackground
                )

                Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
                    HorizontalPager(state = pagerState) { page ->
                        when (page) {
                            0 -> RealtimeScreen(vm)
                            1 -> AnalyticsScreen(vm)
                            2 -> EcoFootprintScreen(vm)
                            3 -> InvoicesScreen(vm)
                            4 -> SimulatorScreen()
                            else -> ProfileScreen(vm) { 
                                vm.saveLoginSession(false)
                            }
                        }
                    }
                }
            }
        }
    }
        
        // Watermark Global de Testes (Abaixo do Notch da Camara)
        androidx.compose.material3.Surface(
            color = Color.Red.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 42.dp),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp
        ) {
            Text(
                text = "VERSÃO DE TESTES (BETA ${com.vera_watt.AppVersion.NAME})",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // Dialog de Atualizacao Disponivel
        if (vm.hasUpdateAvailable) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { vm.dismissUpdateDialog() }
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    shadowElevation = 24.dp,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = com.vera_watt.ui.theme.PrimaryGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.SystemUpdate,
                                    contentDescription = "Atualizacao",
                                    tint = com.vera_watt.ui.theme.PrimaryGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Atualizacao Disponivel",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Instalada: v${com.vera_watt.AppVersion.NAME}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(" → ", fontSize = 12.sp, color = com.vera_watt.ui.theme.PrimaryGreen)
                            Text(
                                "Nova: v${vm.latestVersionName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.vera_watt.ui.theme.PrimaryGreen
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (vm.updateMessage.isNotEmpty()) vm.updateMessage else "Existe uma nova versao disponivel da Vera Watt.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { vm.dismissUpdateDialog() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            ) {
                                Text("Mais Tarde", fontSize = 13.sp, maxLines = 1, softWrap = false)
                            }
                            Button(
                                onClick = {
                                    vm.dismissUpdateDialog()
                                    if (vm.updateDownloadUrl.isNotEmpty()) {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(vm.updateDownloadUrl))
                                            ctx.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(ctx, "Erro ao abrir link de download", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = com.vera_watt.ui.theme.PrimaryGreen)
                            ) {
                                Text("Atualizar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LockScreen(vm: VeraWattViewModel) {
    val ctx = LocalContext.current
    val executor = androidx.core.content.ContextCompat.getMainExecutor(ctx)
    val biometricPrompt = remember {
        androidx.biometric.BiometricPrompt(
            ctx as androidx.fragment.app.FragmentActivity, 
            executor, 
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    vm.isAppLocked = false
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(ctx, "Autenticacao falhou: $errString", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    val authenticate = {
        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Vera Watt Bloqueada")
            .setSubtitle("Confirme a sua identidade para continuar")
            .setNegativeButtonText("Cancelar")
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(Unit) {
        authenticate()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("App Bloqueada", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("Protegido por Biometria", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = authenticate,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(56.dp).padding(horizontal = 32.dp)
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Desbloquear")
        }
    }
}
