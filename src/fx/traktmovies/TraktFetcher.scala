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
case class Movie (title: String, imdb_id: String, year: Long, images: Map[String, String], overview: String) {
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
  implicit val movieFormat = jsonFormat(Movie, "title", "imdb_id", "year", "images", "overview")
}

/**
 * Contains basic Trakt operations
 */
object Trakt {
  import MovieJsonProtocol._
  import Util._

  var authHash: Option[String] = None

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
    val data = s"""{"movies":[{"imdb_id":"${movie.imdb_id}"}]}"""
    val res = HttpRequest.post(url("movie/seen"), data)
    for (h <- authHash) res.setHeader("Authorization", s"Basic $h")
    res.send
  }

  def login (username: String, password: String) = {
    authHash = Some(Base64.encodeToString(s"$username:$password".getBytes, Base64.NO_WRAP))
  }

  def login (hash: String) = { authHash = Some(hash) }

  def logout () = { authHash = None }
}
