package com.ohmybattery.invoicing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ohmybattery.invoicing.ui.OhmybatteryRoot
import com.ohmybattery.invoicing.ui.theme.OhmybatteryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OhmybatteryTheme {
                OhmybatteryRoot()
            }
        }
    }
}
