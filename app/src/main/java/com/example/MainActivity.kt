package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TradingViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                val viewModel: TradingViewModel = viewModel()
                var currentTab by remember { mutableIntStateOf(0) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A) // High-fidelity Slate-Dark Background
                ) {
                    BoxWithConstraints {
                        val isTablet = maxWidth >= 600.dp

                        Row(modifier = Modifier.fillMaxSize()) {
                            // Render Navigation Rail if wide screen (Adaptive tablet layout)
                            if (isTablet) {
                                NavigationRail(
                                    containerColor = Color(0xFF1E293B),
                                    modifier = Modifier.fillMaxHeight(),
                                    header = {
                                        Text(
                                            text = "DT",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF3B82F6),
                                            modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                    }
                                ) {
                                    NavigationRailItem(
                                        selected = currentTab == 0,
                                        onClick = { currentTab = 0 },
                                        icon = { Icon(Icons.Default.Home, contentDescription = "Trade") },
                                        label = { Text("Trade") },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            unselectedIconColor = Color(0xFF94A3B8),
                                            selectedTextColor = Color.White,
                                            unselectedTextColor = Color(0xFF94A3B8),
                                            indicatorColor = Color(0xFF3B82F6)
                                        )
                                    )
                                    NavigationRailItem(
                                        selected = currentTab == 1,
                                        onClick = { currentTab = 1 },
                                        icon = { Icon(Icons.Default.Build, contentDescription = "Bots") },
                                        label = { Text("Bots") },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            unselectedIconColor = Color(0xFF94A3B8),
                                            selectedTextColor = Color.White,
                                            unselectedTextColor = Color(0xFF94A3B8),
                                            indicatorColor = Color(0xFF3B82F6)
                                        )
                                    )
                                    NavigationRailItem(
                                        selected = currentTab == 2,
                                        onClick = { currentTab = 2 },
                                        icon = { Icon(Icons.Default.List, contentDescription = "History") },
                                        label = { Text("History") },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            unselectedIconColor = Color(0xFF94A3B8),
                                            selectedTextColor = Color.White,
                                            unselectedTextColor = Color(0xFF94A3B8),
                                            indicatorColor = Color(0xFF3B82F6)
                                        )
                                    )
                                    NavigationRailItem(
                                        selected = currentTab == 3,
                                        onClick = { currentTab = 3 },
                                        icon = { Icon(Icons.Default.Face, contentDescription = "AI") },
                                        label = { Text("AI") },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            unselectedIconColor = Color(0xFF94A3B8),
                                            selectedTextColor = Color.White,
                                            unselectedTextColor = Color(0xFF94A3B8),
                                            indicatorColor = Color(0xFF3B82F6)
                                        )
                                    )
                                    NavigationRailItem(
                                        selected = currentTab == 4,
                                        onClick = { currentTab = 4 },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                        label = { Text("Settings") },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            unselectedIconColor = Color(0xFF94A3B8),
                                            selectedTextColor = Color.White,
                                            unselectedTextColor = Color(0xFF94A3B8),
                                            indicatorColor = Color(0xFF3B82F6)
                                        )
                                    )
                                }
                            }

                            // Main Area
                            Scaffold(
                                modifier = Modifier.weight(1f),
                                topBar = {
                                    TopAppBar(
                                        title = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "EMRON TRADER",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 18.sp,
                                                    color = Color.White
                                                )
                                                // Simulated Connection Status Badge
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            Color(0x3310B981),
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "DEMO NETWORK",
                                                        color = Color(0xFF10B981),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = Color(0xFF1E293B),
                                            titleContentColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("app_bar")
                                    )
                                },
                                bottomBar = {
                                    // Only show Bottom Navigation if compact (mobile screen)
                                    if (!isTablet) {
                                        NavigationBar(
                                            containerColor = Color(0xFF1E293B),
                                            modifier = Modifier.testTag("bottom_nav")
                                        ) {
                                            NavigationBarItem(
                                                selected = currentTab == 0,
                                                onClick = { currentTab = 0 },
                                                icon = { Icon(Icons.Default.Home, contentDescription = "Trade") },
                                                label = { Text("Trade") },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = Color.White,
                                                    unselectedIconColor = Color(0xFF94A3B8),
                                                    selectedTextColor = Color.White,
                                                    unselectedTextColor = Color(0xFF94A3B8),
                                                    indicatorColor = Color(0xFF3B82F6)
                                                )
                                            )
                                            NavigationBarItem(
                                                selected = currentTab == 1,
                                                onClick = { currentTab = 1 },
                                                icon = { Icon(Icons.Default.Build, contentDescription = "Bots") },
                                                label = { Text("Bots") },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = Color.White,
                                                    unselectedIconColor = Color(0xFF94A3B8),
                                                    selectedTextColor = Color.White,
                                                    unselectedTextColor = Color(0xFF94A3B8),
                                                    indicatorColor = Color(0xFF3B82F6)
                                                )
                                            )
                                            NavigationBarItem(
                                                selected = currentTab == 2,
                                                onClick = { currentTab = 2 },
                                                icon = { Icon(Icons.Default.List, contentDescription = "History") },
                                                label = { Text("History") },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = Color.White,
                                                    unselectedIconColor = Color(0xFF94A3B8),
                                                    selectedTextColor = Color.White,
                                                    unselectedTextColor = Color(0xFF94A3B8),
                                                    indicatorColor = Color(0xFF3B82F6)
                                                )
                                            )
                                            NavigationBarItem(
                                                selected = currentTab == 3,
                                                onClick = { currentTab = 3 },
                                                icon = { Icon(Icons.Default.Face, contentDescription = "AI") },
                                                label = { Text("AI") },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = Color.White,
                                                    unselectedIconColor = Color(0xFF94A3B8),
                                                    selectedTextColor = Color.White,
                                                    unselectedTextColor = Color(0xFF94A3B8),
                                                    indicatorColor = Color(0xFF3B82F6)
                                                )
                                            )
                                            NavigationBarItem(
                                                selected = currentTab == 4,
                                                onClick = { currentTab = 4 },
                                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                                label = { Text("Settings") },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = Color.White,
                                                    unselectedIconColor = Color(0xFF94A3B8),
                                                    selectedTextColor = Color.White,
                                                    unselectedTextColor = Color(0xFF94A3B8),
                                                    indicatorColor = Color(0xFF3B82F6)
                                                )
                                            )
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                val contentModifier = Modifier.padding(innerPadding)
                                when (currentTab) {
                                    0 -> DashboardScreen(viewModel = viewModel, modifier = contentModifier)
                                    1 -> BotConfigScreen(viewModel = viewModel, modifier = contentModifier)
                                    2 -> TradeHistoryScreen(viewModel = viewModel, modifier = contentModifier)
                                    3 -> AiAssistantScreen(viewModel = viewModel, modifier = contentModifier)
                                    4 -> SettingsScreen(viewModel = viewModel, modifier = contentModifier)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
