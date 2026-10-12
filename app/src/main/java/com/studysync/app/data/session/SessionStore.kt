package com.studysync.app.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SessionStore {
    fun read(): JsonObject?
    fun write(value: JsonObject)
    fun clear()
}
class MemorySessionStore : SessionStore {
    private var value: JsonObject?=null
    override fun read()=value?.deepCopy()
    override fun write(value: JsonObject) { this.value=value.deepCopy() }
    override fun clear() { value=null }
}
/** Device-bound encrypted app session. Never stores VTOP passwords or cookies; excluded from backups. */
class EncryptedSessionStore(context: Context) : SessionStore {
    private val file=AtomicFile(File(context.noBackupFilesDir,"studysync-session.enc"))
    private fun key(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("studysync-session-v1",null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("studysync-session-v1",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override fun read(): JsonObject? {
        if(!file.baseFile.exists()) return null
        return try {
            val data=file.readFully()
            require(data.size>28)
            val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,data.copyOfRange(0,12))) }
            JsonParser.parseString(String(cipher.doFinal(data.copyOfRange(12,data.size)),Charsets.UTF_8)).asJsonObject
        } catch (_:Exception) { clear(); null }
    }
    override fun write(value: JsonObject) {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,key()) }
        val data=cipher.iv+cipher.doFinal(value.toString().toByteArray(Charsets.UTF_8))
        val output=file.startWrite()
        try { output.write(data); file.finishWrite(output) } catch(e:Exception) { file.failWrite(output);throw e }
    }
    override fun clear() { file.delete() }
}
