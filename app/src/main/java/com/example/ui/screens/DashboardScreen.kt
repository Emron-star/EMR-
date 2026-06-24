package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.TickData
import com.example.ui.ActiveContract
import com.example.ui.TradingViewModel

@Composable
fun DashboardScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val tickHistory by viewModel.tickHistory.collectAsState()
    val activeContract by viewModel.activeContract.collectAsState()
    val botSettings by viewModel.botSettings.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val tradeLogs by viewModel.tradeLogs.collectAsState()
    val isLookingSignal by viewModel.isAILookingForSignal.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // High-fidelity dark slate
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Metric Panels Row ---
        BalanceMetricCard(
            balance = totalBalance,
            tradeLogs = tradeLogs
        )

        // --- Active Chart ---
        RealTimeChartCard(
            symbolLabel = viewModel.getSymbolLabel(botSettings.selectedSymbol),
            selectedSymbol = botSettings.selectedSymbol,
            ticks = tickHistory,
            activeContract = activeContract,
            selectedStrategy = botSettings.selectedStrategy,
            digitPrediction = botSettings.digitPrediction
        )

        // --- Active Contract / Signal Search Tracker ---
        AnimatedContent(
            targetState = activeContract,
            transitionSpec = {
                fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
            },
            label = "ContractPanel"
        ) { contract ->
            if (contract != null) {
                ActiveContractCard(contract = contract)
            } else if (isLookingSignal) {
                AiSearchingCard()
            }
        }

        // --- Quick Run / Stop Control Card ---
        QuickControlCard(
            isRunning = botSettings.isRunning,
            selectedStrategy = botSettings.selectedStrategy,
            selectedSymbolLabel = viewModel.getSymbolLabel(botSettings.selectedSymbol),
            onToggle = { viewModel.toggleBot() }
        )
    }
}

@Composable
fun BalanceMetricCard(
    balance: Double,
    tradeLogs: List<com.example.data.model.TradeLog>
) {
    // Calculate Stats
    val totalTrades = tradeLogs.size
    val wonTrades = tradeLogs.count { it.status == "WON" }
    val winRate = if (totalTrades > 0) (wonTrades.toDouble() / totalTrades) * 100 else 0.0
    val netProfit = tradeLogs.sumOf { it.profit }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("balance_metric_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DEMO BALANCE",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = String.format("$%,.2f", balance),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Divider(color = Color(0xFF334155), thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn(
                    title = "NET PROFIT",
                    value = String.format("$%,.2f", netProfit),
                    valueColor = if (netProfit >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                )
                VerticalDivider()
                StatColumn(
                    title = "WIN RATE",
                    value = String.format("%.1f%%", winRate),
                    valueColor = Color(0xFF3B82F6)
                )
                VerticalDivider()
                StatColumn(
                    title = "TRADES",
                    value = "$totalTrades",
                    valueColor = Color.White
                )
            }
        }
    }
}

@Composable
fun StatColumn(
    title: String,
    value: String,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = valueColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(Color(0xFF334155))
    )
}

