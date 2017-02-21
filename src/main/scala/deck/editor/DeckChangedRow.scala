package deck.editor

import java.sql.Timestamp

case class DeckChangedRow(id: Long, t: Timestamp, deckId: String, title: String)
