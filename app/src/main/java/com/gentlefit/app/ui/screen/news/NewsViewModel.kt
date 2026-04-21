package com.gentlefit.app.ui.screen.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gentlefit.app.domain.model.NewsArticle
import com.gentlefit.app.domain.model.NewsCategory
import com.gentlefit.app.domain.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<NewsCategory?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    val news: StateFlow<List<NewsArticle>> = _selectedCategory.flatMapLatest { cat ->
        if (cat == null) newsRepository.getAllNews() else newsRepository.getNewsByCategory(cat)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedArticle = MutableStateFlow<NewsArticle?>(null)
    val selectedArticle = _selectedArticle.asStateFlow()

    fun selectCategory(category: NewsCategory?) { _selectedCategory.value = category }

    fun selectArticle(article: NewsArticle) {
        _selectedArticle.value = article
        viewModelScope.launch { newsRepository.markAsRead(article.id) }
    }

    fun clearArticle() { _selectedArticle.value = null }
}
