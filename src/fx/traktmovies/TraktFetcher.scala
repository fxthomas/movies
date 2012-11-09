package fx.traktmovies

import spray.json._

import sun.misc.BASE64Encoder

import scala.concurrent._
import ExecutionContext.Implicits.global

import android.graphics.Bitmap
import android.util.Log
import android.util.Base64

/**
 * Case class containing movie data
 */
case class Movie (
  title: String,
  imdb_id: Option[String],
  tmdb_id: Option[String],
  year: Long,
  images: Map[String, String],
  overview: Option[String]) {

  // Convert movie to string
  override def toString() = title + " (" + year + ")"

  // Future for the poster image
  lazy val poster: Future[Bitmap] = future {
    blocking {
      val poster_small = images("poster").replaceFirst("(\\.[^.]*)?$", "-300$0")
      BitmapDecoder.download (poster_small, 300, 450)
    }
  }
}

object MovieJsonProtocol extends DefaultJsonProtocol {
  implicit val movieFormat = jsonFormat(Movie, "title", "imdb_id", "tmdb_id", "year", "images", "overview")
}

/**
 * Contains basic Trakt operations
 */
object Trakt {
  import MovieJsonProtocol._

  var authHash: Option[String] = None
  var isLoggedIn = false

  /**
   * Returns the Trakt API URL corresponding to the resource
   */
  def url (resource: String, params: String="")(implicit apiKey: String) =
    s"http://api-trakt.apigee.com/$resource/$apiKey" + (if (params != "") s"/$params" else "")

  /**
   * Fetch movie info
   */
  def movie (imdb: String)(implicit apiKey: String) = {
    val enc = java.net.URLEncoder.encode (imdb, "UTF-8")
    val res = HttpRequest.get(url("movie/summary.json", enc))
    for (h <- authHash) res.setHeader("Authorization", s"Basic $h")
    res.send.message.asJson.convertTo[Movie]
  }

  /**
   * Search for movie
   */
  def searchMovie (name: String)(implicit apiKey: String) = {
    val enc = java.net.URLEncoder.encode (name, "UTF-8")
    val res = HttpRequest.get(url("search/movies.json", enc))
    for (h <- authHash) res.setHeader("Authorization", s"Basic $h")
    res.send.message.asJson.convertTo[Array[Movie]]
  }

  def mark_seen (movie: Movie)(implicit apiKey: String) = {
    val data =
      if(movie.imdb_id.isEmpty)
        s"""{"movies":[{"tmdb_id":"${movie.tmdb_id.get}"}]}"""
      else
        s"""{"movies":[{"imdb_id":"${movie.imdb_id.get}"}]}"""

    val res = HttpRequest.post(url("movie/seen"), data)
    for (h <- authHash) res.setHeader("Authorization", s"Basic $h")
    res.send
  }

  def login (hash: String, testLogin: Boolean = true)(implicit apiKey: String): String = {
    if (testLogin) {
      // Create login test request
      val res = HttpRequest.get(url("account/test"))

      // Set auth header
      res.setHeader ("Authorization", s"Basic $hash")

      // Send request
      res.send
    }

    // Prepare variables
    authHash = Option(hash)
    isLoggedIn = true

    // Return the hash for future use
    hash
  }

  def login (username: String, password: String)(implicit apiKey: String): String =
    login(Base64.encodeToString(s"$username:$password".getBytes, Base64.NO_WRAP), true)

  def logout () = { authHash = None; isLoggedIn = false }
}
