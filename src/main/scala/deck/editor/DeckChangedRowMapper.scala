package deck.editor

import java.sql.Timestamp
import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID

object DeckChangedRowMapper {
    def fromDomain(d: Event): DeckChangedRow =
        DeckChangedRow(0, new Timestamp(d.t.toEpochSecond), d.deckId.toString, d.title)

    def toDomain(d: DeckChangedRow): Event = {
        val t = ZonedDateTime.ofInstant(d.t.toInstant, ZoneId.of("UTC"))
        val deckId = UUID.fromString(d.deckId)
        new Event(t, deckId, d.title) {}
    }
}
