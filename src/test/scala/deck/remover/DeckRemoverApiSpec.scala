package deck.remover

import java.util.UUID

import common.{ApiBaseSpec, CouldNotFindEntityWithId, InvalidUuidFormat}
import deck.editor.DeckChangedRow
import io.circe.Decoder._
import io.circe.generic.auto._
import io.circe.parser.decode
import org.http4s.{Method, Status}
import slick.driver.H2Driver.api._

class DeckRemoverApiSpec extends ApiBaseSpec {
    private val removerUri = baseUri / "deck" / "remover"

    private val existingDeckId = "00000000-0000-0000-0000-000000000001"
    private val deletedDeckId = UUID.randomUUID().toString

    override def fillDatabase(): Unit = {
        val action = slick.dbio.DBIO.seq(
            deckChangedTable += DeckChangedRow(0, createTimestamp(), existingDeckId, "Test Deck"),
            deckChangedTable += DeckChangedRow(0, createTimestamp(), deletedDeckId, "Deleted Deck"),
            deckDeletedTable += DeckDeletedRow(0, createTimestamp(), deletedDeckId)
        )
        db.run(action)
    }

    s"POST $removerUri/:id" when {
        "database error" should {
            "give 500 Internal Server Error" in {
                clearDatabase()

                val uri = removerUri / existingDeckId
                val response = executeRequest(Method.POST, uri)
                assert(response.status == Status.InternalServerError)
            }
        }

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

        "invalid UUID format" should {
            "give 400 Bad Request w/ error message" in {
                val uri = removerUri / "xxxxx"
                val response = executeRequest(Method.POST, uri)
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == InvalidUuidFormat("xxxxx").message)
            }
        }

        "no Deck with id" should {
            "give 404 Not Found w/ error message" in {
                val id = "00000000-0000-0000-0000-999999999999"
                val uri = removerUri / id
                val response = executeRequest(Method.POST, uri)
                assert(response.status == Status.NotFound)

                val responseBody = extractBody(response)
                assert(responseBody == CouldNotFindEntityWithId("Deck", id).message)
            }
        }

        "Deck already has been deleted" should {
            "give 404 Not Found w/ error message" in {
                val uri = removerUri / deletedDeckId
                val response = executeRequest(Method.POST, uri)
                assert(response.status == Status.NotFound)

                val responseBody = extractBody(response)
                assert(responseBody == CouldNotFindEntityWithId("Deck", deletedDeckId).message)
            }
        }
    }
}
