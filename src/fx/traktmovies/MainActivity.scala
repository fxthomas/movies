package fx.traktmovies

import android.os.Bundle
import android.os.AsyncTask
import android.app.Activity
import android.content.Context
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.ListView
import android.widget.TextView
import android.widget.ArrayAdapter
import android.util.Log
import android.graphics.drawable.Drawable

import scala.concurrent._
import ExecutionContext.Implicits.global

object Util {
  def ui(activity: Activity)(fun: => Unit) = {
      activity.runOnUiThread(new Runnable() { def run() = fun })
  }
}

class MovieAdapter(context: Context, movies: Array[Movie])
  extends ArrayAdapter[Movie](context, R.layout.movie_list_row, movies) {
  import Util._

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater]
    val row: View = inflater.inflate(R.layout.movie_list_row, parent, false)
    val field_title: TextView = row.findViewById(R.id.movie_title).asInstanceOf[TextView]
    val field_poster: View = row.findViewById(R.id.movie_poster)

    // Set text
    field_title.setText(movies(position).title)

    // Set image
    movies(position).posterImage onSuccess {
      case d => ui(context.asInstanceOf[Activity]) { field_poster.setBackgroundDrawable(d) }
    }

    return row
  }
}

class MainActivity extends Activity {
  import Util._

  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate (savedInstanceState);
    setContentView(R.layout.activity_main);

    val listView: ListView = findViewById(R.id.movie_list).asInstanceOf[ListView]
    val movies = future {
      Log.i ("future{}", "Sending request");
      Trakt.searchMovie("spider-man");
    }

    movies onSuccess {
      case movies_list => ui(this) { listView.setAdapter(new MovieAdapter(this,movies_list.toArray)) }
    }

    movies onFailure {
      case e: Exception => Log.i("MainActivity", e.getMessage)
    }
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }
}
