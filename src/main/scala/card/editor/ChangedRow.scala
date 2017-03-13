package card.editor

import java.sql.Timestamp

case class ChangedRow(id: Long, t: Timestamp,
                      cardId: String, deckId: String,
                      frontTerm: String, frontDescription: Option[String],
                      backTerm: String, backDescription: Option[String])
