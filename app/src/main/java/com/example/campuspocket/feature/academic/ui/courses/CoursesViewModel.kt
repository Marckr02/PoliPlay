package com.example.campuspocket.feature.academic.ui.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campuspocket.feature.academic.domain.model.Course
import com.example.campuspocket.feature.academic.domain.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoursesUiState(
    val courses: List<Course> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CoursesViewModel @Inject constructor(
    private val courseRepository: CourseRepository
) : ViewModel() {

    val uiState: StateFlow<CoursesUiState> = courseRepository.observeCourses()
        .map { courses -> CoursesUiState(courses = courses, isLoading = false) }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            CoursesUiState()
        )

    fun deleteCourse(courseId: Long) {
        viewModelScope.launch {
            courseRepository.deleteCourse(courseId)
        }
    }
}
