package com.yuyin.demo

import android.content.*
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.utils.DisplayUtils
import com.mobvoi.wenet.MediaCaptureService
import com.mobvoi.wenet.MediaCaptureService.mcs_Binder
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.RuningCapture.CaptureAudio_ALL
import com.yuyin.demo.databinding.ActivityMainViewBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Paths


class MainActivityView : AppCompatActivity() {

    val mainAction = "MainActivityAction"

    // 视图绑定
    private lateinit var binding: ActivityMainViewBinding

    private val m_ALL_PERMISSIONS_PERMISSION_CODE = 1000

    private val LOG_TAG = "YUYIN"

    // 层级配置
    private lateinit var appBarConfiguration: AppBarConfiguration

    val model: YuyinViewModel by viewModels()

    // 中转fragment
    private lateinit var host: NavHostFragment

    private lateinit var m_actionReceiver: CaptureAudioReceiver

    private lateinit var frg: RuningCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 膨胀视图
        binding = ActivityMainViewBinding.inflate(layoutInflater)
        val view: View = binding.root
        setContentView(view)

        // 应用自定义toolbar
        setSupportActionBar(binding.actionBar)

        // 获取NavHostFragment
        host =
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




        val filter = IntentFilter()
        filter.addAction(CaptureAudio_ALL)
        filter.addAction(mainAction)
        m_actionReceiver = CaptureAudioReceiver()
        this.registerReceiver(m_actionReceiver, filter)

        // 控制底部导航条只出现在main_dest fileManager_dest
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.runingCapture_dest || destination.id == R.id.runingRecord_dest) {
                runOnUiThread {
                    binding.mainBottomNavigation.visibility = View.INVISIBLE
                    binding.mainBottomNavigation.isEnabled = false
                    actionBar?.show()
                }
            } else {
                runOnUiThread {
                    binding.mainBottomNavigation.visibility = View.VISIBLE
                    binding.mainBottomNavigation.isEnabled = true
                    // 回到顶层清除数据
                    model.results.value?.clear()
                    model.bufferQueue.clear()
                    model.startAsr = false
                    model.startRecord = false
                    if (model.mBound) {
                        model.mcs_binder?.clearQueue()
                    }
                    actionBar?.hide()
                }

            }

        }

        initAudioCapture()


    }


    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        // 模型
        var model_name = "final"
        var dic_name = "words"
        val sharedPreference =
            PreferenceManager.getDefaultSharedPreferences(this@MainActivityView)
        val mod = sharedPreference.getString("languageOfModule", "zh")
        model_name = "$`model_name`_$mod.zip"
        dic_name = "$`dic_name`_$mod.txt"
        try {
            init_model(model_name, dic_name)
        } catch (exception: Exception) {
            YuYinLog.e(LOG_TAG, "can not init model")
        }

        // 权限
        YuYinUtil.checkRequestPermissions(this, this)


//        getExternalFilesDir()
        val docDirPath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val yuYinDir = Paths.get(docDirPath?.absolutePath, "YuYin").toFile()
        if (!yuYinDir.exists()) {
            yuYinDir.mkdir()
        }

        // 开启浮窗
        EasyFloat.with(this@MainActivityView)
            .setLayout(R.layout.floatviewtest)
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
    }


    override fun onPause() { // 另一个activity来到前台调用
        super.onPause()
    }

    override fun onStop() {
        super.onStop() // activity不再可见
    }


    override fun onRestart() {
        super.onRestart()
    }

    override fun onDestroy() {
        super.onDestroy()

        RuningCapture.stopRecordingToActivity(this)
    }






    fun init_model(model: String, dic: String) {
        val model_path = File(assetFilePath(this, model)).absolutePath
        val dic_path = File(assetFilePath(this, dic)).absolutePath
        Recognize.init(model_path, dic_path)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 在actionbar应用自定义菜单
        menuInflater.inflate(R.menu.bar_menu, menu)
        return true
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
            YuYinLog.e(
                LOG_TAG,
                "Error process asset $assetName to file path"
            )
        }
        return null
    }
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            model.mcs_binder = service as mcs_Binder
            model.mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            model.mBound = false
        }
    }

    // TODO Activity启动服务....
    private fun initAudioCapture() {

        // 未启动服务
        // Service
        val m_mediaProjectionManager =
            this.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = m_mediaProjectionManager.createScreenCaptureIntent()
        // 获取录制屏幕权限 并启动服务
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                Thread {
                    val i = Intent(this, MediaCaptureService::class.java)
                    this.bindService(
                        i,
                        connection,
                        BIND_AUTO_CREATE
                    )
                }.start()
                // 启动服务
                val i = Intent(this, MediaCaptureService::class.java)
                i.action = RuningCapture.ACTION_ALL
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                i.putExtra(RuningCapture.EXTRA_RESULT_CODE, RuningCapture.m_CREATE_SCREEN_CAPTURE)
                i.putExtras(result.data!!)
                this.startService(i)
            } else {
                findNavController(
                    this,
                    R.id.yuyin_nav_host_container_fragment
                ).popBackStack()
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
                this@MainActivityView.frg = this@MainActivityView.host.childFragmentManager.fragments[0] as RuningCapture
                val actionName = intent.getStringExtra(RuningCapture.EXTRA_CaptureAudio_NAME)
                if (actionName != null && !actionName.isEmpty()) {
                    if (actionName.equals(
                            RuningCapture.CaptureAudio_START_ASR,
                            ignoreCase = true
                        )
                    ) {
                        // 服务开启录音后回调
                        model.startRecord = true
                        model.startAsr = true
                        frg.startAsrThread()
                        frg.updateUiToStop()
                    } else if (actionName.equals(
                            RuningCapture.CaptureAudio_STOP,
                            ignoreCase = true
                        )
                    ) {
                        // 服务停止录音后回调
                        model.startRecord = false
                        model.startAsr = false
                        frg.waitForFinished()
                    } else if (actionName.equals(
                            RuningCapture.ACTION_STOP_RECORDING_From_Notification,
                            ignoreCase = true
                        )
                    ) {
                        this@MainActivityView.findViewById<Button>(R.id.stop_bt_run_cap).isEnabled = false
                        RuningCapture.stopRecording(this@MainActivityView)
                    } else if (actionName.equals(
                            RuningCapture.ACTION_START_RECORDING_From_Notification,
                            ignoreCase = true
                        )
                    ) {
                        this@MainActivityView.findViewById<Button>(R.id.stop_bt_run_cap).isEnabled = false
                        RuningCapture.startRecording(this@MainActivityView)
                    }
                }
            } else if(action.equals(mainAction,ignoreCase = true)) {
                val actionName = intent.getStringExtra(RuningCapture.EXTRA_CaptureAudio_NAME)
                if (actionName.equals(RuningCapture.CaptureAudio_START, ignoreCase = true)) {

                    // 接收服务已启动的通知
                    // 通知服务准备录制

                    RuningCapture.startRecordingService(this@MainActivityView);
                }
            }
        }
    }



}




