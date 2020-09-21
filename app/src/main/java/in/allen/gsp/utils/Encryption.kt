package `in`.allen.gsp.utils

import android.util.Base64
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class Encryption {

    private val ALGORITHM = "AES"
    private val SALT = "E472471F8D6A1B61".toByteArray() // THE KEY MUST BE SAME

    private fun getSalt(): Key {
        return SecretKeySpec(SALT, ALGORITHM)
    }

    fun encrypt(plainText: String?): String? {
        if (plainText == null) {
            return null
        }
        val salt: Key = getSalt()
        try {
            val cipher: Cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, salt)
            val encodedValue: ByteArray = cipher.doFinal(plainText.toByteArray())
            return Base64.encodeToString(encodedValue, Base64.DEFAULT)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        throw IllegalArgumentException("Failed to encrypt data")
    }

    fun decrypt(encodedText: String?): String? {
        if (encodedText == null) {
            return null
        }
        val salt = getSalt()
        try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, salt)
            val decodedValue = Base64.decode(encodedText, Base64.DEFAULT)
            val decValue = cipher.doFinal(decodedValue)
            return String(decValue)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }

}