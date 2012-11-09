package fx.traktmovies

import android.app.Activity
import android.content.SharedPreferences
import android.content.Context
import android.util.Log
import android.widget.Toast

import scala.concurrent._
import ExecutionContext.Implicits.global

class Configuration(context: Context)(implicit apiKey: String) {

  /**
   * Shared preferences used by the application
   */
  val sharedPreferences = context.getSharedPreferences("trakt", 0)

  /**
   * Remove a key in the preferences
   */
  def remove(key: String) =
    sharedPreferences.edit.remove(key).commit

  /**
   * Sets a String value in the preferences
   */
  def setString(key: String, value: String) =
    sharedPreferences.edit.putString(key,value).commit

  /**
   * Gets a string value in the preferences
   */
  def getString(key: String) =
    Option(sharedPreferences.getString(key,null))

  /**
   * Login
   */
  def login(username: String, password: String) = {
    // Remove previously saved login info
    remove ("auth_hash")

    // Try logging in, and test connection
    val logged_in = future { Trakt.login(username, password) }

    // If it succeeded, save the login info, and inform the user
    logged_in onSuccess {
      case hash => {
        context.asInstanceOf[DefaultActivity].ui {
          setString("auth_hash", hash)
          Toast.makeText(context, "Logged in", Toast.LENGTH_SHORT).show
        }
      }
    }

    // If it failed, warn the user anyway
    logged_in onFailure {
      case e => {
        context.asInstanceOf[DefaultActivity].ui {
          Log.w ("Configuration", "Login failed: " + e.getMessage)
          Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show
        }
      }
    }

    // Return the future if we need to do something else afterwards
    logged_in
  }

  /**
   * Logout
   */
  def logout() = {
    remove ("auth_hash")
    Trakt.logout
    Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show
  }

  /**
   * Restore login, if available
   */
  def restore_login() = {
    getString("auth_hash").foreach(Trakt.login(_, false))
    Trakt.isLoggedIn
  }

  def apply = getString _
}
