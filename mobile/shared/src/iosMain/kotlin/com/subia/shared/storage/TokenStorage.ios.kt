package com.subia.shared.storage

import com.subia.shared.model.AuthTokens
import com.subia.shared.platform.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/** Almacenamiento de tokens usando iOS Keychain Services. */
@OptIn(ExperimentalForeignApi::class)
actual class TokenStorage actual constructor(context: PlatformContext) {

    actual fun saveTokens(tokens: AuthTokens) {
        saveToKeychain(KEY_ACCESS, tokens.accessToken)
        saveToKeychain(KEY_REFRESH, tokens.refreshToken)
    }

    actual fun getTokens(): AuthTokens? {
        val access = readFromKeychain(KEY_ACCESS) ?: return null
        val refresh = readFromKeychain(KEY_REFRESH) ?: return null
        return AuthTokens(accessToken = access, refreshToken = refresh)
    }

    actual fun clearTokens() {
        deleteFromKeychain(KEY_ACCESS)
        deleteFromKeychain(KEY_REFRESH)
    }

    actual fun hasTokens(): Boolean =
        readFromKeychain(KEY_ACCESS) != null && readFromKeychain(KEY_REFRESH) != null

    private fun saveToKeychain(key: String, value: String) {
        val data = NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        deleteFromKeychain(key)
        @Suppress("UNCHECKED_CAST")
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to key,
            kSecValueData to data,
            kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        )
        SecItemAdd(CFBridgingRetain(query) as CFDictionaryRef, null)
    }

    private fun readFromKeychain(key: String): String? = memScoped {
        @Suppress("UNCHECKED_CAST")
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to key,
            kSecReturnData to true,
            kSecMatchLimit to kSecMatchLimitOne
        )
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(CFBridgingRetain(query) as CFDictionaryRef, result.ptr)
        if (status == errSecSuccess) {
            val nsData = CFBridgingRelease(result.value) as? NSData ?: return null
            return NSString.create(data = nsData, encoding = NSUTF8StringEncoding)?.toString()
        }
        null
    }

    private fun deleteFromKeychain(key: String) {
        @Suppress("UNCHECKED_CAST")
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrAccount to key
        )
        SecItemDelete(CFBridgingRetain(query) as CFDictionaryRef)
    }

    companion object {
        private const val KEY_ACCESS = "subia_access_token"
        private const val KEY_REFRESH = "subia_refresh_token"
    }
}
