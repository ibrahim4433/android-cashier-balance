package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.AuditScreen
import com.example.ui.LoadSnapshotScreen
import com.example.ui.ManageProductsScreen
import com.example.ui.theme.MyApplicationTheme

enum class Screen {
    Audit,
    ManageProducts,
    LoadSnapshot
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val db = AppDatabase.getDatabase(applicationContext)
                        val factory = AuditViewModelFactory(db.cashierDao())
                        val viewModel: AuditViewModel = viewModel(factory = factory)
                        
                        val navController = rememberNavController()
                        
                        NavHost(navController = navController, startDestination = Screen.Audit.name) {
                            composable(Screen.Audit.name) {
                                AuditScreen(
                                    viewModel = viewModel,
                                    onNavigateToSettings = { navController.navigate(Screen.ManageProducts.name) },
                                    onNavigateToLoad = { navController.navigate(Screen.LoadSnapshot.name) }
                                )
                            }
                            composable(Screen.ManageProducts.name) {
                                ManageProductsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable(Screen.LoadSnapshot.name) {
                                LoadSnapshotScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

