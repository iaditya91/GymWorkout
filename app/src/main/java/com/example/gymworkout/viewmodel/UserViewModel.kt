package com.example.gymworkout.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymworkout.data.ChecklistItem
import com.example.gymworkout.data.UserProfile
import com.example.gymworkout.data.WorkoutDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = WorkoutDatabase.getDatabase(application).userDao()

    fun getProfile(): Flow<UserProfile?> = dao.getProfile()

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            dao.upsertProfile(profile)
        }
    }

    fun getDos(): Flow<List<ChecklistItem>> = dao.getChecklistItems("DO")

    fun getDonts(): Flow<List<ChecklistItem>> = dao.getChecklistItems("DONT")

    fun addChecklistItem(type: String, text: String) {
        viewModelScope.launch {
            dao.insertChecklistItem(ChecklistItem(type = type, text = text))
        }
    }

    fun toggleChecklistItem(id: Int, checked: Boolean) {
        viewModelScope.launch {
            dao.toggleChecklistItem(id, checked)
        }
    }

    fun deleteChecklistItem(item: ChecklistItem) {
        viewModelScope.launch {
            dao.deleteChecklistItem(item)
        }
    }

    fun addPhotoUri(currentProfile: UserProfile?, uri: String) {
        viewModelScope.launch {
            val profile = currentProfile ?: UserProfile()
            val existing = if (profile.photoUris.isBlank()) emptyList()
            else profile.photoUris.split(",")
            val updated = (existing + uri).joinToString(",")
            dao.upsertProfile(profile.copy(photoUris = updated))
        }
    }

    fun removePhotoUri(currentProfile: UserProfile?, uri: String) {
        viewModelScope.launch {
            val profile = currentProfile ?: return@launch
            val updated = profile.photoUris.split(",")
                .filter { it != uri }
                .joinToString(",")
            dao.upsertProfile(profile.copy(photoUris = updated))
        }
    }
}
