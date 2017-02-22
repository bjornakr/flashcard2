package deck.viewer

import java.time.ZonedDateTime
import java.util.UUID

import org.scalatest.WordSpec

class DeckSpec extends WordSpec {
    private val t = ZonedDateTime.now
    private val d1id = UUID.randomUUID()
    private val d1change = new deck.editor.Event(t, d1id, "Deck 1") {}
    private val d1remove = new deck.remover.Event(d1id) {}
    private val d1result = new deck.viewer.Result(d1id.toString, "Deck 1", 0)
    private val d2id = UUID.randomUUID()
    private val d2change = new deck.editor.Event(t, d2id, "Deck 2") {}
    private val d2remove = new deck.remover.Event(d2id) {}
    private val d2result = new deck.viewer.Result(d2id.toString, "Deck 2", 0)

    "Decks" when {
        "changes: [], removes: []" should {
            "give []" in {
                val decks = Deck(List.empty, List.empty)
                assert(decks == Seq.empty)
            }
        }

        "changes: [d1], removes: []" should {
            "give [d1]" in {
                val decks = Deck(List(d1change), List.empty)
                assert(decks == Seq(d1result))
            }
        }

        "changes: [d1, d2], removes: []" should {
            "give [d1, d2]" in {
                val decks = Deck(List(d1change, d2change), List.empty)
                assert(decks == Seq(d1result, d2result))
            }
        }

        "changes: [d1], removes: [d1]" should {
            "give []" in {
                val decks = Deck(List(d1change), List(d1remove))
                assert(decks == Seq.empty)
            }
        }

        "changes: [d1, d2], removes: [d1, d2]" should {
            "give []" in {
                val decks = Deck(List(d1change, d2change), List(d1remove, d2remove))
                assert(decks == Seq.empty)
            }
        }

        "changes: [d1, d2], removes: [d1]" should {
            "give [d2]" in {
                val decks = Deck(List(d1change, d2change), List(d1remove))
                assert(decks == Seq(d1result))
            }
        }
    }
}
