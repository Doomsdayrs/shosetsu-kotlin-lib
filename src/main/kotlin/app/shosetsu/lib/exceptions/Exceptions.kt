package app.shosetsu.lib.exceptions

/*
 * This file is part of shosetsu-kotlin-lib.
 * shosetsu-kotlin-lib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * shosetsu-kotlin-lib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with shosetsu-kotlin-libs.  If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * Thrown when the HTTP result code is not 200
 */
class HTTPException(
	@Suppress("MemberVisibilityCanBePrivate")
	val code: Int
) : Exception("$code")

class JsonMissingKeyException(
	@Suppress("MemberVisibilityCanBePrivate")
	val obj: String,
	@Suppress("MemberVisibilityCanBePrivate")
	val key: String
) : Exception("$obj is missing $key")

class InvalidFilterIDException(
	@Suppress("MemberVisibilityCanBePrivate")
	val debugName: String,
	@Suppress("MemberVisibilityCanBePrivate")
	val filterID: Int
) : Exception("$debugName using invalid filterID {$filterID}")

class MissingOrInvalidKeysException(
	@Suppress("MemberVisibilityCanBePrivate")
	val debugName: String,
	@Suppress("MemberVisibilityCanBePrivate")
	val keys: Array<String>
) : Exception(
	"$debugName has missing or invalid keys: ${keys.contentToString()}"
) {

	constructor(
		debugName: String,
		keys: List<String>
	) : this(
		debugName,
		keys.toTypedArray()
	)

}

class MissingExtensionLibrary(
	@Suppress("MemberVisibilityCanBePrivate")
	val library: String
) : Exception("Missing $library")