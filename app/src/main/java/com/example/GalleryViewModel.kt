package com.example

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    // UI state states
    val isDarkMode = MutableStateFlow(true) // dark mode by default for premium look
    val gridColumnsCount = MutableStateFlow(3) // 2 to 4 columns, customizable

    val favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val deletedIds = MutableStateFlow<Set<String>>(emptySet())

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission

    private val _localMediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val isLoading = MutableStateFlow(false)

    // Mode to force sample content display (excellent for showing app features when emulator is empty)
    val showSamplesOnly = MutableStateFlow(false)

    init {
        checkPermissionState()
    }

    fun checkPermissionState() {
        val hasStorePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val img = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val vid = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            img && vid
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        _hasPermission.value = hasStorePermission
        if (hasStorePermission) {
            loadLocalMedia()
        }
    }

    // Set permission manually if granted
    fun setPermissionGranted(granted: Boolean) {
        _hasPermission.value = granted
        if (granted) {
            loadLocalMedia()
        }
    }

    // Toggle dark mode
    fun toggleDarkMode() {
        isDarkMode.value = !isDarkMode.value
    }

    // Cycle grid size: 2 -> 3 -> 4 -> 2
    fun cycleGridColumns() {
        gridColumnsCount.value = when (gridColumnsCount.value) {
            2 -> 3
            3 -> 4
            else -> 2
        }
    }

    fun toggleFavorite(itemId: String) {
        val current = favoriteIds.value
        if (current.contains(itemId)) {
            favoriteIds.value = current - itemId
        } else {
            favoriteIds.value = current + itemId
        }
    }

    fun deleteMedia(itemId: String) {
        deletedIds.value = deletedIds.value + itemId
    }

    // Expose final sorted list of media items
    val mediaItems: StateFlow<List<MediaItem>> = combine(
        _localMediaItems,
        showSamplesOnly,
        _hasPermission,
        deletedIds
    ) { localItems, forceShowSamples, isPermitted, deleted ->
        val rawList = if (forceShowSamples || !isPermitted || localItems.isEmpty()) {
            SampleMedia.getSamples().sortedByDescending { it.dateAdded }
        } else {
            localItems.sortedByDescending { it.dateAdded }
        }
        rawList.filter { it.id !in deleted }
    }.stateIn(viewModelScope, SharingStarted.Lazily, SampleMedia.getSamples())

    // Convert flat list of MediaItems into Album folders
    val albums: StateFlow<List<Album>> = combine(
        mediaItems,
        favoriteIds
    ) { items, favIds ->
        val groups = items.groupBy { it.folderName }
        val albumList = ArrayList<Album>()

        // Always put "All" album first
        if (items.isNotEmpty()) {
            albumList.add(
                Album(
                    name = "All Photos",
                    coverUri = items.first().uri,
                    isVideoCover = items.first().isVideo,
                    itemsCount = items.size,
                    items = items
                )
            )
        }

        // Put "Favorites" album second if there are favorites
        val favItems = items.filter { it.id in favIds }
        if (favItems.isNotEmpty()) {
            albumList.add(
                Album(
                    name = "Favorites",
                    coverUri = favItems.first().uri,
                    isVideoCover = favItems.first().isVideo,
                    itemsCount = favItems.size,
                    items = favItems
                )
            )
        }

        groups.forEach { (name, groupItems) ->
            if (name != "All Photos" && name != "Favorites") {
                albumList.add(
                    Album(
                        name = name,
                        coverUri = groupItems.first().uri,
                        isVideoCover = groupItems.first().isVideo,
                        itemsCount = groupItems.size,
                        items = groupItems.sortedByDescending { it.dateAdded }
                    )
                )
            }
        }

        // Put "Videos" album next if there are videos
        val videoItems = items.filter { it.isVideo }
        if (videoItems.isNotEmpty() && !groups.containsKey("Videos")) {
            albumList.add(
                if (favItems.isNotEmpty()) 2 else 1,
                Album(
                    name = "Videos",
                    coverUri = videoItems.first().uri,
                    isVideoCover = true,
                    itemsCount = videoItems.size,
                    items = videoItems.sortedByDescending { it.dateAdded }
                )
            )
        }

        albumList
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadLocalMedia() {
        viewModelScope.launch {
            isLoading.value = true
            val items = queryDeviceMediaStore()
            _localMediaItems.value = items
            isLoading.value = false
        }
    }

    private suspend fun queryDeviceMediaStore(): List<MediaItem> = withContext(Dispatchers.IO) {
        val resultList = ArrayList<MediaItem>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DURATION
        )

        // Query both images and videos
        val selection = (
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?" +
                " OR " +
                "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        )
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val queryUri: Uri = MediaStore.Files.getContentUri("external")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                queryUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { cur ->
                val idCol = cur.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cur.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val typeCol = cur.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val dateCol = cur.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val bucketCol = cur.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                val sizeCol = cur.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val durCol = cur.getColumnIndex(MediaStore.Files.FileColumns.DURATION)

                while (cur.moveToNext()) {
                    val id = cur.getLong(idCol)
                    val name = cur.getString(nameCol) ?: "Media_${id}"
                    val mediaType = cur.getInt(typeCol)
                    val dateAdded = cur.getLong(dateCol) * 1000L // convert to milli
                    val bucketName = cur.getString(bucketCol) ?: "Camera"
                    val size = cur.getLong(sizeCol)

                    val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    val contentUri: Uri = if (isVideo) {
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    }

                    var durationStr: String? = null
                    if (isVideo && durCol != -1) {
                        val durationMs = cur.getLong(durCol)
                        if (durationMs > 0) {
                            val totalSecs = durationMs / 1000
                            val mins = totalSecs / 60
                            val secs = totalSecs % 60
                            durationStr = String.format("%d:%02d", mins, secs)
                        }
                    }

                    resultList.add(
                        MediaItem(
                            id = id.toString(),
                            uri = contentUri.toString(),
                            name = name,
                            isVideo = isVideo,
                            duration = durationStr,
                            dateAdded = dateAdded,
                            folderName = bucketName,
                            size = size
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        resultList
    }
}
