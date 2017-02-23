package deck.editor.creator

import java.util.UUID

import common.{ApiBaseSpec, DatabaseError}
import deck.editor.{EventResponse, RequestDto}
import io.circe.Decoder._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.http4s._


class DeckCreatorApiSpec extends ApiBaseSpec {
    override protected def fillDatabase(): Unit = ()

    private val deckUri = baseUri / "deck" / "creator"

    s"POST $deckUri" when {
        "database error" should {

            "log errors" ignore {
                // TODO: Log errors
            }



            "give 500 Internal Server Error w/ error message" in {
                clearDatabase()
                val title = "Test Deck"
                val request = RequestDto(title)
                val body = toBody(request.asJson.noSpaces)
                val response = executeRequest(Method.POST, deckUri, body)
                assert(response.status == Status.InternalServerError)

                val responseBody = extractBody(response)
                assert(responseBody == DatabaseError.message)
            }
        }

        "no body" should {
            "give 400 Bad Request w/ error message" in {
                val response = executeRequest(Method.POST, deckUri)
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == "Error parsing body.")
            }
        }

        "unparsable body (not json)" should {
            "give 400 Bad Request w/ error message" in {
                val body = toBody("mxyzptlk")
                val response = executeRequest(Method.POST, deckUri, body)
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == "Error parsing body.")

            }
        }

        "no title in body" should {
            "give 400 Bad Request w/ error message" in {
                val body = toBody(s"""{ "something": "else" }""")
                val response = executeRequest(Method.POST, deckUri, body)
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == "Error parsing body.")
            }
        }

        "valid request" should {
            "give 201 Created w/ { deckId, title }" in {
                val title = "Test Deck"
                val request = RequestDto(title)
                val body = toBody(request.asJson.noSpaces)
                val response = executeRequest(Method.POST, deckUri, body)
                assert(response.status == Status.Created)

                val responseBody = extractBody(response)
                val eventResponse = decode[EventResponse](responseBody).valueOr(e => throw e)

                UUID.fromString(eventResponse.deckId) // Checking id integrity
                assert(eventResponse.title == title)
            }
        }
    }
}
