package com.resumemaster.android

import android.app.Application

class ResumeMasterApp : Application() {
  override fun onCreate() {
    super.onCreate()
    AppGraph.init(this)
  }
}
