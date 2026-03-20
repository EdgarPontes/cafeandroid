package br.com.sgsistemas.cafesg.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.sgsistemas.cafesg.data.Funcionario
import br.com.sgsistemas.cafesg.data.RankingItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
    isRankingEnabled: Boolean,
    onConfigChanged: (String, Boolean) -> Unit
) {
    val selectedFuncionario by viewModel.selectedFuncionario.collectAsState()
    val ranking by viewModel.ranking.collectAsState() // Aqui você pega o ranking do ViewModel
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
                    .padding(top = 16.dp, bottom = 16.dp)
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
                Spacer(modifier = Modifier.height(8.dp))

                // CORREÇÃO: Passando o ranking para dentro do UserSearch
                UserSearch(
                    funcionarios = funcionarios,
                    ranking = ranking, // <-- Adicione este parâmetro
                    isRankingEnabled = isRankingEnabled,
                    onFuncionarioSelected = { viewModel.selectFuncionario(it) }
                )
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
                currentRankingEnabled = isRankingEnabled,
                onDismiss = { showIpDialog = false },
                onConfirm = { ip, enabled ->
                    onConfigChanged(ip, enabled)
                    showIpDialog = false
                }
            )
        }
    }
}

@Composable
fun IpConfigDialog(
    currentIp: String,
    currentRankingEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var ipText by remember { mutableStateOf(currentIp) }
    var rankingEnabled by remember { mutableStateOf(currentRankingEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurações do Sistema") },
        text = {
            Column {
                Text("IP ou URL da API:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = ipText,
                    onValueChange = { ipText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("ex: 192.168.1.100:8000") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { rankingEnabled = !rankingEnabled }
                ) {
                    Checkbox(
                        checked = rankingEnabled,
                        onCheckedChange = { rankingEnabled = it }
                    )
                    Text("Exibir Ranking de Consumo", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(ipText, rankingEnabled) }) {
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
                    val cleanName = if (funcionario.nome.startsWith("VISITANTE (")) {
                        funcionario.nome.removePrefix("VISITANTE (").removeSuffix(")")
                    } else {
                        funcionario.nome
                    }
                    val initials = cleanName.split(" ")
                        .filter { it.isNotEmpty() }
                        .take(2).joinToString("") { it.first().uppercase() }
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
                    text = if (funcionario.codigo == "999999") "${funcionario.nome.uppercase()}" else funcionario.nome.uppercase(),
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
    ranking: List<RankingItem>, // <-- ADICIONADO PARÂMETRO AQUI
    isRankingEnabled: Boolean,
    onFuncionarioSelected: (Funcionario) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var isVisitor by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Lógica de filtro corrigida (removido erro de chaves)
    val filtered = remember(query, funcionarios) {
        val isNumeric = query.all { it.isDigit() }
        if (isNumeric) {
            funcionarios.filter { it.codigo.contains(query) }
        } else {
            if (query.length < 3) emptyList()
            else funcionarios.filter { it.nome.contains(query, ignoreCase = true) }
        }
    }

    val onSearchAction = {
        if (query.isNotEmpty()) {
            val isNumeric = query.all { it.isDigit() }
            val match = if (isNumeric) {
                funcionarios.find { it.codigo == query }
            } else {
                funcionarios.find { it.nome.equals(query, ignoreCase = true) }
            }

            if (match != null) {
                val finalFunc = if (isVisitor) {
                    Funcionario(codigo = "999999", nome = match.nome)
                } else {
                    match
                }
                onFuncionarioSelected(finalFunc)
                query = ""
                showError = false
            } else {
                showError = true
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Se a busca estiver vazia, empurra para o meio
        if (query.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
        }

        OutlinedTextField(
            value = query,
            onValueChange = { input ->
                query = input
                showError = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = { Text("Buscar...", color = Color.Gray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = GoldTan,
                unfocusedBorderColor = Color.DarkGray
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (showError) {
            Text(
                text = "Usuário não encontrado.",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { isVisitor = !isVisitor },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isVisitor, onCheckedChange = { isVisitor = it }, colors = CheckboxDefaults.colors(checkedColor = GoldTan))
            Text(text = "Selecionar como visitante", color = Color.White, fontSize = 14.sp)
        }

        // CONTEÚDO DINÂMICO
        Column(
            modifier = Modifier.weight(2f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (query.isNotEmpty()) {
                // Resultados da Busca
                // Resultados da Busca
                filtered.take(3).forEach { funcionario ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                // Se a checkbox de visitante estiver marcada, altera o objeto enviado
                                val finalFunc = if (isVisitor) {
                                    Funcionario(
                                        codigo = "999999",
                                        nome = "${funcionario.nome} (VISITANTE)"
                                    )
                                } else {
                                    funcionario
                                }
                                onFuncionarioSelected(finalFunc)
                                query = ""
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(funcionario.nome.uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Código: ${funcionario.codigo}", color = Color.Gray)
                        }
                    }
                }
            } else if (isRankingEnabled) {
                // RODAPÉ: RANKING REAL
                Spacer(modifier = Modifier.weight(1f)) // Empurra para o rodapé

                Text(
                    text = "TOP 3 CONSUMIDORES (MÊS)",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Agora usamos o ranking que veio por parâmetro!
                RankingPodium(ranking = ranking.take(3))

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
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
