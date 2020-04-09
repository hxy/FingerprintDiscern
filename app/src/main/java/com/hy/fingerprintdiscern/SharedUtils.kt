package com.hy.fingerprintdiscern

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log

import java.io.*

/**
 * SharedPreferences工具类
 */
class SharedUtils {

    private var mShared: SharedPreferences? = null

    /**
     * @param context
     * @param name    SharedPreferences名字
     */
    private constructor(context: Context, name: String) {
        this.mShared = context.getSharedPreferences(name, 0)
    }


    companion object{
        private var instance:SharedUtils? = null
        fun getInstance(context: Context,name:String="mySP"):SharedUtils{
            if(instance == null){
                synchronized(this.javaClass){
                    if(instance == null){
                        instance = SharedUtils(context,name)
                    }
                }
            }
            return instance!!
        }
    }

    /**
     * shared是否包含key
     *
     * @param key
     * @return
     */
    operator fun contains(key: String): Boolean {
        return mShared!!.contains(key)
    }

    /**
     * 插入boolean类型
     *
     * @param key
     * @param value
     */
    fun put(key: String, value: Boolean) {
        val editor = this.mShared!!.edit()
        editor.putBoolean(key, value!!)
        editor.commit()
    }

    /**
     * 获取boolean类型
     *
     * @param key
     * @return
     */
    fun getBoolean(key: String): Boolean {
        return this.getBoolean(key, false)
    }

    /**
     * 获取boolean类型
     *
     * @param key
     * @param defValue 默认值
     * @return
     */
    fun getBoolean(key: String, defValue: Boolean): Boolean {
        return this.mShared!!.getBoolean(key, defValue)
    }

    /**
     * 插入float类型
     *
     * @param key
     * @param value
     * @return
     */
    fun put(key: String, value: Float) {
        val editor = this.mShared!!.edit()
        editor.putFloat(key, value!!)
        editor.commit()
    }

    /**
     * 获取float类型
     *
     * @param key
     * @return
     */
    fun getFloat(key: String): Float {
        return this.getFloat(key, 0.0f)
    }

    /**
     * 获取float类型
     *
     * @param key
     * @param defValue 默认值
     * @return
     */
    fun getFloat(key: String, defValue: Float): Float {
        return this.mShared!!.getFloat(key, defValue)
    }

    /**
     * 插入int类型
     *
     * @param key
     * @param value
     * @return
     */
    fun put(key: String, value: Int) {
        val editor = this.mShared!!.edit()
        editor.putInt(key, value!!)
        editor.commit()
    }

    /**
     * 获取int类型
     *
     * @param key
     * @return
     */
    fun getInt(key: String): Int {
        return this.getInt(key, 0)
    }

    /**
     * 获取int类型
     *
     * @param key
     * @param defValue 默认值
     * @return
     */
    fun getInt(key: String, defValue: Int): Int {
        return this.mShared!!.getInt(key, defValue)
    }

    /**
     * 插入long类型
     *
     * @param key
     * @param value
     * @return
     */
    fun put(key: String, value: Long) {
        val editor = this.mShared!!.edit()
        editor.putLong(key, value!!)
        editor.commit()
    }

    /**
     * 获取Long类型
     *
     * @param key
     * @return
     */
    fun getLong(key: String): Long {
        return this.getLong(key, 0L)
    }

    /**
     * 获取Long类型
     *
     * @param key
     * @param defValue 默认值
     * @return
     */
    fun getLong(key: String, defValue: Long): Long {
        return this.mShared!!.getLong(key, defValue)
    }

    /**
     * 插入String类型
     *
     * @param key
     * @param value
     * @return
     */
    fun put(key: String, value: String) {
        val editor = this.mShared!!.edit()
        editor.putString(key, value)
        editor.commit()
    }

    /**
     * 获取String类型
     *
     * @param key
     * @return
     */
    fun getString(key: String): String? {
        return this.getString(key, "")
    }

    /**
     * 获取String类型
     *
     * @param key
     * @param defValue 默认值
     * @return
     */
    fun getString(key: String, defValue: String): String? {
        return this.mShared!!.getString(key, defValue)
    }

    /**
     * 插入序列化对象
     *
     * @param key
     * @param value
     */
    fun put(key: String, value: Serializable) {
        try {
            val e = ByteArrayOutputStream()
            val oos = ObjectOutputStream(e)
            oos.writeObject(value)
            val str = Base64.encodeToString(e.toByteArray(), 0)
            val editor = this.mShared!!.edit()
            editor.putString(key, str)
            editor.commit()
        } catch (e: IOException) {
            Log.e("SharedUtils", "put # 保存序列化对象错误", e)
        }

    }

    /**
     * 获取存储的序列化对象
     *
     * @param key
     * @return
     */
     fun getObject(key: String): Any? {
        return this.getObject(key, null)
    }

    /**
     * 获取存储的序列化对象
     *
     * @param key
     * @param defValue 默认值
     * @return
     */
    fun getObject(key: String, defValue: Serializable?): Any? {
        val str = this.mShared!!.getString(key, null as String?)
        return if (str == null) {
            defValue
        } else {
            try {
                val e = Base64.decode(str.toByteArray(), 0)
                val fromByte = ByteArrayInputStream(e)
                val ois = ObjectInputStream(fromByte)
                ois.readObject()
            } catch (e: Exception) {
                Log.e("SharedUtils", "get # 获取序列化对象错误", e)
                defValue
            }

        }
    }

    /**
     * 清空share
     */
    fun clear() {
        val editor = this.mShared!!.edit()
        editor.clear()
        editor.commit()
    }

    /**
     * 根据key删除shared
     *
     * @param key
     */
    fun deleteByKey(key: String) {
        val editor = this.mShared!!.edit()
        editor.remove(key)
        editor.commit()
    }
}
