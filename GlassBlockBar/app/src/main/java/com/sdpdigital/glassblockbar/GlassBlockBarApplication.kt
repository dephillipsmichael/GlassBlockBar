package com.sdpdigital.glassblockbar

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.sdpdigital.glassblockbar.viewmodel.GlassBlockBarViewModel


class GlassBlockBarApplication : Application(), ViewModelStoreOwner {
    override fun onCreate() {
        super.onCreate()
    }

    // Allows the app to share instances of the ViewModel
    private val appViewModelStore: ViewModelStore by lazy {
        ViewModelStore()
    }

    override fun getViewModelStore(): ViewModelStore {
        return appViewModelStore
    }
}

public class AppViewModelFactory(val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(Application::class.java)
            .newInstance(app)
    }
}