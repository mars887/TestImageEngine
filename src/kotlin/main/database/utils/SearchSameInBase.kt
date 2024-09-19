package main.database.utils

import ImageData
import main.database.DataBase
import main.database.utils.filters.SameHashFilter

class SearchSameInBase {
    companion object {
        fun searchBy(source: ImageData, db: DataBase): ImageData {
            val list = HashMap<Int, ImageData>()

            db.database.forEach { (_, data) ->
                list.put(
                    SameHashFilter.calculateDiffBetween(source, data),
                    data
                )
            }
            return list.minBy { it.key }.value
        }
    }
}