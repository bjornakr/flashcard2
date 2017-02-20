package common

import deck.editor.creator.{Controller, CreatorService, DeckChangedTable, Repository}
import deck.remover.DeckDeletedTable
import main.Main
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.server.Server
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpec}
import scodec.bits.ByteVector

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import slick.driver.H2Driver.api._
import slick.lifted.TableQuery

abstract class ApiBaseSpec extends WordSpec with BeforeAndAfter with BeforeAndAfterAll {
    protected val db: _root_.slick.driver.H2Driver.backend.DatabaseDef =
        Database.forURL("jdbc:h2:mem:test1;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    private val main = new Main(
        new Controller(new CreatorService(new Repository(db))),
        new deck.remover.Controller(new deck.remover.RemoverService(new deck.remover.Repository(db)))
    )
    private val testPort = 8070
    private var server: Server = _ // = main.createServer(8070)
    private var client: Client = _

    protected val baseUri: Uri = Uri.fromString(s"http://localhost:$testPort/api").valueOr(e => throw e)

    protected def executeRequest(method: Method, uri: Uri): Response =
        executeRequest(method, uri, EmptyBody)


    protected def executeRequest(method: Method, uri: Uri, body: EntityBody): Response = {
        val request = Request(method, uri, HttpVersion.`HTTP/1.1`, Headers.empty, body)
        client.toHttpService.run(request).run
    }

    protected def toBody(body: String): EntityBody = {
        val byteV: ByteVector = ByteVector.encodeUtf8(body).right.getOrElse(ByteVector(0))
        scalaz.stream.Process.emit(byteV)
    }

    protected def extractBody(r: Response): String =
        EntityDecoder.decodeString(r).run

    protected val deckChangedTable: TableQuery[DeckChangedTable] = TableQuery[DeckChangedTable]
    protected val deckDeletedTable: TableQuery[DeckDeletedTable] = TableQuery[DeckDeletedTable]

    private val dropTablesAction = slick.dbio.DBIO.seq(
        deckChangedTable.schema.drop,
        deckDeletedTable.schema.drop
    )

    private val createTablesAction = slick.dbio.DBIO.seq(
        deckChangedTable.schema.create,
        deckDeletedTable.schema.create
    )

    protected def clearDatabase(): Unit = {

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

    protected def constructDatabase(): Unit = {
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


    override def beforeAll: Unit = {
        server = main.createServer(testPort)

    }

    before {
        clearDatabase()
        constructDatabase()
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

    protected def fillDatabase(): Unit
}
