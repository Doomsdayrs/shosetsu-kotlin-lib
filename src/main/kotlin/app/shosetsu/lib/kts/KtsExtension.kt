package app.shosetsu.lib.kts

import app.shosetsu.lib.Filter
import app.shosetsu.lib.IExtension
import app.shosetsu.lib.Novel
import de.swirtz.ktsrunner.objectloader.KtsObjectLoader
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document
import java.io.File
import kotlin.reflect.KClass

/**
 * shosetsu-services
 * 06 / 10 / 2020
 */
class KtsExtension(private val content: String) : IExtension {
	companion object {
		private val acceptedImports: List<String?> = arrayOf<KClass<*>>(
				JSONObject::class,
				JSONArray::class,
				Document::class
		).map { it.qualifiedName }
	}

	private val iExtension: IExtension;

	constructor(file: File) : this(file.readText())

	init {
		content
				.split("\n")
				.filter { it.length > 5 && it.substring(0..5) == "import" }
				.map { it.removePrefix("import ") }
				.filterNot { it.startsWith("app.shosetsu.lib") }.forEach { packageName ->
					if (acceptedImports.any { it == packageName }) return@forEach
					throw IllegalAccessException("KTS Attempting to access out of spec library: `$packageName`")
				}

		iExtension = KtsObjectLoader().load(content)
	}

	override val metaData: JSONObject
		get() = iExtension.metaData


	override val name: String
		get() = iExtension.name

	override val baseURL: String
		get() = iExtension.baseURL

	override val imageURL: String
		get() = iExtension.imageURL

	override val formatterID: Int
		get() = iExtension.formatterID

	override val hasSearch: Boolean
		get() = iExtension.hasSearch

	override val isSearchIncrementing: Boolean
		get() = iExtension.hasSearch

	override val hasCloudFlare: Boolean
		get() = iExtension.hasCloudFlare

	override val listings: Array<IExtension.Listing>
		get() = iExtension.listings

	override val settingsModel: Array<Filter<*>>
		get() = iExtension.settingsModel

	override val searchFiltersModel: Array<Filter<*>>
		get() = iExtension.searchFiltersModel

	override val chapterType: Novel.ChapterType
		get() = iExtension.chapterType

	override fun updateSetting(id: Int, value: Any?) =
			iExtension.updateSetting(id, value)

	override fun search(data: Map<Int, *>): Array<Novel.Listing> =
			iExtension.search(data)

	override fun getPassage(chapterURL: String): String =
			iExtension.getPassage(chapterURL)

	override fun parseNovel(novelURL: String, loadChapters: Boolean): Novel.Info =
			iExtension.parseNovel(novelURL, loadChapters)

	override fun expandURL(smallURL: String, type: Int): String =
			iExtension.expandURL(smallURL, type)

	override fun shrinkURL(longURL: String, type: Int): String =
			iExtension.shrinkURL(longURL, type)
}