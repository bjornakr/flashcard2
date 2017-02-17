package deck.editor.creator

import java.time.ZonedDateTime

import common.CannotBeEmpty
import org.scalatest.WordSpec

class EventSpec extends WordSpec {
    "Event" when {
        "empty title" should {
            "give error" in {
                val event = ChangedEvent("   ")
                assert(event == Left(CannotBeEmpty("title")))
            }
        }

        "valid input" should {
            "create Event" in {
                val title = "Valid title"
                ChangedEvent(title) match {
                    case Left(_) => assert(false)
                    case Right(e) =>
                        assert(e.title == title)
                        assert(Math.abs(e.t.toEpochSecond - ZonedDateTime.now.toEpochSecond) < 10)
                }
            }
        }
    }
}
