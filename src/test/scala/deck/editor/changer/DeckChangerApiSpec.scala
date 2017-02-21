package deck.editor.changer

import java.sql.Timestamp

import common._
import deck.editor.{DeckChangedRow, EventResponse, RequestDto}
import org.http4s.{Method, Status}
import io.circe.Decoder._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import slick.driver.H2Driver.api._

class DeckChangerApiSpec extends ApiBaseSpec {
    private val existingDeckId = "00000000-0000-0000-0000-000000000001"

    override protected def fillDatabase(): Unit = {
        val deckCreatedEvent = DeckChangedRow(0, new Timestamp(System.currentTimeMillis()), existingDeckId, "Test Deck")
        val action = slick.dbio.DBIO.seq(deckChangedTable += deckCreatedEvent)
        db.run(action)
    }

    private val changerUri = baseUri / "deck" / "changer"

    s"POST $changerUri/:id" when {
        "database error" should {
            // TODO: Log errors

            "give 500 Internal Server Error w/ error message" in {
                clearDatabase()
                val title = "Test Deck"
                val request = RequestDto(title)
                val uri = changerUri / existingDeckId
                val body = toBody(request.asJson.noSpaces)
                val response = executeRequest(Method.POST, uri, body)
                assert(response.status == Status.InternalServerError)

                val responseBody = extractBody(response)
                assert(responseBody == DatabaseError.message)
            }
        }

        "valid request" should {
            "give 201 Created w/ { deckId, newTitle }" in {
                val newTitle = "New title"
                val request = RequestDto(newTitle)
                val uri = changerUri / existingDeckId
                val body = toBody(request.asJson.noSpaces)
                val response = executeRequest(Method.POST, uri, body)
                assert(response.status == Status.Created)

                val responseBody = extractBody(response)
                val eventResponse = decode[EventResponse](responseBody).valueOr(e => throw e)
                val expectedEventResponse = EventResponse(existingDeckId, newTitle)
                assert(eventResponse == expectedEventResponse)
            }
        }

        "no body" should {
            "give 401 Bad Request w/ error message" in {
                val uri = changerUri / existingDeckId
                val response = executeRequest(Method.POST, uri)
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == s"Could not parse body into ${RequestDto.getClass.getCanonicalName}.")
            }
        }

        "unparsable body (not json)" should {
            "give 401 Bad Request w/ error message" in {
                val uri = changerUri / existingDeckId
                val response = executeRequest(Method.POST, uri, toBody("invalid body"))
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == s"Could not parse body into ${RequestDto.getClass.getCanonicalName}.")
            }
        }

        "missing [deckId] in url" should {
            "give 404 Not Found" in {
                val uri = changerUri
                val requestBody = toBody(RequestDto("New title").asJson.noSpaces)
                val response = executeRequest(Method.POST, uri, requestBody)
                assert(response.status == Status.NotFound)
            }
        }

        "invalid UUID format" should {
            "give 400 Bad Request w/ error message" in {
                val invalidUuid = "not-a-uuid"
                val uri = changerUri / invalidUuid
                val requestBody = toBody(RequestDto("New title").asJson.noSpaces)
                val response = executeRequest(Method.POST, uri, requestBody)
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == InvalidUuidFormat(invalidUuid).message)
            }
        }

        "no deck with [deckId] exists" should {
            "give 404 Not Found w/ error message" in {
                val id = "99999999-9999-9999-9999-999999999999"
                val uri = changerUri / id
                val requestBody = toBody(RequestDto("New title").asJson.noSpaces)
                val response = executeRequest(Method.POST, uri, requestBody)
                assert(response.status == Status.NotFound)

                val responseBody = extractBody(response)
                assert(responseBody == CouldNotFindEntityWithId("Deck", id).message)
            }
        }

        "empty [title]" should {
            "give 401 Bad Request w/ error message" in {
                val uri = changerUri / existingDeckId
                val requestBody = toBody(RequestDto("").asJson.noSpaces)
                val response = executeRequest(Method.POST, uri, requestBody)
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == CannotBeEmpty("title").message)
            }
        }
    }
}
