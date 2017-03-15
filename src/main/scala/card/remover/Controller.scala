package card.remover

import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.UUID
import org.http4s.dsl._

import card.CardExistsQuery
import common._
import io.circe.generic.auto._
import io.circe.syntax._

import slick.driver.H2Driver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.Future

class Controller(appService: AppService) {
    def apply(cardId: String) = {
        appService.saveEvent(cardId) match {
            case Left(e) => ErrorToHttpStatus(e)
            case Right(r: ResponseDto) => Created(r.asJson.noSpaces)
        }
    }
}

case class ResponseDto(cardId: String)


class AppService(repository: Repository) {
    def saveEvent(cardId: String): Either[ErrorMessage, ResponseDto] = {
        UuidParser(cardId) match {
            case Left(e) => Left(e)
            case Right(uuid) => {
                FutureAwaiter(repository.cardExists(cardId))(cardExists => {
                    if (cardExists)
                        FutureAwaiter(repository.save(uuid))(id => Right(id))
                    else
                        Left(CouldNotFindEntityWithId("Card", cardId))
                })
            }
        }
    }
}


class Repository(db: Database) extends CardExistsQuery {
    def cardExists(cardId: String): Future[Boolean] = cardExists(db)(cardId)

    def save(cardId: UUID): Future[ResponseDto] = {
        val table = TableQuery[Table]
        val insertQuery = table returning table.map(_.id) into ((row, _) => ResponseDto(row.cardId))
        val row = TableRow(0, new Timestamp(ZonedDateTime.now.toInstant.getEpochSecond * 1000L), cardId.toString)
        val action = insertQuery += row
        db.run(action)
    }
}


case class TableRow(id: Long, t: Timestamp, cardId: String)

class Table(tag: Tag) extends slick.driver.H2Driver.api.Table[TableRow](tag, "card_deleted_events") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def t = column[Timestamp]("t")

    def cardId = column[String]("card_id")

    def * : ProvenShape[TableRow] = (id, t, cardId) <> (TableRow.tupled, TableRow.unapply)
}