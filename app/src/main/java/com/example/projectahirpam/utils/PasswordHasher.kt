package com.example.projectahirpam.utils

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 32

    fun hash(password: String): String {
        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        return "$salt:$hash"
    }

    fun verify(password: String, hashedPassword: String): Boolean {
        val parts = hashedPassword.split(":")
        if (parts.size != 2) return false
        
        val salt = parts[0]
        val hash = parts[1]
        return hashPassword(password, salt) == hash
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return salt.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        return hash.fold("") { str, it -> str + "%02x".format(it) }
    }
}
