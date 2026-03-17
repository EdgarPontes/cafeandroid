package br.com.sgsistemas.cafesg.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.sgsistemas.cafesg.data.Funcionario
import br.com.sgsistemas.cafesg.data.RankingItem
import androidx.compose.material.icons.filled.Search

// Theme Colors
val DarkBackground = Color(0xFF121212)
val CardBackground = Color(0xFF212121)
val GoldTan = Color(0xFFC5A059)
val ButtonBrown = Color(0xFF8B5E3C)
val AvatarLime = Color(0xFFC0CA33)

@Composable
fun CafeSGApp(
    viewModel: MainViewModel,
    currentIp: String,
    onIpChanged: (String) -> Unit
) {
    val selectedFuncionario by viewModel.selectedFuncionario.collectAsState()
    val ranking by viewModel.ranking.collectAsState()
    val funcionarios by viewModel.funcionarios.collectAsState()
    val status by viewModel.consumoStatus.collectAsState()

    var clickCount by remember { mutableIntStateOf(0) }
    var showIpDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(DarkBackground)
        .padding(16.dp)) {
        
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CAFÉ SG",
                style = MaterialTheme.typography.displayMedium,
                color = GoldTan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(vertical = 32.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        clickCount++
                        if (clickCount >= 10) {
                            showIpDialog = true
                            clickCount = 0
                        }
                    }
            )

            if (selectedFuncionario == null) {
                // Spacer above search to push it to center
                Spacer(modifier = Modifier.weight(1f))

                UserSearch(
                    funcionarios = funcionarios,
                    onFuncionarioSelected = { viewModel.selectFuncionario(it) }
                )

                // Spacer below search to keep it in center
                Spacer(modifier = Modifier.weight(1f))

                RankingPodium(ranking = ranking.take(3))
                
                // Dinamic space: 10dp above the system navigation bars
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        AnimatedVisibility(
            visible = selectedFuncionario != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground.copy(alpha = 0.9f))
                    .clickable { viewModel.selectFuncionario(null) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable(enabled = false) {},
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    selectedFuncionario?.let { funcionario ->
                        UserHeaderCard(funcionario)
                        Spacer(modifier = Modifier.height(16.dp))
                        ValueSelectionCard(
                            onValueSelected = { viewModel.registrarConsumo(it) },
                            onCancel = { viewModel.selectFuncionario(null) }
                        )
                    }
                }
            }
        }

        if (status is UiState.Success || status is UiState.Error) {
            FeedbackOverlay(
                status = status,
                onDismiss = { viewModel.selectFuncionario(null) }
            )
        }

        if (showIpDialog) {
            IpConfigDialog(
                currentIp = currentIp,
                onDismiss = { showIpDialog = false },
                onConfirm = { 
                    onIpChanged(it)
                    showIpDialog = false
                }
            )
        }
    }
}

