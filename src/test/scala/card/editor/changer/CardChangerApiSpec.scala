package card.editor.changer

import java.util.UUID

import card.editor.{BackDto, FrontDto, RequestDto, ResponseDto}
import common.{ApiBaseSpec, CannotBeEmpty, CouldNotFindEntityWithId, CouldNotParse}
import deck.editor.DeckChangedRow
import deck.remover.DeckDeletedRow
import io.circe.Decoder._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.http4s._
import slick.driver.H2Driver.api._

class CardChangerApiSpec extends ApiBaseSpec {
    private val existingDeckId = UUID.randomUUID().toString
    private val deletedDeckId = UUID.randomUUID().toString
    private val existingCardId = UUID.randomUUID().toString

    override protected def fillDatabase(): Unit = {
        val action = slick.dbio.DBIO.seq(
            deckChangedTable += DeckChangedRow(0, createTimestamp(), existingDeckId, "Test Deck"),
            deckChangedTable += DeckChangedRow(0, createTimestamp(), deletedDeckId, "Deleted Deck"),
            deckDeletedTable += DeckDeletedRow(0, createTimestamp(), deletedDeckId),
            cardChangedTable += card.editor.ChangedRow(0, createTimestamp(), existingCardId, existingDeckId,
                "Test Card Front", None, "Test Card Back", Some("Back Description"))
        )
        db.run(action)
    }

    private val front = FrontDto("Front", None)
    private val back = BackDto("Back", Some("Description B"))
    private val validCard = RequestDto(front, back)

    s"POST $baseUri/deck/:deckId/card/:cardId/creator" when {
        "valid request" should {
            "give 201 Created w/ { cardId, deckId, front { term, description }, back { term, description } }" in {
                val uri = baseUri / "deck" / existingDeckId / "card" / existingCardId
                val body = toBody(validCard.asJson.noSpaces)
                val response = executeRequest(Method.POST, uri, body)
                assert(response.status == Status.Created)

                val responseBody = extractBody(response)
                Console.println(responseBody)
                val responseDto = decode[ResponseDto](responseBody).valueOr(e => throw e)
                val expectedResponseDto = ResponseDto(responseDto.cardId, existingDeckId.toString, front, back)
                assert(responseDto == expectedResponseDto)
            }
        }

        "Deck does not exist" should {
            "give 404 Not Found w/ error message" in {
                val body = toBody(validCard.asJson.noSpaces)
                val nonExistingId = "99999999-9999-9999-9999-999999999999"
                val uri = baseUri / "deck" / nonExistingId / "card"
                val response = executeRequest(Method.POST, uri, body)
                assert(response.status == Status.NotFound)

                val message = extractBody(response)
                assert(message == CouldNotFindEntityWithId("Deck", nonExistingId).message)
            }
        }

        "Deck has been deleted" should {
            "give 404 Not Found w/ error message" in {
                val body = toBody(validCard.asJson.noSpaces)
                val uri = baseUri / "deck" / deletedDeckId / "card"
                val response = executeRequest(Method.POST, uri, body)
                assert(response.status == Status.NotFound)

                val message = extractBody(response)
                assert(message == CouldNotFindEntityWithId("Deck", deletedDeckId).message)
            }
        }

        "malformed body" should {
            "give 400 Bad Request w/ error message" in {
                val uri = baseUri / "deck" / existingDeckId / "card"
                val body = toBody("!@#$%")
                val response = executeRequest(Method.POST, uri, body)
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == CouldNotParse("body", RequestDto).message)
            }
        }

        "Front side Term is empty" should {
            "give 400 Bad Request w/ error message" in {
                val uri = baseUri / "deck" / existingDeckId / "card"

                val dto = RequestDto(FrontDto("   ", None), BackDto("Back", None))
                val body = toBody(dto.asJson.noSpaces)
                val response = executeRequest(Method.POST, uri, body)
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == CannotBeEmpty("Front: Term").message)
            }
        }

        "Back side Term is empty" should {
            "give 400 Bad Request w/ error message" in {
                val uri = baseUri / "deck" / existingDeckId / "card"

                val dto = RequestDto(FrontDto("Front", None), BackDto("   ", None))
                val body = toBody(dto.asJson.noSpaces)
                val response = executeRequest(Method.POST, uri, body)
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == CannotBeEmpty("Back: Term").message)
            }
        }
    }
}
