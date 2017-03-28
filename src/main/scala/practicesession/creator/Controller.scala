package practicesession.creator

import java.sql.Timestamp
import java.time.ZonedDateTime
import java.util.UUID

import org.http4s.dsl._
import card.CardExistsQuery
import card.scorer.TableRow
import common._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpService
import practicesession.creator.Application.Dto
import practicesession.creator.Application.Dto.CardStatistics
import slick.driver.H2Driver.api._
import slick.lifted.{ProvenShape, TableQuery, Tag}

import scala.concurrent.Future

import java.time.temporal.ChronoUnit.DAYS

class Controller(appService: Application.Service) {
    val httpService = HttpService {
        case GET -> Root / deckId / noOfCards => {
            appService.create(deckId, noOfCards)
            Ok()
        }
    }

}

object Application {

    object Dto {

        case class CardStatistics(wins: Int, losses: Int, winStreak: Int, lastVisited: Option[String])

        case class Result(cardId: String, front: String, frontDescription: Option[String], back: String, backDescription: String,
                          statistics: CardStatistics)

    }

    class Service(repository: Repository) {
        def create(deckId: String, noOfCards: String): Either[ErrorMessage, Dto.Result] = {
            ???
        }
    }

}

object Domain {

    abstract case class CardStatistics(wins: Int, losses: Int, winStreak: Int, lastVisited: Option[String], score: Int)

    abstract case class CardContent(cardId: UUID, front: String, frontDescription: Option[String],
                             back: String, backDescription: Option[String])


    abstract case class Card(cardId: UUID, front: String, frontDescription: Option[String],
                                 back: String, backDescription: Option[String], statistics: CardStatistics)

    abstract case class PracticeSession(cards: Seq[Card])

    abstract case class ScoreEvent(t: ZonedDateTime, cardId: UUID, outcome: card.scorer.Outcome)

    object PracticeSession {

        def apply(cardContents: Seq[CardContent], scoreEvents: Seq[ScoreEvent], noOfCards: Int): PracticeSession = {

            def calculateWinStreak(scoreEvents: Seq[ScoreEvent]): Seq[(ScoreEvent, Int)] = {

                def proc(rest: Seq[ScoreEvent], acc: List[(ScoreEvent, Int)]): Seq[(ScoreEvent, Int)] = {
                    rest match {
                        case Nil => acc
                        case a :: tail => {
                            val isWin = a.outcome == card.scorer.Win

                            val winStreak =
                                if (!isWin)
                                    0
                                else {
                                    acc.headOption match {
                                        case None => 1
                                        case Some((b, i)) => {
                                            if (a.cardId.equals(b.cardId))
                                                i + 1
                                            else
                                                1
                                        }
                                    }
                                }
                            proc(tail, (a, winStreak) :: acc)
                        }
                    }
                }

                val sorted = scoreEvents.sortBy(_.t)(Ordering.fromLessThan(_ isBefore _))
                proc(sorted, List()).reverse
            }

            def score(lastVisited: String, winStreak: Int): Int = {
                // The formula is (1 + NoOfDaysSinceLastVisited) - (MAX(0, WinStreak - 3))^2
                //
                // The cards with higher score are selected for practice sessions.
                //
                // New, unpracticed cards have a score of 0.
                // When a card has been scored the first time, (1 + NoOfDaysSinceLastVisited) ensures that it will
                // have a higher score than untouched cards.
                //
                // WinStreak = consecutive wins. A loss breaks the streak and resets it to 0.
                // When a card has a WinStreak > 3, the score will start to lower exponentially, until it is in the negative.
                // New cards will be preferred to this card, until enough time has passed to give the
                // card a positive score again.

                val lv = ZonedDateTime.parse(lastVisited)
                val now = ZonedDateTime.now()
                val noOfDaysSinceLastVisited = DAYS.between(lv, now)
                val dateBonus = 1 + noOfDaysSinceLastVisited.toInt
                val winStreakPenalty = Math.pow(Math.max(0, winStreak - 3), 2).toInt
                dateBonus - winStreakPenalty
            }

            val scoresWithWinStreak: Seq[(ScoreEvent, Int)] = calculateWinStreak(scoreEvents)
            val idToCardStatistics: Map[UUID, CardStatistics] = scoresWithWinStreak.groupBy(_._1.cardId).map {
                case (id, v: Seq[(ScoreEvent, Int)]) =>
                    val timestamp = v.last._1.t.toString
                    val winstreak = v.last._2
                    id -> new CardStatistics(
                        v.count(_._1.outcome == card.scorer.Win),
                        v.count(_._1.outcome == card.scorer.Lose),
                        v.last._2,
                        Some(v.last._1.t.toString),
                        score(timestamp, winstreak)) {}
            }


            val initStatistics = new CardStatistics(0, 0, 0, None, 0) {}

            val cards = cardContents.map(a => new Card(a.cardId, a.front, a.frontDescription, a.back, a.backDescription,
                idToCardStatistics.getOrElse(a.cardId, initStatistics)) {})

            val sorted = cards.sortBy(- _.statistics.score)

            new PracticeSession(sorted.take(noOfCards)) {}
        }
    }
}

class Repository(db: Database) {

}
