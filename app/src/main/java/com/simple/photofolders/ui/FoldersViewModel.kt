package com.simple.photofolders.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simple.photofolders.data.Folder
import com.simple.photofolders.data.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class FoldersViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FolderRepository(app)

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    /** Folder currently opened in the detail / viewer screens. */
    private val _currentFolder = MutableStateFlow<Folder?>(null)
    val currentFolder: StateFlow<Folder?> = _currentFolder.asStateFlow()

    private val _photos = MutableStateFlow<List<File>>(emptyList())
    val photos: StateFlow<List<File>> = _photos.asStateFlow()

    /** True while photos are being copied into the app. */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        refreshFolders()
    }

    fun refreshFolders() {
        viewModelScope.launch { _folders.value = repo.listFolders() }
    }

    fun openFolder(id: String) {
        if (_currentFolder.value?.id != id) {
            _currentFolder.value = null
            _photos.value = emptyList()
        }
        viewModelScope.launch { reloadCurrent(id) }
    }

    fun createFolderWithPhotos(name: String, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _busy.value = true
            try {
                val id = repo.createFolder(name)
                repo.addPhotos(id, uris)
                _folders.value = repo.listFolders()
            } finally {
                _busy.value = false
            }
        }
    }

    fun addPhotos(id: String, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _busy.value = true
            try {
                repo.addPhotos(id, uris)
                reloadCurrent(id)
                _folders.value = repo.listFolders()
            } finally {
                _busy.value = false
            }
        }
    }

    fun deletePhotos(id: String, files: Collection<File>) {
        viewModelScope.launch {
            repo.deletePhotos(files)
            reloadCurrent(id)
            _folders.value = repo.listFolders()
        }
    }

    fun renameFolder(id: String, newName: String) {
        viewModelScope.launch {
            repo.renameFolder(id, newName)
            _folders.value = repo.listFolders()
            if (_currentFolder.value?.id == id) reloadCurrent(id)
        }
    }

    fun deleteFolder(id: String) {
        viewModelScope.launch {
            repo.deleteFolder(id)
            _folders.value = repo.listFolders()
        }
    }

    private suspend fun reloadCurrent(id: String) {
        // Photos first so screens never see the folder as loaded with stale photos.
        _photos.value = repo.listPhotos(id)
        _currentFolder.value = repo.getFolder(id)
    }
}
