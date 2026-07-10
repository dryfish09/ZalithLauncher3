package com.movtery.zalithlauncher.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.iffly.compose.markdown.config.MarkdownRenderConfig
import com.iffly.compose.markdown.style.MarkdownTheme
import com.iffly.compose.markdown.MarkdownView as ComposeMarkdownView
import com.movtery.zalithlauncher.ui.theme.cardColor
import com.vladsch.flexmark.util.ast.Node

@Composable
fun MarkdownView(
    content: String,
    modifier: Modifier = Modifier,
    config: MarkdownRenderConfig = defaultMarkdownConfig(),
    bodyFontSize: TextUnit? = null,
) {
    val finalConfig = if (bodyFontSize != null) {
        remember(bodyFontSize, config) {
            MarkdownRenderConfig.Builder()
                .markdownTheme(
                    MarkdownTheme(
                        textStyle = TextStyle(fontSize = bodyFontSize)
                    )
                )
                .build()
        }
    } else {
        config
    }
    ComposeMarkdownView(
        content = content,
        markdownRenderConfig = finalConfig,
        modifier = modifier,
    )
}

@Composable
fun MarkdownView(
    node: Node,
    modifier: Modifier = Modifier,
    config: MarkdownRenderConfig = defaultMarkdownConfig(),
) {
    ComposeMarkdownView(
        node = node,
        markdownRenderConfig = config,
        modifier = modifier,
    )
}

@Composable
fun defaultMarkdownConfig(
    influencedByBackground: Boolean = true,
    codeBackground: Color = cardColor(influencedByBackground),
): MarkdownRenderConfig {
    return remember(influencedByBackground, codeBackground) {
        MarkdownRenderConfig.Builder()
            .markdownTheme(
                MarkdownTheme(
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                    headStyle = mapOf(
                        0 to TextStyle(
                            fontSize = 34.sp,
                            lineHeight = 42.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        1 to TextStyle(
                            fontSize = 24.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        2 to TextStyle(
                            fontSize = 20.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        3 to TextStyle(
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        4 to TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        5 to TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    ),
                    code = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        background = codeBackground,
                    ),
                    link = TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold,
                        ),
                        pressedStyle = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold,
                        ),
                    ),
                )
            )
            .build()
    }
}
