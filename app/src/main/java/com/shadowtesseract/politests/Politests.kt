package com.shadowtesseract.politests

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.evernote.android.job.JobRequest
import org.acra.ACRA
import org.acra.config.CoreConfigurationBuilder
import org.acra.config.MailSenderConfigurationBuilder
import org.acra.config.SchedulerConfigurationBuilder
import org.acra.config.ToastConfigurationBuilder
import org.acra.data.StringFormat

class Politests : Application() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        val coreConfigurationBuilder = CoreConfigurationBuilder(this)
                .setBuildConfigClass(BuildConfig::class.java)
                .setReportFormat(StringFormat.JSON)
        coreConfigurationBuilder.getPluginConfigurationBuilder(MailSenderConfigurationBuilder::class.java)
                .setMailTo("shadow.tesseract.studio@gmail.com")
                .setReportAsFile(true)
                .setReportFileName("[CRASH] ${Build.MANUFACTURER}/${Build.MODEL}/${Build.VERSION.SDK_INT}")
                .setSubject("[TESTOWNIK] Crash report")
                .setEnabled(true)
        coreConfigurationBuilder.getPluginConfigurationBuilder(ToastConfigurationBuilder::class.java)
                .setResText(R.string.acra_toast_text)
                .setEnabled(true)
        coreConfigurationBuilder.getPluginConfigurationBuilder(SchedulerConfigurationBuilder::class.java)
                .setRequiresNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setEnabled(true)
        ACRA.init(this, coreConfigurationBuilder)
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }
}