package com.phantom.ai

import android.app.Application
import com.topjohnwu.superuser.Shell

class PhantomApp : Application() {

    companion object {
        init {
            // Configure libsu shell
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER)
                    .setTimeout(10)
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
    }
}
