package com.resumemaster.android.util

import com.resumemaster.android.models.SwipeAction
import kotlin.math.abs

object SwipeThresholds {
    const val SOFT_OFFSET=80f; const val HARD_OFFSET=160f; const val ROTATION_FACTOR=0.04f; const val STACK_SCALE=0.04f; const val STACK_OFFSET=8f


    /**
     * The ONE mapping from drag displacement to action.
     *
     * CardStackLayout decides what a released drag DOES and SwipeOverlay decides what the card SAYS
     * while it is being dragged. They each carried their own copy of this decision, including a bare
     * `-60f` literal for the diagonal, so the label on the card and the action taken on release were
     * two implementations that happened to agree. Both read this now, so the label cannot promise one
     * action and the release perform another. SwipeResolveTest pins the mapping.
     *
     * Displacement only -- there is no velocity term (see README).
     */
    // Two directions, one meaning each, on every client (UI_REDESIGN §3.4): right DRAFTS, left PASSES.
    // The diagonal Star and the hard-right "Queue first" are gone (09-30) — two accident-prone motions
    // bound to two intents. `y` is accepted and ignored, so a diagonal drag is simply a right or a left.
    fun resolve(x:Float,@Suppress("UNUSED_PARAMETER") y:Float):SwipeAction?{ if(abs(x)<SOFT_OFFSET)return null; return if(x<0) SwipeAction.Dislike else SwipeAction.Queue }
}

