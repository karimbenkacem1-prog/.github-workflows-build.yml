package com.karimbenkacem.yourtime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karimbenkacem.yourtime.ui.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot(viewModel()) }
    }
}

@Composable
fun AppRoot(vm: MainVM) {
    val s by vm.settings.collectAsState()
    val ui by vm.ui.collectAsState()
    val st = s ?: return
    val ar = st.lang == "ar"
    val dark = when (st.theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { vm.refresh(); delay(30_000) } }
    }
    CompositionLocalProvider(LocalAr provides ar,
        LocalLayoutDirection provides if (ar) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        AppTheme(dark) {
            IslamicBackground(st.bg, dark) {
                if (!st.onboarded) Onboarding(vm, st) else MainScaffold(vm, st, ui)
            }
        }
    }
}
