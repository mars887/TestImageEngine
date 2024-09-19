package main.database.utils.filters

import ImageData

interface BlacklistFilter {
    fun filter(data: ImageData,values: HashSet<ImageData>): Boolean
}
