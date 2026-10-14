package com.vera_watt.ui

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphManager
import com.vera_watt.AppVersion
import com.vera_watt.R
import com.vera_watt.ui.screens.*
import com.vera_watt.ui.theme.PrimaryGreen
import com.vera_watt.viewmodel.VeraWattViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VeraWattApp(vm: VeraWattViewModel) {
    val ctx = LocalContext.current
    var sessionOpen by remember { mutableStateOf(false) }
    val gm = remember {
        try {
            GlyphManager.getInstance(ctx.applicationContext)
        } catch (e: Exception) {
            null
        }
    }

    // 1. Inicialização segura do Nothing Glyph SDK sem bloquear a UI
    DisposableEffect(Unit) {
        val callback = object : GlyphManager.Callback {
            override fun onServiceConnected(p0: ComponentName?) {
                try {
                    val manager = gm ?: return
                    val registered = when {
                        Common.is20111() -> manager.register(Glyph.DEVICE_20111)
                        Common.is22111() -> manager.register(Glyph.DEVICE_22111)
                        Common.is23111() -> manager.register(Glyph.DEVICE_23111)
                        Common.is23113() -> manager.register(Glyph.DEVICE_23113)
                        Common.is24111() -> manager.register(Glyph.DEVICE_24111)
                        Common.is25111() -> manager.register(Glyph.DEVICE_25111)
                        Common.is25131() -> manager.register(Glyph.DEVICE_25131)
                        else -> manager.register(Glyph.DEVICE_23111)
                    }
                    if (registered) {
                        manager.openSession()
                        sessionOpen = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
                sessionOpen = false
            }
        }
        try {
            gm?.init(callback)
        } catch (e: Exception) {}

        onDispose {
            try {
                gm?.turnOff()
                if (sessionOpen) gm?.closeSession()
                gm?.unInit()
            } catch (e: Exception) {}
        }
    }

    // 2. OTIMIZAÇÃO CRÍTICA: Só atualiza o Glyph se o nível (0, 1, 2 ou 3) mudar,
    // e corre em Background Thread (Dispatchers.IO) para NUNCA congelar os cliques!
    val glyphLevel by remember {
        derivedStateOf {
            if (!vm.isNothingMode) 0
            else {
                val pct = (vm.watts / 3500f).coerceIn(0f, 1f)
                when {
                    pct > 0.66f -> 3
                    pct > 0.33f -> 2
                    else -> 1
                }
            }
        }
    }

    LaunchedEffect(glyphLevel, sessionOpen) {
        if (sessionOpen && gm != null) {
            withContext(Dispatchers.IO) {
                try {
                    if (glyphLevel > 0) {
                        val builder = gm.glyphFrameBuilder
                        if (builder != null) {
                            when (glyphLevel) {
                                3 -> builder.buildChannelA().buildChannelB().buildChannelC()
                                2 -> builder.buildChannelA().buildChannelB()
                                1 -> builder.buildChannelA()
                            }
                            gm.toggle(builder.build())
                        }
                    } else {
                        gm.turnOff()
                    }
                } catch (e: Exception) {}
            }
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

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1800)
        showSplash = false
    }

    Box(Modifier.fillMaxSize()) {
        if (vm.isAppLocked) {
            LockScreen(vm)
        } else if (!vm.loggedIn) {
            AuthScreen(vm) { vm.saveLoginSession(true) }
        } else if (!vm.hasCompletedOnboarding) {
            OnboardingScreen { vm.saveLoginSession(true, true) }
        } else {
            val tabs = remember {
                listOf(
                    Icons.Default.ElectricBolt to "Live",
                    Icons.Default.Analytics to "Gráficos",
                    Icons.Default.Eco to "Eco",
                    Icons.Default.Receipt to "Faturas",
                    Icons.Default.Calculate to "Simular",
                    Icons.Default.Person to "Perfil"
                )
            }
            val pagerState = rememberPagerState(pageCount = { tabs.size })
            val coroutineScope = rememberCoroutineScope()

            // Deteta automaticamente se o tema atual da App é Escuro ou Claro
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        var taps by remember { mutableIntStateOf(0) }

                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        taps++
                                        if (taps == 5) {
                                            vm.isNothingMode = !vm.isNothingMode
                                            if (vm.isNothingMode) {
                                                Toast.makeText(
                                                    ctx,
                                                    "Glyph Interface Connected. Nothing OS Detected.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            taps = 0
                                        }
                                    }
                                ) {
                                    // LOGÓTIPO ANIMADO ADAPTATIVO (Tema Claro / Escuro)
                                    VeraWattAnimatedLogo(isDarkTheme = isDark)

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (vm.isNothingMode) "V E R A  W A T T" else "VeraWatt",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 19.sp,
                                                fontFamily = if (vm.isNothingMode) FontFamily(Font(R.font.dot)) else FontFamily.Default,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            // Badge Beta elegante integrada no cabeçalho (não bloqueia cliques!)
                                            Surface(
                                                color = PrimaryGreen.copy(alpha = 0.16f),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(0.5.dp, PrimaryGreen.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "BETA ${AppVersion.NAME}",
                                                    color = PrimaryGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Energia Renovável Inteligente",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                                        )
                                    }
                                }
                            },
                            actions = {
                                // Pill de Watts Isolada (Evita recompor a TopAppBar inteira!)
                                LiveWattsBadge(vm = vm)
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )

                        // Banner Offline com Animação Suave
                        AnimatedVisibility(
                            visible = vm.isServerOffline,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Servidor em Manutenção (Modo Offline)",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                },
                bottomBar = {
                    // Bolha Flutuante (Bubble Dock) Otimizada e Sem Lag
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp, start = 12.dp, end = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(32.dp)),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f)),
                            shadowElevation = 16.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                tabs.forEachIndexed { i, t ->
                                    val isSelected = pagerState.currentPage == i
                                    val width by animateDpAsState(
                                        targetValue = if (isSelected) 98.dp else 42.dp,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        ),
                                        label = "tab_width"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .height(42.dp)
                                            .width(width)
                                            .clip(RoundedCornerShape(21.dp))
                                            .background(
                                                if (isSelected) PrimaryGreen.copy(alpha = 0.22f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                if (pagerState.currentPage != i) {
                                                    coroutineScope.launch {
                                                        // FIX DO CONGELAMENTO: Se saltar mais de 1 aba, usa scrollToPage
                                                        // para não obrigar o Android a carregar todos os ecrãs pelo meio!
                                                        if (abs(pagerState.currentPage - i) > 1) {
                                                            pagerState.scrollToPage(i)
                                                        } else {
                                                            pagerState.animateScrollToPage(i)
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = t.first,
                                                contentDescription = t.second,
                                                modifier = Modifier.size(20.dp),
                                                tint = if (isSelected) PrimaryGreen
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            AnimatedVisibility(
                                                visible = isSelected,
                                                enter = fadeIn(tween(150)) + expandHorizontally(tween(150)),
                                                exit = fadeOut(tween(100)) + shrinkHorizontally(tween(100))
                                            ) {
                                                Row {
                                                    Spacer(Modifier.width(5.dp))
                                                    Text(
                                                        text = t.second,
                                                        color = PrimaryGreen,
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
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding())
                ) {
                    // Marca d'água de fundo animada com Crossfade suave
                    Crossfade(
                        targetState = tabs[pagerState.currentPage].first,
                        animationSpec = tween(400),
                        modifier = Modifier.align(Alignment.Center),
                        label = "bg_watermark"
                    ) { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(320.dp)
                                .offset(y = (-30).dp)
                                .alpha(0.035f),
                            tint = PrimaryGreen
                        )
                    }

                    // HorizontalPager otimizado com key única por página
                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 0, // Evita renderizar ecrãs pesados escondidos
                        key = { it },
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        // Animação suave de entrada de cada página
                        Box(Modifier.fillMaxSize()) {
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

        // Dialog de Atualização Disponível
        if (vm.hasUpdateAvailable) {
            Dialog(onDismissRequest = { vm.dismissUpdateDialog() }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f)),
                    shadowElevation = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.SystemUpdate,
                                    contentDescription = "Atualização",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Atualização Disponível",
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
                                "Instalada: v${AppVersion.NAME}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(" → ", fontSize = 12.sp, color = PrimaryGreen)
                            Text(
                                "Nova: v${vm.latestVersionName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (vm.updateMessage.isNotEmpty()) vm.updateMessage
                            else "Existe uma nova versão disponível da VeraWatt.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { vm.dismissUpdateDialog() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                            ) {
                                Text("Mais Tarde", fontSize = 13.sp, maxLines = 1, softWrap = false)
                            }
                            Button(
                                onClick = {
                                    vm.dismissUpdateDialog()
                                    if (vm.updateDownloadUrl.isNotEmpty()) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(vm.updateDownloadUrl))
                                            ctx.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(ctx, "Erro ao abrir link de download", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                            ) {
                                Text("Atualizar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Ecrã de abertura oficial animado Vera Watt (Splash Screen)
        val isSystemDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut(tween(400))
        ) {
            VeraWattSplashScreen(isDarkTheme = isSystemDark)
        }
    }
}

/**
 * Componente isolado para o contador de Watts no topo.
 * Isolar isto num @Composable próprio impede que a TopAppBar e o Pager inteiro
 * recomponham a cada 2 segundos quando os Watts mudam!
 */
@Composable
fun LiveWattsBadge(vm: VeraWattViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "watts_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "dot_alpha"
    )

    Surface(
        color = PrimaryGreen.copy(alpha = 0.14f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.35f)),
        modifier = Modifier.padding(end = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(dotAlpha)
                    .background(PrimaryGreen, CircleShape)
            )
            Text(
                text = "${vm.watts} W",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = PrimaryGreen
            )
        }
    }
}

/**
 * Logótipo Animado da VeraWatt com suporte automático a Tema Claro e Tema Escuro.
 * Alterna dinamicamente entre verawatt_ativo1_4x (claro) e verawatt_ativo2_4x (escuro).
 */
@Composable
fun VeraWattAnimatedLogo(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val iconRes = if (isDarkTheme) R.drawable.verawatt_ativo2_4x else R.drawable.verawatt_ativo1_4x

    Box(
        modifier = modifier.size(42.dp),
        contentAlignment = Alignment.Center
    ) {
        // Halo luminoso verde animado atrás do logótipo
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(pulseScale * 1.15f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryGreen.copy(alpha = glowAlpha),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "VeraWatt Logo",
            modifier = Modifier
                .size(34.dp)
                .scale(pulseScale)
                .clip(RoundedCornerShape(9.dp))
        )
    }
}

/**
 * Ecrã de Carregamento e Abertura Oficial Vera Watt (Splash Screen Animada).
 * Apresenta o logótipo oficial, animação de halo elétrico e tipografia Vera Watt.
 */
@Composable
fun VeraWattSplashScreen(isDarkTheme: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_energy")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progressAnim.animateTo(1f, animationSpec = tween(1600, easing = LinearOutSlowInEasing))
    }

    val iconRes = if (isDarkTheme) R.drawable.verawatt_ativo2_4x else R.drawable.verawatt_ativo1_4x

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Halo de energia radial verde
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseScale * 1.25f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PrimaryGreen.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Emblema Oficial Vera Watt
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "VeraWatt Emblem",
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulseScale)
                    .clip(RoundedCornerShape(22.dp))
            )
        }

        Spacer(Modifier.height(26.dp))

        // Tipografia Oficial Vera Watt 4x
        Image(
            painter = painterResource(id = R.drawable.verawatt_typography_4x),
            contentDescription = "VERAWATT",
            modifier = Modifier
                .width(180.dp)
                .height(44.dp)
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Energia Renovável Inteligente",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = (if (isDarkTheme) Color.White else Color(0xFF1E293B)).copy(alpha = 0.75f),
            letterSpacing = 1.1.sp
        )

        Spacer(Modifier.height(48.dp))

        // Barra de progresso energética
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background((if (isDarkTheme) Color.White else Color.Black).copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressAnim.value)
                    .background(PrimaryGreen)
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "BETA ${AppVersion.NAME}",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryGreen.copy(alpha = 0.9f)
        )
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
                    Toast.makeText(ctx, "Autenticação falhou: $errString", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    val authenticate = {
        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("VeraWatt Bloqueada")
            .setSubtitle("Confirme a sua identidade para continuar")
            .setNegativeButtonText("Cancelar")
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(Unit) {
        authenticate()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VeraWattAnimatedLogo(
            isDarkTheme = isSystemInDarkTheme(),
            modifier = Modifier.scale(1.8f)
        )
        Spacer(Modifier.height(32.dp))
        Text(
            "App Bloqueada",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Protegido por Biometria",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = authenticate,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .height(56.dp)
                .padding(horizontal = 32.dp)
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Desbloquear", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}