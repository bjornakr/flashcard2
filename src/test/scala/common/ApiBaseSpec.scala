package common

import java.sql.Timestamp

import card.editor
import card.editor.ChangedTable
import deck.editor.{DeckChangedTable, Repository}
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
        Database.forURL("jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")


    private val cardEditorRepository = new card.editor.Repository(db)
    private val cardController = new card.Controller(
        new card.editor.creator.Controller(new card.editor.creator.AppService(cardEditorRepository)),
        new card.editor.changer.Controller(new card.editor.changer.AppService(cardEditorRepository)),
        new card.remover.Controller(new card.remover.AppService(new card.remover.Repository(db))),
        new card.scorer.Controller(new card.scorer.AppService(new card.scorer.Repository(db)))
    )

    private val main = new Main(
        new authentication.Controller,
        new deck.editor.creator.Controller(new deck.editor.creator.AppService(new deck.editor.Repository(db))),
        new deck.editor.changer.Controller(new deck.editor.changer.AppService(new deck.editor.Repository(db))),
        new deck.remover.Controller(new deck.remover.AppService(new deck.remover.Repository(db))),
        new deck.viewer.Controller(new deck.viewer.AppService(new deck.viewer.Repository(db))),
        cardController)

    private val testPort = 8070
    private var server: Server = _ // = main.createServer(8070)
    protected var client: Client = _

    protected val baseUri: Uri = Uri.fromString(s"http://localhost:$testPort/api").valueOr(e => throw e)

    protected def executeRequest(method: Method, uri: Uri): Response =
        executeRequest(method, uri, EmptyBody)


    protected def executeRequest(method: Method, uri: Uri, body: EntityBody): Response = {
        val request = Request(method, uri, HttpVersion.`HTTP/1.1`, Headers.empty, body)
        client.toHttpService.run(request).run
    }

    protected def toBody(jsonBody: String): EntityBody = {
        val byteV: ByteVector = ByteVector.encodeUtf8(jsonBody).right.getOrElse(ByteVector(0))
        scalaz.stream.Process.emit(byteV)
    }

    protected def extractBody(r: Response): String =
        EntityDecoder.decodeString(r).run

    protected def createTimestamp() = new Timestamp(System.currentTimeMillis())


    protected val deckChangedTable: TableQuery[DeckChangedTable] = TableQuery[DeckChangedTable]
    protected val deckDeletedTable: TableQuery[DeckDeletedTable] = TableQuery[DeckDeletedTable]
    protected val cardChangedTable: TableQuery[card.editor.ChangedTable] = TableQuery[card.editor.ChangedTable]
    protected val cardDeletedTable: TableQuery[card.remover.Table] = TableQuery[card.remover.Table]
    protected val cardScoredTable: TableQuery[card.scorer.Table] = TableQuery[card.scorer.Table]

    private val dropTablesAction = slick.dbio.DBIO.seq(
        deckChangedTable.schema.drop,
        deckDeletedTable.schema.drop,
        cardChangedTable.schema.drop,
        cardDeletedTable.schema.drop,
        cardScoredTable.schema.drop
    )

    private val createTablesAction = slick.dbio.DBIO.seq(
        deckChangedTable.schema.create,
        deckDeletedTable.schema.create,
        cardChangedTable.schema.create,
        cardDeletedTable.schema.create,
        cardScoredTable.schema.create
    )

    protected def clearDatabase(): Unit = {

        val future = db.run(dropTablesAction)
        Await.ready(future, Duration.Inf)
    }

    protected def constructDatabase(): Unit = {
        val future = db.run(createTablesAction)
        Await.ready(future, Duration.Inf)
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
