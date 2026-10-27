package com.kumpello.whereiseveryone.feature.main.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import com.kumpello.whereiseveryone.data.model.PointOfInterest
import com.kumpello.whereiseveryone.feature.main.R
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.extension.compose.style.BooleanValue
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.DoubleListValue
import com.mapbox.maps.extension.compose.style.DoubleValue
import com.mapbox.maps.extension.compose.style.layers.FormattedValue
import com.mapbox.maps.extension.compose.style.layers.ImageValue
import com.mapbox.maps.extension.compose.style.layers.generated.IconAnchorValue
import com.mapbox.maps.extension.compose.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.compose.style.layers.generated.TextAnchorValue
import com.mapbox.maps.extension.compose.style.rememberStyleImage
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.GeoJsonSourceState
import com.mapbox.maps.extension.style.expressions.generated.Expression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(MapboxExperimental::class)
@Composable
internal fun PointsOfInterestLayer(points: List<PointOfInterest>, onClick: (String) -> Unit) {
    val source = remember { GeoJsonSourceState() }
    val currentOnClick by rememberUpdatedState(onClick)
    LaunchedEffect(points) {
        source.data = withContext(Dispatchers.Default) { GeoJSONData(pointFeatures(points)) }
    }
    val flag = rememberStyleImage(imageId = "saved-point-flag", resourceId = R.drawable.ic_map_point_flag, sdf = true)
    SymbolLayer(sourceState = source, layerId = "saved-points-layer") {
        iconImage = ImageValue(flag)
        iconAnchor = IconAnchorValue.BOTTOM
        iconColor = ColorValue(Expression.toColor(Expression.get("color")))
        iconAllowOverlap = BooleanValue(true)
        iconIgnorePlacement = BooleanValue(true)
        textField = FormattedValue(Expression.get("name"))
        textSize = DoubleValue(14.0)
        textAnchor = TextAnchorValue.BOTTOM
        textOffset = DoubleListValue(0.0, -2.1)
        textColor = ColorValue(Color.Black)
        textHaloColor = ColorValue(Color.White)
        textHaloWidth = DoubleValue(1.5)
        textAllowOverlap = BooleanValue(true)
        textIgnorePlacement = BooleanValue(true)
        interactionsState.onClicked { feature, _ ->
            currentOnClick(feature.properties.getString("id"))
            true
        }
        interactionsState.onLongClicked { feature, _ ->
            currentOnClick(feature.properties.getString("id"))
            true
        }
    }
}

internal fun pointFeatures(points: List<PointOfInterest>): List<Feature> = points.map { point ->
    Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude), null, point.id).apply {
        addStringProperty("id", point.id)
        addStringProperty("name", point.name)
        val color = "#" + (colorForUsername(point.creatorUsername) and 0xFFFFFF).toString(16).padStart(6, '0')
        addStringProperty("color", color)
    }
}
