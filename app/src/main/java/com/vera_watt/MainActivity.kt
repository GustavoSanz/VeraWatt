package com.vera_watt

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.vera_watt.ui.VeraWattApp
import com.vera_watt.ui.theme.VeraWattTheme
import com.vera_watt.viewmodel.VeraWattViewModel

class MainActivity : FragmentActivity() {
    private val vm: VeraWattViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VeraWattTheme { VeraWattApp(vm) } }
    }
}
