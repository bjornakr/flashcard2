package deck.editor.creator

import java.util.UUID

import common.DatabaseError
import deck.remover.DeckDeletedTable
import slick.driver.H2Driver.api._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpec}
import org.http4s._
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.dsl._
import io.circe.Decoder._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import main.Main
import scodec.bits.ByteVector

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class ControllerSpec extends WordSpec with BeforeAndAfter with BeforeAndAfterAll {
    private val db = Database.forURL("jdbc:h2:mem:test1;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

    private val main = new Main(new Controller(new CreatorService(new Repository(db))))
    private val server = main.createServer
    private val baseUri = Uri.fromString("http://localhost:8070/api/decks").valueOr(e => throw e)
    private var client = PooledHttp1Client()

    def executeRequest(method: Method, uri: Uri): Response =
        executeRequest(method, uri, EmptyBody)


    def executeRequest(method: Method, uri: Uri, body: EntityBody): Response = {
        val request = Request(method, uri, HttpVersion.`HTTP/1.1`, Headers.empty, body)
        client.toHttpService.run(request).run
    }

    def toBody(body: String): EntityBody = {
        val byteV: ByteVector = ByteVector.encodeUtf8(body).right.getOrElse(ByteVector(0))
        scalaz.stream.Process.emit(byteV)
    }

    private def extractBody(r: Response): String =
        EntityDecoder.decodeString(r).run

    private val deckChangedTable = TableQuery[DeckChangedTable]
    private val deckDeletedTable = TableQuery[DeckDeletedTable]

    private val dropTablesAction = slick.dbio.DBIO.seq(
        deckChangedTable.schema.drop,
        deckDeletedTable.schema.drop
    )

    private val createTablesAction = slick.dbio.DBIO.seq(
        deckChangedTable.schema.create,
        deckDeletedTable.schema.create
    )

    def clearDatabase(): Unit = {

        val future = db.run(dropTablesAction)
        Await.ready(future, Duration.Inf)
//        val decks = TableQuery[DeckTable]
//        val cards = TableQuery[CardTable]
//        val schema = decks.schema ++ cards.schema
//
//        val dropCardsAction = slick.dbio.DBIO.seq(schema.drop)
//        val dropCardsFuture = db.run(dropCardsAction)
//        Await.ready(dropCardsFuture, Duration.Inf) //.value.get
    }
    def fillDatabase(): Unit = {

        val future = db.run(createTablesAction)
        Await.ready(future, Duration.Inf)
//        val decks = TableQuery[DeckTable]
//        val cards = TableQuery[CardTable]
//        val schema = decks.schema ++ cards.schema
//
//        val setup = slick.dbio.DBIO.seq(schema.create)
//        val dbSetupFuture = db.run(setup)
//        Await.ready(dbSetupFuture, Duration.Inf).value.get
//
//        val createDecks = deckDao.saveAll(allDecks)
//        Await.result(createDecks, Duration.Inf)
//        val createCards = cardDao.saveAll(allCards)
//        Await.result(createCards, Duration.Inf)
    }

    before {
        clearDatabase()
        fillDatabase()
        client = PooledHttp1Client()

    }

    after {
        client.shutdownNow() // I need to shut down the client after each call, otherwise it hangs after a certain no of calls.
    }

    override def afterAll {
        server.shutdownNow()
        Database.forURL("jdbc:h2:mem:test1").close()
    }

    s"POST $baseUri" when {
        "database error" should {
            // TODO: Log errors

            "give 500 Internal Server Error w/ error message" in {
                clearDatabase()
                val title = "Test Deck"
                val request = CreateRequest(title)
                val body = toBody(request.asJson.noSpaces)
                val response = executeRequest(Method.POST, baseUri, body)
                assert(response.status == Status.InternalServerError)

                val responseBody = extractBody(response)
                assert(responseBody == DatabaseError.message)
            }
        }

        "no body" should {
            "give 400 Bad Request w/ error message" in {
                val response = executeRequest(Method.POST, baseUri)
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == "Error parsing body.")
            }
        }

        "unparsable body (not json)" should {
            "give 400 Bad Request w/ error message" in {
                val body = toBody("mxyzptlk")
                val response = executeRequest(Method.POST, baseUri, body)
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == "Error parsing body.")

            }
        }

        "no title in body" should {
            "give 400 Bad Request w/ error message" in {
                val body = toBody(s"""{ "something": "else" }""")
                val response = executeRequest(Method.POST, baseUri, body)
                assert(response.status == Status.BadRequest)

                val responseBody = extractBody(response)
                assert(responseBody == "Error parsing body.")
            }
        }

        "valid request" should {
            "give 201 Created w/ ChangeEventResult" in {
                val title = "Test Deck"
                val request = CreateRequest(title)
                val body = toBody(request.asJson.noSpaces)
                val response = executeRequest(Method.POST, baseUri, body)
                assert(response.status == Status.Created)

                val responseBody = extractBody(response)
                val eventResponse = decode[ChangedEventResult](responseBody).valueOr(e => throw e)

                UUID.fromString(eventResponse.deckId) // Checking id integrity
                assert(eventResponse.title == title)
            }
        }
    }
}
