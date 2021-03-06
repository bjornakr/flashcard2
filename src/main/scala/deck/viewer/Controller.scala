package deck.viewer

import java.sql.Timestamp

import common._
import deck.editor._
import deck.remover.DeckDeletedTable
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpService
import org.http4s.dsl.{Root, _}
import slick.dbio.DBIOAction
import slick.dbio.Effect.Read
import slick.driver.H2Driver.api._
import slick.lifted.{QueryBase, TableQuery}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class Controller(appService: AppService) {
    val httpService = HttpService {
        case GET -> Root => {
            appService.getAll match {
                case Left(e) => ErrorToHttpStatus(e)
                case Right(result) => Ok(result.asJson.noSpaces)
            }
        }
    }
}

case class Result(deckId: String, title: String)
object ResultMapper {
    def apply(changeEvent: deck.editor.Event): Result = {
        Result(changeEvent.deckId.toString, changeEvent.title)
    }
}

class AppService(repository: Repository) {
    def getAll: Either[ErrorMessage, Seq[Result]] = {
        val future = repository.getAll
        Await.ready(future, DurationInt(3).seconds).value.get match {
            case Failure(e) => {
                // TODO: Log error
                Left(DatabaseError) // or database error - but do not send error message to api?
            }
            case Success(events) => Right(events.map(ResultMapper(_)))
        }
    }
}

// DOMAIN

//case class
//object Deck {
//    def apply(changeEvents: Seq[deck.editor.Event], deleteEvents: Seq[deck.remover.Event]): Seq[Result] = {
//        changeEvents
//            .filter(ce => !deleteEvents.exists(de => ce.deckId == de.deckId))
//            .map(ResultMapper(_))
//    }
//}

class Repository(db: Database) {

    //    http://stackoverflow.com/questions/12341579/select-rows-based-on-max-values-of-a-column-in-scalaquery-slick
    private val deckChangedTable = TableQuery[DeckChangedTable]
    private val deckDeletedTable = TableQuery[DeckDeletedTable]

    def getAll: Future[Seq[deck.editor.Event]] = {
        val deletedDeckIds = deckDeletedTable.map(_.deckId)
        val notDeletedQuery = deckChangedTable
            .filterNot(ce => ce.deckId in deletedDeckIds)

        // group by userId and select the userId and the max of the actionDate
        val latestQuery =
            deckChangedTable
                .groupBy { _.deckId }
                .map {
                    case (deckId, event) =>
                        deckId -> event.map(_.t).max
                }

        val qqq: QueryBase[Seq[DeckChangedRow]] = for {
            q1: DeckChangedTable <- notDeletedQuery
            q2: (Rep[String], Rep[Option[Timestamp]]) <- latestQuery
            if q1.deckId === q2._1 && q1.t === q2._2
        } yield q1

        val qx1: DBIOAction[Seq[Event], NoStream, Read] =
            qqq.result.map(_.map(a => DeckChangedRowMapper.toDomain(a)))(scala.concurrent.ExecutionContext.global)
//        qqq.

//        val xxx = qqq.result.map(ResultMapper _)
        db.run(qx1)
    }
}

