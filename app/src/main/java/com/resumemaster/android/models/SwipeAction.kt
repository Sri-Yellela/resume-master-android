package com.resumemaster.android.models

// Queue is DRAFT (a server-side run held for review), Star is SAVE (a button only), Dislike is PASS. QueuePriority
// is gone with the deck (09-30): it had no server meaning, and a hard right was the most accident-prone motion.
sealed class SwipeAction { data object Queue: SwipeAction(); data object Star: SwipeAction(); data object Dislike: SwipeAction() }
fun SwipeAction.label(): String = when(this){ SwipeAction.Queue -> "Draft"; SwipeAction.Star -> "Save"; SwipeAction.Dislike -> "Pass" }

