package com.resumemaster.android.viewmodel

import androidx.lifecycle.ViewModel
import com.resumemaster.android.data.JobRepository
import com.resumemaster.android.models.Job
import com.resumemaster.android.models.SwipeAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class JobsViewModel:ViewModel(){ private val repository=JobRepository(); val jobs=repository.jobs; private val _currentIndex=MutableStateFlow(0); val currentIndex:StateFlow<Int> = _currentIndex; private val _lastAction=MutableStateFlow<SwipeAction?>(null); val lastAction:StateFlow<SwipeAction?> = _lastAction; val queue=mutableListOf<Job>(); val starred=mutableListOf<Job>(); fun onSwipe(action:SwipeAction,job:Job){ when(action){ SwipeAction.Queue -> if(queue.none{it.id==job.id}) queue.add(job); SwipeAction.QueuePriority -> if(queue.none{it.id==job.id}) queue.add(0,job); SwipeAction.Star -> if(starred.none{it.id==job.id}) starred.add(job); SwipeAction.Dislike, SwipeAction.Skip -> Unit }; _lastAction.value=action; repository.rotate(job); _currentIndex.value=0 }; fun onButtonAction(action:SwipeAction){ jobs.value.firstOrNull()?.let{onSwipe(action,it)} } }

