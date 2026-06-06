package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.di.ServiceLocator
import com.example.data.model.UserProfile
import com.example.data.remote.RemoteDatabaseService
import com.example.ui.locals.LocaleResources
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the local user profile (persisted in `SharedPreferences`) plus the
 * list of customers that the admin panel surfaces.
 *
 * Backed by [ServiceLocator] for the remote service so it stays consistent
 * with the rest of the app.
 */
class UserProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val remoteService: RemoteDatabaseService = ServiceLocator.getRemoteService(context)

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _userPhone = MutableStateFlow("")
    val userPhone: StateFlow<String> = _userPhone.asStateFlow()

    private val _userCountryCode = MutableStateFlow(UserProfile.DEFAULT_COUNTRY_CODE)
    val userCountryCode: StateFlow<String> = _userCountryCode.asStateFlow()

    private val _userLocation = MutableStateFlow("")
    val userLocation: StateFlow<String> = _userLocation.asStateFlow()

    private val _isUserRegistered = MutableStateFlow(false)
    val isUserRegistered: StateFlow<Boolean> = _isUserRegistered.asStateFlow()

    private val _userAvatarIndex = MutableStateFlow(0)
    val userAvatarIndex: StateFlow<Int> = _userAvatarIndex.asStateFlow()

    private val _onlineCustomers = MutableStateFlow<List<UserProfile>>(emptyList())
    val onlineCustomers: StateFlow<List<UserProfile>> = _onlineCustomers.asStateFlow()

    val isOnline: StateFlow<Boolean> = remoteService.isOnlineFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        true
    )

    fun loadUserProfile() {
        var name = prefs.getString(KEY_USER_NAME, "") ?: ""
        if (name.isBlank()) {
            name = "مستخدم_${(1000..9999).random()}"
            prefs.edit().putString(KEY_USER_NAME, name).apply()
        }
        _username.value = name
        _userPhone.value = prefs.getString(KEY_USER_PHONE, "") ?: ""
        _userCountryCode.value = prefs.getString(
            KEY_USER_COUNTRY_CODE,
            UserProfile.DEFAULT_COUNTRY_CODE
        ) ?: UserProfile.DEFAULT_COUNTRY_CODE
        _userLocation.value = prefs.getString(KEY_USER_LOCATION, "") ?: ""
        _isUserRegistered.value = prefs.getBoolean(KEY_IS_SIGNED_UP, false)
        _userAvatarIndex.value = prefs.getInt(KEY_USER_AVATAR_INDEX, 0)
    }

    fun updateUserProfile(
        name: String,
        phone: String,
        countryCode: String,
        location: String,
        isRegistered: Boolean,
        avatarIdx: Int
    ) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_PHONE, phone)
            putString(KEY_USER_COUNTRY_CODE, countryCode)
            putString(KEY_USER_LOCATION, location)
            putBoolean(KEY_IS_SIGNED_UP, isRegistered)
            putInt(KEY_USER_AVATAR_INDEX, avatarIdx)
            apply()
        }
        _username.value = name
        _userPhone.value = phone
        _userCountryCode.value = countryCode
        _userLocation.value = location
        _isUserRegistered.value = isRegistered
        _userAvatarIndex.value = avatarIdx

        viewModelScope.launch {
            val isCloudSynced = if (isOnline.value) {
                remoteService.uploadUserProfileOnline(
                    username = name,
                    phone = phone,
                    countryCode = countryCode,
                    location = location,
                    avatarIndex = avatarIdx
                )
            } else false

            if (isCloudSynced) {
                MessageBus.publish(LocaleResources.getString(R.string.msg_profile_synced))
            } else {
                MessageBus.publish(LocaleResources.getString(R.string.msg_profile_local))
            }
        }
    }

    fun loadOnlineCustomers() {
        viewModelScope.launch {
            try {
                if (isOnline.value) {
                    _onlineCustomers.value = remoteService.getCustomersOnline()
                }
            } catch (e: Exception) {
                // Ignore gracefully — the screen will just keep the previous list.
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "ecommerce_prefs"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_COUNTRY_CODE = "user_country_code"
        private const val KEY_USER_LOCATION = "user_location"
        private const val KEY_IS_SIGNED_UP = "is_signed_up"
        private const val KEY_USER_AVATAR_INDEX = "user_avatar_index"
    }
}
