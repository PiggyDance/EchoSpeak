package io.piggydance.echospeak.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GoogleUser(
    val id: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
)

sealed class SignInResult {
    data class Success(val user: GoogleUser) : SignInResult()
    data object Cancelled : SignInResult()
    /** [errorCode] 对应 [SignInError] */
    data class Error(val errorCode: SignInError, val detail: String = "") : SignInResult()
}

enum class SignInError {
    NOT_CONFIGURED,   // WEB_CLIENT_ID 未填写
    NO_CREDENTIAL,    // 设备上没有 Google 账号，或 SHA-1 未注册
    UNKNOWN,
}

/**
 * Google 登录管理器
 *
 * 使用 Android Credential Manager 实现 Google Sign-In。
 *
 * ── 使用前必须完成以下配置 ────────────────────────────────────────
 * 1. 在 Google Cloud Console 创建 OAuth 2.0 Web Application 客户端
 * 2. 将 Web Client ID 填入下方 [WEB_CLIENT_ID]
 * 3. 将 debug/release 的 SHA-1 指纹注册到同一项目的 Android 应用
 * ──────────────────────────────────────────────────────────────────
 */
object GoogleAuthManager {

    // ── 将 Google Cloud Console → OAuth 2.0 → Web 客户端 → 客户端 ID 粘贴到此处 ──
    const val WEB_CLIENT_ID = "718461286662-bu6lrp4077vjh2nc1kbp6k1csqibiot9.apps.googleusercontent.com"

    private const val PREFS_NAME = "echospeak_auth"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_PHOTO = "user_photo"

    private val _currentUser = MutableStateFlow<GoogleUser?>(null)
    val currentUser: StateFlow<GoogleUser?> = _currentUser.asStateFlow()

    /** 是否已完成配置 */
    val isConfigured: Boolean
        get() = WEB_CLIENT_ID != "YOUR_WEB_CLIENT_ID_HERE" && WEB_CLIENT_ID.isNotBlank()

    /** 从持久化存储恢复登录状态（在 Application.onCreate 调用） */
    fun restoreSession(context: Context) {
        val prefs = prefs(context)
        val id = prefs.getString(KEY_USER_ID, null) ?: return
        _currentUser.value = GoogleUser(
            id = id,
            displayName = prefs.getString(KEY_USER_NAME, "") ?: "",
            email = prefs.getString(KEY_USER_EMAIL, "") ?: "",
            photoUrl = prefs.getString(KEY_USER_PHOTO, null),
        )
    }

    /**
     * 发起 Google 登录
     *
     * 采用两阶段策略：
     * 1. 先尝试已授权账号（静默登录，体验最好）
     * 2. 如果没有已授权账号，再弹出完整账号选择器
     */
    suspend fun signIn(activityContext: Context): SignInResult {
        if (!isConfigured) {
            return SignInResult.Error(SignInError.NOT_CONFIGURED)
        }

        val credentialManager = CredentialManager.create(activityContext)

        // 阶段 1：尝试已授权账号（filterByAuthorizedAccounts = true）
        val phase1Result = runCatching {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(true)
                .build()
            credentialManager.getCredential(
                activityContext,
                GetCredentialRequest.Builder().addCredentialOption(option).build()
            )
        }

        // 阶段 2：如果阶段 1 没有找到已授权账号，显示完整账号选择器
        val credentialResult = if (phase1Result.isSuccess) {
            phase1Result.getOrThrow()
        } else {
            try {
                val option = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()
                credentialManager.getCredential(
                    activityContext,
                    GetCredentialRequest.Builder().addCredentialOption(option).build()
                )
            } catch (e: GetCredentialCancellationException) {
                return SignInResult.Cancelled
            } catch (e: NoCredentialException) {
                return SignInResult.Error(SignInError.NO_CREDENTIAL, e.message ?: "")
            } catch (e: GetCredentialException) {
                return SignInResult.Error(SignInError.UNKNOWN, e.message ?: "")
            } catch (e: Exception) {
                return SignInResult.Error(SignInError.UNKNOWN, e.message ?: "")
            }
        }

        return try {
            val googleIdTokenCredential =
                GoogleIdTokenCredential.createFrom(credentialResult.credential.data)
            val user = GoogleUser(
                id = googleIdTokenCredential.id,
                displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id,
                email = googleIdTokenCredential.id,
                photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
            )
            persistUser(activityContext, user)
            _currentUser.value = user
            SignInResult.Success(user)
        } catch (e: Exception) {
            SignInResult.Error(SignInError.UNKNOWN, e.message ?: "")
        }
    }

    /** 退出登录 */
    suspend fun signOut(context: Context) {
        runCatching {
            CredentialManager.create(context)
                .clearCredentialState(ClearCredentialStateRequest())
        }
        clearPersistedUser(context)
        _currentUser.value = null
    }

    private fun persistUser(context: Context, user: GoogleUser) {
        prefs(context).edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.displayName)
            .putString(KEY_USER_EMAIL, user.email)
            .putString(KEY_USER_PHOTO, user.photoUrl)
            .apply()
    }

    private fun clearPersistedUser(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
