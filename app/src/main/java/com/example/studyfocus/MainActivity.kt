package com.example.studyfocus

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.studyfocus.ui.theme.StudyFocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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

// 탭 종류 정의
enum class ScreenTab(val title: String) {
    TIMER("타이머"),
    APPS("허용 앱"),
    HISTORY("공부 기록")
}

// 앱 정보 데이터 클래스
data class AppItem(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val isEssential: Boolean = false
)

// 권한 확인 헬퍼 함수
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

@Composable
fun MainAppScreen() {
    var selectedTab by remember { mutableStateOf(ScreenTab.TIMER) }
    val allowedPackages = remember { mutableStateListOf<String>() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.TIMER,
                    onClick = { selectedTab = ScreenTab.TIMER },
                    label = { Text("타이머") },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "타이머") }
                )
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.APPS,
                    onClick = { selectedTab = ScreenTab.APPS },
                    label = { Text("허용 앱") },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "허용 앱") }
                )
                NavigationBarItem(
                    selected = selectedTab == ScreenTab.HISTORY,
                    onClick = { selectedTab = ScreenTab.HISTORY },
                    label = { Text("기록") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "기록") }
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
                ScreenTab.TIMER -> TimerScreen(allowedCount = allowedPackages.size)
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

// 1. 타이머 화면
@Composable
fun TimerScreen(allowedCount: Int) {
    val context = LocalContext.current
    var hasUsagePermission by remember { mutableStateOf(hasUsageStatsPermission(context)) }
    var hasDrawOverlayPermission by remember { mutableStateOf(hasOverlayPermission(context)) }

    // 설정 화면에 갔다가 복귀했을 때 권한 상태 다시 갱신
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsagePermission = hasUsageStatsPermission(context)
                hasDrawOverlayPermission = hasOverlayPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var isRunning by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    val allPermissionsGranted = hasUsagePermission && hasDrawOverlayPermission

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "집중 모드",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 권한 미허용 시 경고 및 설정 유도 카드
        if (!allPermissionsGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                        text = "딴짓 앱 차단 기능을 위해 아래 권한 허용이 필요합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!hasUsagePermission) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("1. 사용 정보 접근 권한 허용하기")
                        }
                    }

                    if (!hasDrawOverlayPermission) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("2. 다른 앱 위에 표시 권한 허용하기")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = "허용된 추가 앱: ${allowedCount}개 (전화/메시지는 기본 포함)",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(
                    if (isRunning) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormatted,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isRunning) "공부 진행 중" else "대기 상태",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { isRunning = !isRunning },
            enabled = allPermissionsGranted,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.height(52.dp)
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRunning) "공부 종료" else "공부 시작",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// 2. 허용 앱 관리 화면
@Composable
fun AllowedAppsScreen(
    allowedPackages: List<String>,
    onToggle: (String) -> Unit
) {
    val context = LocalContext.current
    var appList by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "허용 앱 관리",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "공부 시간 동안 사용할 수 있는 앱을 선택하세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(appList) { app ->
                    val isChecked = app.isEssential || allowedPackages.contains(app.packageName)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !app.isEssential) {
                                onToggle(app.packageName)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (app.isEssential) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.name.take(1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (app.isEssential) "기본 필수 허용 (전화/메시지)" else app.packageName,
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
                                Checkbox(
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

// 3. 공부 기록 화면 (임시)
@Composable
fun HistoryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "공부 기록",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "날짜별 총 집중 시간 통계가 이곳에 저장됩니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Text(
                text = "📊 데이터베이스(Room) 연동 후 실제 기록이 날짜별로 표시됩니다.",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}