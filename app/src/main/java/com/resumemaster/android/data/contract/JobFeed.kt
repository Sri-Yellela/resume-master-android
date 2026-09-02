package com.resumemaster.android.data.contract

import org.json.JSONObject

/** Which paging mode answered a request. The server says so rather than leaving it to be inferred. */
enum class PagingMode(val wire: String) {
  CURSOR("cursor"),
  OFFSET("offset");

  companion object {
    fun from(wire: String?): PagingMode = if (wire == CURSOR.wire) CURSOR else OFFSET
  }
}

/**
 * One page of the board.
 *
 * ── WHY `page` AND `totalPages` ARE NULLABLE HERE ───────────────────────────────────────────────
 *
 * They describe OFFSET paging and are meaningless while paging by cursor: there is no page number
 * to be on. The server emits them regardless, and the contract is blunt about the consequence —
 * rendering "page 1 of 34" on every cursor page is the quietly-wrong surface `paging` exists to
 * prevent. They are therefore dropped to null in CURSOR mode HERE, at the boundary, so no UI can
 * read a number that looks valid and is not.
 */
data class JobFeedPage(
  val jobs: List<ContractJob>,
  val total: Int,
  val nextCursor: String?,
  val paging: PagingMode,
  val fromCache: Boolean,
  val reason: String?,
  val page: Int?,
  val totalPages: Int?,
  /** Rows the server sent that carried no `id` and were therefore unusable. Normally 0. */
  val droppedRows: Int,
) {
  /** null nextCursor means LAST PAGE — a fact the server establishes by over-fetching one row. */
  val isLastPage: Boolean get() = nextCursor == null

  companion object {
    fun fromJson(o: JSONObject): JobFeedPage {
      val arr = o.optJSONArray("jobs")
      val decoded = ArrayList<ContractJob>()
      var dropped = 0
      if (arr != null) {
        for (i in 0 until arr.length()) {
          val row = arr.optJSONObject(i)
          if (row == null) { dropped++; continue }
          val job = ContractJob.fromJson(row)
          if (job == null) dropped++ else decoded.add(job)
        }
      }

      val paging = PagingMode.from(if (o.isNull("paging")) null else o.optString("paging", null))

      return JobFeedPage(
        jobs = decoded,
        total = o.optInt("total", 0),
        // optJSONArray/optString style throughout: `nextCursor` is ALWAYS emitted, but a client
        // that assumes presence breaks the moment an error body is parsed by this same function.
        nextCursor = if (!o.has("nextCursor") || o.isNull("nextCursor")) null
                     else o.optString("nextCursor", "").ifEmpty { null },
        paging = paging,
        fromCache = o.optBoolean("fromCache", false),
        reason = if (!o.has("reason") || o.isNull("reason")) null
                 else o.optString("reason", "").ifEmpty { null },
        page = if (paging == PagingMode.CURSOR) null else o.optInt("page", 1),
        totalPages = if (paging == PagingMode.CURSOR) null else o.optInt("totalPages", 0),
        droppedRows = dropped,
      )
    }
  }
}

/**
 * Why a feed request failed in a way the caller must act on differently.
 *
 * CURSOR_SORT_MISMATCH is not an error to retry — it means the cursor was issued for a different
 * ordering or profile, so the ONLY correct response is to restart the feed from the first page.
 * Retrying the same cursor would fail identically, forever.
 */
enum class FeedError {
  UNAUTHORIZED,
  CURSOR_SORT_MISMATCH,
  CURSOR_MALFORMED,
  BAD_FILTER,
  SERVER,
  NETWORK;

  val restartsFeed: Boolean get() = this == CURSOR_SORT_MISMATCH || this == CURSOR_MALFORMED
}

class FeedException(val kind: FeedError, message: String) : Exception(message)
