package deck.remover

import java.sql.Timestamp

import common.ApiBaseSpec
import deck.editor.creator.DeckChangedRow
import org.http4s.{Method, Status, Uri}
import slick.driver.H2Driver.api._

class ApiSpec extends ApiBaseSpec {
    private val bbbaseUri = Uri.fromString("http://localhost:8070/api/decks").valueOr(e => throw e)

    private val existingDeckId = "00000000-0000-0000-0000-000000000001"

    override def fillDatabase(): Unit = {
        val deckCreatedEvent = DeckChangedRow(0, new Timestamp(System.currentTimeMillis()), existingDeckId, "Test Deck")
//        val zok = TableQuery[DeckChangedTable] += deckCreatedEvent
        val action = slick.dbio.DBIO.seq(deckChangedTable += deckCreatedEvent)
        db.run(action)
    }




    "DELETE/:id" when {
        "valid id" should {
            "give 2XX No Content" in {
                val uri = bbbaseUri / existingDeckId
                val response = executeRequest(Method.DELETE, uri)
                assert(response.status == Status.NoContent)
            }
        }

        "invalid UUID" should {
            "give 402 Bad Request w/ error message" ignore {

            }
        }

        "no deck with id" should {
            "give 404 Not Found w/ error message" ignore {

            }
        }
    }
}
