package com.voicetasker.app.ui.resources

import com.voicetasker.app.R
import com.voicetasker.app.domain.model.Category
import com.voicetasker.app.domain.model.DefaultCategoryKey

fun Category.displayName(): UiText = when (canonicalKey) {
    DefaultCategoryKey.WORK ->
        if (name == "Lavoro") UiText.Resource(R.string.category_default_work)
        else UiText.Dynamic(name)
    DefaultCategoryKey.FAMILY ->
        if (name == "Famiglia" || name == "Personale") {
            UiText.Resource(R.string.category_default_family)
        } else {
            UiText.Dynamic(name)
        }
    DefaultCategoryKey.HEALTH ->
        if (name == "Salute") UiText.Resource(R.string.category_default_health)
        else UiText.Dynamic(name)
    null -> UiText.Dynamic(name)
}

fun persistedCategoryName(
    originalPersistedName: String,
    editedName: String,
    hasNameChanged: Boolean
): String = if (hasNameChanged) editedName else originalPersistedName
