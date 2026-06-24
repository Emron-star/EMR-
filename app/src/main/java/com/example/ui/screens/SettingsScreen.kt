package com.example.ui.screens

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.TradingViewModel

@Composable
fun SettingsScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val apiCredentials by viewModel.apiCredentials.collectAsState()
    val botSettings by viewModel.botSettings.collectAsState()

    var appIdText by remember(apiCredentials.appId) { mutableStateOf(apiCredentials.appId) }
    var apiTokenText by remember(apiCredentials.apiToken) { mutableStateOf(apiCredentials.apiToken) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showOAuthDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "DERIV API INTEGRATION",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // --- SECTION 0.5: OAuth Connection ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure Connect",
                        tint = Color(0xFF3B82F6)
                    )
                    Text(
                        text = "Deriv OAuth 2.0 Secure Connect",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Log in securely using Deriv's official authentication server. The application will intercept the authorized redirect to safely establish the API credentials on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Button(
                    onClick = {
                        showOAuthDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("oauth_connect_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Login",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "CONNECT DERIV ACCOUNT", fontWeight = FontWeight.Bold)
                }

                if (apiTokenText.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(Color(0x1F10B981), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "OAuth API Connection Active",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // --- SECTION 1: Credentials Form ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "API Configurations",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Specify your registered Deriv App ID and Personal Access Token (PAT) to authorize contract execution on live servers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                // App ID Input
                OutlinedTextField(
                    value = appIdText,
                    onValueChange = { appIdText = it },
                    label = { Text("Deriv App ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_id_input"),
                    singleLine = true
                )

                // API Token Input
                OutlinedTextField(
                    value = apiTokenText,
                    onValueChange = { apiTokenText = it },
                    label = { Text("Personal Access Token (PAT)") },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pat_input"),
                    singleLine = true
                )

                Button(
                    onClick = {
                        viewModel.updateApiCredentials(appIdText, apiTokenText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_api_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "SAVE INTEGRATION KEY", fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- SECTION 2: Monetization System ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Platform Monetization",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "As an official Deriv API developer partner, this software is set up to accumulate a broker markup of up to 3% on contracts executed via authorized App IDs. Additional revenue is generated through premium strategy unlocks and referral registrations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    lineHeight = 16.sp
                )
            }
        }

        // --- SECTION 3: Legal Compliance & Disclaimer ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444)),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFEF4444))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LEGAL COMPLIANCE NOTICE",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFFCA5A5),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Trading CFDs, options, and volatility synthetic indices carries high investment risks. Automated trading algorithms, technical overlays, and Gemini predictive indicators represent statistical models and do not guarantee profitable returns. Users are entirely responsible for managing account risk limits, leverage multipliers, and ensuring compliance with financial authorities in their respective local jurisdictions. Double-check all risk metrics on demo networks before connecting real financial funds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFCA5A5),
                    lineHeight = 16.sp
                )
            }
        }
    }

    // --- WebView OAuth Dialog ---
    if (showOAuthDialog) {
        var isPageLoading by remember { mutableStateOf(true) }
        var webViewRef by remember { mutableStateOf<WebView?>(null) }

        Dialog(
            onDismissRequest = { showOAuthDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)),
                color = Color(0xFF0F172A)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // --- Header ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure Connection",
                                tint = Color(0xFF10B981)
                            )
                            Column {
                                Text(
                                    text = "Deriv Secure Authentication",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "OAuth 2.0 SSL Connection",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Reload button
                            IconButton(
                                onClick = {
                                    webViewRef?.reload()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = Color.White
                                )
                            }

                            // Close button
                            IconButton(
                                onClick = { showOAuthDialog = false }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // --- Progress Indicator ---
                    if (isPageLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF3B82F6),
                            trackColor = Color(0xFF1E293B)
                        )
                    } else {
                        Divider(color = Color(0xFF334155), thickness = 1.dp)
                    }

                    // --- WebView ---
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.White)
                    ) {
                        AndroidView(
                            factory = { context ->
                                WebView(context).apply {
                                    webViewRef = this
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                    }
                                    
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                            super.onPageStarted(view, url, favicon)
                                            isPageLoading = true
                                            
                                            url?.let {
                                                val parsed = parseDerivRedirectUrl(it)
                                                if (parsed != null) {
                                                    viewModel.updateApiCredentials(appIdText.ifEmpty { "1089" }, parsed.second)
                                                    Toast.makeText(context, "Connected successfully via Deriv OAuth!", Toast.LENGTH_LONG).show()
                                                    showOAuthDialog = false
                                                }
                                            }
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isPageLoading = false
                                        }

                                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                            val url = request?.url?.toString()
                                            url?.let {
                                                val parsed = parseDerivRedirectUrl(it)
                                                if (parsed != null) {
                                                    viewModel.updateApiCredentials(appIdText.ifEmpty { "1089" }, parsed.second)
                                                    Toast.makeText(context, "Connected successfully via Deriv OAuth!", Toast.LENGTH_LONG).show()
                                                    showOAuthDialog = false
                                                    return true
                                                }
                                            }
                                            return false
                                        }
                                    }

                                    val finalAppId = appIdText.ifEmpty { "1089" }
                                    val authUrl = "https://oauth.deriv.com/oauth2/authorize?app_id=$finalAppId"
                                    loadUrl(authUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

private fun parseDerivRedirectUrl(url: String): Pair<String, String>? {
    if (url.contains("token1=")) {
        val tokenPart = url.substringAfter("token1=").substringBefore("&")
        val acctPart = if (url.contains("acct1=")) {
            url.substringAfter("acct1=").substringBefore("&")
        } else {
            ""
        }
        if (tokenPart.isNotEmpty()) {
            return Pair(acctPart, tokenPart)
        }
    }
    return null
}
