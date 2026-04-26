package com.nemjava.pomodoro

import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.Window
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun Window.applyAppSystemBars(root: View, @ColorRes backgroundColorRes: Int = R.color.screen_background) {
    val backgroundColor = ContextCompat.getColor(root.context, backgroundColorRes)
    val isNightMode = (root.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    val insetsController = WindowInsetsControllerCompat(this, root)
    insetsController.isAppearanceLightStatusBars = !isNightMode
    insetsController.isAppearanceLightNavigationBars = !isNightMode

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        WindowCompat.setDecorFitsSystemWindows(this, false)
        statusBarColor = android.graphics.Color.TRANSPARENT
        navigationBarColor = android.graphics.Color.TRANSPARENT
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    } else {
        WindowCompat.setDecorFitsSystemWindows(this, true)
        statusBarColor = backgroundColor
        navigationBarColor = backgroundColor
    }
}
