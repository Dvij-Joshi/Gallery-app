package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.text.format.Formatter
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import com.example.Album
import com.example.GalleryViewModel
import com.example.MediaItem
import com.example.ui.components.CustomVideoPlayer
import com.example.ui.components.PinchZoomImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// Flattened Grid Items for Chronological Camera Roll
sealed class GridItem {
    data class Header(val title: String) : GridItem()
    data class Media(val item: MediaItem) : GridItem()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryMainScreen(
    viewModel: GalleryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val columnsCount by viewModel.gridColumnsCount.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val showSamplesOnly by viewModel.showSamplesOnly.collectAsState()

    // Screen navigation layout
    var activeViewerMedia by remember { mutableStateOf<MediaItem?>(null) }
    var activeAlbumDetails by remember { mutableStateOf<Album?>(null) }

    // Double tab swipe setups using native HorizontalPager
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    // Permission launcher
    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        viewModel.setPermissionGranted(granted)
        vibrate(view)
    }

    // Dynamic chronological flattening calculation
    val cameraRollGridItems = remember(mediaItems) {
        val list = ArrayList<GridItem>()
        val grouped = mediaItems.groupBy { it.monthYearString }
        grouped.forEach { (monthAndYear, items) ->
            list.add(GridItem.Header(monthAndYear))
            items.forEach { list.add(GridItem.Media(it)) }
        }
        list
    }

    LaunchedEffect(pagerState.currentPage) {
        vibrate(view)
    }

    // Intercept physical back button to close overlays gracefully
    BackHandler(enabled = activeViewerMedia != null) {
        vibrate(view)
        activeViewerMedia = null
    }

