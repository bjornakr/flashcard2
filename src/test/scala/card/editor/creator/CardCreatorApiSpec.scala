package card.editor.creator

import java.util.UUID

import common.{ApiBaseSpec, CouldNotFindEntityWithId, DatabaseError}
import deck.editor.{EventResponse, RequestDto}
import io.circe.Decoder._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.http4s._

class CardCreatorApiSpec extends ApiBaseSpec {
    override protected def fillDatabase(): Unit = ()

    private val deckId = UUID.randomUUID()
    private val deckUri = baseUri / "deck" / deckId.toString / "card"

    private val front = FrontDto("Front", None)
    private val back = BackDto("Back", Some("Description B"))
    private val validCard = CreateCardRequestDto(front, back)

    s"POST $baseUri/deck/:deckId/card/:cardId" when {
        "valid request" should {
            "give 201 Created w/ { cardId, deckId, front { term, description }, back { term, description } }" in {
                val body = toBody(validCard.asJson.noSpaces)
                val response = executeRequest(Method.POST, deckUri, body)
                assert(response.status == Status.Created)

                val responseBody = extractBody(response)
                val responseDto = decode[CreateCardResponseDto](responseBody).valueOr(e => throw e)
                val expectedResponseDto = CreateCardResponseDto(responseDto.cardId, deckId.toString, front, back)
                assert(responseDto == expectedResponseDto)
            }
        }

        "deck does not exist" should {
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
    }
}
