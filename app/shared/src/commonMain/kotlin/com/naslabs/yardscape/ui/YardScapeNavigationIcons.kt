/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.naslabs.yardscape.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal fun navigationIcon(destination: YardScapePrimaryDestination): ImageVector = when (destination) {
    YardScapePrimaryDestination.Browse -> SearchIcon
    YardScapePrimaryDestination.MyFinds -> FavoriteBorderIcon
    YardScapePrimaryDestination.Host -> AddCircleOutlineIcon
    YardScapePrimaryDestination.Messages -> ChatBubbleOutlineIcon
    YardScapePrimaryDestination.Account -> PersonOutlineIcon
}

private fun materialNavigationIcon(name: String, pathBuilder: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24.0f,
        viewportHeight = 24.0f,
    ).apply {
        path(fill = SolidColor(Color.Black), pathBuilder = pathBuilder)
    }.build()

internal val SearchIcon = materialNavigationIcon("Search") {
    moveTo(15.5f, 14.0f)
    horizontalLineToRelative(-0.79f)
    lineToRelative(-0.28f, -0.27f)
    curveTo(15.41f, 12.59f, 16.0f, 11.11f, 16.0f, 9.5f)
    curveTo(16.0f, 5.91f, 13.09f, 3.0f, 9.5f, 3.0f)
    reflectiveCurveTo(3.0f, 5.91f, 3.0f, 9.5f)
    reflectiveCurveTo(5.91f, 16.0f, 9.5f, 16.0f)
    curveToRelative(1.61f, 0.0f, 3.09f, -0.59f, 4.23f, -1.57f)
    lineToRelative(0.27f, 0.28f)
    verticalLineToRelative(0.79f)
    lineToRelative(5.0f, 4.99f)
    lineTo(20.49f, 19.0f)
    lineToRelative(-4.99f, -5.0f)
    close()
    moveTo(9.5f, 14.0f)
    curveTo(7.01f, 14.0f, 5.0f, 11.99f, 5.0f, 9.5f)
    reflectiveCurveTo(7.01f, 5.0f, 9.5f, 5.0f)
    reflectiveCurveTo(14.0f, 7.01f, 14.0f, 9.5f)
    reflectiveCurveTo(11.99f, 14.0f, 9.5f, 14.0f)
    close()
}

internal val FavoriteBorderIcon = materialNavigationIcon("FavoriteBorder") {
    moveTo(16.5f, 3.0f)
    curveToRelative(-1.74f, 0.0f, -3.41f, 0.81f, -4.5f, 2.09f)
    curveTo(10.91f, 3.81f, 9.24f, 3.0f, 7.5f, 3.0f)
    curveTo(4.42f, 3.0f, 2.0f, 5.42f, 2.0f, 8.5f)
    curveToRelative(0.0f, 3.78f, 3.4f, 6.86f, 8.55f, 11.54f)
    lineTo(12.0f, 21.35f)
    lineToRelative(1.45f, -1.32f)
    curveTo(18.6f, 15.36f, 22.0f, 12.28f, 22.0f, 8.5f)
    curveTo(22.0f, 5.42f, 19.58f, 3.0f, 16.5f, 3.0f)
    close()
    moveTo(12.1f, 18.55f)
    lineToRelative(-0.1f, 0.1f)
    lineToRelative(-0.1f, -0.1f)
    curveTo(7.14f, 14.24f, 4.0f, 11.39f, 4.0f, 8.5f)
    curveTo(4.0f, 6.5f, 5.5f, 5.0f, 7.5f, 5.0f)
    curveToRelative(1.54f, 0.0f, 3.04f, 0.99f, 3.57f, 2.36f)
    horizontalLineToRelative(1.87f)
    curveTo(13.46f, 5.99f, 14.96f, 5.0f, 16.5f, 5.0f)
    curveToRelative(2.0f, 0.0f, 3.5f, 1.5f, 3.5f, 3.5f)
    curveToRelative(0.0f, 2.89f, -3.14f, 5.74f, -7.9f, 10.05f)
    close()
}

private val AddCircleOutlineIcon = materialNavigationIcon("AddCircleOutline") {
    moveTo(13.0f, 7.0f)
    horizontalLineToRelative(-2.0f)
    verticalLineToRelative(4.0f)
    lineTo(7.0f, 11.0f)
    verticalLineToRelative(2.0f)
    horizontalLineToRelative(4.0f)
    verticalLineToRelative(4.0f)
    horizontalLineToRelative(2.0f)
    verticalLineToRelative(-4.0f)
    horizontalLineToRelative(4.0f)
    verticalLineToRelative(-2.0f)
    horizontalLineToRelative(-4.0f)
    lineTo(13.0f, 7.0f)
    close()
    moveTo(12.0f, 2.0f)
    curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
    reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
    reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
    reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
    close()
    moveTo(12.0f, 20.0f)
    curveToRelative(-4.41f, 0.0f, -8.0f, -3.59f, -8.0f, -8.0f)
    reflectiveCurveToRelative(3.59f, -8.0f, 8.0f, -8.0f)
    reflectiveCurveToRelative(8.0f, 3.59f, 8.0f, 8.0f)
    reflectiveCurveToRelative(-3.59f, 8.0f, -8.0f, 8.0f)
    close()
}

private val ChatBubbleOutlineIcon = materialNavigationIcon("ChatBubbleOutline") {
    moveTo(20.0f, 2.0f)
    lineTo(4.0f, 2.0f)
    curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
    verticalLineToRelative(18.0f)
    lineToRelative(4.0f, -4.0f)
    horizontalLineToRelative(14.0f)
    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
    lineTo(22.0f, 4.0f)
    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
    close()
    moveTo(20.0f, 16.0f)
    lineTo(6.0f, 16.0f)
    lineToRelative(-2.0f, 2.0f)
    lineTo(4.0f, 4.0f)
    horizontalLineToRelative(16.0f)
    verticalLineToRelative(12.0f)
    close()
}

private val PersonOutlineIcon = materialNavigationIcon("PersonOutline") {
    moveTo(12.0f, 5.9f)
    curveToRelative(1.16f, 0.0f, 2.1f, 0.94f, 2.1f, 2.1f)
    reflectiveCurveToRelative(-0.94f, 2.1f, -2.1f, 2.1f)
    reflectiveCurveTo(9.9f, 9.16f, 9.9f, 8.0f)
    reflectiveCurveToRelative(0.94f, -2.1f, 2.1f, -2.1f)
    moveToRelative(0.0f, 9.0f)
    curveToRelative(2.97f, 0.0f, 6.1f, 1.46f, 6.1f, 2.1f)
    verticalLineToRelative(1.1f)
    lineTo(5.9f, 18.1f)
    lineTo(5.9f, 17.0f)
    curveToRelative(0.0f, -0.64f, 3.13f, -2.1f, 6.1f, -2.1f)
    moveTo(12.0f, 4.0f)
    curveTo(9.79f, 4.0f, 8.0f, 5.79f, 8.0f, 8.0f)
    reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
    reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f)
    reflectiveCurveToRelative(-1.79f, -4.0f, -4.0f, -4.0f)
    close()
    moveTo(12.0f, 13.0f)
    curveToRelative(-2.67f, 0.0f, -8.0f, 1.34f, -8.0f, 4.0f)
    verticalLineToRelative(3.0f)
    horizontalLineToRelative(16.0f)
    verticalLineToRelative(-3.0f)
    curveToRelative(0.0f, -2.66f, -5.33f, -4.0f, -8.0f, -4.0f)
    close()
}
