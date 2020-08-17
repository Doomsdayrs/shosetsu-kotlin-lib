package app.shosetsu.lib

import org.json.JSONException
import org.json.JSONObject
import org.luaj.vm2.LuaString.EMPTYSTRING
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaValue.*
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua.coerce
import org.luaj.vm2.lib.jse.CoerceLuaToJava
import java.io.File
import java.io.IOException

/*
 * This file is part of shosetsu-services.
 * shosetsu-services is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * shosetsu-services is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with shosetsu-services.  If not, see https://www.gnu.org/licenses/.
 */

/**
 * shosetsu-kotlin-lib
 * 16 / 01 / 2020
 *
 * @param content extension script
 */
class LuaFormatter(val content: String) : Formatter {
	companion object {
		/**
		 * Values that may not be present
		 */
		val defaults: Map<String, LuaValue> = mapOf(
				"imageURL" to EMPTYSTRING,
				"hasCloudFlare" to FALSE,
				"hasSearch" to TRUE,
				"searchFilters" to LuaTable(),
				"settings" to LuaTable()
		)

		/**
		 * Values that must be present
		 */
		val hardKeys: Map<String, Int> = mapOf(
				"id" to TNUMBER,
				"name" to TSTRING,
				"listings" to TTABLE,
				"getPassage" to TFUNCTION,
				"parseNovel" to TFUNCTION
		)

		/**
		 * What is unique about soft keys is they are conditional on the soft key beforehand
		 * IE, if hasSearch is false, then search does not need to be present in script
		 */
		val softKeys: Map<String, Pair<Pair<String, Int>, (LuaValue) -> Boolean>> = mapOf(
				"hasSearch" to Pair(Pair("search", TFUNCTION), { v -> (v == TRUE) }),
				"settings" to Pair(Pair("updateSetting", TFUNCTION), { v -> (v as LuaTable).length() != 0 })
		)

		/***/
		@Deprecated("Moved to globals", replaceWith = ReplaceWith("QUERY_INDEX"))
		const val FILTER_POSITION_QUERY: Int = 0

		private fun makeLuaReporter(f: (status: String) -> Unit) = object : OneArgFunction() {
			override fun call(p0: LuaValue?): LuaValue {
				f(p0!!.tojstring())
				return LuaValue.NIL
			}
		}

		private fun tableToFilters(table: LuaTable): Array<Filter<*>> =
				table.keys().map { table[it] }.filter { !it.isnil() }
						.map { CoerceLuaToJava.coerce(it, Any::class.java) as Filter<*> }.toTypedArray()


	}

	constructor(file: File) : this(file.readText())

	/**
	 * Returns the metadata that is at the header of the extension
	 */
	@Suppress("unused")
	fun getMetaData(): JSONObject? = try {
		JSONObject(content.substring(0, content.indexOf("\n")).replace("--", "").trim())
	} catch (e: JSONException) {
		e.printStackTrace(); null
	} catch (e: IOException) {
		e.printStackTrace(); null
	}

	private val source: LuaTable

	init {
		val globals = shosetsuGlobals()
		val l = try {
			globals.load(content)!!
		} catch (e: Error) {
			throw e
		}
		source = l.call() as LuaTable

		// If the modified default value doesnt exist, it assigns the default
		defaults.filter { source[it.key].isnil() }.forEach { source[it.key] = it.value }

		// If any required value is not found, it throws an NullPointerException
		with(hardKeys.filter { source.get(it.key).type() != it.value }.map { it.key }) {
			if (isNotEmpty())
				throw NullPointerException(
						"LuaScript has missing or invalid:" + fold("", { a, s -> "$a\n\t\t$s;" })
				)
		}

		// If any of the softKeys matching their condition of requirement are not found, it throws an NullPointerException
		with(softKeys.filter {
			val t = it.value.first
			if (it.value.second(source[it.key])) {
				source.get(t.first).type() != t.second
			} else false
		}.map { it.value.first.first }) {
			if (isNotEmpty())
				throw NullPointerException(
						"LuaScript has missing or invalid:" + fold("", { a, s -> "$a\n\t\t$s;" })
				)
		}

	}

	override val name: String by lazy { source["name"].tojstring() }
	override val baseURL: String by lazy { source["baseURL"].tojstring() }
	override val formatterID by lazy { source["id"].toint() }
	override val imageURL: String by lazy { source["imageURL"].tojstring() }
	override val hasCloudFlare by lazy { source["hasCloudFlare"].toboolean() }
	override val hasSearch by lazy { source["hasSearch"].toboolean() }

	@Suppress("UNCHECKED_CAST")
	override val listings: Array<Formatter.Listing> by lazy {
		coerceLuaToJava<Array<Formatter.Listing>>(source["listings"])
	}

	@Suppress("UNCHECKED_CAST")
	override val searchFiltersModel: Array<Filter<*>> by lazy {
		tableToFilters(source["searchFilters"] as LuaTable)
	}

	@Suppress("UNCHECKED_CAST")
	override val settingsModel: Array<Filter<*>> by lazy {
		tableToFilters(source["settings"] as LuaTable)
	}

	override fun updateSetting(id: Int, value: Any?) {
		source["updateSetting"].takeIf { it.type() == TFUNCTION }?.call(valueOf(id), coerce(value)) ?: return
	}

	override fun getPassage(chapterURL: String): String =
			source["getPassage"].call(expandURL(chapterURL, 1)).tojstring()

	override fun parseNovel(novelURL: String, loadChapters: Boolean, reporter: (status: String) -> Unit): Novel.Info =
			coerceLuaToJava(source["parseNovel"].call(
					valueOf(expandURL(novelURL, 1)),
					valueOf(loadChapters),
					makeLuaReporter(reporter)
			))

	@Suppress("UNCHECKED_CAST")
	override fun search(data: Map<Int, *>, reporter: (status: String) -> Unit): Array<Novel.Listing> =
			coerceLuaToJava(source["search"].call(
					data.toLua(),
					makeLuaReporter(reporter)
			))

	override fun expandURL(smallURL: String, type: Int): String {
		val f = source["expandURL"]
		if (f.type() != TFUNCTION) return smallURL
		return f.call(valueOf(smallURL), valueOf(type)).tojstring()
	}

	override fun shrinkURL(longURL: String, type: Int): String {
		val f = source["shrinkURL"]
		if (f.type() != TFUNCTION) return longURL
		return f.call(valueOf(longURL), valueOf(type)).tojstring()
	}
}