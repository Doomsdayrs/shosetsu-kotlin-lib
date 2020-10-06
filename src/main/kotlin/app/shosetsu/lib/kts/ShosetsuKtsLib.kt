package app.shosetsu.lib.kts

import app.shosetsu.lib.HTTPException
import app.shosetsu.lib.ShosetsuSharedLib
import okhttp3.*
import okhttp3.internal.closeQuietly
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.InputStream
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

/**
 * shosetsu-kotlin-lib
 * 06 / 10 / 2020
 */
object ShosetsuKtsLib {

	val okHttpClient: OkHttpClient
		get() = ShosetsuSharedLib.httpClient

	fun DEFAULT_CACHE_CONTROL(): CacheControl = CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build()
	fun DEFAULT_HEADERS(): Headers = Headers.Builder().build()
	fun DEFAULT_BODY(): RequestBody = FormBody.Builder().build()


	// For normal extensions, these simple functions are sufficient.
	fun _GET(url: String, headers: Headers, cacheControl: CacheControl): Request =
			Request.Builder().url(url).headers(headers).cacheControl(cacheControl).build()

	fun _POST(url: String, headers: Headers, body: RequestBody, cacheControl: CacheControl): Request =
			Request.Builder().url(url).post(body).headers(headers).cacheControl(cacheControl).build()


	fun Document(str: String): Document = Jsoup.parse(str)!!
	fun Request(req: Request): Response = ShosetsuSharedLib.httpClient.newCall(req).execute()
	fun RequestDocument(req: Request): Document = Document(
			Request(req).let { r ->
				r.takeIf { it.code == 200 }?.body?.string() ?: {
					r.closeQuietly()
					throw HTTPException(r.code)
				}()
			}
	)

	fun GETDocument(url: String): Document = RequestDocument(_GET(url, DEFAULT_HEADERS(), DEFAULT_CACHE_CONTROL()))


}