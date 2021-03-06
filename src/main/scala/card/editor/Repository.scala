package card.editor

import java.util.UUID

import card.CardExistsQuery
import deck.DeckExistsQuery
import slick.lifted.TableQuery
import slick.driver.H2Driver.api._

import scala.concurrent.Future

class Repository(db: Database) extends DeckExistsQuery with CardExistsQuery {

    private val changedTable = TableQuery[ChangedTable]
    private val insertQuery = changedTable returning changedTable.map(_.id) into ((dto, id) => ChangedRowToDomain(dto.copy(id = id)))

    def save(event: Event): Future[Event] = {
        val action = insertQuery += DomainToChangedRow(event)
        db.run(action)
    }

    def deckExists(deckId: String): Future[Boolean] = deckExists(db, deckId)
    def cardExists(cardId: UUID): Future[Boolean] = cardExists(db, cardId)
}
