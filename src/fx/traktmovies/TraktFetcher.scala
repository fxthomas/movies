package fx.traktmovies

import spray.json._

import scala.concurrent._
import ExecutionContext.Implicits.global

import android.graphics.Bitmap
import android.util.Log

/**
 * Default object to add before parsing JSON from Trakt
 */
object Additions {
  val API_KEY = "93f34432220b4d372501cff4f0caeb9c"
}

/**
 * Case class containing movie data
 */
case class Movie (title: String, year: Long, images: Map[String, String]) {
  // Convert movie to string
  override def toString() = title + " (" + year + ")"

  // Future for the poster image
  lazy val poster: Future[Bitmap] = future {
    blocking {
      BitmapDecoder.download (images("poster"), 48, 48)
    }
  }
}

object MovieJsonProtocol extends DefaultJsonProtocol {
  implicit val movieFormat = jsonFormat(Movie, "title", "year", "images")
}

/**
 * Contains basic Trakt operations
 */
object Trakt {
  import Additions._
  import MovieJsonProtocol._

  /**
   * Fetch movie info
   */
  def movie (imdb: String): Movie = {
    val enc = java.net.URLEncoder.encode (imdb, "UTF-8");
    val res = scala.io.Source.fromURL ("http://api-trakt.apigee.com/movie/summary.json/" + API_KEY + "/" + enc).mkString;
    res.asJson.convertTo[Movie]
  }
  /**
   * Search for movie
   */
  def searchMovie (name: String): List[Movie] = {
    val enc = java.net.URLEncoder.encode (name, "UTF-8");
    val res = scala.io.Source.fromURL ("http://api-trakt.apigee.com/search/movies.json/" + API_KEY + "/" + enc).mkString;
    res.asJson.convertTo[List[Movie]]
  }
}
