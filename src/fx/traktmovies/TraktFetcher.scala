package fx.traktmovies

import spray.json._

import sun.misc.BASE64Encoder

import scala.concurrent._
import ExecutionContext.Implicits.global

import android.graphics.Bitmap
import android.util.Log
import android.util.Base64

/**
 * Trakt ratings
 */
sealed abstract class Rating
case object Love extends Rating
case object Hate extends Rating
case object NotRated extends Rating

/**
 * Case class containing movie data
 */
case class Movie (
  title: String,
  imdb_id: Option[String],
  tmdb_id: Option[String],
  watched: Option[Boolean],
  rating: Option[Rating],
  year: Long,
  images: Map[String, String],
  overview: Option[String]) {

  // Convert movie to string
  override def toString() = title + " (" + year + ")"

  // Convenience
  val isWatched = watched getOrElse false
  val isLoved = rating match { case Some(Love) => true; case _ => false }
  val isHated = rating match { case Some(Hate) => true; case _ => false }

  // Future for the poster image
  lazy val poster: Future[Bitmap] = future {
    blocking {
      val poster_small = images("poster").replaceFirst("(\\.[^.]*)?$", "-300$0")
      BitmapDecoder.download (poster_small, 300, 450)
    }
  }
}

object MovieJsonProtocol extends DefaultJsonProtocol {
  implicit val movieFormat = jsonFormat(Movie, "title", "imdb_id", "tmdb_id", "watched", "rating", "year", "images", "overview")
  implicit object ratingFormat extends RootJsonFormat[Rating] {
    def write(c: Rating) = c match {
      case Love => JsString("love")
      case Hate => JsString("hate")
      case _ => JsFalse
    }

    def read(value: JsValue) = value match {
      case JsString("love") => Love
      case JsString("hate") => Hate
      case JsFalse => NotRated
      case _ => deserializationError ("Expected love, hate or false")
    }
  }
}

/**
 * Contains basic Trakt operations
 */
object Trakt {
  import MovieJsonProtocol._

  var authHash: Option[String] = None
  var authUsername: Option[String] = None
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

  /**
   * Trending movies
   */
  def trendingMovies ()(implicit apiKey: String) = {
    val res = HttpRequest.get(url("movies/trending.json"))
    for (h <- authHash) res.setHeader("Authorization", s"Basic $h")
    res.send.message.asJson.convertTo[Array[Movie]]
  }

  /**
   * Get user watchlist
   */
  def watchlistMovies ()(implicit apiKey: String): Array[Movie] = {
    authUsername map { case n => {
      val enc = java.net.URLEncoder.encode (n, "UTF-8")
      val res = HttpRequest.get(url("user/watchlist/movies.json", enc))
      for (h <- authHash) res.setHeader("Authorization", s"Basic $h")
      res.send.message.asJson.convertTo[Array[Movie]]
    }} getOrElse Array()
  }

  /**
   * Mark a movie as seen
   */
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

  def login_hash (username: String, hash: String)(implicit apiKey: String): String = {
    authUsername = Option(username)
    authHash = Option(hash)
    isLoggedIn = !authUsername.isEmpty && !authHash.isEmpty
    hash
  }

  def login (username: String, password: String)(implicit apiKey: String): String = {
    // Create login test request
    val hash = Base64.encodeToString(s"$username:$password".getBytes, Base64.NO_WRAP)
    val res = HttpRequest.get(url("account/test"))
    res.setHeader ("Authorization", s"Basic $hash")
    res.send

    // And login with hash if everything's okay
    login_hash(username, hash)
  }

  def logout () = { authHash = None; isLoggedIn = false }
}
