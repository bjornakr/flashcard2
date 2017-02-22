package deck.viewer

import java.sql.Timestamp
import java.util.UUID

import common.ApiBaseSpec
import common.{ApiBaseSpec, CouldNotFindEntityWithId, InvalidUuidFormat}
import deck.editor.DeckChangedRow
import io.circe.Decoder._
import io.circe.generic.auto._
import io.circe.parser.decode
import org.http4s.{Method, Status}
import slick.driver.H2Driver.api._

class DeckViewerApiSpec extends ApiBaseSpec {
    override protected def fillDatabase(): Unit = {
        val ids = Vector.fill(3) { UUID.randomUUID().toString }
        val timestamp = new Timestamp(System.currentTimeMillis())
        val deckCreatedEvents = List(
            DeckChangedRow(1, timestamp, ids(0), "Test Deck 0"),
            DeckChangedRow(2, timestamp, ids(1), "Test Deck 1"),
            DeckChangedRow(3, timestamp, ids(2), "Test Deck 2")
        )
        val action = slick.dbio.DBIO.seq(deckChangedTable ++= deckCreatedEvents)
        db.run(action)
    }

    private val viewerUri = baseUri / "deck" / "viewer"


    s"GET $viewerUri" should {
        "give 200 Ok w/ [ { deckId, title, noOfCards } ]" in {
            val response = executeRequest(Method.GET, viewerUri)
            assert(response.status == Status.Ok)

            val responseBody = extractBody(response)
            val result = decode[Seq[Result]](responseBody).valueOr(e => throw e)
            assert(result.length == 3)

            // TODO: Validate content
        }
    }

}
