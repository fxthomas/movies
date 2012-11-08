package fx.traktmovies

import android.os.Bundle
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import android.util.Log
import android.graphics.Bitmap

import scala.concurrent._
import ExecutionContext.Implicits.global

class MovieInfoView extends Activity {
  import Util._
  import Configuration._

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

    // Login to trakt, if possible
    Trakt.login (getSharedPreferences("trakt",0).getString("auth_hash", null))

    // Handle intent, if necessary
    handleIntent(getIntent())
  }

  
  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    // Inflate menu from XML
    getMenuInflater().inflate(R.menu.activity_movieinfo, menu);

    // Configure search view
    val markSeenMenuItem = menu.findItem(R.id.menu_mark_seen).asInstanceOf[MenuItem]
    markSeenMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
      override def onMenuItemClick(item: MenuItem) = {
        // Create future
        val f = future { Trakt.mark_seen(movie) }

        // On success, show it to the user
        f onSuccess { case e => 
          ui(MovieInfoView.this) {
            Toast.makeText(MovieInfoView.this, "Marked as seen", Toast.LENGTH_SHORT).show
          }
        }

        // If it failed, tell the user as well, and log the exception
        f onFailure { case e =>
          ui(MovieInfoView.this) {
            Toast.makeText(MovieInfoView.this, "Unable to reach Trakt", Toast.LENGTH_SHORT).show
            Log.w ("MovieInfoView", "Unable to reach Trakt: " + e.getMessage)
          }
        }
        true
      }
    })

    val loginMenuItem = menu.findItem(R.id.menu_login).asInstanceOf[MenuItem]
    loginMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
      override def onMenuItemClick(item: MenuItem) = {
        if (Trakt.isLoggedIn) Trakt.logout
        else (new TraktLoginDialogFragment(b => {
          loginMenuItem.setTitle(if(b) "Logout" else "Login")
        })).show(getFragmentManager, "dialog")
        true
      }
    })

    if (Trakt.isLoggedIn) loginMenuItem.setTitle ("Logout")
    else loginMenuItem.setTitle ("Login")

    return true;
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

    // If the movie index does not exist
    } else Log.w("MovieInfoView", s"Wrong movie id ($movieIndex)")
  }

  def handleIntent(intent: Intent) = {
    if (Intent.ACTION_VIEW equals (intent.getAction())) {
      updateMovie (intent.getIntExtra("movie", -1))
    }
  }
}