@Composable
fun RealTimeChartCard(
    symbolLabel: String,
    selectedSymbol: String,
    ticks: List<TickData>,
    activeContract: ActiveContract?,
    selectedStrategy: String,
    digitPrediction: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = symbolLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Live Tick Streaming via Deriv Broker",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
                
                // Current Price Indicator
                val lastPrice = ticks.lastOrNull()?.quote ?: 0.0
                Text(
                    text = if (lastPrice > 0) String.format("%.5f", lastPrice) else "Awaiting Data",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Drawing Canvas for Trading Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                if (ticks.size < 2) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF3B82F6)
                    )
                } else {
                    val priceValues = ticks.map { it.quote }
                    val maxVal = priceValues.maxOrNull() ?: 1.0
                    val minVal = priceValues.minOrNull() ?: 0.0
                    val delta = if (maxVal - minVal == 0.0) 1.0 else maxVal - minVal

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        val points = ticks.mapIndexed { idx, t ->
                            val x = idx * (width / (ticks.size - 1))
                            // invert Y as 0 is at top
                            val y = height - ((t.quote - minVal) / delta * height).toFloat()
                            Offset(x, y)
                        }

                        // Draw Gradient Area under path
                        val gradientPath = Path().apply {
                            moveTo(0f, height)
                            points.forEachIndexed { i, pt ->
                                if (i == 0) lineTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                            }
                            lineTo(width, height)
                            close()
                        }
                        drawPath(
                            path = gradientPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0x3310B981), Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // Draw Connecting Price Line
                        val priceLinePath = Path().apply {
                            points.forEachIndexed { i, pt ->
                                if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                            }
                        }
                        drawPath(
                            path = priceLinePath,
                            color = Color(0xFF10B981),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // If active contract is open, draw dashed Entry Threshold line
                        activeContract?.let { contract ->
                            val entryPrice = contract.entryPrice
                            if (entryPrice in minVal..maxVal) {
                                val entryY = height - ((entryPrice - minVal) / delta * height).toFloat()
                                drawLine(
                                    color = Color(0xFF3B82F6),
                                    start = Offset(0f, entryY),
                                    end = Offset(width, entryY),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            }
                        }

                        // Draw Dot on current price (last index)
                        points.lastOrNull()?.let { lastPoint ->
                            drawCircle(
                                color = Color(0xFF10B981),
                                radius = 4.dp.toPx(),
                                center = lastPoint
                            )
                        }
                    }
                }
            }

            // --- Digit Analysis Section ---
            if (selectedStrategy.startsWith("DIGIT_")) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFF334155), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LATEST DIGITS TAPE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Target: $digitPrediction",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF3B82F6),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lastTicks = ticks.takeLast(10)
                    lastTicks.forEach { tick ->
                        val digit = getLastDigit(tick.quote, selectedSymbol)
                        val isTarget = digit == digitPrediction
                        val isEven = digit % 2 == 0

                        val (bgColor, textColor) = when (selectedStrategy) {
                            "DIGIT_MATCH" -> {
                                if (isTarget) Color(0xFF3B82F6) to Color.White
                                else Color(0xFF334155) to Color(0xFF94A3B8)
                            }
                            "DIGIT_DIFF" -> {
                                if (isTarget) Color(0xFFEF4444) to Color.White
                                else Color(0xFF10B981) to Color.White
                            }
                            "DIGIT_EVEN" -> {
                                if (isEven) Color(0xFF10B981) to Color.White
                                else Color(0xFF334155) to Color(0xFF94A3B8)
                            }
                            "DIGIT_ODD" -> {
                                if (!isEven) Color(0xFF10B981) to Color.White
                                else Color(0xFF334155) to Color(0xFF94A3B8)
                            }
                            "DIGIT_OVER" -> {
                                if (digit > digitPrediction) Color(0xFF10B981) to Color.White
                                else Color(0xFF334155) to Color(0xFF94A3B8)
                            }
                            "DIGIT_UNDER" -> {
                                if (digit < digitPrediction) Color(0xFF10B981) to Color.White
                                else Color(0xFF334155) to Color(0xFF94A3B8)
                            }
                            else -> {
                                Color(0xFF334155) to Color(0xFF94A3B8)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(bgColor, RoundedCornerShape(50.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = digit.toString(),
                                color = textColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Add a simple stats distribution bar chart
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "DIGIT FREQUENCY DISTRIBUTION (Last 50 ticks)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val digitCounts = IntArray(10)
                ticks.forEach { tick ->
                    val digit = getLastDigit(tick.quote, selectedSymbol)
                    if (digit in 0..9) {
                        digitCounts[digit]++
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxCount = digitCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
                    (0..9).forEach { d ->
                        val count = digitCounts[d]
                        val pct = count.toFloat() / maxCount
                        val isTarget = d == digitPrediction

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height((32 * pct).dp.coerceAtLeast(4.dp))
                                    .background(
                                        if (isTarget) Color(0xFF3B82F6)
                                        else if (d % 2 == 0) Color(0xFF10B981)
                                        else Color(0xFFEF4444),
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = d.toString(),
                                color = if (isTarget) Color(0xFF3B82F6) else Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getLastDigit(price: Double, symbol: String): Int {
    val pip = when (symbol) {
        "R_10" -> 2
        "R_100" -> 2
        "frxEURUSD" -> 5
        "cryBTCUSD" -> 2
        "frxXAUUSD" -> 2
        else -> 2
    }
    val formatted = String.format(java.util.Locale.US, "%.${pip}f", price)
    val lastChar = formatted.lastOrNull { it.isDigit() }
    return lastChar?.toString()?.toIntOrNull() ?: 0
}

@Composable
fun ActiveContractCard(contract: ActiveContract) {
    val lastDigit = getLastDigit(contract.currentPrice, contract.symbol)
    val target = contract.targetDigit ?: 5
    val isDigitContract = contract.tradeType in listOf("MATCH", "DIFF", "EVEN", "ODD", "OVER", "UNDER")

    val isProfit = when (contract.tradeType) {
        "RISE" -> contract.currentPrice > contract.entryPrice
        "FALL" -> contract.currentPrice < contract.entryPrice
        "MATCH" -> lastDigit == target
        "DIFF" -> lastDigit != target
        "EVEN" -> lastDigit % 2 == 0
        "ODD" -> lastDigit % 2 != 0
        "OVER" -> lastDigit > target
        "UNDER" -> lastDigit < target
        else -> contract.currentPrice > contract.entryPrice
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_contract_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)), // Dark indigo accent
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(if (isProfit) Color(0xFF10B981) else Color(0xFFEF4444))
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val directionIcon = when (contract.tradeType) {
                        "RISE", "MATCH", "EVEN", "OVER" -> Icons.Default.KeyboardArrowUp
                        else -> Icons.Default.KeyboardArrowDown
                    }
                    val directionColor = when (contract.tradeType) {
                        "RISE", "MATCH", "EVEN", "OVER" -> Color(0xFF10B981)
                        else -> Color(0xFFEF4444)
                    }
                    Icon(
                        imageVector = directionIcon,
                        contentDescription = "Contract direction",
                        tint = directionColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACTIVE ${contract.tradeType} CONTRACT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Badge(
                    containerColor = if (isProfit) Color(0xFF10B981) else Color(0xFFEF4444),
                    contentColor = Color.White
                ) {
                    Text(
                        text = if (isProfit) "IN PROFIT" else "IN LOSS",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar representing tick steps
            LinearProgressIndicator(
                progress = { (contract.maxTicks - contract.ticksRemaining).toFloat() / contract.maxTicks },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Color(0xFF6366F1),
                trackColor = Color(0xFF312E81)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isDigitContract) "Target/Type" else "Entry Barrier",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC7D2FE)
                    )
                    Text(
                        text = if (isDigitContract) {
                            when (contract.tradeType) {
                                "EVEN" -> "EVEN DIGIT"
                                "ODD" -> "ODD DIGIT"
                                "OVER" -> "> $target"
                                "UNDER" -> "< $target"
                                else -> "DIGIT: $target"
                            }
                        } else {
                            String.format("%.4f", contract.entryPrice)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isDigitContract) "Last Digit Spot" else "Current Spot",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC7D2FE)
                    )
                    Text(
                        text = if (isDigitContract) {
                            "[$lastDigit]  (Spot: ${String.format("%.4f", contract.currentPrice)})"
                        } else {
                            String.format("%.4f", contract.currentPrice)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isProfit) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Duration", style = MaterialTheme.typography.bodySmall, color = Color(0xFFC7D2FE))
                    Text(
                        text = "${contract.ticksRemaining} Ticks Left",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AiSearchingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(Color(0xFF3B82F6))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color(0xFF3B82F6),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Gemini AI Signal Search",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Analyzing tick sequence matrices to locate breakout signals...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun QuickControlCard(
    isRunning: Boolean,
    selectedStrategy: String,
    selectedSymbolLabel: String,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bot_control_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AUTOMATED EXECUTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = selectedSymbolLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Strategy: " + selectedStrategy.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF60A5FA)
                )
            }

            Button(
                onClick = onToggle,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("toggle_bot_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFEF4444) else Color(0xFF10B981)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Stop bot" else "Start bot"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "STOP BOT" else "RUN BOT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
