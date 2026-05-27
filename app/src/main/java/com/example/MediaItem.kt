package com.example

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MediaItem(
    val id: String,
    val uri: String,
    val name: String,
    val isVideo: Boolean,
    val duration: String? = null,
    val dateAdded: Long, // timestamp
    val folderName: String,
    val size: Long = 0L
) {
    val monthYearString: String
        get() {
            val date = Date(dateAdded)
            val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            return formatter.format(date)
        }
}

data class Album(
    val name: String,
    val coverUri: String,
    val isVideoCover: Boolean,
    val itemsCount: Int,
    val items: List<MediaItem>
)

object SampleMedia {
    fun getSamples(): List<MediaItem> {
        // Base timestamp is current time, backdating items to simulate chunks of months
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        val oneMonth = 30 * oneDay

        return listOf(
            // --- Nature & Landscapes ---
            MediaItem(
                id = "s_nat_1",
                uri = "https://images.unsplash.com/photo-1501854140801-50d01698950b?w=1200&q=85",
                name = "Golden Mountain Peak.jpg",
                isVideo = false,
                dateAdded = now - (2 * oneDay),
                folderName = "Nature"
            ),
            MediaItem(
                id = "s_nat_2",
                uri = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=1200&q=85",
                name = "Mist Over Forest Canopy.jpg",
                isVideo = false,
                dateAdded = now - (5 * oneDay),
                folderName = "Nature"
            ),
            MediaItem(
                id = "s_nat_3",
                uri = "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=1200&q=85",
                name = "Wooden Bridge in Woodland.jpg",
                isVideo = false,
                dateAdded = now - (15 * oneDay),
                folderName = "Nature"
            ),

            // --- Videos ---
            MediaItem(
                id = "s_vid_1",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                name = "Blazing Flames Cinematic.mp4",
                isVideo = true,
                duration = "0:15",
                dateAdded = now - (12 * oneDay),
                folderName = "Videos"
            ),
            MediaItem(
                id = "s_vid_2",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                name = "Big Buck Bunny Classic.mp4",
                isVideo = true,
                duration = "9:56",
                dateAdded = now - (60 * oneDay),
                folderName = "Videos"
            ),
            MediaItem(
                id = "s_vid_3",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                name = "Sintel Animated Film.mp4",
                isVideo = true,
                duration = "0:52",
                dateAdded = now - (120 * oneDay),
                folderName = "Videos"
            ),

            // --- Urban Dark & Cyberpunk ---
            MediaItem(
                id = "s_urb_1",
                uri = "https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=1200&q=85",
                name = "Tokyo Midnight Neon.jpg",
                isVideo = false,
                dateAdded = now - (35 * oneDay),
                folderName = "Urban Noir"
            ),
            MediaItem(
                id = "s_urb_2",
                uri = "https://images.unsplash.com/photo-1496568818309-53d7c7753022?w=1200&q=85",
                name = "Cyber City Rain.jpg",
                isVideo = false,
                dateAdded = now - (42 * oneDay),
                folderName = "Urban Noir"
            ),
            MediaItem(
                id = "s_urb_3",
                uri = "https://images.unsplash.com/photo-1514565131-fce0801e5785?w=1200&q=85",
                name = "Skyscraper Light Lines.jpg",
                isVideo = false,
                dateAdded = now - (48 * oneDay),
                folderName = "Urban Noir"
            ),

            // --- Abstract & Space ---
            MediaItem(
                id = "s_spc_1",
                uri = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1200&q=85",
                name = "Global Network Core.jpg",
                isVideo = false,
                dateAdded = now - (70 * oneDay),
                folderName = "Abstract"
            ),
            MediaItem(
                id = "s_spc_2",
                uri = "https://images.unsplash.com/photo-1464802686167-b939a6910659?w=1200&q=85",
                name = "Andromeda Galaxy Hub.jpg",
                isVideo = false,
                dateAdded = now - (75 * oneDay),
                folderName = "Abstract"
            ),
            MediaItem(
                id = "s_spc_3",
                uri = "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?w=1200&q=85",
                name = "Cosmic Dust Pillars.jpg",
                isVideo = false,
                dateAdded = now - (85 * oneDay),
                folderName = "Abstract"
            ),

            // --- Architecture ---
            MediaItem(
                id = "s_arc_1",
                uri = "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=1200&q=85",
                name = "Concrete Minimalist Room.jpg",
                isVideo = false,
                dateAdded = now - (100 * oneDay),
                folderName = "Architecture"
            ),
            MediaItem(
                id = "s_arc_2",
                uri = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1200&q=85",
                name = "Symmetrical Facade.jpg",
                isVideo = false,
                dateAdded = now - (105 * oneDay),
                folderName = "Architecture"
            )
        )
    }
}
