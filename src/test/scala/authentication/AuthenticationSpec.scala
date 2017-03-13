package authentication

import common.ApiBaseSpec
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._

class AuthenticationSpec extends ApiBaseSpec {
    override protected def fillDatabase(): Unit = ()

    "bad login" in {
        val loginUri = baseUri / "login"
        val credencials = Credentials("invalid", "user")
        val requestBody = toBody(credencials.asJson.noSpaces)
        val response = executeRequest(Method.POST, loginUri, requestBody)

        assert(response.status == Status.Unauthorized)
//        val responseBody = extractBody(response)
//        assert(responseBody == "Hei")
    }


    "hopapa" in {
        def requestToken: String = {
            val loginUri = baseUri / "login"
            val credencials = Credentials("guest", "guest")
            val requestBody = toBody(credencials.asJson.noSpaces)
            val response = executeRequest(Method.POST, loginUri, requestBody)
            assert(response.status == Status.Ok)

            val token = extractBody(response)
            token
//            decode[MegaToken](token).valueOr(e => throw new Exception(e))
        }

        val uri = baseUri / "auth" / "welcome"
//        val response = executeRequest(Method.GET, uri)

        val header = Header("Authorization", requestToken)
        val request = Request(Method.GET, uri, HttpVersion.`HTTP/1.1`, Headers(header), EmptyBody)
        val response = client.toHttpService.run(request).run
        val body = extractBody(response)
//        assert(response.status == Status.Ok)
        assert(body == "Welcome, guest.")
    }

}
