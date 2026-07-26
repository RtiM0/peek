package com.mustafashakir.peek.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.mustafashakir.peek.ui.model.UiImage

@Composable
fun PeekImage(
    image: UiImage,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val model = when (image) {
        is UiImage.Resource -> image.id
        is UiImage.Url -> image.value
    }
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}
