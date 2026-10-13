package com.vera_watt.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.vera_watt.ui.theme.PrimaryGreen

@Composable
fun SimulatorScreen() {
    var panels by remember { mutableStateOf(0f) }
    var bi by remember { mutableStateOf(false) }
    var cut by remember { mutableStateOf(0f) }
    val ctx = LocalContext.current

    val kwhSaved = panels.toInt() * 420.0 + (145 * 24 * 365 / 1000.0) * (cut / 100.0)
    val euros = kwhSaved * 0.15 + if (bi) 60.0 else 0.0
    val invest = panels.toInt() * 350.0
    val co2 = kwhSaved * 0.2
    
    val glassColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val glassBorder = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp), 
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Simulador de Poupança",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = glassColor,
            border = glassBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Painéis adicionais", fontWeight = FontWeight.SemiBold)
                    Text("${panels.toInt()} painéis", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = panels,
                    onValueChange = { panels = it },
                    valueRange = 0f..10f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryGreen,
                        activeTrackColor = PrimaryGreen
                    )
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tarifa bi-horária", fontWeight = FontWeight.SemiBold)
                    Switch(checked = bi, onCheckedChange = { bi = it }, colors = SwitchDefaults.colors(checkedTrackColor = PrimaryGreen))
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Corte de cargas vampiro", fontWeight = FontWeight.SemiBold)
                    Text("${cut.toInt()}%", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = cut,
                    onValueChange = { cut = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryGreen,
                        activeTrackColor = PrimaryGreen
                    )
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = glassColor,
            border = glassBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Resultados Anuais", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Poupança Estimada", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text("%.0f €".format(euros), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = PrimaryGreen)
                }
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("CO₂ Evitado", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("%.0f kg".format(co2), fontWeight = FontWeight.Medium)
                }
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Retorno (ROI)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (euros > 0 && invest > 0) "%.1f anos".format(invest / euros) else "-", fontWeight = FontWeight.Medium)
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { Toast.makeText(ctx, "Pedido de proposta enviado (simulado)", Toast.LENGTH_SHORT).show() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) { 
            Text("Pedir Proposta de Instalação", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(Modifier.height(100.dp))
    }
}