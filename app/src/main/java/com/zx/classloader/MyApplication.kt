package com.zx.classloader

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * @descripe
 *
 * @author zhouxu
 * @e-mail 374952705@qq.com
 * @time   2020/4/9
 */


class MyApplication : Application() {
    companion object {
        lateinit var mContext: Context
    }

    override fun onCreate() {
        super.onCreate()
        Log.e("tag", "Application  onCreate: ")
        mContext = this
        loadPatch()
    }

    /**
     * 1、这里测试是直接把补丁dex放在data/data目录，实际开发过程中可以把dex文件放在服务器，
     * 在特定时机通过网络下载到app指定目录，然后在Application加载补丁dex
     *
     * 2、第二次进入的时候可以根据目录下是否已经下载过，处理，避免重新下载
     *
     */
    private fun loadPatch() {
//        var dexFilePath: String = filesDir.path + "/classes.dex"
//        if (Preference.getValue("patch", 0) == 1) {
//            return
//        }
        HotFixEngine.loadPatch(this)
//        Preference.saveValue("path", 1)
//        var intent = Intent(this, MainActivity::class.java)
//        startActivity(intent)
    }
}