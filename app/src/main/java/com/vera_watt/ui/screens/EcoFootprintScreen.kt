package com.vera_watt.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vera_watt.ui.theme.*
import com.vera_watt.viewmodel.VeraWattViewModel

@Composable
fun EcoFootprintScreen(vm: VeraWattViewModel) {
    val score = vm.ecoScore
    val level = score / 25 + 1
    
    val glassColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val glassBorder = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    val glassShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp), // Fix scroll padding
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val st = Stroke(20f, cap = StrokeCap.Round)
                        val tl = Offset(10f, 10f); val sz = Size(size.width - 20f, size.height - 20f)
                        drawArc(Color.Gray.copy(alpha = .25f), 0f, 360f, false, topLeft = tl, size = sz, style = st)
                        drawArc(Emerald, -90f, 360f * score / 100f, false, topLeft = tl, size = sz, style = st)
                    }
                    Text("$score", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                }
                Text("Guardiao Solar - Nivel $level", fontWeight = FontWeight.Bold)
                Text("%.1f Arvores - %.1f kg CO2 evitados".format(vm.co2Avoided / 21.0, vm.co2Avoided))
                Text("Poupanca mensal das acoes: %.2f €".format(vm.savedMonthly))
            }
        }
        item { Text("O Que Melhorar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
        items(vm.ecoActions) { a ->
            Surface(modifier = Modifier.fillMaxWidth(), color = glassColor, border = glassBorder, shape = glassShape) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(a.done, { vm.toggleAction(a.id) })
                    Column {
                        Text(a.title)
                        Text("+${a.points} pts - %.2f €/mes".format(a.monthlySaving), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        item { Text("O Que Evitar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
        item { 
            Surface(modifier = Modifier.fillMaxWidth(), color = glassColor, border = glassBorder, shape = glassShape) { 
                Text("Detetados 145 W em standby a noite (cargas vampiro).", Modifier.padding(16.dp)) 
            } 
        }
        item { 
            Surface(modifier = Modifier.fillMaxWidth(), color = glassColor, border = glassBorder, shape = glassShape) { 
                Text("Pico de consumo no horario de ponta as 20:00.", Modifier.padding(16.dp)) 
            } 
        }
    }
}
