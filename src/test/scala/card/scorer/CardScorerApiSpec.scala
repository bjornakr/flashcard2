package card.scorer

import java.util.UUID

import common.{ApiBaseSpec, CouldNotFindEntityWithId, InvalidUuidFormat}
import deck.editor.DeckChangedRow
import io.circe.Decoder._
import io.circe.generic.auto._
import io.circe.parser.decode
import org.http4s.{Method, Status}
import org.scalatest.Matchers
import slick.driver.H2Driver.api._

class CardScorerApiSpec extends ApiBaseSpec  with Matchers {
    private val testDeckId = UUID.randomUUID().toString
    private val testCardId = UUID.randomUUID().toString
    private val troublesomeCardId = UUID.randomUUID().toString
    private val deletedCardId = UUID.randomUUID().toString

    override protected def fillDatabase(): Unit = {
        val action = slick.dbio.DBIO.seq(

            deckChangedTable += DeckChangedRow(0, createTimestamp(), testDeckId, "Test Deck"),

            cardChangedTable += card.editor.ChangedRow(0, createTimestamp(), testCardId, testDeckId,
                "Test Card Front", None, "Test Card Back", Some("Back Description")),
            cardChangedTable += card.editor.ChangedRow(0, createTimestamp(), deletedCardId, testDeckId,
                "Deleted Card Front", None, "Deleted Card Back", None),
            cardChangedTable += card.editor.ChangedRow(0, createTimestamp(), troublesomeCardId, testDeckId,
                "Troublesome Card Front", None, "Troublesome Card Back", None),

            cardDeletedTable += card.remover.TableRow(0, createTimestamp(), deletedCardId),


            cardScoredTable += card.scorer.TableRow(0, createTimestamp(), troublesomeCardId, card.scorer.Lose.toString),
            cardScoredTable += card.scorer.TableRow(0, createTimestamp(), troublesomeCardId, card.scorer.Lose.toString),
            cardScoredTable += card.scorer.TableRow(0, createTimestamp(), troublesomeCardId, card.scorer.Win.toString),
            cardScoredTable += card.scorer.TableRow(0, createTimestamp(), troublesomeCardId, card.scorer.Lose.toString)

        )
        db.run(action)
    }

    private def scorerUri(cardId: String) = baseUri / "card" / cardId / "scorer"

    private def winUri(cardId: String) = scorerUri(cardId) / "win"

    private def loseUri(cardId: String) = scorerUri(cardId) / "lose"

    s"POST $baseUri/card/:id/scorer/win" when {
        "valid request" should {
            "give 201 Created w/ { cardId, wins, losses }" in {
                val response = executeRequest(Method.POST, winUri(testCardId))
                response.status shouldBe Status.Created

                val bodyJson = extractBody(response)
                val body = decode[ResponseDto](bodyJson).valueOr(e => throw e)
                val expectedResponse = ResponseDto(testCardId, 1, 0)
                body shouldBe expectedResponse
            }

            "show correct win/lose statistics" in {
                val response = executeRequest(Method.POST, winUri(troublesomeCardId))
                response.status shouldBe Status.Created

                val bodyJson = extractBody(response)
                val body = decode[ResponseDto](bodyJson).valueOr(e => throw e)
                val expectedResponse = ResponseDto(troublesomeCardId, 2, 3)
                body shouldBe expectedResponse
            }
        }


//        "database error" should {
//            "give 500 Internal Server Error" in {
//                clearDatabase()
//                val response = executeRequest(Method.POST, winUri(testCardId))
//                assert(response.status == Status.InternalServerError)
//            }
//
//            "log errors" ignore {
//
//            }
//        }

        "invalid Card UUID format" should {
            "give 400 Bad Request w/ error message" in {
                val response = executeRequest(Method.POST, winUri("!@#$%"))
                assert(response.status == Status.BadRequest)

                val body = extractBody(response)
                body shouldBe InvalidUuidFormat("!@#$%").message
            }
        }

        "no Card with id" should {
            "give 404 Not Found w/ error message" in {
                val nonExistentId = UUID.randomUUID()
                val response = executeRequest(Method.POST, winUri(nonExistentId.toString))
                assert(response.status == Status.NotFound)

                val body = extractBody(response)
                assert(body == CouldNotFindEntityWithId("Card", nonExistentId.toString).message)
            }
        }

        "Card has been deleted" should {
            "give 404 Not Found w/ error message" in {
                val response = executeRequest(Method.POST, winUri(deletedCardId.toString))
                assert(response.status == Status.NotFound)

                val body = extractBody(response)
                assert(body == CouldNotFindEntityWithId("Card", deletedCardId.toString).message)
            }
        }
    }

    s"POST $baseUri/card/:id/scorer/lose" when {
        "valid request" should {
            "give 201 Created w/ { cardId, wins, losses }" in {
                val response = executeRequest(Method.POST, loseUri(testCardId))
                response.status shouldBe Status.Created

                val bodyJson = extractBody(response)
                val body = decode[ResponseDto](bodyJson).valueOr(e => throw e)
                val expectedResponse = ResponseDto(testCardId, 0, 1)
                body shouldBe expectedResponse
            }

            "show correct win/lose statistics" in {
                val response = executeRequest(Method.POST, loseUri(troublesomeCardId))
                response.status shouldBe Status.Created

                val bodyJson = extractBody(response)
                val body = decode[ResponseDto](bodyJson).valueOr(e => throw e)
                val expectedResponse = ResponseDto(troublesomeCardId, 1, 4)
                body shouldBe expectedResponse
            }
        }
    }
}