package com.example.photogallery

import android.os.Build.VERSION_CODES
import android.provider.ContactsContract.Contacts.Photo
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.photogallery.api.GalleryItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.handleCoroutineException
import kotlinx.coroutines.launch
import retrofit2.http.Query

class PhotoGalleryViewModel : ViewModel() {
    private val photoRepository = PhotoRepository()
    private val preferencesRepository = PreferencesRepository.get()

    private val _uiState: MutableStateFlow<PhotoGalleryUiState> =
        MutableStateFlow(PhotoGalleryUiState())
    val uiState: StateFlow<PhotoGalleryUiState>
        get() = _uiState.asStateFlow()


    init {
        viewModelScope.launch {
            //since the user can submit many queries in the time that it takes to perform a single network request
            preferencesRepository.storedQuery.collectLatest { storedQuery ->
                try {
                    _uiState.update {
                        it.copy(items = fetchGalleryItem(storedQuery))
                    }
                } catch (e: Exception) {
                    Log.e("MyTag", "Failed to fetch gallery items", e)
                }
            }
        }
        viewModelScope.launch {
            preferencesRepository.isPolling.collect { isPolling ->
                _uiState.update {
                    it.copy(isPolling = isPolling)
                }
            }
        }
    }

    fun toggleIsPolling(){
        viewModelScope.launch { preferencesRepository.setPolling(!uiState.value.isPolling) }
    }
    fun setQuery(query: String) {
        viewModelScope.launch {
            preferencesRepository.setStoredQuery(query)
        }
    }

    private suspend fun fetchGalleryItem(query: String): List<GalleryItem> {
        return if (query.isEmpty()) {
            photoRepository.fetchPhotos()
        } else {
            photoRepository.searchPhotos(query)
        }
    }
}

data class PhotoGalleryUiState(
    val items: List<GalleryItem> = listOf(),
    val isPolling: Boolean = false
)