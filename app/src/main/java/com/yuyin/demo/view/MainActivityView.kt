package com.yuyin.demo.view

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.utils.DisplayUtils
import com.squareup.moshi.JsonAdapter
import com.vmadalin.easypermissions.EasyPermissions
import com.yuyin.demo.R
import com.yuyin.demo.utils.YuYinUtil
import com.yuyin.demo.utils.YuYinUtil.ACTION_ALL
import com.yuyin.demo.utils.YuYinUtil.CaptureAudio_ALL
import com.yuyin.demo.utils.YuYinUtil.CaptureAudio_START
import com.yuyin.demo.utils.YuYinUtil.EXTRA_CaptureAudio_NAME
import com.yuyin.demo.utils.YuYinUtil.EXTRA_RESULT_CODE
import com.yuyin.demo.utils.YuYinUtil.m_CREATE_SCREEN_CAPTURE
import com.yuyin.demo.databinding.ActivityMainViewBinding
import com.yuyin.demo.models.AudioPlay
import com.yuyin.demo.models.LocalSettings
import com.yuyin.demo.service.MediaCaptureService
import com.yuyin.demo.service.MediaCaptureService.Companion.m_NOTIFICATION_CHANNEL_ID
import com.yuyin.demo.viewmodel.YuyinViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log

class MainActivityView : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    companion object {
        const val TAG = "YUYIN_ACTIVITY"

        const val floatTag = "float view"

        const val zipMineType = "application/zip"

