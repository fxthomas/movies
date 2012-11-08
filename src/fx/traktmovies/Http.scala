package fx.traktmovies

import java.io.{BufferedWriter,BufferedReader}
import java.io.{OutputStreamWriter,InputStreamReader}
import java.io.IOException
import java.net.URL
import java.net.HttpURLConnection

import android.util.Log

case class HttpResponse(code: Int, message: String)

case class HttpRequest(method: String, urlString: String) {
  val url = new URL(urlString)
  var data: Option[String] = None
  var headers = scala.collection.mutable.Map(
    "Accept-Encoding" -> "application/json",
    "Content-Type" -> "application/json"
  )

  def setData(s: String) = { data = Some(s) }
  def setHeader(header: String, value: String) = { headers(header) = value }

  def send(): HttpResponse = {
    // Prepare connection
    val urlConnection = url.openConnection().asInstanceOf[HttpURLConnection]

    // Tells the request to prepare an output stream
    // if we have data to send
    for (_ <- data) urlConnection.setDoOutput(true)

    // Set request method
    urlConnection.setRequestMethod(method)

    // Set request headers
    for ((header, value) <- headers)
      urlConnection.setRequestProperty(header, value)

    // Response string and code
    var response:String = null
    var code:Int = 0

    try {
      // Post data to the server
      for (s <- data) {
        val os = urlConnection.getOutputStream
        val bw = new BufferedWriter(new OutputStreamWriter(os))
        bw.write (s, 0, s.length); bw.flush
      }

      // Read data from server
      Log.i ("Http", "Reading data...")
      val is = urlConnection.getInputStream
      val br = new BufferedReader(new InputStreamReader(is))

      // Reade HTTP code
      code = urlConnection.getResponseCode

      // If the request failed, throw exception
      if (code >= 400) throw new IOException(s"Request to $urlString failed with code $code")

      // Set response
      response = Stream.continually(br.readLine).takeWhile(null !=).toList.mkString("\n")

    // And, at the end, close the connection and return the string
    } finally { urlConnection.disconnect }

    // Return response
    return HttpResponse(code, response)
  }
}

object HttpRequest {
  def post (urlString: String, data: String): HttpRequest = {
    val h = HttpRequest("POST", urlString)
    h.setData (data)
    return h
  }

  def get (urlString: String) = HttpRequest("GET", urlString)
}
