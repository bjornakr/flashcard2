package card.editor

import deck.DeckExistsQuery
import slick.lifted.TableQuery
import slick.driver.H2Driver.api._

import scala.concurrent.Future

class Repository(db: Database) extends DeckExistsQuery {

    private val changedTable = TableQuery[ChangedTable]
    private val insertQuery = changedTable returning changedTable.map(_.id) into ((dto, id) => ChangedRowToDomain(dto.copy(id = id)))

    def save(event: Event): Future[Event] = {
        val action = insertQuery += DomainToChangedRow(event)
        db.run(action)
    }

    def deckExists(deckId: String): Future[Boolean] = deckExists(db, deckId)

    def cardExists(cardId: String): Future[Boolean] = {
        val created = TableQuery[ChangedTable]
            .map(a => a.cardId)
            .distinct

        // Insert when possible
//        val deleted = TableQuery[DeletedTable]
//            .map(_.cardId)
//            .distinct

        val query = created  // .filterNot(_.in(deleted))
            .filter(_ === cardId)
            .exists

        db.run(query.result)
    }
}
