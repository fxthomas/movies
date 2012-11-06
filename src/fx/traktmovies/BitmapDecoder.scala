package fx.traktmovies

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.URL
import java.io.InputStream
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream

object BitmapDecoder {
  def toByteArray(input: InputStream): Array[Byte] = {
    val out = new ByteArrayOutputStream(1024)
    val buf = new Array[Byte](512)
    Stream.continually(input.read(buf)).takeWhile(-1 !=).foreach(out.write(buf, 0, _))
    return out.toByteArray
  }

  def calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int = {
    // Raw height and width of image
    val height = options.outHeight.asInstanceOf[Float]
    val width = options.outWidth.asInstanceOf[Float]

    if (height > reqHeight || width > reqWidth) {
      if (width > height) Math.round(height / reqHeight.asInstanceOf[Float])
      else Math.round(width / reqWidth.asInstanceOf[Float])
    } else 1
  }

  def download(urlString: String): Bitmap = {
    val image_stream = new BufferedInputStream(new URL(urlString).getContent().asInstanceOf[InputStream])
    val image_buffer = toByteArray(image_stream)
    return BitmapFactory.decodeByteArray (image_buffer, 0, image_buffer.length)
  }

  def download(urlString: String, reqWidth: Int, reqHeight: Int): Bitmap = {
    // Create input stream from URL
    val image_stream = new BufferedInputStream(new URL(urlString).getContent().asInstanceOf[InputStream])
    val image_buffer = toByteArray(image_stream)

    // First decode with inJustDecodeBounds=true to check dimensions
    val options = new BitmapFactory.Options()
    options.inJustDecodeBounds = true

    // Decode header, and reset the input stream to the beginning
    BitmapFactory.decodeByteArray (image_buffer, 0, image_buffer.length, options)

    // Calculate inSampleSize
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

    // Decode bitmap with inSampleSize set
    options.inJustDecodeBounds = false
    return BitmapFactory.decodeByteArray (image_buffer, 0, image_buffer.length, options)
  }
}
