package com.yuyin.demo

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.EnvironmentCompat
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.mobvoi.wenet.MediaCaptureService.mcs_Binder
import com.mobvoi.wenet.Recognize
import com.yuyin.demo.databinding.ActivityMainViewBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Paths

class MainActivityView : AppCompatActivity() {

    // 视图绑定
    private lateinit var binding: ActivityMainViewBinding

    private val m_ALL_PERMISSIONS_PERMISSION_CODE = 1000

    private val LOG_TAG = "YUYIN"

    // 层级配置
    private lateinit var appBarConfiguration: AppBarConfiguration

    val model: YuyinViewModel by viewModels()

    // 服务
    var mBound = false
    private var mcs_binder: mcs_Binder? = null

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
                        model.context = this@MainActivityView
                        actionBar?.show()
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
                        if (mBound) {
                            model.mcs_binder?.clearQueue()
                        }
                        actionBar?.hide()
                    }

                }

                // 导航到 runningCapture 时
                if (destination.id == R.id.runingCapture_dest) {


                }
            }
        })


    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        // 权限
        YuYinUtil.checkRequestPermissions(this,this)

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

        Thread{
            val broadCastIntent = Intent()
            broadCastIntent.action = RuningCapture.ACTION_ALL
            broadCastIntent.putExtra(
                RuningCapture.EXTRA_ACTION_NAME,
                RuningCapture.ACTION_STOP
            )
            model.context.sendBroadcast(broadCastIntent)
        }
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
            Log.e(
                LOG_TAG,
                "Error process asset $assetName to file path"
            )
        }
        return null
    }


}




