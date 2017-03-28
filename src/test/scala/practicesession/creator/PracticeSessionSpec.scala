package practicesession.creator

import java.time.ZonedDateTime
import java.util.UUID

import org.scalatest.WordSpec
import practicesession.creator.Domain.{CardContent, PracticeSession, ScoreEvent}

class PracticeSessionSpec extends WordSpec {
    val ids = List(
        "00000000-0000-0000-0000-000000000001",
        "00000000-0000-0000-0000-000000000002",
        "00000000-0000-0000-0000-000000000003",
        "00000000-0000-0000-0000-000000000004",
        "00000000-0000-0000-0000-000000000005"
    ).map(UUID.fromString)

    val cards = List(
        new CardContent(ids(0), "Card 1 Front", None, "Card 1 Back", None) {},
        new CardContent(ids(1), "Card 2 Front", None, "Card 2 Back", None) {},
        new CardContent(ids(2), "Card 3 Front", None, "Card 3 Back", None) {},
        new CardContent(ids(3), "Card 4 Front", None, "Card 4 Back", None) {}
    )

    val card1Scores = List(
        new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {}
    )


    "Create PracticeSession" when {

        "4 Cards and ScoreEvents, noOfCards = 2" should {
            "give cards with highest score" in {
                val scoreEvents = List(
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now.minusDays(2), ids(1), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now.minusDays(1), ids(2), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now.minusDays(3), ids(3), card.scorer.Win) {}
                )
                val result = PracticeSession(cards, scoreEvents, noOfCards = 2)
                assert(result.cards(0).cardId == ids(3))
                assert(result.cards(1).cardId == ids(1))

            }
        }

        "no ScoreEvents" when {
            "cardContents = [], noOfCards = 3" should {
                "be []" in {
                    val result = PracticeSession(cardContents = List.empty, scoreEvents = List.empty, noOfCards = 3)
                    assert(result == new PracticeSession(List.empty) {})
                }
            }

            "cardContents = [card1, card2, card3, card4]" when {
                "noOfCards = 0" should {
                    "be []" in {
                        val result: PracticeSession = PracticeSession(cards, List.empty, noOfCards = 0)
                        assert(result.cards == List.empty)
                    }
                }

                "noOfCards = 1" should {
                    "return 1 card" in {
                        val result: PracticeSession = PracticeSession(cards, List.empty, noOfCards = 1)
                        assert(result.cards.length == 1)
                    }
                }

                "noOfCards = 3" should {
                    "return 3 cards" in {
                        val result: PracticeSession = PracticeSession(cards, List.empty, noOfCards = 3)
                        assert(result.cards.length == 3)
                    }
                }

                "noOfCards = 999" should {
                    "return 4 cards" in {
                        val result: PracticeSession = PracticeSession(cards, List.empty, noOfCards = 999)
                        assert(result.cards.length == 4)
                    }
                }
            }
        }

        "cardContents = [card1], noOfCards = 1" when {
            "scoreEvents = []" should {
                "have score = 0" in {
                    val result: PracticeSession = PracticeSession(cards.take(1), List.empty, noOfCards = 1)
                    assert(result.cards.head.statistics.score == 0)
                }
            }

            "scoreEvents = [(today, win)]" should {
                val card1Scores = List(
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {}
                )

                "have score = 1" in {
                    val result: PracticeSession = PracticeSession(cards.take(1), card1Scores, noOfCards = 1)
                    assert(result.cards.head.statistics.score == 1)
                }
            }

            "scoreEvents = [(today, win) * 2]" should {
                val card1Scores = List(
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {}
                )

                "have score = 1" in {
                    val result: PracticeSession = PracticeSession(cards.take(1), card1Scores, noOfCards = 1)
                    assert(result.cards.head.statistics.score == 1)
                }
            }

            "scoreEvents = [(today, win) * 3]" should {
                val card1Scores = List(
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {}
                )

                "have score = 1" in {
                    val result: PracticeSession = PracticeSession(cards.take(1), card1Scores, noOfCards = 1)
                    assert(result.cards.head.statistics.score == 1)
                }
            }

            "scoreEvents = [(today, win) * 4]" should {
                val card1Scores = List(
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {}
                )

                "have score = 0" in {
                    val result: PracticeSession = PracticeSession(cards.take(1), card1Scores, noOfCards = 1)
                    assert(result.cards.head.statistics.score == 0)
                }
            }

            "scoreEvents = [(today, win) * 5]" should {
                val card1Scores = List(
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {}
                )

                "have score = -3" in {
                    val result: PracticeSession = PracticeSession(cards.take(1), card1Scores, noOfCards = 1)
                    assert(result.cards.head.statistics.score == -3)
                }
            }

            "scoreEvents = [(today, win) * 5, (today, lose)]" should {
                val card1Scores = List(
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Win) {},
                    new ScoreEvent(ZonedDateTime.now, ids(0), card.scorer.Lose) {}
                )

                "have score = 1" in {
                    val result: PracticeSession = PracticeSession(cards.take(1), card1Scores, noOfCards = 1)
                    assert(result.cards.head.statistics.score == 1)
                }
            }

            "scoreEvents = [(yesterday, win)]" should {
                val card1Scores = List(
                    new ScoreEvent(ZonedDateTime.now.minusDays(1), ids(0), card.scorer.Win) {}
                )

                "have score = 2" in {
                    val result: PracticeSession = PracticeSession(cards.take(1), card1Scores, noOfCards = 1)
                    assert(result.cards.head.statistics.score == 2)
                }
            }
        }
    }
}
