package com.vera_watt.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pular Botão (Opcional) no topo
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) {
                Text("Pular", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }

        // Pager para as "Slides"
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> OnboardingPage(
                    title = "Bem-vindo a\nVera Watt ⚡",
                    description = "O teu ecossistema inteligente de energia. Monitoriza, reduz e poupa todos os meses.",
                    emoji = "🔋"
                )
                1 -> OnboardingPage(
                    title = "Paineis Solares?",
                    description = "Personaliza a tua experiencia. Se tens paineis solares, ativamos o grafico duplo para ti.",
                    emoji = "☀️"
                )
                2 -> OnboardingPage(
                    title = "Tudo Pronto!",
                    description = "A tua casa inteligente ja esta ligada. Comeca a cortar na tua pegada ecologica hoje.",
                    emoji = "🌍"
                )
            }
        }

        // Indicador de Progresso (Bolinhas)
        Row(
            modifier = Modifier.padding(vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) { i ->
                val color = if (pagerState.currentPage == i) Color(0xFF10B981) else Color(0xFFE5E7EB)
                val width = if (pagerState.currentPage == i) 24.dp else 12.dp
                Box(
                    modifier = Modifier
                        .height(12.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        // Botão Inferior (Próximo / Começar)
        Button(
            onClick = {
                if (pagerState.currentPage < 2) {
                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    onFinish()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
        ) {
            Text(
                text = if (pagerState.currentPage == 2) "Entrar na App" else "Continuar",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun OnboardingPage(title: String, description: String, emoji: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = emoji, fontSize = 64.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = title,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = description,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}
