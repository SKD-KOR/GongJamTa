package com.example.studyfocus

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri

import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.studyfocus.data.AppDatabase
import com.example.studyfocus.data.StudyRecord
import com.example.studyfocus.data.TimerManager
import com.example.studyfocus.ui.theme.StudyFocusTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyFocusTheme {
                MainAppScreen()
            }
        }
    }
}

enum class ScreenTab(val title: String) {
    TIMER("타이머"),
    APPS("허용 앱"),
    HISTORY("공부 기록")
}

data class AppItem(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val isEssential: Boolean = false
)

enum class AppFilterCategory(val label: String) {
    ALL("전체"),
    ALLOWED("허용됨"),
    ESSENTIAL("기본 필수"),
    UNALLOWED("차단됨")
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun hasOverlayPermission(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d시간 %d분 %d초", hours, minutes, seconds)
    } else if (minutes > 0) {
        String.format("%d분 %d초", minutes, seconds)
    } else {
        String.format("%d초", seconds)
    }
}

fun formatTimeOnly(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun MainAppScreen() {
    var selectedTab by remember { mutableStateOf(ScreenTab.TIMER) }
    val allowedPackages = remember { mutableStateListOf<String>() }
    var isTimerRunningGlobal by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.TIMER,
                    onClick = { selectedTab = ScreenTab.TIMER },
                    label = { Text("타이머", fontWeight = if (selectedTab == ScreenTab.TIMER) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "타이머") }
                )
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.APPS,
                    onClick = { selectedTab = ScreenTab.APPS },
                    label = { Text("허용 앱", fontWeight = if (selectedTab == ScreenTab.APPS) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "허용 앱") }
                )
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.HISTORY,
                    onClick = { selectedTab = ScreenTab.HISTORY },
                    label = { Text("기록 통계", fontWeight = if (selectedTab == ScreenTab.HISTORY) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "기록 통계") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                ScreenTab.TIMER -> TimerScreen(
                    allowedCount = allowedPackages.size,
                    allowedPackages = allowedPackages,
                    onTimerStateChange = { isTimerRunningGlobal = it }
                )
                ScreenTab.APPS -> AllowedAppsScreen(
                    allowedPackages = allowedPackages,
                    onToggle = { pkg ->
                        if (allowedPackages.contains(pkg)) {
                            allowedPackages.remove(pkg)
                        } else {
                            allowedPackages.add(pkg)
                        }
                    }
                )
                ScreenTab.HISTORY -> HistoryScreen()
            }
        }
    }
}

