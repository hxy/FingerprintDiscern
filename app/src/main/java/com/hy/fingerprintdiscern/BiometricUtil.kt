package com.hy.fingerprintdiscern

import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import javax.crypto.KeyGenerator
import android.os.CancellationSignal
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import javax.crypto.Cipher
import javax.crypto.SecretKey
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import java.lang.StringBuilder


/**
 * Created by yue.huang on 2020/2/25.
 * 生物识别工具类
 */

object BiometricUtil {
    private const val DEFAULT_KEY_NAME = "borgward_fingerprint_key"
    private const val HAS_FINGER_KEY = "has_finger_key"
    private const val IS_FINGER_CHANGED = "is_finger_changed"
    private const val LOCAL_FINGERPRINT = "local_fingerprint"
    const val FAIL_TOO_MANY_TIMES = 7
    private var cancellationSignal:CancellationSignal? = null
    /**
     * 手机硬件是否支持指纹
     */
    fun isSupportFingerprint(context: Context):Boolean{
        return if(Build.VERSION.SDK_INT < 23){
            false
        }else{
            val fingerprintManager = getSystemService(context,FingerprintManager::class.java)
            if(fingerprintManager==null){
                false
            }else{
                fingerprintManager?.isHardwareDetected()
            }
        }
    }


    /**
     * 系统是否开启指纹并且已经录入过指纹
     */
    fun isHaveFingerprint(context: Context):Boolean{
         if(Build.VERSION.SDK_INT < 23){
             return false
        }else{
            val fingerprintManager = getSystemService(context,FingerprintManager::class.java)
            if(fingerprintManager==null){return false}
            else{
                return  fingerprintManager?.hasEnrolledFingerprints()
            }
        }
    }

    /**
     * 保存当前系统中指令列表
     */
    fun saveFingerprintList(context: Context):Boolean{
        var idList = getCurrentFingerprintIdList(context)
        if(idList!=null && idList.size>0){
            var flag = StringBuilder("")
            for(item in idList){
                flag.append(item)
            }
            SharedUtils.getInstance(context).put(LOCAL_FINGERPRINT,flag.toString())
            return true
        }
        return false
    }



