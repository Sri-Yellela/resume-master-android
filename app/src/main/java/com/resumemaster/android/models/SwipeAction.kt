package com.resumemaster.android.models

sealed class SwipeAction { data object Queue: SwipeAction(); data object QueuePriority: SwipeAction(); data object Star: SwipeAction(); data object Dislike: SwipeAction(); data object Skip: SwipeAction() }
fun SwipeAction.label(): String = when(this){ SwipeAction.Queue -> "Queue"; SwipeAction.QueuePriority -> "Queue first"; SwipeAction.Star -> "Star"; SwipeAction.Dislike -> "Dislike"; SwipeAction.Skip -> "Skip" }

