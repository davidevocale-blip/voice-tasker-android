package com.voicetasker.app.ui.resources

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

sealed interface UiText {
    data class Dynamic(val value: String) : UiText

    data class Resource(
        @StringRes val resId: Int,
        val args: List<String> = emptyList()
    ) : UiText
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> stringResource(resId, *args.toTypedArray())
}

interface StringResolver {
    fun resolve(@StringRes resId: Int): String
}

class AndroidStringResolver @Inject constructor(
    @ApplicationContext private val context: Context
) : StringResolver {
    override fun resolve(@StringRes resId: Int): String = context.getString(resId)
}