    BackHandler(enabled = activeAlbumDetails != null && activeViewerMedia == null) {
        vibrate(view)
        activeAlbumDetails = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val showMainContent = hasPermission || showSamplesOnly

        AnimatedContent(
            targetState = showMainContent,
            transitionSpec = {
                (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        scaleIn(initialScale = 0.95f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                    .togetherWith(
                        fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                scaleOut(targetScale = 0.95f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    )
            },
            label = "app_entrance_transition",
            modifier = Modifier.fillMaxSize()
        ) { targetShown ->
            if (!targetShown) {
                // High-fidelity Minimalist Grant Permission Welcome view
                PermissionWelcomeView(
                    onGrantClick = {
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO
                            )
                        } else {
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        multiplePermissionsLauncher.launch(permissions)
                    },
                    onDemoClick = {
                        vibrate(view)
                        viewModel.showSamplesOnly.value = true
                    }
                )
            } else {
                // Main App View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // Main Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MEMORIES",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Demo mode chip Indicator
                            if (showSamplesOnly) {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            RoundedCornerShape(50)
                                        )
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            RoundedCornerShape(50)
                                        )
                                        .clickable {
                                            vibrate(view)
                                            viewModel.showSamplesOnly.value = false
                                            viewModel.checkPermissionState()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "DEMO MODE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Dynamic Grid Cycle Button
                            IconButton(
                                onClick = {
                                    vibrate(view)
                                    viewModel.cycleGridColumns()
                                },
                                modifier = Modifier.testTag("grid_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Cycle Column Grid",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }

                            // Beautiful Dark Mode Slider Toggle
                            IconButton(
                                onClick = {
                                    vibrate(view)
                                    viewModel.toggleDarkMode()
                                },
                                modifier = Modifier.testTag("dark_mode_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Themes",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    // Smooth Underlined M3 Tabs matching HorizontalPager offsets
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            if (tabPositions.isNotEmpty() && pagerState.currentPage < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 2.dp
                                )
                            }
                        },
                        divider = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            )
                        },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Tab(
                            selected = pagerState.currentPage == 0,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            },
                            text = {
                                Text(
                                    "CAMERA ROLL",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.5.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_camera_roll")
                        )
                        Tab(
                            selected = pagerState.currentPage == 1,
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            },
                            text = {
                                Text(
                                    "ALBUMS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.5.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_albums")
                        )
                    }

                    // Dual Swiping Page Frame
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { page ->
                        when (page) {
                            0 -> {
                                // Camera Roll chronologically grouped list
                                CameraRollView(
                                    gridItems = cameraRollGridItems,
                                    columns = columnsCount,
                                    onMediaClick = { activeViewerMedia = it }
                                )
                            }

                            1 -> {
                                // Folders & Albums grid view
                                AlbumsGridView(
                                    albums = albums,
                                    onAlbumClick = { activeAlbumDetails = it }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Slide-up detail screen for viewed folder folder ---
        AnimatedVisibility(
            visible = activeAlbumDetails != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        ) {
            activeAlbumDetails?.let { album ->
                AlbumDetailsOverlay(
                    album = album,
                    columns = columnsCount,
                    onBackClick = {
                        vibrate(view)
                        activeAlbumDetails = null
                    },
                    onMediaClick = { activeViewerMedia = it }
                )
            }
        }

        // --- FULL-SCREEN IMMERSIVE MEDIA VIEW BAR EXCLUSIVE PAGE ---
        AnimatedVisibility(
            visible = activeViewerMedia != null,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            activeViewerMedia?.let { clickedMedia ->
                // Collect flat sequence context to enable left/right paging in active viewer
                val flatMediaList = remember(activeAlbumDetails, mediaItems) {
                    activeAlbumDetails?.items ?: mediaItems
                }
                
                val favoriteIds by viewModel.favoriteIds.collectAsState()
                MediaImmersiveViewer(
                    mediaList = flatMediaList,
                    initialItem = clickedMedia,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                    onDeleteMedia = {
                        viewModel.deleteMedia(it.id)
                    },
                    onDismiss = {
                        vibrate(view)
                        activeViewerMedia = null
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionWelcomeView(
    onGrantClick: () -> Unit,
    onDemoClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Dark cinematic feel for welcome
            .padding(40.dp)
            .navigationBarsPadding()
            .statusBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Photos Folder Icon",
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "MINIMAL GALLERY",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = 6.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "An offline-first aesthetic framework featuring fluid 120 FPS swiping, haptic sliding fast-scrolling, and responsive physical pinch gestures.",
            fontSize = 13.sp,
            fontWeight = FontWeight.Light,
            lineHeight = 22.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onGrantClick,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("grant_permission_button")
        ) {
            Text(
                "GRANT PHOTO ACCESS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDemoClick,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.1f),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                .testTag("demo_samples_button")
        ) {
            Text(
                "EXPLORE DEMO PORTFOLIO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.5.sp
            )
        }
    }
}

// --- Screen Page 0: Camera Roll feed ---
@Composable
fun CameraRollView(
    gridItems: List<GridItem>,
    columns: Int,
    onMediaClick: (MediaItem) -> Unit
) {
    val lazyGridState = rememberLazyGridState()
    val scrollScope = rememberCoroutineScope()
    val view = LocalView.current

    // Keep track of active index to emit haptic feedback during scrolling jumps
    var lastVibratedSectionIndex by remember { mutableIntStateOf(-1) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = lazyGridState,
            contentPadding = PaddingValues(top = 16.dp, start = 12.dp, end = 20.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = gridItems,
                key = { idx, item ->
                    when (item) {
                        is GridItem.Header -> "header_${idx}_${item.title}"
                        is GridItem.Media -> "media_${item.item.id}"
                    }
                },
                span = { _, item ->
                    when (item) {
                        is GridItem.Header -> GridItemSpan(maxLineSpan)
                        is GridItem.Media -> GridItemSpan(1)
                    }
                }
            ) { _, item ->
                when (item) {
                    is GridItem.Header -> {
                        MonthHeader(title = item.title)
                    }

                    is GridItem.Media -> {
                        MediaGridCard(
                            media = item.item,
                            onClick = { onMediaClick(item.item) }
                        )
                    }
                }
            }
        }

        // FAST TACTILE SCROLL DRAG BAR INDEX
        if (gridItems.size > 8) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(32.dp)
                    .align(Alignment.CenterEnd)
            ) {
                val containerHeightPx = constraints.maxHeight.toFloat()
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(gridItems) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    vibrate(view)
                                    val index = calculateScrollIndex(offset.y, containerHeightPx, gridItems.size)
                                    if (index in gridItems.indices) {
                                        scrollScope.launch { lazyGridState.scrollToItem(index) }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    val index = calculateScrollIndex(change.position.y, containerHeightPx, gridItems.size)
                                    if (index in gridItems.indices && index != lastVibratedSectionIndex) {
                                        lastVibratedSectionIndex = index
                                        vibrate(view) // Emits mechanical haptic clicks during dragging!!
                                        scrollScope.launch { lazyGridState.scrollToItem(index) }
                                    }
                                }
                            )
                        }
                ) {
                    // Modern minimalist slim scroll line
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .fillMaxHeight(0.85f)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                            .align(Alignment.Center)
                    )

                    // Floating scroll tracker pill mapping to viewports
                    val listIndex by remember { derivedStateOf { lazyGridState.firstVisibleItemIndex } }
                    val fraction = if (gridItems.isNotEmpty()) {
                        listIndex.toFloat() / gridItems.size.toFloat()
                    } else 0f
                    
                    val offsetAnim by animateFloatAsState(
                        targetValue = (fraction * (containerHeightPx * 0.82f)),
                        animationSpec = spring(stiffness = Spring.StiffnessHigh),
                        label = "scrollbar_scroll"
                    )

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(0, offsetAnim.roundToInt()) }
                            .padding(top = 28.dp)
                            .size(10.dp, 36.dp)
                            .background(
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                RoundedCornerShape(50)
                            )
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

private fun calculateScrollIndex(y: Float, containerHeight: Float, totalCount: Int): Int {
    val usableHeight = containerHeight * 0.85f
    val relativeY = (y - (containerHeight * 0.075f)).coerceIn(0f, usableHeight)
    val fraction = relativeY / usableHeight
    return (fraction * (totalCount - 1)).roundToInt().coerceIn(0, totalCount - 1)
}

@Composable
fun MonthHeader(title: String) {
    Text(
        text = title.uppercase(Locale.getDefault()),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun MediaGridCard(
    media: MediaItem,
    onClick: () -> Unit
) {
    val view = LocalView.current
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "card_alpha"
    )
    val cardOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 28f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "card_offset_y"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "press_scale"
    )

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                alpha = cardAlpha
                translationY = cardOffsetY
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current
            ) {
                vibrate(view)
                onClick()
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = media.uri,
                contentDescription = media.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Video decoration overlays with dynamic transparent background
            if (media.isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Video marker",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = media.duration ?: "0:00",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


// --- Screen Page 1: Albums list folders ---
@Composable
fun AlbumsGridView(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    val view = LocalView.current
    if (albums.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "NO ALBUMS DETECTED",
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(albums) { _, album ->
                AlbumFolderCard(
                    album = album,
                    onClick = {
                        vibrate(view)
                        onAlbumClick(album)
                    }
                )
            }
        }
    }
}

@Composable
fun AlbumFolderCard(
    album: Album,
    onClick: () -> Unit
) {
    val view = LocalView.current
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    val cardAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "folder_alpha"
    )
    val cardOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 28f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "folder_offset_y"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "folder_press_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = cardAlpha
                translationY = cardOffsetY
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    vibrate(view)
                    onClick()
                }
            )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = album.coverUri,
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Elegant folder tag accent on top of cover images
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = album.itemsCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (album.isVideoCover) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Video folders cover",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = album.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "Media Collection",
            fontSize = 11.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}


// --- Overlay View: Open an Album detail grid ---
@Composable
fun AlbumDetailsOverlay(
    album: Album,
    columns: Int,
    onBackClick: () -> Unit,
    onMediaClick: (MediaItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back home",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = album.name.uppercase(Locale.getDefault()),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${album.itemsCount} elements in this folder",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            itemsIndexed(album.items) { _, mediaItem ->
                MediaGridCard(mediaItem, onClick = { onMediaClick(mediaItem) })
            }
        }
    }
}


// --- IMMERSIVE FULL-SCREEN VISUALIZER FRAME WITH PAGING SWIPES ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaImmersiveViewer(
    mediaList: List<MediaItem>,
    initialItem: MediaItem,
    favoriteIds: Set<String>,
    onToggleFavorite: (MediaItem) -> Unit,
    onDeleteMedia: (MediaItem) -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    // Find index of clicked item
    val startIndex = remember(initialItem, mediaList) {
        val index = mediaList.indexOfFirst { it.id == initialItem.id }
        if (index != -1) index else 0
    }

    val pageCount = mediaList.size
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { pageCount })
    var showOverlays by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf<MediaItem?>(null) }

    LaunchedEffect(pagerState.currentPage) {
        vibrate(view)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Horizontal Pager allows quick swiping between album files
        HorizontalPager(
            state = pagerState,
            key = { index -> mediaList.getOrNull(index)?.id ?: index.toString() },
            modifier = Modifier.fillMaxSize()
        ) { index ->
            val item = mediaList.getOrNull(index)
            if (item != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showOverlays = !showOverlays },
                    contentAlignment = Alignment.Center
                ) {
                    if (item.isVideo) {
                        CustomVideoPlayer(
                            videoUri = item.uri,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Image with custom spring physics and manual zoom gesture modifiers
                        PinchZoomImage(
                            imageUri = item.uri,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            onDismiss = onDismiss
                        )
                    }
                }
            }
        }

        // --- Custom Immersive overlays ---
        AnimatedVisibility(
            visible = showOverlays,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Exit Navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss viewer",
                            tint = Color.White
                        )
                    }

                    // Pager step marker indicator (e.g. "4 / 24")
                    val currentStep = pagerState.currentPage + 1
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$currentStep of $pageCount",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    val currentMedia = mediaList.getOrNull(pagerState.currentPage)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentMedia != null) {
                            val isFav = currentMedia.id in favoriteIds
                            val iconScale by animateFloatAsState(
                                targetValue = if (isFav) 1.2f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.5f),
                                label = "fav_scale"
                            )
                            IconButton(
                                onClick = {
                                    vibrate(view)
                                    onToggleFavorite(currentMedia)
                                },
                                modifier = Modifier
                                    .testTag("viewer_favorite_button")
                                    .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite image or video",
                                    tint = if (isFav) Color(0xFFE91E63) else Color.White
                                )
                            }

