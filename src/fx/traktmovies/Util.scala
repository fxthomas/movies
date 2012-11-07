package fx.traktmovies

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface

object Util {
  def ui(activity: Activity)(fun: => Unit) = {
      activity.runOnUiThread(new Runnable() { def run() = fun })
  }

  def error(context: Context, message: String) = {
    val alertbox = new AlertDialog.Builder(context)
    alertbox.setMessage(message)
    alertbox.setNeutralButton("OK", new DialogInterface.OnClickListener() {
      def onClick(arg0: DialogInterface, arg1: Int) = {}
    })
    alertbox
  }
}
