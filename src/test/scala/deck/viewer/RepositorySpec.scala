package deck.viewer

import java.sql.Timestamp
import java.util.UUID

import deck.editor.{DeckChangedRow, DeckChangedTable}
import deck.remover.{DeckDeletedRow, DeckDeletedTable}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpec}
import slick.driver.H2Driver.api._
import slick.lifted.TableQuery

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class RepositorySpec extends WordSpec with BeforeAndAfter with BeforeAndAfterAll {
    private val db: _root_.slick.driver.H2Driver.backend.DatabaseDef =
        Database.forURL("jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    val repository = new deck.viewer.Repository(db)

    protected val deckChangedTable: TableQuery[DeckChangedTable] = TableQuery[DeckChangedTable]
    protected val deckDeletedTable: TableQuery[DeckDeletedTable] = TableQuery[DeckDeletedTable]


    protected def clearDatabase(): Unit = {
        val dropTablesAction = slick.dbio.DBIO.seq(
            deckChangedTable.schema.drop,
            deckDeletedTable.schema.drop
        )

        val future = db.run(dropTablesAction)
        Await.ready(future, Duration.Inf)
    }

    protected def constructDatabase(): Unit = {
        val createTablesAction = slick.dbio.DBIO.seq(
            deckChangedTable.schema.create,
            deckDeletedTable.schema.create
        )

        val future = db.run(createTablesAction)
        Await.ready(future, Duration.Inf)
    }


    def execute[A](f: Future[A]): A = {
        Await.ready(f, Duration.Inf).value.get match {
            case Success(a) => a
        }
    }

    override def beforeAll(): Unit = {

    }

    before {
        clearDatabase()
        constructDatabase()
    }

    def insertChangeEvent(title: String): UUID = {
        val t = new Timestamp(System.currentTimeMillis())
        val id = UUID.randomUUID
        val query = deckChangedTable += DeckChangedRow(0, t, id.toString, title)
        execute(db.run(query))
        id
    }

    def insertDeleteEvent(id: UUID): Unit = {
        val t = new Timestamp(System.currentTimeMillis())
        val query = deckDeletedTable += DeckDeletedRow(0, t, id.toString)
        execute(db.run(query))
    }

    override def afterAll(): Unit = {
        Database.forURL("jdbc:h2:mem:test1").close()
    }


    // To save some space, I've been a bit creative with the terminology:
    // []          = List (empty)
    // c           = Change events
    // r           = Remove events
    // c: [d1]     = List of change events, this one has one event with title "d1" (deck 1)
    // r: [d1, d2] = List of remove events, here we have two events with title "d1" and "d2"

    "getAll" when {
        "empty database" should {
            "give []" in {
                val results = execute(repository.getAll)
                assert(results.isEmpty)
            }
        }

        "c: [d1]" should {
            "give [d1]" in {
                insertChangeEvent("d1")
                val results = execute(repository.getAll)
                assert(results.length == 1)
                assert(results.exists(p => p.title == "d1"))
            }
        }

        "c: [d1, d2]" should {
            "give [d1, d2]" in {
                insertChangeEvent("d1")
                insertChangeEvent("d2")
                val results = execute(repository.getAll)
                assert(results.length == 2)
                assert(results(0).title == "d1")
                assert(results(1).title == "d2")
            }
        }


        "c: [d1], r: [d1]" should {
            "give []" in {
                val d1id = insertChangeEvent("d1")
                insertDeleteEvent(d1id)
                val results = execute(repository.getAll)
                assert(results.isEmpty)
            }
        }

        "c: [d1, d2], r: [d1]" should {
            "give [d2]" in {
                val d1id = insertChangeEvent("d1")
                insertChangeEvent("d2")
                insertDeleteEvent(d1id)
                val results = execute(repository.getAll)
                assert(results.length == 1)
                assert(results(0).title == "d2")
            }
        }

        "c: [d1, d2], r: [d2]" should {
            "give [d1]" in {
                insertChangeEvent("d1")
                val d2id = insertChangeEvent("d2")
                insertDeleteEvent(d2id)
                val results = execute(repository.getAll)
                assert(results.length == 1)
                assert(results(0).title == "d1")
            }
        }
    }
}
