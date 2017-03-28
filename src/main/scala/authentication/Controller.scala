package authentication

import java.time.Clock

import cats.data.Xor
import common.CouldNotParse
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.http4s.dsl._
import org.http4s.server._
import org.http4s.util.CaseInsensitiveString
import org.http4s.{EntityDecoder, HttpService, _}
import org.reactormonk.{CryptoBits, PrivateKey}

import scalaz.Scalaz._
import scalaz._
import scalaz.concurrent.Task

trait AccessLevel
case object AdminAccess extends AccessLevel
case object BasicAccess extends AccessLevel
case class MegaToken(username: String, accessLevel: Int)

class Controller {
    private val key = PrivateKey(scala.io.Codec.toUTF8(scala.util.Random.alphanumeric.take(20).mkString("")))
    private val crypto = CryptoBits(key)
    private val clock = Clock.systemUTC

    private val authedService: AuthedService[MegaToken] = AuthedService {
        case GET -> Root / "welcome" as token => Ok(s"Welcome, ${token.username}.")
    }

    private val authorize: Service[Request, String \/ MegaToken] = Kleisli(request => Task.delay(getToken(request)))


    private def getToken(request: Request): String \/ MegaToken = {
        def decodeToken(encryptedToken: String): String \/ MegaToken = {
            crypto.validateSignedToken(encryptedToken) match {
                case None => -\/("Invalid token")
                case Some(t) => {
                    decode[MegaToken](t) match {
                        case Xor.Left(_) => -\/("Invalid token")
                        case Xor.Right(a: MegaToken) => \/-(a)
                    }
                }
            }
        }

        for {
//            header <- request.headers.get(Authorization).toRightDisjunction("Couldn't find an Authorization header")
            header <- request.headers.get(CaseInsensitiveString("authorization")).toRightDisjunction("Couldn't find an Authorization header")
            token <- decodeToken(header.value)
        } yield token

    }



    private val onFailure: AuthedService[String] = Kleisli(req => Forbidden(req.authInfo))

    private val middleware = AuthMiddleware(authorize, onFailure)


    val httpService: HttpService = middleware(authedService)


    val loginService: HttpService = HttpService {
        case request@POST -> Root => {
            val requestJson = EntityDecoder.decodeString(request).run
            decode[Credentials](requestJson) match {
                case Xor.Left(_) => BadRequest(CouldNotParse("body", Credentials).message)
                case Xor.Right(a) => {
                    if (validateCredencials(a)) {
                        val token = MegaToken("guest", 1).asJson.noSpaces
                        val encryptedToken = crypto.signToken(token, clock.millis.toString)
                        Ok(encryptedToken)
                    }
                    else
                        Unauthorized(Challenge("Scheme?", "Realm?"))
                }
            }
        }
    }

    private def validateCredencials(c: Credentials): Boolean =
        c.username == "guest" && c.password == "guest"

}






case class Credentials(username: String, password: String)

object LoginValidator {
    def apply(cred: Credentials): String \/ User = {
        if (cred.username == "huleknurr" && cred.password == "qwerty123")
            \/-(User(40, "huleknurr"))
        else
            -\/("Access denied!")
    }
}

case class User(id: Int, name: String)