@Composable
fun IpConfigDialog(
    currentIp: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var ipText by remember { mutableStateOf(currentIp) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar IP da API") },
        text = {
            Column {
                Text("Digite o novo IP ou URL:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = ipText,
                    onValueChange = { ipText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("ex: 192.168.1.100:8000") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(ipText) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun UserHeaderCard(funcionario: Funcionario) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(AvatarLime)
                        .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = funcionario.nome.split(" ")
                        .filter { it.isNotEmpty() }
                        .take(2)
                        .map { it.first().uppercase() }
                        .joinToString("")
                    Text(
                        text = initials,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .border(2.dp, CardBackground, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = if (funcionario.codigo == "999999") "VISITANTE: ${funcionario.nome.uppercase()}" else funcionario.nome.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = GoldTan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (funcionario.codigo == "999999") "VISITANTE" else "#${funcionario.codigo}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ValueSelectionCard(
    onValueSelected: (Double) -> Unit,
    onCancel: () -> Unit
) {

    var customValue by remember { mutableStateOf("") }

    val valuesHistory = remember { mutableStateListOf<Double>() }

    val total = valuesHistory.sum()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Adicionar Créditos",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Total",
                color = Color.Gray
            )

            Text(
                text = "R$ %.2f".format(total),
                style = MaterialTheme.typography.headlineLarge,
                color = GoldTan,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            val values = listOf(0.25, 0.50, 1.00)

            Column {

                for (i in 0 until values.size step 2) {

                    Row(modifier = Modifier.fillMaxWidth()) {

                        ValueButton(
                            value = values[i],
                            onClick = { valuesHistory.add(values[i]) },
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                        )

                        if (i + 1 < values.size) {

                            ValueButton(
                                value = values[i + 1],
                                onClick = { valuesHistory.add(values[i + 1]) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                Button(
                    onClick = { valuesHistory.clear() },
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF7A1F1F)
                    )
                ) {
                    Text("Limpar")
                }

                Button(
                    onClick = {
                        if (valuesHistory.isNotEmpty()) {
                            valuesHistory.removeAt(valuesHistory.lastIndex)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF444444)
                    )
                ) {
                    Text("Desfazer")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {

                    if (total > 0) {
                        onValueSelected(total)
                    }

                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldTan
                )
            ) {
                Text(
                    "Confirmar",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Cancelar",
                modifier = Modifier.clickable { onCancel() },
                color = Color.Gray,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun ValueButton(value: Double, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ButtonBrown),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "R$ ${String.format("%.2f", value)}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearch(
    funcionarios: List<Funcionario>,
    onFuncionarioSelected: (Funcionario) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var isVisitor by remember { mutableStateOf(false) }
    
    val filtered = if (query.isEmpty()) emptyList() else funcionarios.filter {
        it.nome.contains(query, ignoreCase = true) || it.codigo.contains(query)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                label = { Text("Buscar...", color = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GoldTan,
                    unfocusedBorderColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    if (query.isNotEmpty()) {
                        // Ao clicar em AVANÇAR, considera como visitante com o texto da busca
                        onFuncionarioSelected(Funcionario(codigo = "999999", nome = query))
                        query = ""
                    }
                },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldTan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("AVANÇAR", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { isVisitor = !isVisitor },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isVisitor,
                onCheckedChange = { isVisitor = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = GoldTan,
                    uncheckedColor = Color.Gray
                )
            )
            Text(
                text = "Selecionar como visitante",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (filtered.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filtered.take(3).forEach { funcionario ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable {
                                    val finalFunc = if (isVisitor) {
                                        // Envia o nome real do funcionário, mas com o código de visitante
                                        Funcionario(codigo = "999999", nome = funcionario.nome)
                                    } else {
                                        funcionario
                                    }
                                    onFuncionarioSelected(finalFunc)
                                    query = ""
                                }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = funcionario.nome.uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Código/RFID: ${funcionario.codigo}",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            if (isVisitor) {
                                Text(
                                    text = "SELECIONADO COMO VISITANTE",
                                    color = GoldTan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "TOP 3 CONSUMIDORES (MÊS)",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RankingPodium(ranking: List<RankingItem>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            ranking.getOrNull(1)?.let { PodiumItem(it, 2, 100.dp) }
            ranking.getOrNull(0)?.let { PodiumItem(it, 1, 140.dp) }
            ranking.getOrNull(2)?.let { PodiumItem(it, 3, 80.dp) }
        }
    }
}

@Composable
fun PodiumItem(item: RankingItem, position: Int, height: androidx.compose.ui.unit.Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val displayName = remember(item.nome) {
            val parts = item.nome.split(" ").filter { it.isNotEmpty() }
            if (parts.size >= 2) {
                "${parts[0]} ${parts[1].take(1)}."
            } else {
                item.nome
            }
        }
        Text(displayName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(height)
                .background(
                    when (position) {
                        1 -> Color(0xFFFFD700)
                        2 -> Color(0xFFC0C0C0)
                        else -> Color(0xFFCD7F32)
                    },
                    RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(position.toString() + "º", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun FeedbackOverlay(status: UiState<String>, onDismiss: () -> Unit) {
    val color = if (status is UiState.Success) Color(0xFF4CAF50) else Color(0xFFF44336)
    val text = if (status is UiState.Success) (status as UiState.Success).data else (status as UiState.Error).message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.95f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (status is UiState.Success) "✓" else "✕",
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Toque para fechar", color = Color.Gray)
        }
    }

    LaunchedEffect(status) {
        if (status is UiState.Success) {
            kotlinx.coroutines.delay(3000)
            onDismiss()
        }
    }
}
