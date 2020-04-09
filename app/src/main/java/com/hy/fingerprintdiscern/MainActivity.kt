package com.hy.fingerprintdiscern

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        open.setOnClickListener {
            if(BiometricUtil.isSupportFingerprint(this@MainActivity)&&BiometricUtil.isHaveFingerprint(this@MainActivity)){
                //开启时根据当前指纹库创建key(9.0及以上)或保持指纹列表(9.0以下)，这样就能检查到开启后调起指纹前的指纹修改
                BiometricUtil.saveFingerprintList(this@MainActivity)
                BiometricUtil.createKey(this@MainActivity)
                Toast.makeText(this@MainActivity,"开启成功",Toast.LENGTH_SHORT).show()
            }
        }

        close.setOnClickListener {
            //清除数据操作不是必要的
            BiometricUtil.clearCache(this@MainActivity)
            Toast.makeText(this@MainActivity,"关闭成功",Toast.LENGTH_SHORT).show()
        }

        start.setOnClickListener {
            Toast.makeText(this@MainActivity,"验证指纹",Toast.LENGTH_SHORT).show()
            BiometricUtil.startAuthenticate(this@MainActivity,object : BiometricUtil.AuthenticateListener{
                override fun onAuthenticationSucceeded() {
                    Toast.makeText(this@MainActivity,"验证通过",Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(this@MainActivity,"验证失败，再试一次",Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if(errorCode == BiometricUtil.FAIL_TOO_MANY_TIMES){Toast.makeText(this@MainActivity,"指纹识别短时间内失败多次，需要验证车控密码",Toast.LENGTH_SHORT).show()}
                    else{Toast.makeText(this@MainActivity,"$errorCode：$errString",Toast.LENGTH_SHORT).show()}
                }

                override fun onFingerChanged() {
                    Toast.makeText(this@MainActivity,"系统指纹已变更，输入密码",Toast.LENGTH_SHORT).show()
                }

            })
        }

        password.setOnClickListener {
            //验证成功后重置指纹变化监听
            BiometricUtil.resetFingerChangeChecker(this@MainActivity)
            Toast.makeText(this@MainActivity,"密码验证通过",Toast.LENGTH_SHORT).show()
        }
    }
}
