package main.database.utils.filters

import ImageData
import kotlin.math.abs

class SameHashFilter(private val diffLimit: Int) : ImageDataFilter {
    override fun filter(data: ImageData, database: MutableCollection<ImageData>): FilterCallback {
        var minDiff = Integer.MAX_VALUE
        var minDiffId = 0L

        database.forEach {
            var diff = 0
            data.hash.forEachIndexed { index, i ->
                diff += abs(it.hash[index] - i)
            }

            if (diff < minDiff) {
                minDiff = diff
                minDiffId = it.id
            }
        }
        return if (minDiff > diffLimit) NormalFilterCallback() else {
            SameImageFindedCallBack(data, minDiffId)
        }
    }

    override fun fastFilter(data: ImageData, database: MutableCollection<ImageData>): Boolean {
        database.forEach {
            var diff = 0
            data.hash.forEachIndexed { index, i ->
                diff += abs(it.hash[index] - i)
            }
            if (diff <= diffLimit) return false
        }
        return true
    }

    companion object {
        fun calculateDiffBetween(source: ImageData, data: ImageData): Int {
            var diff = 0
            data.hash.forEachIndexed { index, i ->
                diff += abs(source.hash[index] - i)
            }
            return diff
        }
    }
}