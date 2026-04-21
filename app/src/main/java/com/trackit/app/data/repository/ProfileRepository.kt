package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.ProfileDao
import com.trackit.app.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao
) {
    fun getAllProfiles(): Flow<List<ProfileEntity>> = profileDao.getAllProfiles()

    suspend fun getProfileById(id: Long): ProfileEntity? = profileDao.getProfileById(id)

    suspend fun insert(profile: ProfileEntity): Long = profileDao.insert(profile)

    suspend fun update(profile: ProfileEntity) = profileDao.update(profile)

    suspend fun delete(profile: ProfileEntity) = profileDao.delete(profile)
    
    suspend fun getCount(): Int = profileDao.getCount()
}