                            val context = LocalContext.current
                            IconButton(
                                onClick = {
                                    vibrate(view)
                                    shareMedia(context, currentMedia)
                                },
                                modifier = Modifier.testTag("viewer_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share image or video",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = {
                                    vibrate(view)
                                    showDeleteConfirm = currentMedia
                                },
                                modifier = Modifier.testTag("viewer_delete_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete image or video",
                                    tint = Color.White
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(36.dp))
                        }
                    }
                }

                // Bottom Metadata Info Overlay Card
                val currentMedia = mediaList.getOrNull(pagerState.currentPage)
                if (currentMedia != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 28.dp)
                    ) {
                        Text(
                            text = currentMedia.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val readableDate = remember(currentMedia.dateAdded) {
                            val formatter = SimpleDateFormat("dd MMMM yyyy, h:mm a", Locale.getDefault())
                            formatter.format(Date(currentMedia.dateAdded))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Metadata indicators",
                                tint = Color.LightGray.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = readableDate,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.LightGray
                            )
                            if (currentMedia.size > 0L) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "|  " + formatSize(currentMedia.size),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.LightGray.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Confirmation dialog overlay
        AnimatedVisibility(
            visible = showDeleteConfirm != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { showDeleteConfirm = null },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clickable(enabled = false) {}, // consume clicks
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "DELETE THIS FILE?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "It will be removed from your current viewing feed.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    vibrate(view)
                                    showDeleteConfirm = null
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("CANCEL", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            Button(
                                onClick = {
                                    vibrate(view)
                                    val itemToDelete = showDeleteConfirm
                                    showDeleteConfirm = null
                                    if (itemToDelete != null) {
                                        onDeleteMedia(itemToDelete)
                                        // Auto-dismiss or auto-slide pages
                                        if (pageCount <= 1) {
                                            onDismiss()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("confirm_delete_button")
                            ) {
                                Text("DELETE", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Intent sharing system
private fun shareMedia(context: android.content.Context, media: MediaItem) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        if (media.uri.startsWith("http")) {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, "Check out this beautiful media: ${media.uri}")
        } else {
            type = if (media.isVideo) "video/*" else "image/*"
            putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.parse(media.uri))
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share Media"))
}

// Emulate tactile vibration mechanical feel
private fun vibrate(view: View) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        view.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
    } else {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}

// Format local media sizes
private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "Unknown"
    val k = 1024L
    val m = k * k
    return when {
        bytes >= m -> String.format("%.1f MB", bytes.toFloat() / m)
        bytes >= k -> String.format("%.1f KB", bytes.toFloat() / k)
        else -> "$bytes Bytes"
    }
}
