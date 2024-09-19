package main.database.utils.filters

import ImageData

class DefBlackListFilter: BlacklistFilter {
    private val hashFilter = SameHashFilter(300)

    override fun filter(data: ImageData, values: HashSet<ImageData>): Boolean {
        return !hashFilter.fastFilter(data, values)
    }
}