// 1. 타이머 화면 (TimerManager 연동으로 액티비티 재시작 시 시간 유지)
@Composable
fun TimerScreen(
    allowedCount: Int,
    allowedPackages: List<String>,
    onTimerStateChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val timerManager = remember { TimerManager(context) }

    var hasUsagePermission by remember { mutableStateOf(hasUsageStatsPermission(context)) }
    var hasDrawOverlayPermission by remember { mutableStateOf(hasOverlayPermission(context)) }

    var isRunning by remember { mutableStateOf(timerManager.isRunning) }
    var elapsedSeconds by remember { mutableLongStateOf(timerManager.getElapsedSeconds()) }
    var targetMinutes by remember { mutableIntStateOf(timerManager.targetMinutes) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showEarlyStopWarningDialog by remember { mutableStateOf(false) }

    val db = remember { AppDatabase.getDatabase(context) }

    // 앱 복귀 및 Lifecycle ON_RESUME 발생 시 타이머 상태 동기화
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsagePermission = hasUsageStatsPermission(context)
                hasDrawOverlayPermission = hasOverlayPermission(context)

                // 오버레이에서 돌아오거나 앱 복귀 시 상태 복원
                isRunning = timerManager.isRunning
                elapsedSeconds = timerManager.getElapsedSeconds()
                targetMinutes = timerManager.targetMinutes
                onTimerStateChange(isRunning)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isRunning) {
        onTimerStateChange(isRunning)
        while (isRunning) {
            delay(1000L)
            elapsedSeconds = timerManager.getElapsedSeconds()
        }
    }

    val targetSeconds = targetMinutes * 60L
    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    val progress = if (targetSeconds > 0) {
        (elapsedSeconds.toFloat() / targetSeconds.toFloat()).coerceAtMost(1f)
    } else 0f

    val allPermissionsGranted = hasUsagePermission && hasDrawOverlayPermission

    // 맥박 효과 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    fun stopAndSaveRecord() {
        val savedSeconds = timerManager.stopTimer()
        isRunning = false
        onTimerStateChange(false)

        context.stopService(Intent(context, BlockService::class.java))

        if (savedSeconds > 0) {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - (savedSeconds * 1000L)
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(endTime))

            val record = StudyRecord(
                startTime = startTime,
                endTime = endTime,
                totalFocusSeconds = savedSeconds,
                date = dateStr
            )

            CoroutineScope(Dispatchers.IO).launch {
                db.studyRecordDao().insertRecord(record)
            }

            Toast.makeText(
                context,
                "🎉 집중 성공! 기록이 저장되었습니다. (${formatDuration(savedSeconds)})",
                Toast.LENGTH_LONG
            ).show()

            elapsedSeconds = 0L
        }
    }

    fun startTimer() {
        timerManager.startTimer(targetMinutes)
        isRunning = true
        elapsedSeconds = 0L
        onTimerStateChange(true)

        val serviceIntent = Intent(context, BlockService::class.java).apply {
            putStringArrayListExtra("ALLOWED_PACKAGES", ArrayList(allowedPackages))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "공타잠",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isRunning) "정갈한 몰입의 시간입니다. 계속 집중하세요." else "목표 시간을 설정하고 몰입을 시작해보세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (!allPermissionsGranted) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "필수 권한 설정 필요",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "딴짓 앱 차단을 위해 권한 허용이 필요합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (!hasUsagePermission) {
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("1. 사용 정보 접근 권한 허용")
                            }
                        }

                        if (!hasDrawOverlayPermission) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("2. 다른 앱 위에 표시 권한 허용")
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "허용 앱 ${allowedCount}개",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable(enabled = !isRunning) {
                            showTimePickerDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = "목표 시간",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (targetMinutes > 0) "목표 ${targetMinutes / 60}시간 ${targetMinutes % 60}분" else "목표 시간 설정",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 중앙 메인 원형 타이머 UI (노션/에어비앤비 웜 모던 스타일)
        Box(
            modifier = Modifier
                .size(260.dp)
                .scale(if (isRunning) pulseScale else 1f),
            contentAlignment = Alignment.Center
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

            if (targetSeconds > 0) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = primaryColor,
                    trackColor = surfaceVariant,
                    strokeWidth = 12.dp
                )
            }

            Box(
                modifier = Modifier
                    .size(228.dp)
                    .clip(CircleShape)
                    .background(
                        brush = if (isRunning) {
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isRunning) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "잠금",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "FOCUSING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        text = timeFormatted,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRunning) "집중 진행 중..." else "대기 중",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (targetMinutes > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        ) {
                            Text(
                                text = "목표 ${targetMinutes}분 중 ${(progress * 100).toInt()}% 달성",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // 하단 동작 버튼 영역
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    if (isRunning) {
                        showEarlyStopWarningDialog = true
                    } else {
                        startTimer()
                    }
                },
                enabled = allPermissionsGranted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "공부 종료하기" else "공부 시작하기",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showTimePickerDialog) {
        TargetTimePickerDialog(
            initialMinutes = targetMinutes,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { selectedMins ->
                targetMinutes = selectedMins
                timerManager.targetMinutes = selectedMins
                showTimePickerDialog = false
            }
        )
    }

    if (showEarlyStopWarningDialog) {
        AlertDialog(
            onDismissRequest = { showEarlyStopWarningDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("정말로 공부를 종료하시겠습니까?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "현재 몰입 공부가 진행 중입니다.\n종료하면 지금까지의 시간(${formatDuration(elapsedSeconds)})이 기록되고 모드가 종료됩니다.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEarlyStopWarningDialog = false
                        stopAndSaveRecord()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("네, 종료합니다")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEarlyStopWarningDialog = false }) {
                    Text("계속 공부하기")
                }
            }
        )
    }
}

