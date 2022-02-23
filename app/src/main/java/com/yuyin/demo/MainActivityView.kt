package com.yuyin.demo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.databinding.ActivityMainViewBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Paths

class MainActivityView : AppCompatActivity() {

    // 视图绑定
    private lateinit var binding: ActivityMainViewBinding

    // 所需请求的权限
    private val appPermissions: Array<String> = arrayOf<String>(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.FOREGROUND_SERVICE,
    )
    private val m_ALL_PERMISSIONS_PERMISSION_CODE = 1000


    private val MY_PERMISSIONS_RECORD_AUDIO = 1
    private var miniBufferSize = 0
    private val LOG_TAG = "YUYIN"
    private val SAMPLE_RATE = 16000 // The sampling rate

    // 层级配置
    private lateinit var appBarConfiguration: AppBarConfiguration



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 膨胀视图
        binding = ActivityMainViewBinding.inflate(layoutInflater)
        val view: View = binding.root
        setContentView(view)

        // 应用自定义toolbar
        setSupportActionBar(binding.actionBar)

        // 获取NavHostFragment
        val host: NavHostFragment = supportFragmentManager.findFragmentById(R.id.yuyin_nav_host_container_fragment) as NavHostFragment? ?: return
        val navController: NavController = host.navController

        // 设定顶层
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.main_dest,R.id.filesManager_dest),
        )

        // 使得 actionbar 适应导航图 在非顶层可以返回
        setupActionBar(navController,appBarConfiguration)

        // 应用底层导航菜单
        setupBottomNavMenu(navController)

        val model: YuyinViewModel by viewModels()

        // 控制底部导航条只出现在main_dest fileManager_dest
        navController.addOnDestinationChangedListener(object : NavController.OnDestinationChangedListener {
            override fun onDestinationChanged(
                controller: NavController,
                destination: NavDestination,
                arguments: Bundle?
            ) {
                if (destination.id == R.id.runingCapture_dest || destination.id == R.id.runingRecord_dest) {
                    runOnUiThread {
                        binding.mainBottomNavigation.visibility =  View.INVISIBLE
                        binding.mainBottomNavigation.isEnabled = false
                    }
                } else {
                    runOnUiThread {
                        binding.mainBottomNavigation.visibility =  View.VISIBLE
                        binding.mainBottomNavigation.isEnabled = true
                        // 回到顶层清除数据
                        model.results.value?.clear()
                        model.bufferQueue.clear()
                        model.startAsr = true
                        model.startRecord = true
                    }

                }
            }
        })


        // 权限
        checkRequestPermissions()

        // 模型
        var model_name = "final"
        var dic_name = "words"
        val sharedPreference = PreferenceManager.getDefaultSharedPreferences(this)
        val mod = sharedPreference.getString("languageOfModule","zh")
        model_name = "$`model_name`_$`mod`.zip"
        dic_name = "$`dic_name`_$`mod`.txt"
        try {
            init_model(model_name,dic_name)
        } catch (exception:Exception) {
            Log.e(LOG_TAG, "can not init model")
        }




//        getExternalFilesDir()
        val docDirPath = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val yuYinDir = Paths.get(docDirPath?.absolutePath, "YuYin").toFile()
        if (!yuYinDir.exists()) {
            yuYinDir.mkdir()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }



    fun init_model(model: String, dic: String) {
        val model_path = File(assetFilePath(this,model)).absolutePath
        val dic_path = File(assetFilePath(this,dic)).absolutePath
        Recognize.init(model_path,dic_path)
    }


    override fun onStart() {
        super.onStart()

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 在actionbar应用自定义菜单
        menuInflater.inflate(R.menu.bar_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // 跳转至设定界面
            R.id.setting_option -> {
                val intent = Intent(this,SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        //
        return findNavController(R.id.yuyin_nav_host_container_fragment).navigateUp(appBarConfiguration)
    }

    private fun setupBottomNavMenu(navController: NavController) {
        val bottomNav = binding.mainBottomNavigation
        bottomNav.setupWithNavController(navController)
    }

    private fun setupActionBar(navController: NavController, appBarConfig : AppBarConfiguration) {
        setupActionBarWithNavController(navController,appBarConfig)
    }

    private fun requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO),
                MY_PERMISSIONS_RECORD_AUDIO
            )
        } else {
            initRecoder()
        }
    }


    private fun initRecoder()
    {
//    // buffer size in bytes 1280

//    // buffer size in bytes 1280
        miniBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (miniBufferSize == AudioRecord.ERROR || miniBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(LOG_TAG, "Audio buffer can't initialize!")
            return
        }
    }


    fun checkRequestPermissions(): Boolean {
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        for (permission in appPermissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionsNeeded.add(permission)
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                m_ALL_PERMISSIONS_PERMISSION_CODE
            )
            return false
        }


        return true
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
            if (deniedCount != 0) Log.e(
                LOG_TAG,
                "Permission Denied!  Now you must allow  permission from settings."
            )
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
                LOG_TAG,
                "Error process asset $assetName to file path"
            )
        }
        return null
    }


}