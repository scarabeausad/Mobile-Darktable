package com.example.model

data class HistogramData(
    val redChannel: IntArray = IntArray(256),
    val greenChannel: IntArray = IntArray(256),
    val blueChannel: IntArray = IntArray(256),
    val lumaChannel: IntArray = IntArray(256),
    val maxBinCount: Int = 1,
    val clippedShadowsPercent: Float = 0f,
    val clippedHighlightsPercent: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HistogramData) return false
        return redChannel.contentEquals(other.redChannel) &&
                greenChannel.contentEquals(other.greenChannel) &&
                blueChannel.contentEquals(other.blueChannel) &&
                lumaChannel.contentEquals(other.lumaChannel)
    }

    override fun hashCode(): Int {
        var result = redChannel.contentHashCode()
        result = 31 * result + greenChannel.contentHashCode()
        result = 31 * result + blueChannel.contentHashCode()
        result = 31 * result + lumaChannel.contentHashCode()
        return result
    }
}
