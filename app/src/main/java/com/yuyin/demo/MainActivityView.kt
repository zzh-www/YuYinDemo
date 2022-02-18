package com.yuyin.demo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import com.yuyin.demo.databinding.ActivityMainViewBinding

class MainActivityView : AppCompatActivity() {

    // 视图绑定
    private lateinit var binding: ActivityMainViewBinding

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
    }

    override fun onDestroy() {
        super.onDestroy()
        binding
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




}