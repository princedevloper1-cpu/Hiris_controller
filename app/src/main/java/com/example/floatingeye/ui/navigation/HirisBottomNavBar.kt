package com.example.floatingeye.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.annotation.StringRes
import com.example.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeonPrimary

enum class HirisNavScreen(@StringRes val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.nav_dashboard, Icons.Rounded.Dashboard),
    EXTENSIONS(R.string.nav_extensions, Icons.Rounded.Widgets),
    EYE(R.string.nav_hud_studio, Icons.Rounded.Visibility),
    PROFILE(R.string.nav_profile, Icons.Rounded.Settings)
}

@Composable
fun HirisBottomNavBar(
    currentScreen: HirisNavScreen,
    onNavigate: (HirisNavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF070B13))
            .navigationBarsPadding()
    ) {
        // Thin glowing divider line at top
        HorizontalDivider(
            thickness = 1.dp,
            color = Color(0xFF141F30)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            HirisNavScreen.values().forEach { screen ->
                val label = stringResource(screen.labelRes)
                val isSelected = currentScreen == screen

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) CyanNeonPrimary else Color(0xFF6B7F96),
                    animationSpec = tween(200),
                    label = "navIconColor"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) CyanNeonPrimary else Color(0xFF6B7F96),
                    animationSpec = tween(200),
                    label = "navTextColor"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigate(screen) }
                        .padding(vertical = 4.dp)
                        .testTag("nav_item_${screen.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = label,
                        tint = iconColor,
                        modifier = Modifier.size(23.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 11.5.sp
                        ),
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Luminous Cyan Dot underneath active item
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) CyanNeonPrimary else Color.Transparent
                            )
                    )
                }
            }
        }
    }
}
