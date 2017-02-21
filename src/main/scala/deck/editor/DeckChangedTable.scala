package deck.editor

import java.sql.Timestamp

import slick.driver.H2Driver.api._
import slick.lifted.{ProvenShape, Tag}

class DeckChangedTable(tag: Tag) extends Table[DeckChangedRow](tag, "deck_changed_events") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def t = column[Timestamp]("t")

    def deckId = column[String]("deck_id")

    def title = column[String]("title")

    def * : ProvenShape[DeckChangedRow] = (id, t, deckId, title) <> (DeckChangedRow.tupled, DeckChangedRow.unapply)
}
