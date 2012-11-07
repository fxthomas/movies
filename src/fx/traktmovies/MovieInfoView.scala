package fx.traktmovies

import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import android.util.Log
import android.graphics.Bitmap

import scala.concurrent._
import ExecutionContext.Implicits.global

class MovieInfoView extends Activity {
  import Util._
  var posterView: ImageView = null
  var titleView: TextView = null
  var descriptionView: TextView = null
  var movie: Movie = null

  override def onCreate(savedInstanceState: Bundle) = {
    // Configure activity
    super.onCreate (savedInstanceState);
    setContentView(R.layout.activity_movieinfo)

    // Configure subviews
    posterView = findViewById(R.id.movie_info_poster).asInstanceOf[ImageView]
    titleView = findViewById(R.id.movie_info_title).asInstanceOf[TextView]
    descriptionView = findViewById(R.id.movie_info_description).asInstanceOf[TextView]

    // Handle intent, if necessary
    handleIntent(getIntent())
  }

  override def onNewIntent(intent: Intent) = { setIntent(intent); handleIntent(intent) }

  def updateMovie(movieIndex: Int) = {
    // If the movie index exists
    if (movieIndex >= 0 && movieIndex < MovieListView.movies.length) {
      // Retrieve movie from static storage
      movie = MovieListView.movies(movieIndex)

      // Set views
      titleView.setText(movie.title)
      descriptionView.setText(movie.overview)
      movie.poster onSuccess { case b => posterView.setImageBitmap(b) }

      // Log info
      Log.i ("MovieInfoView", s"Update with $movieIndex: ${movie.title}")

    // If the movie index does not exist
    } else Log.w("MovieInfoView", s"Wrong movie id ($movieIndex)")
  }

  def handleIntent(intent: Intent) = {
    if (Intent.ACTION_VIEW equals (intent.getAction())) {
      updateMovie (intent.getIntExtra("movie", -1))
    }
  }
}