        const val textMineType = "text/plain"
    }

    // 视图绑定
    private lateinit var binding: ActivityMainViewBinding

    private val m_ALL_PERMISSIONS_PERMISSION_CODE = 1000

    // 层级配置
    private lateinit var appBarConfiguration: AppBarConfiguration

    val model: YuyinViewModel by viewModels()

    lateinit var navController: NavController

    // 服务
    private lateinit var actionReceiver: CaptureAudioReceiver
    private lateinit var mediaService_binder: MediaCaptureService.MediaServiceBinder
    private var mBound = false
    private val requestPermissionForCapture =         // 获取录制屏幕权限 并启动服务
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
                exitApp()
            }
        }

    // 通知
    lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 膨胀视图
        binding = ActivityMainViewBinding.inflate(layoutInflater)

        val view: View = binding.root
        setContentView(view)

        showFloatView()

        // 应用自定义toolbar
        setSupportActionBar(binding.actionBar)


        // 获取NavHostFragment
        val navHost =
            supportFragmentManager.findFragmentById(R.id.yuyin_nav_host_container_fragment) as NavHostFragment?
                ?: return
        navController = navHost.navController

        // 设定顶层
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.main_dest, R.id.filesManager_dest),
        )

        // 使得 actionbar 适应导航图 在非顶层可以返回
        setupActionBarWithNavController(navController, appBarConfiguration)

        // 应用底层导航菜单
        val bottomNav = binding.mainBottomNavigation
        bottomNav.setupWithNavController(navController)


        // 控制底部导航条只出现在main_dest fileManager_dest
        navController.addOnDestinationChangedListener() { _, destination, _ ->
            runOnUiThread {
                if (destination.label == this.getString(R.string.capture_label) || destination.label == this.getString(
                        R.string.record_label
                    ) || destination.label == this.getString(R.string.setting_label) || destination.label == getString(
                        R.string.edit_label
                    )
                ) {
                    binding.mainBottomNavigation.let {
                        it.visibility = View.GONE
                        it.isEnabled = false
                    }
                } else {
                    binding.mainBottomNavigation.let {
                        it.visibility = View.VISIBLE
                        it.isEnabled = true
                    }
                }
            }
        }
        initNotification()
        initProfile()
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "onStart")
    }

    override fun onRestart() {
        super.onRestart()
        Log.i(TAG, "onRestart")
        // what ever just hid
        destroyFloatView()
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
        // 权限
        YuYinUtil.checkRequestPermissions(this, this)
    }

    fun initProfile() {
        val docDirPath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        model.yuYinDirPath = Paths.get(docDirPath?.absolutePath, "YuYin")
        val yuYinDir = Paths.get(docDirPath?.absolutePath, "YuYin").toFile()
        if (!yuYinDir.exists()) {
            yuYinDir.mkdir()
        }
        val yuYinDataDir = Paths.get(yuYinDir.absolutePath, "data").toFile()
        if (!yuYinDataDir.exists()) {
            yuYinDataDir.mkdir()
        }
        model.yuYinDataDir = yuYinDataDir.toPath()
        model.settingProfilePath = Paths.get(yuYinDir.absolutePath, "settings.json")
        model.settingProfilePath.let {
            val jsonAdapter: JsonAdapter<LocalSettings> =
                model.moshi.adapter(LocalSettings::class.java)
            if (it.toFile().exists()) {
                val json = it.toFile().readText()
                model.settings = jsonAdapter.fromJson(json)!!
                if (File(model.modelPath).exists() && File(model.dicPath).exists()) {
                    Log.e(TAG, "load settings successful")
                } else {
                    val localSettings = defaultSettings()
                    model.settings = localSettings
                    val dialog =
                        MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.save_settings)
                            .setMessage(R.string.not_found_settings)
                            .setNegativeButton(R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setPositiveButton(R.string.confirm) { _, _ ->
                                it.toFile().writeText(jsonAdapter.toJson(localSettings))
                            }.create()
                    dialog.show()
                }
            } else {
                val localSettings = defaultSettings()
                model.settings = localSettings
                val json = jsonAdapter.toJson(localSettings)
                it.toFile().createNewFile()
                it.toFile().writeText(json)
            }
        }
        if (!model.yuYinTmpDir.mkdir()) {
            model.yuYinTmpDir.mkdir()
        }
    }

    private fun defaultModel(mod: String): MutableList<String> {
        var modelPath = "final"
        var dicPath = "words"
        modelPath = "${modelPath}_$mod.zip"
        dicPath = "${dicPath}_$mod.txt"
        modelPath = assetFilePath(this, modelPath) ?: ""
        dicPath = assetFilePath(this, dicPath) ?: ""
        return mutableListOf(modelPath, dicPath)
    }

    private fun defaultSettings() = LocalSettings(
        0,
        mutableMapOf(
            "zh" to defaultModel("zh"),
            "en" to defaultModel("en"),
            "自定义" to mutableListOf("", "")
        ),
        "zh"
    )

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "onPause")
        val current = navController.currentDestination
        Log.i(TAG, "id: ${current?.id} label ${current?.label}")
        if (current?.label == this.getString(R.string.capture_label) || current?.label == this.getString(
                R.string.record_label
            )
        ) {
            showFloatView()
        } else {
            destroyFloatView()
        }
    }

    override fun onStop() {
        super.onStop()
        // 进入后台
        Log.i(TAG, "onStop")
    }


    override fun onDestroy() {
        super.onDestroy()
        model.recorder?.let {
            if (it.state == AudioRecord.STATE_INITIALIZED) {
                it.release()
            }
        }
        model.recorder = null
        model.yuYinTmpDir.listFiles()?.forEach {
            it.delete()
        }
        AudioPlay.audioTrack.release()
        Log.i(TAG, "onDestroy")
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        // 如果是点击事件，获取点击的view，并判断是否要收起键盘
        ev?.let { it ->
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    currentFocus?.let { v ->
                        // 判断是否需要收起
                        if (isShouldHideKeyboard(v, ev)) {
                            val textInputText = v as EditText
                            textInputText.clearFocus()
                            if (textInputText.windowToken != null) {
                                try {
                                    val im: InputMethodManager? =
                                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                                    im?.let {
                                        im.hideSoftInputFromWindow(
                                            textInputText.windowToken,
                                            InputMethodManager.HIDE_NOT_ALWAYS
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, e.message)
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun isShouldHideKeyboard(v: View, event: MotionEvent): Boolean {
        // 判断获取焦点view是否是editview
        if (v is EditText) {
            // 判断点击位置
            val l = intArrayOf(0, 0)
            // 获取位置
            v.getLocationInWindow(l)
            val left = l[0]
            val top = l[1]
            val bottom = top + v.height
            val right = left + v.width
            // 当前view的点击事件忽略
            return event.x <= left || event.x >= right || event.y <= top || event.y >= bottom
        }
        return false
    }

    private fun assetFilePath(context: Context, assetName: String): String? {
        val file = File(model.yuYinDirPath.absolutePathString(), assetName)
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
                TAG,
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
        requestPermissionForCapture.launch(intent)
    }

    private fun exitApp() {
        finishAffinity()
        exitProcess(1)
    }

    private fun showFloatView() {
        if (!checkFloatView()) {
            EasyFloat.with(this@MainActivityView)
                .setLayout(R.layout.floatview)
                .setShowPattern(ShowPattern.BACKGROUND) // 应用后台时显示 手动调用显示隐藏方法后 自动逻辑失效
                .setSidePattern(SidePattern.RESULT_HORIZONTAL) // 吸附 根据移动后的位置贴附到边缘
                .setTag(floatTag) // 设置TAG管理
                .setDragEnable(true) // 可拖拽
                .hasEditText(false) // 无编辑框，无需适配键盘
                .setLocation(0, 0)
                .setGravity(Gravity.START or Gravity.CENTER_VERTICAL, 0, 0)
                .setLayoutChangedGravity(Gravity.START)
                //  .setBorder()
                .setMatchParent(false, false)
                .setAnimator(com.lzf.easyfloat.anim.DefaultAnimator())
                .setDisplayHeight { context -> DisplayUtils.rejectedNavHeight(context) }
                .setFilter(MainActivityView::class.java)
                .registerCallback {
                    show {
                        Log.i(TAG, "show float view");
                    }
                    hide {
                        Log.i(TAG, "hide float view")
                    }
                    dismiss {
                        Log.i(TAG, "dismiss float view")
                    }
                    drag { _, _ -> }
                    dragEnd {
                        //TODO 获取当前重新绘制
                        //it.draw()
                    }
                }
                .show()
        } else {
            EasyFloat.show(floatTag)
        }
    }

    fun checkFloatView(): Boolean = EasyFloat.getFloatView(floatTag) != null

    private fun destroyFloatView() {
        EasyFloat.hide(floatTag)
    }

    // 广播服务
    // 不可以耗时操作  在主线程中
    // 避免使用 binding 可能是全局创建的唯一实例...
    inner class CaptureAudioReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
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




