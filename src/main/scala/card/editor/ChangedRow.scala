package card.editor

import java.sql.Timestamp

/**
  * Created by bjornkri on 13.03.2017.
  */
case class ChangedRow(id: Long, t: Timestamp,
                      cardId: String, deckId: String,
                      frontTerm: String, frontDescription: Option[String],
                      backTerm: String, backDescription: Option[String])
