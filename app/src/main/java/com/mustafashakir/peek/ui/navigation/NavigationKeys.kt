package com.mustafashakir.peek.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeKey : NavKey

@Serializable
data class ViewerKey(val url: String) : NavKey

@Serializable
data class PlayerKey(val url: String, val mediaIndex: Int) : NavKey
