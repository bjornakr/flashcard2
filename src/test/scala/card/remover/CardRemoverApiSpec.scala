package card.remover

import java.util.UUID

import common.{ApiBaseSpec, CouldNotFindEntityWithId, InvalidUuidFormat}
import deck.editor.DeckChangedRow
import io.circe.Decoder._
import io.circe.generic.auto._
import io.circe.parser.decode
import org.http4s.{Method, Status}
import slick.driver.H2Driver.api._

class CardRemoverApiSpec extends ApiBaseSpec {
    private def uri(cardId: String) =
        baseUri / "card" / cardId / "remover"

    private val existingDeckId = UUID.randomUUID().toString
    private val existingCardId = UUID.randomUUID().toString
    private val deletedCardId = UUID.randomUUID().toString
    private val nonExistentId = "99999999-9999-9999-9999-999999999999"

    override def fillDatabase(): Unit = {
        val action = slick.dbio.DBIO.seq(
            deckChangedTable += DeckChangedRow(0, createTimestamp(), existingDeckId, "Test Deck"),
            cardChangedTable += card.editor.ChangedRow(0, createTimestamp(), existingCardId, existingDeckId,
                "Test Card Front", None, "Test Card Back", Some("Back Description")),
            cardChangedTable += card.editor.ChangedRow(0, createTimestamp(), deletedCardId, existingDeckId,
                "Test Card Front", None, "Test Card Back", Some("Back Description")),
            cardDeletedTable += card.remover.TableRow(0, createTimestamp(), deletedCardId)
        )
        db.run(action)
    }

    s"POST $baseUri/card/:cardId/remover" when {

        "database error" should {
            "give 500 Internal Server Error" in {
                clearDatabase()

                val response = executeRequest(Method.POST, uri(existingCardId))
                assert(response.status == Status.InternalServerError)
            }
        }

        "valid id" should {
            "give 201 Created w/ { cardId }" in {
                val response = executeRequest(Method.POST, uri(existingCardId))
                assert(response.status == Status.Created)

                val responseBody = extractBody(response)
                val result = decode[ResponseDto](responseBody).valueOr(e => throw e)
                assert(result.cardId == existingCardId)
            }
        }

        "invalid Card UUID format" should {
            "give 400 Bad Request w/ error message" in {
                val response = executeRequest(Method.POST, uri("!@#$%"))
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == InvalidUuidFormat("!@#$%").message)
            }
        }

        "no Card with id" should {
            "give 404 Not Found w/ error message" in {
                val response = executeRequest(Method.POST, uri(nonExistentId))
                assert(response.status == Status.NotFound)

                val responseBody = extractBody(response)
                assert(responseBody == CouldNotFindEntityWithId("Card", nonExistentId).message)
            }
        }


        "Card has already been deleted" should {
            "give 404 Not Found w/ error message" in {
                val response = executeRequest(Method.POST, uri(deletedCardId))
                assert(response.status == Status.NotFound)

                val responseBody = extractBody(response)
                assert(responseBody == CouldNotFindEntityWithId("Card", deletedCardId).message)
            }
        }
    }
}
