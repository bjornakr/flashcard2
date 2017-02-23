package deck.viewer

import java.sql.Timestamp
import java.util.UUID

import common.{ApiBaseSpec, CouldNotFindEntityWithId, DatabaseError, InvalidUuidFormat}
import deck.editor.{DeckChangedRow, RequestDto}
import io.circe.Decoder._
import io.circe.generic.auto._
import io.circe.parser.decode
import org.http4s.{Method, Status}
import slick.driver.H2Driver.api._

class DeckViewerApiSpec extends ApiBaseSpec {
    private val ids = Vector.fill(3) { UUID.randomUUID().toString }
    private val timestamp = new Timestamp(System.currentTimeMillis())

    override protected def fillDatabase(): Unit = {
        val deckCreatedEvents = List(
            DeckChangedRow(0, timestamp, ids(0), "Test Deck 0"),
            DeckChangedRow(0, timestamp, ids(1), "Test Deck 1"),
            DeckChangedRow(0, timestamp, ids(2), "Test Deck 2")
        )
        val action = slick.dbio.DBIO.seq(deckChangedTable ++= deckCreatedEvents)
        db.run(action)
    }

    private val viewerUri = baseUri / "deck" / "viewer"


    s"GET $viewerUri" when {
        "database error" should {

            "log errors" ignore {
                // TODO: Log errors
            }


            "give 500 Internal Server Error w/ error message" in {
                clearDatabase()
                val response = executeRequest(Method.GET, viewerUri)
                assert(response.status == Status.InternalServerError)

                val responseBody = extractBody(response)
                assert(responseBody == DatabaseError.message)
            }
        }

        "valid request" should {
            "give 200 Ok w/ [ { deckId, title } ]" in {
                val response = executeRequest(Method.GET, viewerUri)
                assert(response.status == Status.Ok)

                val responseBody = extractBody(response)
                val results = decode[Seq[Result]](responseBody).valueOr(e => throw e)
                assert(results.length == 3)

                def expectedResult(i: Int) = Result(ids(i), s"Test Deck $i")
                assert(results.contains(expectedResult(0)))
                assert(results.contains(expectedResult(1)))
                assert(results.contains(expectedResult(2)))
            }
        }
    }
}
