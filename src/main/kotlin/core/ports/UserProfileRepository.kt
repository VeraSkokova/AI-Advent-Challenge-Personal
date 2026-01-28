package core.ports

import core.domain.model.UserPreferences

interface UserProfileRepository {
    suspend fun loadProfile(): UserPreferences?
    suspend fun saveProfile(profile: UserPreferences)
}
