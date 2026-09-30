package com.resumemaster.android.util

import com.resumemaster.android.models.SwipeAction
import kotlin.math.abs

object SwipeThresholds {
    const val SOFT_OFFSET=80f; const val HARD_OFFSET=160f; const val ROTATION_FACTOR=0.04f; const val STACK_SCALE=0.04f; const val STACK_OFFSET=8f

    /** Upward travel (px) past which a right-swipe is a Star. Negative y is up. */
    const val DIAGONAL_OFFSET=60f

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
    fun resolve(x:Float,y:Float):SwipeAction?{ val ax=abs(x); if(ax<SOFT_OFFSET)return null; return if(x<0) SwipeAction.Dislike else if(y < -DIAGONAL_OFFSET) SwipeAction.Star else if(ax>HARD_OFFSET) SwipeAction.QueuePriority else SwipeAction.Queue }
}

