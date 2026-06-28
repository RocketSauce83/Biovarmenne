package com.rocketsauce83.biovarmennepro.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.rocketsauce83.biovarmennepro.R

@OptIn(ExperimentalTextApi::class)
val RobotoFlexHeadline: FontFamily = FontFamily(
    Font(
        R.font.roboto_flex_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(450),
            FontVariation.width(60f),
            FontVariation.Setting("opsz", 40f),
            FontVariation.Setting("GRAD", 0f),
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val RobotoFlexSubtitle: FontFamily = FontFamily(
    Font(
        R.font.roboto_flex_variable,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(450),
            FontVariation.width(60f),
            FontVariation.Setting("opsz", 40f),
            FontVariation.Setting("GRAD", 0f),
        )
    )
)