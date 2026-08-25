package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.ProfileDao
import com.trackit.app.data.local.entity.ProfileEntity
import com.trackit.app.util.SyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao,
    private val syncManager: SyncManager
) {
    fun getAllProfiles(): Flow<List<ProfileEntity>> = profileDao.getAllProfiles()

    suspend fun getProfileById(id: Long): ProfileEntity? = profileDao.getProfileById(id)

    suspend fun insert(profile: ProfileEntity): Long {
        val id = profileDao.insert(profile)
        syncManager.pushProfile(profile.copy(id = id))
        return id
    }

    suspend fun update(profile: ProfileEntity) {
        profileDao.update(profile)
        syncManager.pushProfile(profile)
    }

    suspend fun delete(profile: ProfileEntity) {
        profileDao.delete(profile)
        syncManager.deleteProfile(profile)
    }

    suspend fun getCount(): Int = profileDao.getCount()
}
