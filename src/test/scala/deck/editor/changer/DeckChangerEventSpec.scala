package deck.editor.changer

import java.util.UUID

import common.CouldNotFindEntityWithId
import org.scalatest.WordSpec

class DeckChangerEventSpec extends WordSpec {

    "create Event" when {
        val createdDecks = List(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        )

        "deck exists" should {
            "create Event" in {
                val result = deck.editor.changer.Event(createdDecks.head, "New title", deckExists = true)
                result match {
                    case Left(_) => assert(false)
                    case Right(_) => assert(true)
                }
            }
        }

        "deck does not exist" should {
            "give Error" in {
                val id = UUID.randomUUID()
                val result = Event(id, "New title", deckExists = false)
                assert(result == Left(CouldNotFindEntityWithId("Deck", id.toString)))
            }
        }
    }
}
