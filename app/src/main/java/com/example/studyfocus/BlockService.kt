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
import android.widget.TextView
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
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

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
            // 기본 본인 앱 및 시스템 필수 힌트 추가
            allowedList.add(packageName)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        unregisterReceiver(screenReceiver)
        removeOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // 주기적 감시 루프
    private fun startMonitoringLoop() {
        serviceScope.launch {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            while (isActive) {
                // 1. 화면 꺼짐 시 감시 중단 (배터리 절약)
                if (!isScreenOn) {
                    delay(2000L)
                    continue
                }

                // 2. 최상단 앱 패키지 확인
                val topPackage = getTopPackage(usageStatsManager)

                if (topPackage != null && !isAllowed(topPackage)) {
                    // 미승인 앱 감지 -> 메인 스레드에서 차단 화면 띄우기
                    withContext(Dispatchers.Main) {
                        showOverlay()
                    }
                    delay(1000L)
                } else {
                    // 승인 앱이거나 내 앱인 경우
                    withContext(Dispatchers.Main) {
                        removeOverlay()
                    }
                    delay(800L) // 적응형 주기
                }
            }
        }
    }

    // 허용 앱 검사
    private fun isAllowed(pkg: String): Boolean {
        if (allowedList.contains(pkg)) return true
        // 전화, 메시지, 홈 런처 관련 시스템 앱 허용
        if (pkg.contains("dialer") || pkg.contains("telecom") ||
            pkg.contains("messaging") || pkg.contains("launcher")) {
            return true
        }
        return false
    }

    // 최상단 앱 이벤트 조회 (최신 방식)
    private fun getTopPackage(usageStatsManager: UsageStatsManager): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000L // 최근 10초간의 이벤트 쿼리
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var lastResumedPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastResumedPackage = event.packageName
            }
        }
        return lastResumedPackage
    }

    // 차단 오버레이 띄우기
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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        // 오버레이 뷰 동적 생성
        val layout = LayoutInflater.from(this).inflate(R.layout.overlay_block, null)

        // 홈으로 가기 버튼 리스너
        layout.findViewById<Button>(R.id.btnGoHome).setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            removeOverlay()
        }

        overlayView = layout
        windowManager?.addView(layout, params)
    }

    private fun removeOverlay() {
        overlayView?.let {
            windowManager?.removeView(it)
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
            .setContentTitle("집중 모드 실행 중")
            .setContentText("승인된 앱 외 실행이 제한됩니다.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}

