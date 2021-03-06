package card.editor

import java.sql.Timestamp

object DomainToChangedRow {
    def apply(e: Event): ChangedRow =
        ChangedRow(
            id = 0,
            t = new Timestamp(e.t.toEpochSecond),
            cardId = e.cardId.toString,
            deckId = e.deckId.toString,
            frontTerm = e.front.term,
            frontDescription = e.front.description,
            backTerm = e.back.term,
            backDescription = e.back.description
        )
}
