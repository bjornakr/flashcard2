package deck.remover

import java.util.UUID

import common.CouldNotFindEntityWithId
import org.scalatest.WordSpec

class DeckRemoverEventSpec extends WordSpec {

    "create [Deck][Remove]Event" when {
        val createdDecks = List(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        )
        "deck exists" should {
            "create Event" in {
                val result = Event(createdDecks.head, deckExists = true)
                assert(result == Right(new Event(createdDecks.head) {}))
            }
        }

        "deck has not been created" should {
            "give Error" in {
                val id = UUID.randomUUID()
                val result = Event(id, deckExists = false)
                assert(result == Left(CouldNotFindEntityWithId("Deck", id.toString)))
            }
        }
    }
}