    /**
     * 开始识别
     */
    fun startAuthenticate(context: Context,callBack:AuthenticateListener){
        if(Build.VERSION.SDK_INT >= 23 && isSupportFingerprint(context) && isHaveFingerprint(context)){
            val keyStore = createKey(context) ?: return
            val cipher = createCipher()
            if(initCipher(cipher,keyStore!!)){
                SharedUtils.getInstance(context).put(IS_FINGER_CHANGED,true)
                clearHasFingerKeyFlag(context)
                startAuthenticate(context,callBack)
                return
            }
            val fingerprintManager = getSystemService(context,FingerprintManager::class.java)
            cancellationSignal = CancellationSignal()
            fingerprintManager?.authenticate(FingerprintManager.CryptoObject(cipher), cancellationSignal,0,object : FingerprintManager.AuthenticationCallback(){
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    if(errorCode != 5 && errorCode != 10){
                        callBack?.onAuthenticationError(errorCode,errString?:"")
                    }
                }

                override fun onAuthenticationFailed() {
                    callBack?.onAuthenticationFailed()
                }

                override fun onAuthenticationHelp(helpCode: Int, helpString: CharSequence?) {}

                override fun onAuthenticationSucceeded(result: FingerprintManager.AuthenticationResult?) {
                    if (SharedUtils.getInstance(context).getBoolean(IS_FINGER_CHANGED,false)){
                        callBack?.onFingerChanged()
                    }else if (Build.VERSION.SDK_INT in 23..27){
                        if(checkFingerprintChanged(context)){
                            callBack?.onFingerChanged()
                        }else{
                            callBack?.onAuthenticationSucceeded()
                        }
                    }else{
                        try {
                            result?.cryptoObject?.cipher?.doFinal()
                            callBack?.onAuthenticationSucceeded()
                        }catch (e:Exception){
                            e.printStackTrace()
                            callBack?.onFingerChanged()
                        }
                    }
                }
            },null)
        }else{
            callBack.onAuthenticationError(-1,"不支持指纹识别")
        }
    }

    /**
     * 停止识别
     */
    fun stopAuthenticate(){
       cancellationSignal?.cancel()
    }


    /*
    清除本地指纹记录
     */
    fun clearCache(context: Context){
        SharedUtils.getInstance(context).deleteByKey(HAS_FINGER_KEY)
        SharedUtils.getInstance(context).deleteByKey(IS_FINGER_CHANGED)
        SharedUtils.getInstance(context).deleteByKey(LOCAL_FINGERPRINT)
    }

    /**
     * 重置指纹库改变检查状态
     */
    fun resetFingerChangeChecker(context: Context){
        SharedUtils.getInstance(context).deleteByKey(HAS_FINGER_KEY)
        SharedUtils.getInstance(context).deleteByKey(IS_FINGER_CHANGED)
        saveFingerprintList(context)
        createKey(context)
    }

    /**
     * 清除是否有钥匙标记，使下次调起指纹时重新生成钥匙
     */
    private fun clearHasFingerKeyFlag(context: Context){
        SharedUtils.getInstance(context).deleteByKey(HAS_FINGER_KEY)
    }


    private fun checkFingerprintChanged(context: Context):Boolean{
        var local = SharedUtils.getInstance(context).getString(LOCAL_FINGERPRINT)
        var idList = getCurrentFingerprintIdList(context)
        if(idList!=null && idList.size>0){
            var current = StringBuilder()
            for(item in idList){
                current.append(item)
            }
            return local != current.toString()
        }else{
            return false
        }
    }

    /**
     * 获取当前系统中的指纹id列表
     */

    private fun getCurrentFingerprintIdList(context: Context):ArrayList<Int>?{
        if(Build.VERSION.SDK_INT>=23){
            if(Build.VERSION.SDK_INT<28){
                var idList = ArrayList<Int>()
                val fingerprintManager = getSystemService(context,FingerprintManager::class.java)
                val method = FingerprintManager::class.java.getDeclaredMethod("getEnrolledFingerprints")
                val obj = method.invoke(fingerprintManager)
                if (obj != null) {
                    val clazz = Class.forName("android.hardware.fingerprint.Fingerprint")
                    val getFingerId = clazz.getDeclaredMethod("getFingerId")
                    for (i in 0 until (obj as List<*>).size) {
                        if(obj[i] != null){
                            getFingerId.invoke(obj[i])?.let {
                                idList.add(it as Int)
                            }
                        }
                    }
                }
                return idList
            }
        }
        return null
    }





    fun createKey(context: Context):KeyStore?{
        if(Build.VERSION.SDK_INT < 23 || !isSupportFingerprint(context) || !isHaveFingerprint(context)){return null}
        try {
            var keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore?.load(null)
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            if(!SharedUtils.getInstance(context).getBoolean(HAS_FINGER_KEY,false)){
                var builder = KeyGenParameterSpec.Builder(DEFAULT_KEY_NAME, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        .setUserAuthenticationRequired(true)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    builder.setInvalidatedByBiometricEnrollment(true)
                }
                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
                SharedUtils.getInstance(context).put(HAS_FINGER_KEY,true)
            }
            return keyStore
        }catch (e:Exception){
            return null
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createCipher():Cipher{
        return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                + KeyProperties.BLOCK_MODE_CBC + "/"
                + KeyProperties.ENCRYPTION_PADDING_PKCS7)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initCipher(cipher:Cipher,keyStore: KeyStore):Boolean{
        try {
            val key = keyStore.getKey(DEFAULT_KEY_NAME, null) as SecretKey?
            cipher.init(Cipher.ENCRYPT_MODE, key)
            return false
        }catch (e: KeyPermanentlyInvalidatedException ){
            return true
        }catch (e: UnrecoverableKeyException) {
            return true
        }catch (e: KeyStoreException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: CertificateException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: IOException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        }
    }


    interface AuthenticateListener{
        fun onAuthenticationSucceeded()
        fun onAuthenticationFailed()
        fun onAuthenticationError(errorCode:Int, errString:CharSequence)
        fun onFingerChanged()
    }

}