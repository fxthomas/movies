package fx.traktmovies

import android.os.Bundle
import android.app.Activity
import android.app.SearchManager
import android.app.ActionBar
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.ListView
import android.widget.GridView
import android.widget.TextView
import android.widget.ImageView
import android.widget.SearchView
import android.widget.AdapterView
import android.widget.ProgressBar
import android.widget.BaseAdapter
import android.widget.ArrayAdapter
import android.widget.SpinnerAdapter
import android.util.Log
import android.graphics.drawable.Drawable
import android.graphics.Bitmap

import scala.concurrent._
import ExecutionContext.Implicits.global

class MovieViewHolder(v: View) {
  // View fields
  val title = v.findViewById(R.id.movie_title).asInstanceOf[TextView]
  val poster = v.findViewById(R.id.movie_poster).asInstanceOf[ImageView]
  val loved = v.findViewById(R.id.movie_loved).asInstanceOf[ImageView]
  val watched = v.findViewById(R.id.movie_watched).asInstanceOf[ImageView]

  // Registered futures
  var f_poster: Future[Bitmap] = null
}

class MovieAdapter(context: Context, movies: Array[Movie])
  extends ArrayAdapter[Movie](context, R.layout.movie_list_row, movies) {

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    // Retrieve row
    val row = if (convertView != null) convertView else {
      context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
      .asInstanceOf[LayoutInflater]
      .inflate(R.layout.movie_list_row, null)
    }

    // Set view holder
    if (row.getTag == null) { row.setTag (new MovieViewHolder(row)) }
    val holder = row.getTag.asInstanceOf[MovieViewHolder]

    // Set default view parameters
    holder.title.setText(movies(position).title)
    holder.poster.setImageResource(android.R.color.transparent)
    holder.loved.setVisibility(if (movies(position).isLoved) View.VISIBLE else View.GONE) 
    holder.watched.setVisibility(if (movies(position).isWatched) View.VISIBLE else View.GONE)
    holder.f_poster = movies(position).poster

    // Download image (if necessary)
    movies(position).poster onSuccess {
      case bitmap => context.asInstanceOf[DefaultActivity].ui {
        if (holder.f_poster == movies(position).poster)
          holder.poster.setImageBitmap(bitmap)
      }
    }

    return row
  }
}

class MovieListView extends Activity
with DefaultActivity
with SearchView.OnQueryTextListener
with AdapterView.OnItemClickListener
with ActionBar.OnNavigationListener {

  private val dropDownTitlesIfLoggedIn = Array("Trending", "Watchlist")
  private val dropDownTitlesIfNotLoggedIn = Array("Trending")

  var progressView: ProgressBar = null
  var listView: GridView = null
  var searchView: SearchView = null

  override def onCreate(savedInstanceState: Bundle) = {
    // Configure activity
    super.onCreate (savedInstanceState);
    requestWindowFeature (Window.FEATURE_INDETERMINATE_PROGRESS)
    setContentView(R.layout.activity_main)

    // Configure action bar
    getActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST)

    // Configure subviews
    progressView = findViewById(R.id.movie_list_progress).asInstanceOf[ProgressBar]
    listView = findViewById(R.id.movie_list).asInstanceOf[GridView]
    listView.setOnItemClickListener(this)

    // Run default setup
    setupActivity
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    // Inflate menu from XML
    getMenuInflater().inflate(R.menu.activity_main, menu);

    // Configure search view
    searchView = menu.findItem(R.id.menu_search).getActionView().asInstanceOf[SearchView]
    searchView.setOnQueryTextListener(this)

    // Run default setup
    setupOptionsMenu(menu)

    return true;
  }

  override def onLoginStatusChanged(loggedIn: Boolean) = {
    getActionBar.setListNavigationCallbacks(
      new ArrayAdapter(
        this,
        android.R.layout.simple_spinner_dropdown_item,
        if (loggedIn) dropDownTitlesIfLoggedIn
        else dropDownTitlesIfNotLoggedIn
      ),
      this
    )
  }

  def onNavigationItemSelected(itemPosition: Int, itemId: Long): Boolean = {
    itemPosition match {
      case 0 => displayTrendingMovies
      case 1 => displayWatchlistMovies
      case _ => ()
    }
    return true
  }

  def getDropDownView(position: Int, convertView: View, parent: ViewGroup): View = {
    val tv = new TextView(this)
    tv.setText (
      if (Trakt.isLoggedIn) dropDownTitlesIfLoggedIn(position)
      else dropDownTitlesIfNotLoggedIn(position)
    )
    return tv
  }

  def displayMovies(f_movies: Future[Array[Movie]]) = {
    MovieListView.movies = null
    progressView.setVisibility(View.VISIBLE)
    listView.setVisibility(View.GONE)

    f_movies onSuccess {
      case m => {
        Log.i ("MovieListView", s"Got movies: " + m.map(_.rating.toString).mkString(","))
        ui {
          MovieListView.movies = m
          listView.setAdapter(new MovieAdapter(this,m))
          listView.setVisibility(View.VISIBLE)
          progressView.setVisibility(View.GONE)
        }
      }
    }
    f_movies onFailure {
      case e: Exception => {
        Log.i("Future-Exception", e.toString)
        ui {
          listView.setAdapter(null)
          listView.setVisibility(View.VISIBLE)
          progressView.setVisibility(View.GONE)
          error("Trakt is unavailable").show
        }
      }
    }
  }

  def searchMovie(movie: String) =
    displayMovies (future { Trakt.searchMovie(movie) })

  def displayTrendingMovies() =
    displayMovies (future { Trakt.trendingMovies })

  def displayWatchlistMovies() =
    displayMovies (future { Trakt.watchlistMovies })
  
  def handleIntent(intent: Intent) = {
    if (Intent.ACTION_SEARCH equals (intent.getAction())) {
      searchMovie (intent.getStringExtra (SearchManager.QUERY))
    }
  }

  def onQueryTextChange(text: String) = { true }
  def onQueryTextSubmit(text: String) = {
    // Hide keyboard
    getSystemService(Context.INPUT_METHOD_SERVICE)
    .asInstanceOf[InputMethodManager]
    .hideSoftInputFromWindow(searchView.getWindowToken(), 0)

    // Search for movie
    searchMovie(text)

    // And return true
    true
  }

  def onItemClick (parent: AdapterView[_], view: View, position: Int, id: Long) = {
    if (MovieListView.movies != null) {
      MovieListView.movies(position).poster onSuccess {
        case poster => {
          val intent = new Intent(this, classOf[MovieInfoView])
          intent.setAction (Intent.ACTION_VIEW)
          intent.putExtra ("movie", position)
          startActivity (intent)
        }
      }
    }
  }
}

object MovieListView {
  var movies: Array[Movie] = null
}
