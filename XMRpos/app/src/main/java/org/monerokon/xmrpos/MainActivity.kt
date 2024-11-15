package org.monerokon.xmrpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.monerokon.xmrpos.data.DataStoreManager
import org.monerokon.xmrpos.ui.NavGraphRoot
import org.monerokon.xmrpos.ui.theme.XMRposTheme

class MainActivity : ComponentActivity() {
    private lateinit var dataStoreManager: DataStoreManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars.
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        dataStoreManager = DataStoreManager(applicationContext)
        setContent {
            XMRposTheme {
                NavGraphRoot()
            }
        }
    }
}