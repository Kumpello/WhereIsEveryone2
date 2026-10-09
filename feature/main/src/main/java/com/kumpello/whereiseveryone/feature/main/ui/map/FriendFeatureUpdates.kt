package com.kumpello.whereiseveryone.feature.main.ui.map

import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point

/** Tracks changes for Mapbox's partial GeoJSON updates. Use from one coroutine at a time. */
internal class FriendFeatureUpdates(
    private val colorForId: (String) -> Int = ::colorForUsername,
) {
    private var previous: Map<String, AnimatedFriendData> = emptyMap()
    private val colors = mutableMapOf<String, String>()

    fun update(friends: Map<String, AnimatedFriendData>): Changes {
        val added = mutableListOf<Feature>()
        val updated = mutableListOf<Feature>()
        val removed = previous.keys.filter { it !in friends }
        removed.forEach(colors::remove)

        friends.forEach { (id, data) ->
            val old = previous[id]
            if (old != data) {
                val color = colors.getOrPut(id) {
                    "#" + (colorForId(id) and 0xFFFFFF).toString(16).padStart(6, '0')
                }
                val feature = Feature.fromGeometry(Point.fromLngLat(data.lon, data.lat), null, id).apply {
                    addStringProperty("id", id)
                    addStringProperty("avatarId", "avatar-$id")
                    addNumberProperty("bearing", data.bearing)
                    addNumberProperty("opacity", data.opacity)
                    addNumberProperty("haloWidth", data.haloWidth)
                    addNumberProperty("speed", data.speed)
                    addStringProperty("color", color)
                }
                if (old == null) added += feature else updated += feature
            }
        }
        previous = friends
        return Changes(added, updated, removed)
    }

    data class Changes(
        val added: List<Feature>,
        val updated: List<Feature>,
        val removed: List<String>,
    )
}