// 목표 시간 설정 다이얼로그
@Composable
fun TargetTimePickerDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var hoursText by remember { mutableStateOf((initialMinutes / 60).let { if (it > 0) it.toString() else "" }) }
    var minsText by remember { mutableStateOf((initialMinutes % 60).let { if (it > 0) it.toString() else "" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "목표 공부 시간 설정",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "원하는 목표 공부 시간을 설정하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 2) hoursText = it },
                        label = { Text("시간 (h)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minsText,
                        onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 2) minsText = it },
                        label = { Text("분 (m)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                }

                Text(
                    text = "빠른 선택:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {
                            val current = (hoursText.toIntOrNull() ?: 0) * 60 + (minsText.toIntOrNull() ?: 0) + 15
                            hoursText = (current / 60).let { if (it > 0) it.toString() else "" }
                            minsText = (current % 60).toString()
                        },
                        label = { Text("+15분") }
                    )
                    AssistChip(
                        onClick = {
                            val current = (hoursText.toIntOrNull() ?: 0) * 60 + (minsText.toIntOrNull() ?: 0) + 30
                            hoursText = (current / 60).let { if (it > 0) it.toString() else "" }
                            minsText = (current % 60).toString()
                        },
                        label = { Text("+30분") }
                    )
                    AssistChip(
                        onClick = {
                            val current = (hoursText.toIntOrNull() ?: 0) * 60 + (minsText.toIntOrNull() ?: 0) + 60
                            hoursText = (current / 60).toString()
                            minsText = (current % 60).toString()
                        },
                        label = { Text("+1시간") }
                    )
                    AssistChip(
                        onClick = {
                            hoursText = ""
                            minsText = ""
                        },
                        label = { Text("초기화") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hoursText.toIntOrNull() ?: 0
                    val m = minsText.toIntOrNull() ?: 0
                    onConfirm(h * 60 + m)
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("설정 완료")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

// 2. 허용 앱 관리 화면 (Notion / Airbnb 정갈한 스타일)
@Composable
fun AllowedAppsScreen(
    allowedPackages: List<String>,
    onToggle: (String) -> Unit
) {
    val context = LocalContext.current
    var appList by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(AppFilterCategory.ALL) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)

            val apps = resolveInfos.mapNotNull { resolveInfo ->
                val pkgName = resolveInfo.activityInfo.packageName
                if (pkgName == context.packageName) return@mapNotNull null

                val appName = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                val isEssential = pkgName.contains("dialer") || pkgName.contains("telecom") ||
                        pkgName.contains("messaging") || pkgName.contains("message")

                AppItem(appName, pkgName, icon, isEssential)
            }.distinctBy { it.packageName }.sortedWith(
                compareByDescending<AppItem> { it.isEssential }.thenBy { it.name }
            )

            appList = apps
            isLoading = false
        }
    }

    val filteredApps = remember(searchQuery, selectedFilter, appList, allowedPackages) {
        appList.filter { app ->
            val matchesSearch = app.name.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val isAllowed = app.isEssential || allowedPackages.contains(app.packageName)

            val matchesCategory = when (selectedFilter) {
                AppFilterCategory.ALL -> true
                AppFilterCategory.ALLOWED -> isAllowed
                AppFilterCategory.ESSENTIAL -> app.isEssential
                AppFilterCategory.UNALLOWED -> !isAllowed
            }

            matchesSearch && matchesCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "허용 앱 관리",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "집중 시간 중 사용을 허용할 앱을 설정해보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("앱 이름 또는 패키지 검색...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "지우기")
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppFilterCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedFilter == category,
                    onClick = { selectedFilter = category },
                    label = { Text(category.label) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "검색 결과가 없습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                text = "${filteredApps.size}개의 앱 목록",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isChecked = app.isEssential || allowedPackages.contains(app.packageName)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !app.isEssential) {
                                onToggle(app.packageName)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (app.isEssential) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        else if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.name.take(1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (app.isEssential) "기본 필수 허용" else app.packageName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (app.isEssential) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "필수 허용",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { onToggle(app.packageName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. 공부 기록 통계 화면 (웜 노션 / 에어비앤비 대시보드)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val recordList by db.studyRecordDao().getAllRecords().collectAsState(initial = emptyList())

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val totalSeconds = recordList.sumOf { it.totalFocusSeconds }
    val todaySeconds = recordList.filter { it.date == todayStr }.sumOf { it.totalFocusSeconds }
    val avgSeconds = if (recordList.isNotEmpty()) totalSeconds / recordList.size else 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "공부 기록 & 통계",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "당신의 몰입 히스토리입니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 대시보드 요약
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "오늘의 집중",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatDuration(todaySeconds),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "총 누적 몰입",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatDuration(totalSeconds),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "평균 세션 시간",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(avgSeconds),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(30.dp)
                        .width(1.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "총 완료 세션",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${recordList.size}회",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "세션 히스토리",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (recordList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "아직 기록된 공부 세션이 없습니다.\n타이머를 시작해보세요!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recordList, key = { it.id }) { record ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = record.date,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${formatTimeOnly(record.startTime)} ~ ${formatTimeOnly(record.endTime)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatDuration(record.totalFocusSeconds),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            db.studyRecordDao().deleteRecord(record)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "삭제",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
