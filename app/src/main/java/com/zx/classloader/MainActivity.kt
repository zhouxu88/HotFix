package com.zx.classloader

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import dalvik.system.DexClassLoader
import dalvik.system.PathClassLoader
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

/**
 * ClassLoader 类加载机制实现热修复（仅修复代码）
 * 资源文件、so库 ，其他的底层低唤、Instant Run方案后面再补充
 */
class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    var mContext: Context? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mContext = this

        Log.e(TAG, "Activity.class 由：" + Activity::class.java.classLoader + " 加载")
        var classLoader = classLoader
        Log.e(TAG, "MainActivity.class 由：$classLoader 加载")
        while (classLoader != null) {
            classLoader = classLoader.parent
            Log.e(TAG, "parent classLoader : $classLoader")
        }

//        Log.e(TAG, "onCreate: this is fixed class")

//        tv.text = "this is fixed class"

//        loadDexClass()

    }


    /**
     * 加载dex文件中的class，并调用其中的sayHello方法
     */
    private fun loadDexClass() {
        // /data/data/com.zx.classloader/person.dex
        val internalPath = filesDir.path + "/person.dex"
        val file = File(internalPath)

        //下面开始加载dex class
        val dexClassLoader = PathClassLoader(file.path, null, classLoader)
        val libClazz = dexClassLoader.loadClass("com.zx.classloader.Person")
        var person = libClazz.newInstance() as IAction
        tv.text = person.say()
    }


    class Test : ClassLoader() {

    }


}
