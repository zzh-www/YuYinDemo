package com.yuyin.demo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.utils.DisplayUtils
import com.mobvoi.wenet.MediaCaptureService
import com.mobvoi.wenet.MediaCaptureService.Companion.m_NOTIFICATION_CHANNEL_ID
import com.vmadalin.easypermissions.EasyPermissions
import com.yuyin.demo.YuYinUtil.ACTION_ALL
import com.yuyin.demo.YuYinUtil.CaptureAudio_ALL
import com.yuyin.demo.YuYinUtil.CaptureAudio_START
import com.yuyin.demo.YuYinUtil.EXTRA_CaptureAudio_NAME
import com.yuyin.demo.YuYinUtil.EXTRA_RESULT_CODE
import com.yuyin.demo.YuYinUtil.m_CREATE_SCREEN_CAPTURE
import com.yuyin.demo.databinding.ActivityMainViewBinding
import com.yuyin.demo.models.YuyinViewModel
import com.yuyin.demo.view.speech.SettingsActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Paths
import kotlin.system.exitProcess
import com.yuyin.demo.YuYinUtil.YuYinLog as Log

class MainActivityView : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    // 视图绑定
    private lateinit var binding: ActivityMainViewBinding

    private val m_ALL_PERMISSIONS_PERMISSION_CODE = 1000

    private val YuYinLog_TAG = "YUYIN"

    // 层级配置
    private lateinit var appBarConfiguration: AppBarConfiguration

    val model: YuyinViewModel by viewModels()

    // 服务
    private lateinit var actionReceiver: CaptureAudioReceiver
    private lateinit var mediaService_binder: MediaCaptureService.MediaServiceBinder
    private var mBound = false

    // 通知
    private lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 膨胀视图
        binding = ActivityMainViewBinding.inflate(layoutInflater)
        val view: View = binding.root
        setContentView(view)

        // 应用自定义toolbar
        setSupportActionBar(binding.actionBar)

        // 获取NavHostFragment
        val host: NavHostFragment =
            supportFragmentManager.findFragmentById(R.id.yuyin_nav_host_container_fragment) as NavHostFragment?
                ?: return
        val navController: NavController = host.navController

        // 设定顶层
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.main_dest, R.id.filesManager_dest),
        )

        // 使得 actionbar 适应导航图 在非顶层可以返回
        setupActionBar(navController, appBarConfiguration)

        // 应用底层导航菜单
        setupBottomNavMenu(navController)

        // 控制底部导航条只出现在main_dest fileManager_dest
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            if (destination.id == R.id.runingCapture_dest || destination.id == R.id.runingRecord_dest) {
                binding.mainBottomNavigation.visibility = View.INVISIBLE
                binding.mainBottomNavigation.isEnabled = false
            } else {
                binding.mainBottomNavigation.visibility = View.VISIBLE
                binding.mainBottomNavigation.isEnabled = true
            }
        }
        // 开启浮窗
        EasyFloat.with(this@MainActivityView)
            .setLayout(R.layout.floatview)
            .setShowPattern(ShowPattern.BACKGROUND) // 应用后台时显示
            .setSidePattern(SidePattern.RESULT_HORIZONTAL) // 吸附 根据移动后的位置贴附到边缘
            .setTag("Capture") // 设置TAG管理
            .setDragEnable(true) // 可拖拽
            .hasEditText(false) // 无编辑框，无需适配键盘
            .setLocation(100, 0)
            .setGravity(Gravity.END or Gravity.CENTER_VERTICAL, 0, 0)
            .setLayoutChangedGravity(Gravity.END)
            //  .setBorder()
            .setMatchParent(false, false)
            .setAnimator(com.lzf.easyfloat.anim.DefaultAnimator())
            .setFilter(SettingsActivity::class.java) // 过滤ACTIVITY
            .setFilter(MainActivityView::class.java)
            .setDisplayHeight { context -> DisplayUtils.rejectedNavHeight(context) }
            .registerCallback {
                dragEnd {
                    //TODO 获取当前重新绘制
                    //it.draw()
                }
            }
            .show()
        initNotification()

    }

    override fun onResume() {
        super.onResume()
        // 权限
        YuYinUtil.checkRequestPermissions(this, this)
        val docDirPath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val yuYinDir = Paths.get(docDirPath?.absolutePath, "YuYin").toFile()
        if (!yuYinDir.exists()) {
            yuYinDir.mkdir()
        }

    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 在actionbar应用自定义菜单
        menuInflater.inflate(R.menu.bar_menu, menu)
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        model.recorder?.release()
        model.recorder = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // 跳转至设定界面
            R.id.setting_option -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        //
        return findNavController(R.id.yuyin_nav_host_container_fragment).navigateUp(
            appBarConfiguration
        )
    }

    private fun setupBottomNavMenu(navController: NavController) {
        val bottomNav = binding.mainBottomNavigation
        bottomNav.setupWithNavController(navController)
    }

    private fun setupActionBar(navController: NavController, appBarConfig: AppBarConfiguration) {
        setupActionBarWithNavController(navController, appBarConfig)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        TODO("Not yet implemented")
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        TODO("Not yet implemented")
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == m_ALL_PERMISSIONS_PERMISSION_CODE) {
            val permissionResults = HashMap<String, Int>()
            var deniedCount = 0
            for (permissionIndx in permissions.indices) {
                if (grantResults[permissionIndx] != PackageManager.PERMISSION_GRANTED) {
                    permissionResults[permissions[permissionIndx]] = grantResults[permissionIndx]
                    deniedCount++
                }
            }
            if (deniedCount != 0) {
                Toast.makeText(this, "must allow", Toast.LENGTH_SHORT).show()
                this.finish()
            }

        }
    }

    fun assetFilePath(context: Context, assetName: String): String? {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        try {
            context.assets.open(assetName).use { `is` ->
                FileOutputStream(file).use { os ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (`is`.read(buffer).also { read = it } != -1) {
                        os.write(buffer, 0, read)
                    }
                    os.flush()
                }
                return file.absolutePath
            }
        } catch (e: IOException) {
            Log.e(
                YuYinLog_TAG,
                "Error process asset $assetName to file path"
            )
        }
        return null
    }

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mediaService_binder = service as MediaCaptureService.MediaServiceBinder
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mBound = false
        }
    }

    private fun initNotification() {
        // 未启动服务
        val channel = NotificationChannel(
            m_NOTIFICATION_CHANNEL_ID,
            MediaCaptureService.m_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = MediaCaptureService.m_NOTIFICATION_CHANNEL_DESC
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        notificationManager.notificationChannels

        if (!notificationManager.areNotificationsEnabled()) {
            val intent = Intent().apply {
                this.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                this.data = Uri.fromParts("package", packageName, null)
            }
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    initAudioCapture()
                }
            }.launch(intent)
        } else {
            initAudioCapture()
        }
    }

    private fun initAudioCapture() {

        val filter = IntentFilter()
        filter.addAction(CaptureAudio_ALL)
        actionReceiver = CaptureAudioReceiver()
        this.registerReceiver(actionReceiver, filter)
        // 注册广播


        // Service
        val m_mediaProjectionManager =
            this.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = m_mediaProjectionManager.createScreenCaptureIntent()
        // 获取录制屏幕权限 并启动服务
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {

                var i = Intent(this, MediaCaptureService::class.java)
                this.bindService(
                    i,
                    connection,
                    BIND_AUTO_CREATE
                )
                // 启动前台服务
                i = Intent(this, MediaCaptureService::class.java)
                i.action = ACTION_ALL
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                i.putExtra(EXTRA_RESULT_CODE, m_CREATE_SCREEN_CAPTURE)
                i.putExtras(result.data!!)
                this.startForegroundService(i)
            } else {
                // 退出应用
                finishAffinity()
                exitProcess(0)
            }
        }.launch(intent)
    }

    // 广播服务
    // 不可以耗时操作  在主线程中
    // 避免使用 binding 可能是全局创建的唯一实例...
    // 不要直接启动线程
    inner class CaptureAudioReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            // 获取frg
            // 每次都会重新创建一个新的实例,所以要强制更新,之前frag会被销毁,不更新或者调用之前fra会导致null错误
            if (action.equals(CaptureAudio_ALL, ignoreCase = true)) {
                val actionName = intent.getStringExtra(EXTRA_CaptureAudio_NAME)
                if (actionName != null && !actionName.isEmpty()) {
                    if (actionName.equals(
                            CaptureAudio_START,
                            ignoreCase = true
                        )
                    ) {
                        // 服务开启
                        model.recorder = mediaService_binder.serviceRecorder()
                    }
                }
            }
        }
    }
}




