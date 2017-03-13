package card.editor

import java.sql.Timestamp

import slick.driver.H2Driver.api._
import slick.lifted.{ProvenShape, Tag}

class ChangedTable(tag: Tag) extends Table[ChangedRow](tag, "card_changed_events") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def t = column[Timestamp]("t")

    def cardId = column[String]("card_id")

    def deckId = column[String]("deck_id")

    def frontTerm = column[String]("front_term")

    def frontDescription = column[Option[String]]("front_description")

    def backTerm = column[String]("back_term")

    def backDescription = column[Option[String]]("back_description")

    def * : ProvenShape[ChangedRow] = (id, t, cardId, deckId, frontTerm, frontDescription, backTerm, backDescription) <> (ChangedRow.tupled, ChangedRow.unapply)
}
