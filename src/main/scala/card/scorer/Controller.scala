package card.scorer

import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.UUID

import card.CardExistsQuery
import common._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.Response
import org.http4s.dsl._
import slick.driver.H2Driver.api._
import slick.lifted.{ProvenShape, QueryBase, TableQuery, Tag}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.concurrent.Task

class Controller(appService: AppService) {
    private def execute(f: Either[ErrorMessage, ResponseDto]): Task[Response] =
        f match {
            case Left(e) => ErrorToHttpStatus(e)
            case Right(r: ResponseDto) => Created(r.asJson.noSpaces)
        }

    def win(cardId: String): Task[Response] =
        execute(appService.saveWinEvent(cardId))

    def lose(cardId: String): Task[Response] =
        execute(appService.saveLoseEvent(cardId))
}


// APP


case class ResponseDto(cardId: String, wins: Int, losses: Int)

private trait Outcome

private case object Win extends Outcome {
    override def toString: String = "win"
}

private case object Lose extends Outcome {
    override def toString: String = "lose"
}

class AppService(repository: Repository) {
    private def saveEvent(cardId: String, outcome: Outcome): Either[ErrorMessage, ResponseDto] = {
        UuidParser(cardId) match {
            case Left(e) => Left(e)
            case Right(uuid) => {
                FutureAwaiter(repository.cardExists(uuid))(cardExists => {
                    if (cardExists) {
                        FutureAwaiter(repository.saveEvent(uuid, outcome))(_ => {
                            FutureAwaiter(repository.getWinLossCount(uuid))(Right(_))
                        })

                    }
                    else
                        Left(CouldNotFindEntityWithId("Card", cardId))
                })
            }
        }
    }

    def saveWinEvent(cardId: String): Either[ErrorMessage, ResponseDto] = {
        saveEvent(cardId, Win)
    }

    def saveLoseEvent(cardId: String): Either[ErrorMessage, ResponseDto] = {
        saveEvent(cardId, Lose)
    }

}


private[scorer] case class TableRow(id: Long, t: Timestamp, cardId: String, outcome: String)


class Table(tag: Tag) extends slick.driver.H2Driver.api.Table[TableRow](tag, "card_scored_events") {
    def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def t: Rep[Timestamp] = column[Timestamp]("t")

    def cardId: Rep[String] = column[String]("card_id")

    def outcome: Rep[String] = column[String]("outcome")

    def * : ProvenShape[TableRow] = (id, t, cardId, outcome) <> (TableRow.tupled, TableRow.unapply)
}


class Repository(db: Database) extends CardExistsQuery {
    val win = "win"
    val lose = "lose"
    private val tableQuery = TableQuery[Table]

    def cardExists(cardId: UUID): Future[Boolean] = cardExists(db, cardId)

    def saveEvent(cardId: UUID, outcome: Outcome): Future[Unit] = {
        val action = tableQuery += TableRow(0,
            new Timestamp(ZonedDateTime.now.toInstant.getEpochSecond * 1000L), cardId.toString, outcome.toString)
        db.run(action).map(_ => ())
    }

    def getWinLossCount(cardId: UUID): Future[ResponseDto] = {


        val deleted = TableQuery[card.remover.Table]
            .map(_.cardId)
            .distinct

        val query: QueryBase[Seq[(String, Int)]] = TableQuery[Table]
            .filterNot(_.cardId.in(deleted))
            .filter(r => r.cardId === cardId.toString)
            .groupBy(t => t.outcome)
            .map { case (o, res) => o -> res.countDistinct }

        val result = query.result

        db.run(result)
            .map(_.toMap[String, Int])
            .map(a => ResponseDto(cardId.toString, a.getOrElse(win, 0), a.getOrElse(lose, 0)))
    }
}

