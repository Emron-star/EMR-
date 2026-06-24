package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TradingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotConfigScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val botSettings by viewModel.botSettings.collectAsState()

    val scrollState = rememberScrollState()

    // Temporary local states for editing settings
    var stakeText by remember(botSettings.stake) { mutableStateOf(botSettings.stake.toString()) }
    var takeProfitText by remember(botSettings.takeProfit) { mutableStateOf(botSettings.takeProfit.toString()) }
    var stopLossText by remember(botSettings.stopLoss) { mutableStateOf(botSettings.stopLoss.toString()) }
    var martingaleMultText by remember(botSettings.martingaleMultiplier) { mutableStateOf(botSettings.martingaleMultiplier.toString()) }
    var useMartingale by remember(botSettings.useMartingale) { mutableStateOf(botSettings.useMartingale) }

    val strategies = listOf(
        "SMA_CROSS" to "SMA Crossover (3 vs 8 Ticks)",
        "RSI_OS" to "RSI Oversold/Overbought (30/70 Levels)",
        "BOLL_BAND" to "Bollinger Bands Breakout Reversals",
        "GEMINI_AI" to "Gemini 3.5 Quantitative AI Signal Engine",
        "DIGIT_MATCH" to "Deriv Digit - Matches",
        "DIGIT_DIFF" to "Deriv Digit - Differs",
        "DIGIT_EVEN" to "Deriv Digit - Even",
        "DIGIT_ODD" to "Deriv Digit - Odd",
        "DIGIT_OVER" to "Deriv Digit - Over",
        "DIGIT_UNDER" to "Deriv Digit - Under"
    )

    val symbols = listOf(
        "R_10" to "Volatility 10 Index",
        "R_100" to "Volatility 100 Index",
        "frxEURUSD" to "EUR/USD Forex",
        "cryBTCUSD" to "BTC/USD Crypto",
        "frxXAUUSD" to "XAU/USD Gold"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "BOT STRATEGY & PARAMETERS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 1.2.sp
        )

        // --- SECTION 1: Symbol & Market Selection ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. Active Market Asset",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                symbols.forEach { (sym, label) ->
                    val isSelected = botSettings.selectedSymbol == sym
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF334155) else Color.Transparent)
                            .clickable {
                                if (!botSettings.isRunning) {
                                    viewModel.connectToSymbol(sym)
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                if (!botSettings.isRunning) {
                                    viewModel.connectToSymbol(sym)
                                }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF3B82F6))
                        )
                    }
                }
            }
        }

        // --- SECTION 2: Quantitative Bot Strategies ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. Bot Trade Strategy",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                strategies.forEach { (stratKey, label) ->
                    val isSelected = botSettings.selectedStrategy == stratKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF334155) else Color.Transparent)
                            .clickable {
                                if (!botSettings.isRunning) {
                                    viewModel.updateSettings(
                                        strategy = stratKey,
                                        stake = botSettings.stake,
                                        takeProfit = botSettings.takeProfit,
                                        stopLoss = botSettings.stopLoss,
                                        useMartingale = useMartingale,
                                        martingaleMultiplier = botSettings.martingaleMultiplier,
                                        digitPrediction = botSettings.digitPrediction
                                    )
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            val strategyDescription = when (stratKey) {
                                "SMA_CROSS" -> "Executes contracts when short SMA crosses long SMA."
                                "RSI_OS" -> "Enters when RSI leaves oversold (<30) or overbought (>70)."
                                "BOLL_BAND" -> "Triggers on price breakouts of upper or lower volatility bounds."
                                "GEMINI_AI" -> "Calls Gemini 3.5 to process matrix ticks and supply structured signals."
                                "DIGIT_MATCH" -> "Wins when last spot digit matches your prediction. High return."
                                "DIGIT_DIFF" -> "Wins when last spot digit differs from your prediction. High probability."
                                "DIGIT_EVEN" -> "Wins when last spot digit is even."
                                "DIGIT_ODD" -> "Wins when last spot digit is odd."
                                "DIGIT_OVER" -> "Wins when last spot digit is strictly greater than barrier."
                                "DIGIT_UNDER" -> "Wins when last spot digit is strictly less than barrier."
                                else -> ""
                            }
                            Text(
                                text = strategyDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                if (!botSettings.isRunning) {
                                    viewModel.updateSettings(
                                        strategy = stratKey,
                                        stake = botSettings.stake,
                                        takeProfit = botSettings.takeProfit,
                                        stopLoss = botSettings.stopLoss,
                                        useMartingale = useMartingale,
                                        martingaleMultiplier = botSettings.martingaleMultiplier,
                                        digitPrediction = botSettings.digitPrediction
                                    )
                                }
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF3B82F6))
                        )
                    }
                }
            }
        }

        // --- SECTION 2.5: Digit Prediction Target selector ---
        if (botSettings.selectedStrategy.startsWith("DIGIT_") && botSettings.selectedStrategy != "DIGIT_EVEN" && botSettings.selectedStrategy != "DIGIT_ODD") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2.5 Digit Target Prediction (0 - 9)",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose the target digit used for prediction / barrier evaluation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (0..9).forEach { digit ->
                            val isDigitSelected = botSettings.digitPrediction == digit
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDigitSelected) Color(0xFF3B82F6) else Color(0xFF334155))
                                    .clickable {
                                        if (!botSettings.isRunning) {
                                            viewModel.updateSettings(
                                                strategy = botSettings.selectedStrategy,
                                                stake = botSettings.stake,
                                                takeProfit = botSettings.takeProfit,
                                                stopLoss = botSettings.stopLoss,
                                                useMartingale = useMartingale,
                                                martingaleMultiplier = botSettings.martingaleMultiplier,
                                                digitPrediction = digit
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit.toString(),
                                    color = if (isDigitSelected) Color.White else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 3: Risk Management Parameters ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "3. Risk Parameters ($)",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Stake
                OutlinedTextField(
                    value = stakeText,
                    onValueChange = { stakeText = it },
                    label = { Text("Base Trade Stake ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stake_input_field"),
                    singleLine = true,
                    enabled = !botSettings.isRunning
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Row for TP and SL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = takeProfitText,
                        onValueChange = { takeProfitText = it },
                        label = { Text("Take Profit ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tp_input_field"),
                        singleLine = true,
                        enabled = !botSettings.isRunning
                    )

                    OutlinedTextField(
                        value = stopLossText,
                        onValueChange = { stopLossText = it },
                        label = { Text("Stop Loss ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sl_input_field"),
                        singleLine = true,
                        enabled = !botSettings.isRunning
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Martingale settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Martingale Multiplier",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Doubles or scales the stake automatically after a loss to recover losses quickly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    }
                    Switch(
                        checked = useMartingale,
                        onCheckedChange = { useMartingale = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF3B82F6)),
                        enabled = !botSettings.isRunning
                    )
                }

                if (useMartingale) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = martingaleMultText,
                        onValueChange = { martingaleMultText = it },
                        label = { Text("Martingale Multiplier (e.g. 2.0)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !botSettings.isRunning
                    )
                }
            }
        }

        // --- Save Changes Button ---
        if (!botSettings.isRunning) {
            Button(
                onClick = {
                    val stake = stakeText.toDoubleOrNull() ?: botSettings.stake
                    val tp = takeProfitText.toDoubleOrNull() ?: botSettings.takeProfit
                    val sl = stopLossText.toDoubleOrNull() ?: botSettings.stopLoss
                    val mult = martingaleMultText.toDoubleOrNull() ?: botSettings.martingaleMultiplier

                    viewModel.updateSettings(
                        strategy = botSettings.selectedStrategy,
                        stake = stake,
                        takeProfit = tp,
                        stopLoss = sl,
                        useMartingale = useMartingale,
                        martingaleMultiplier = mult,
                        digitPrediction = botSettings.digitPrediction
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_config_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Text(text = "APPLY BOT PARAMETERS", fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Trading bot is currently active. Pause the bot execution on the Trade tab to modify risk variables or strategies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }
        }
    }
}
