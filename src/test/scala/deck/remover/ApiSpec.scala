package deck.remover

import java.sql.Timestamp

import common.ApiBaseSpec
import deck.editor.creator.DeckChangedRow
import io.circe.Decoder._
import io.circe.generic.auto._
import io.circe.parser.decode
import org.http4s.{Method, Status}
import slick.driver.H2Driver.api._

class ApiSpec extends ApiBaseSpec {
    private val removerUri = baseUri / "deck" / "remover"

    private val existingDeckId = "00000000-0000-0000-0000-000000000001"

    override def fillDatabase(): Unit = {
        val deckCreatedEvent = DeckChangedRow(0, new Timestamp(System.currentTimeMillis()), existingDeckId, "Test Deck")
        val action = slick.dbio.DBIO.seq(deckChangedTable += deckCreatedEvent)
        db.run(action)
    }

    s"POST $removerUri/:id" when {
        "valid id" should {
            "give 201 Created w/ { deckId }" in {
                val uri = removerUri / existingDeckId
                val response = executeRequest(Method.POST, uri)
                assert(response.status == Status.Created)

                val responseBody = extractBody(response)
                val result = decode[Result](responseBody).valueOr(e => throw e)
                assert(result.deckId == existingDeckId)
            }
        }

        "wrong UUID format" should {
            "give 402 Bad Request w/ error message" ignore {
                val uri = removerUri / "Not a UUID."
                val response = executeRequest(Method.DELETE, uri)
                assert(response.status == Status.BadRequest)

                // TODO: Verify error
            }
        }

        "no deck with id" should {
            "give 404 Not Found w/ error message" ignore {
                val id = "00000000-0000-0000-0000-999999999999"
                val uri = removerUri / id
                val response = executeRequest(Method.DELETE, uri)
                assert(response.status == Status.NotFound)

                // TODO: Verify error
            }
        }
    }
}
