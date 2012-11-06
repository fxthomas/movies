package fx.traktmovies

import android.os.Bundle
import android.os.AsyncTask
import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.Window
import android.widget.ListView
import android.widget.GridView
import android.widget.TextView
import android.widget.ImageView
import android.widget.SearchView
import android.widget.ArrayAdapter
import android.util.Log
import android.graphics.drawable.Drawable
import android.graphics.Bitmap

import scala.concurrent._
import ExecutionContext.Implicits.global

object Util {
  def ui(activity: Activity)(fun: => Unit) = {
      activity.runOnUiThread(new Runnable() { def run() = fun })
  }
}

class MovieViewHolder(v: View) {
  // View fields
  val title = v.findViewById(R.id.movie_title).asInstanceOf[TextView]
  val poster = v.findViewById(R.id.movie_poster).asInstanceOf[ImageView]

  // Registered futures
  var f_poster: Future[Bitmap] = null
}

class MovieAdapter(context: Context, movies: Array[Movie])
  extends ArrayAdapter[Movie](context, R.layout.movie_list_row, movies) {
  import Util._

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
    holder.f_poster = movies(position).poster

    // Download image (if necessary)
    movies(position).poster onSuccess {
      case bitmap => ui(context.asInstanceOf[Activity]) {
        if (holder.f_poster == movies(position).poster)
          holder.poster.setImageBitmap(bitmap)
      }
    }

    return row
  }
}

class MovieList extends Activity with SearchView.OnQueryTextListener {
  import Util._

  def searchMovie(movie: String) = {
    val listView: GridView = findViewById(R.id.movie_list).asInstanceOf[GridView]
    val movies = future { Trakt.searchMovie(movie) }
    movies onSuccess {
      case movies_list => {
        ui(this) { listView.setAdapter(new MovieAdapter(this,movies_list.toArray)) }
      }
    }
    movies onFailure {
      case e: Exception => { Log.i("Future-Exception", e.getMessage) }
    }
  }
  
  def handleIntent(intent: Intent) = {
    Log.i ("MovieList", "Handling intent " + intent.getAction())
    if (Intent.ACTION_SEARCH equals (intent.getAction())) {
      searchMovie (intent.getStringExtra (SearchManager.QUERY))
    }
  }

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate (savedInstanceState);
    getWindow().requestFeature (Window.FEATURE_INDETERMINATE_PROGRESS)
    setContentView(R.layout.activity_main)
    handleIntent(getIntent())
  }

  override def onNewIntent(intent: Intent) = { setIntent(intent); handleIntent(intent) }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    // Inflate menu from XML
    getMenuInflater().inflate(R.menu.activity_main, menu);

    // Configure search view
    val searchView = menu.findItem(R.id.menu_search).getActionView().asInstanceOf[SearchView]
    searchView.setOnQueryTextListener(this)
    return true;
  }

  def onQueryTextChange(text: String) = { true }
  def onQueryTextSubmit(text: String) = {
    searchMovie(text)
    true
  }
}
