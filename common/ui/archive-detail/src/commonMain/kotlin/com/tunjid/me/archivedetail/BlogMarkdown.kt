package com.tunjid.me.archivedetail

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.MarkdownElement
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.State
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import com.mikepenz.markdown.model.State as InnerMarkdownState

@Composable
fun BlogMarkdown(
    modifier: Modifier = Modifier,
    markdown: String?,
    content: @Composable (InnerMarkdownState, MarkdownComponents, Modifier) -> Unit,
) {
    val bodyTypography = MaterialTheme.typography.bodyLarge
    val textStyle = remember {
        bodyTypography.copy(lineHeight = 32.sp)
    }

    val isDarkTheme = isSystemInDarkTheme()
    val highlightsBuilder = remember(isDarkTheme) {
        Highlights.Builder().theme(SyntaxThemes.default(darkMode = isDarkTheme))
    }
    val typography = markdownTypography(
        paragraph = textStyle,
        ordered = textStyle,
        bullet = textStyle,
        list = textStyle,
    )
    val components = markdownComponents(
        codeBlock = {
            MarkdownHighlightedCodeBlock(
                content = it.content,
                node = it.node,
                highlights = highlightsBuilder
            )
        },
        codeFence = {
            MarkdownHighlightedCodeFence(
                content = it.content,
                node = it.node,
                highlights = highlightsBuilder
            )
        },
    )

    Markdown(
        modifier = modifier,
        content = markdown ?: "",
        colors = markdownColor(),
        typography = typography,
        imageTransformer = Coil3ImageTransformerImpl,
        components = components,
        success = { markdownState, innerComponents, innerModifier ->
            content(markdownState, innerComponents, innerModifier)
        }
    )
}

internal fun LazyListScope.blogItems(
    innerMarkdownState: InnerMarkdownState,
    components: MarkdownComponents,
) {
    when (innerMarkdownState) {
        is State.Error -> Unit
        is State.Loading -> Unit
        is State.Success -> {
            items(
                items = innerMarkdownState.node.children,
                itemContent = { node ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        MarkdownElement(
                            node = node,
                            components = components,
                            content = innerMarkdownState.content,
                            skipLinkDefinition = innerMarkdownState.linksLookedUp
                        )
                    }
                },
            )
        }
    }
}