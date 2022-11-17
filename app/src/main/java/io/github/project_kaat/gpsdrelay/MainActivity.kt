package io.github.project_kaat.gpsdrelay


import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.project_kaat.gpsdrelay.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator

val tabTitleArray = arrayOf(
    R.string.tab_main_title,
    R.string.tab_settings_title
)

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val viewPager = binding.viewPager2
        val tabs = binding.tabs

        val adapter = myFragmentAdapter(supportFragmentManager, lifecycle)
        viewPager.adapter = adapter

        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = getString(tabTitleArray[position])
        }.attach()

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 0)
        }
    }


}