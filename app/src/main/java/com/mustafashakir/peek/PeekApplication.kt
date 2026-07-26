package com.mustafashakir.peek

import android.app.Application
import com.mustafashakir.peek.app.AppContainer
import com.mustafashakir.peek.app.DefaultAppContainer

class PeekApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(applicationContext) }
}
