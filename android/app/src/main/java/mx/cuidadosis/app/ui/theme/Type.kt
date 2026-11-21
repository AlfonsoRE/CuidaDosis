package mx.cuidadosis.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Letra más grande que la de Material para adultos mayores (RNF-04). Todo en sp, así que
// además crece con el tamaño de letra que el usuario elija en el sistema.
private val base = Typography()

val Typography = Typography(
    headlineSmall = base.headlineSmall.copy(fontSize = 26.sp, lineHeight = 34.sp),
    titleLarge = base.titleLarge.copy(fontSize = 24.sp, lineHeight = 32.sp),
    titleMedium = base.titleMedium.copy(fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontSize = 17.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, letterSpacing = 0.3.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 16.sp, lineHeight = 23.sp),
    bodySmall = base.bodySmall.copy(fontSize = 15.sp, lineHeight = 21.sp),
    labelLarge = base.labelLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
    labelMedium = base.labelMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
    labelSmall = base.labelSmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
)
