package com.example.studyfocus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class BlockService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var isScreenOn = true
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    // 메인 화면에서 넘겨받을 허용 앱 패키지 목록
    private val allowedList = mutableSetOf<String>()

    // 화면 꺼짐/켜짐 감지기 (배터리 최적화)
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> isScreenOn = false
                Intent.ACTION_SCREEN_ON -> isScreenOn = true
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 화면 On/Off 브로드캐스트 등록
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)

        // 상단바 알림(Foreground Service 필수 요구사항)
        startForeground(1001, createNotification())

        // 앱 감시 루프 시작
        startMonitoringLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val apps = intent?.getStringArrayListExtra("ALLOWED_PACKAGES")
        if (apps != null) {
            allowedList.clear()
            allowedList.addAll(apps)
            allowedList.add(packageName)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        removeOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // 주기적 감시 루프
    private fun startMonitoringLoop() {
        serviceScope.launch {
            val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager

            while (isActive) {
                if (!isScreenOn) {
                    delay(1500L)
                    continue
                }

                val topPackage = getTopPackage(usageStatsManager)

                if (topPackage != null && !isAllowed(topPackage)) {
                    withContext(Dispatchers.Main) {
                        showOverlay()
                    }
                    delay(500L)
                } else {
                    withContext(Dispatchers.Main) {
                        removeOverlay()
                    }
                    delay(500L)
                }
            }
        }
    }

    private fun isAllowed(pkg: String): Boolean {
        if (pkg.isBlank()) return true
        if (allowedList.contains(pkg)) return true
        if (pkg == packageName) return true

        val lowerPkg = pkg.lowercase()
        // 전화, 메시지, 홈 런처, 시스템 UI, 키보드
        if (lowerPkg.contains("dialer") || lowerPkg.contains("telecom") ||
            lowerPkg.contains("messaging") || lowerPkg.contains("launcher") ||
            lowerPkg.contains("systemui") || lowerPkg.contains("inputmethod") ||
            lowerPkg.contains("incallui") || lowerPkg == "android") {
            return true
        }
        return false
    }

    private fun getTopPackage(usageStatsManager: UsageStatsManager): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000L
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastResumedPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == 1) { // MOVE_TO_FOREGROUND
                lastResumedPackage = event.packageName
            }
        }

        if (lastResumedPackage != null) {
            return lastResumedPackage
        }

        // Fallback: UsageStats query
        try {
            val statsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            if (!statsList.isNullOrEmpty()) {
                val recent = statsList.maxByOrNull { it.lastTimeUsed }
                if (recent != null && (endTime - recent.lastTimeUsed) < 3000L) {
                    return recent.packageName
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val layout = LayoutInflater.from(this).inflate(R.layout.overlay_block, null)

        layout.findViewById<Button>(R.id.btnGoHome)?.setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            removeOverlay()
        }

        layout.findViewById<Button>(R.id.btnGoApp)?.setOnClickListener {
            val appIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(appIntent)
            removeOverlay()
        }

        overlayView = layout
        try {
            windowManager?.addView(layout, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    private fun createNotification(): Notification {
        val channelId = "study_channel"
        val channelName = "공부 집중 모드"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("🔒 집중 모드 실행 중")
            .setContentText("허용되지 않은 앱 사용이 제한됩니다.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
