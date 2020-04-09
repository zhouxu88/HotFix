package com.zx.classloader

import android.content.Context
import android.util.Log
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexClassLoader
import dalvik.system.PathClassLoader
import java.io.File
import java.lang.reflect.Array

/**
 * @describe 热修复的步骤：
 *  step1：构建DexClassLoader来加载补丁文件；
 *  step2：通过反射BaseDexClassLoader和DexPathList这2个类去拿到Element数组（里面就是一个或多个dex文件）；
 *       step2.1:先反射BaseDexClassLoader中变量: DexPathList pathList；
 *       step2.2:再反射DexPathList中 变量 Element[] dexElements；
 *  step3：将已经加载的apk中的Element数组和补丁中的Element数组合并，把我们的补丁dex放在数组的最前面；
 *  step4：通过反射给PathList里面的Element[] dexElements赋值
 *
 * @author zx
 * @e-mail 374952705@qq.com
 * @time   2020/4/4
 */


object HotFixEngine {
    private const val BASE_DEX_CLASSLOADER = "dalvik.system.BaseDexClassLoader"

    /**
     * BaseClassLoader中的pathList字段
     */
    private const val PATH_LIST_FIELD = "pathList"

    /**
     * DexPathList中的dexElements字段
     */
    private const val DEX_ELEMENTS_FIELD = "dexElements"

    /**
     * 补丁Dex存储的路径
     */
    private var dexPath: String = ""

    /**
     * apk/jar/zip/dex优化后的路径
     */
    private var optPath = ""


    /**
     * 反射获取对象的属性值
     * @param obj
     * @param clazz
     * @param fieldName
     */
    private fun getFieldValue(obj: Any, clazz: Class<*>, fieldName: String): Any {
        var field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(obj)
    }

    /**
     * 反射给对象中的属性重新赋值
     * @param obj
     * @param clazz
     * @param value
     * @exception NoSuchFieldException
     * @exception IllegalAccessException
     */
    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    private fun setField(obj: Any, clazz: Class<*>, field: String, value: Any) {
        val declaredField = clazz.getDeclaredField(field)
        declaredField.isAccessible = true
        declaredField.set(obj, value)
    }

    /**
     * 反射获取到BaseDexClassLoader中的DexPathList pathList属性值
     * @param baseDexClassLoader
     */
    private fun getPathList(baseDexClassLoader: BaseDexClassLoader): Any {
        var bdClazz = Class.forName(BASE_DEX_CLASSLOADER)
        return getFieldValue(baseDexClassLoader, bdClazz, PATH_LIST_FIELD)
    }

    /**
     * 反射获取DexPathList中 变量 Element[] dexElements；
     */
    private fun getDexElements(dexPathList: Any): Any {
        return getFieldValue(dexPathList, dexPathList.javaClass, DEX_ELEMENTS_FIELD)
    }

    /**
     * 尝试加载app私有目录中的补丁dex去修复bug
     * @param context
     */
    fun loadPatch(context: Context) {
        //这个是补丁dex存放的位置
        dexPath = context.filesDir.path + "/patch0.dex"
        if (!File(dexPath).exists())
            return
        //注意，只能用app私有目录去存放优化后的dex文件,如果放在外部存储存在注入攻击的风险
        // data/data/包名/files/opt_dex
        //在8.0及这个参数没有作用，只能用系统指定位置
        optPath = context.filesDir.path + "/opt_dex"
        var optFile = File(optPath)
        if (!optFile.exists()) {
            optFile.mkdirs()
        }

        //第一步，获取／创建apk和补丁dex的类加载器
        var pathClassLoader: PathClassLoader = context.classLoader as PathClassLoader
        var dexClassLoader = DexClassLoader(dexPath, optPath, null, pathClassLoader)
        //第二步，反射获取BaseDexClassLoader中的DexPathList pathList属性
        var pathPathList = getPathList(pathClassLoader)
        var dexPathList = getPathList(dexClassLoader)
        //第三步，反射获取获取DexPathList中的Element[] dexElements数组;
        var pathElements = getDexElements(pathPathList)
        var dexElements = getDexElements(dexPathList)
        //第四步，合并Elements数组,注意补丁dex的数组要放在前面
        var combineElements = combineArray(dexElements, pathElements)
        //第五步，重新给PathClassLoader中的Element[] dexElements赋值（其实在PathList里面）
        var pathList = getPathList(pathClassLoader) //再次获取apk中的PathList对象
        setField(pathList, pathList.javaClass, DEX_ELEMENTS_FIELD, combineElements)
    }

    /**
     * 将apk和补丁dex中的2个Elements数组合并，补丁dex放在最前面
     * 这里数组的拷贝和ArrayList内部数组扩容的逻辑基本一样的
     * @param pathElements
     * @param dexElements
     */
    private fun combineArray(pathElements: Any, dexElements: Any): Any {
        //反射获取数组类型
        val clazz: Class<*>? = pathElements.javaClass.componentType
        //1.获取数组长度
        var peLength = Array.getLength(pathElements)
        var deLength = Array.getLength(dexElements)
        //2.创建一个新的数组
        val newArrays = Array.newInstance(clazz, peLength + deLength) // 创建一个类型为clazz，长度为k的新数组
        //3.数组拷贝
        System.arraycopy(pathElements, 0, newArrays, 0, peLength)
        System.arraycopy(dexElements, 0, newArrays, peLength, deLength)
        return newArrays
    }
}