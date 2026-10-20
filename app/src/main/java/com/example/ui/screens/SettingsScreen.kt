package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.network.CloudflareRelayClient
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val relayClient = viewModel.relayClient
    val scope = rememberCoroutineScope()

    var customApiKey by remember { mutableStateOf(relayClient.getCustomApiKey()) }
    var workerUrl by remember { mutableStateOf(relayClient.getWorkerRelayUrl()) }
    var workerToken by remember { mutableStateOf(relayClient.getWorkerRelayToken()) }
    var useRelay by remember { mutableStateOf(relayClient.isRelayEnabled()) }
    var selectedModel by remember { mutableStateOf(relayClient.getSelectedModel()) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }
    var showWorkerCode by remember { mutableStateOf(false) }

    val hasActiveApiKey = relayClient.getEffectiveApiKey().isNotBlank() &&
            relayClient.getEffectiveApiKey() != "MY_GEMINI_API_KEY"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "تنظیمات هوش مصنوعی و اتصال",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Primary Connection Mode Card (Direct Gemini API)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "اتصال مستقیم به گوگل جمینای",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (!useRelay) "حالت فعال: ارسال مستقیم صوت به سرورهای گوگل" else "در حال حاضر رله کلودفلر فعال است",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (!useRelay) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (!useRelay) "مستقیم (پیش‌فرض)" else "غیرفعال",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (!useRelay) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom API Key input
                OutlinedTextField(
                    value = customApiKey,
                    onValueChange = { customApiKey = it },
                    label = { Text("کلید اختصاصی Gemini API (اختیاری)") },
                    placeholder = { Text("AIzaSy...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (customApiKey.isNotBlank()) {
                            IconButton(onClick = {
                                customApiKey = ""
                                relayClient.setCustomApiKey("")
                                Toast.makeText(context, "کلید اختصاصی پاک شد؛ کلید پیش‌فرض استفاده می‌شود.", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(imageVector = Icons.Default.Restore, contentDescription = "پاک کردن")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (hasActiveApiKey) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasActiveApiKey) "کلید هوش مصنوعی فعال است" else "از کلید محیطی برنامه استفاده می‌شود",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    if (customApiKey.isNotBlank()) {
                        Button(
                            onClick = {
                                relayClient.setCustomApiKey(customApiKey)
                                Toast.makeText(context, "کلید اختصاصی ذخیره شد.", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ذخیره کلید")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // AI Model Selection Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "مدل تبدیل صوت هوش مصنوعی",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "مدل مورد نظر برای پیاده‌سازی صوت کلاس",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val models = listOf(
                    "gemini-3.5-transcribe" to "gemini-3.5-transcribe (پیشنهادی صوتی)",
                    "gemini-3.8-flash" to "gemini-3.8-flash (سریع)",
                    "gemini-3.5-flash" to "gemini-3.5-flash (پایدار)"
                )

                models.forEach { (modelKey, label) ->
                    val isSelected = selectedModel == modelKey
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedModel = modelKey
                            relayClient.setSelectedModel(modelKey)
                        },
                        label = { Text(label) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cloudflare Worker Relay Card (Optional Proxy)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = if (useRelay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "رله ورکر کلودفلر (اختیاری)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (useRelay) "فعال - ارسال از طریق رله" else "غیرفعال - اتصال به صورت مستقیم است",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Switch(
                        checked = useRelay,
                        onCheckedChange = { checked ->
                            useRelay = checked
                            relayClient.setRelayEnabled(checked)
                            Toast.makeText(
                                context,
                                if (checked) "رله کلودفلر فعال شد." else "اتصال مستقیم به جمینای فعال شد.",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.testTag("relay_switch")
                    )
                }

                if (useRelay) {
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = workerUrl,
                        onValueChange = { workerUrl = it },
                        label = { Text("آدرس رله ورکر (Worker Relay URL)") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    workerUrl = CloudflareRelayClient.DEFAULT_WORKER_URL
                                    relayClient.setWorkerRelayUrl(workerUrl)
                                    Toast.makeText(context, "آدرس به مقدار پیش‌فرض تغییر یافت.", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Restore, contentDescription = "بازنشانی")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("worker_url_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = workerToken,
                        onValueChange = { workerToken = it },
                        label = { Text("توکن احراز هویت ورکر / کلودفلر (اختیاری)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                relayClient.setWorkerRelayUrl(workerUrl)
                                relayClient.setWorkerRelayToken(workerToken)
                                Toast.makeText(context, "تنظیمات رله ذخیره شد.", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ذخیره رله")
                        }

                        OutlinedButton(
                            onClick = {
                                isTestingConnection = true
                                connectionStatus = null
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val client = OkHttpClient.Builder()
                                            .connectTimeout(8, TimeUnit.SECONDS)
                                            .build()
                                        val reqBuilder = Request.Builder().url(workerUrl)
                                        if (workerToken.isNotBlank()) {
                                            reqBuilder.addHeader("Authorization", "Bearer $workerToken")
                                        }
                                        val resp = client.newCall(reqBuilder.build()).execute()
                                        withContext(Dispatchers.Main) {
                                            connectionStatus = "اتصال رله برقرار شد (${resp.code})"
                                            isTestingConnection = false
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            connectionStatus = "خطا در تست: ${e.message}"
                                            isTestingConnection = false
                                        }
                                    }
                                }
                            },
                            enabled = !isTestingConnection
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تست اتصال رله")
                        }
                    }

                    connectionStatus?.let { status ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (status.startsWith("اتصال")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cloudflare Worker Script & Code Viewer Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "کد رله کلودفلر جهت ساخت سرور پروکسی", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "در صورتی که می‌خواهید رله اختصاصی خودتان را در کلودفلر ایجاد کنید:",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Cloudflare Worker Script", CloudflareRelayClient.CLOUDFLARE_WORKER_CODE)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "کد اسکریپت رله کلودفلر در حافظه کپی شد.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("کپی کردن کد رله")
                    }

                    OutlinedButton(
                        onClick = { showWorkerCode = !showWorkerCode },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (showWorkerCode) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showWorkerCode) "بستن کد" else "مشاهده کد")
                    }
                }

                if (showWorkerCode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = CloudflareRelayClient.CLOUDFLARE_WORKER_CODE,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF38BDF8),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // About & Instructions Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "راهنمای استفاده و فرمت‌ها", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• اتصال مستقیم به هوش مصنوعی گوگل (جمینای) برای تبدیل صدا فعال است.\n• پشتیبانی از انواع فرمت‌های صوتی: MP3، M4A، WAV، AAC، OGG، AMR و FLAC.\n• نام‌گذاری خودکار بر اساس برنامه کلاسی هفتگی: «[روز] - [نام درس] - جلسه [شماره] - پارت [شماره]»\n• خروجی مستقیم فایل PDF استاندارد با پشتیبانی کامل از متن فارسی راست‌به‌چپ (RTL).\n• امکان کپی فوری متن برای اشتراک در تلگرام، ایتا یا جزوات.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
