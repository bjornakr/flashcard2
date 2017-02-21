package deck.editor

import java.time.ZonedDateTime
import java.util.UUID

import common.CannotBeEmpty
import org.scalatest.WordSpec

class DeckEditorEventSpec extends WordSpec {
    "Event" when {
        val id = UUID.randomUUID()
        "empty title" should {
            "give error" in {
                val event = Event(id, "   ")
                assert(event == Left(CannotBeEmpty("title")))
            }
        }

        "valid input" should {
            "create Event" in {
                val title = "Valid title"
                Event(id, title) match {
                    case Left(_) => assert(false)
                    case Right(e) =>
                        assert(e.deckId == id)
                        assert(e.title == title)
                        assert(Math.abs(e.t.toEpochSecond - ZonedDateTime.now.toEpochSecond) < 10)
                }
            }
        }
    }
}
