package card.editor

import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID

/**
  * Created by bjornkri on 13.03.2017.
  */
object ChangedRowToDomain {
    def apply(row: ChangedRow): Event = {
        val t = ZonedDateTime.ofInstant(row.t.toInstant, ZoneId.of("UTC"))
        val front = new CardSide(row.frontTerm, row.frontDescription) {}
        val back = new CardSide(row.backTerm, row.backDescription) {}
        new Event(t, UUID.fromString(row.cardId), UUID.fromString(row.deckId), front, back) {}
    }
}
