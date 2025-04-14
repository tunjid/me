package com.tunjid.me.archivedetail

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.compose.LocalImageTransformer
import com.mikepenz.markdown.compose.LocalMarkdownAnimations
import com.mikepenz.markdown.compose.LocalMarkdownAnnotator
import com.mikepenz.markdown.compose.LocalMarkdownColors
import com.mikepenz.markdown.compose.LocalMarkdownComponents
import com.mikepenz.markdown.compose.LocalMarkdownDimens
import com.mikepenz.markdown.compose.LocalMarkdownExtendedSpans
import com.mikepenz.markdown.compose.LocalMarkdownPadding
import com.mikepenz.markdown.compose.LocalMarkdownTypography
import com.mikepenz.markdown.compose.LocalReferenceLinkHandler
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ReferenceLinkHandler
import com.mikepenz.markdown.model.ReferenceLinkHandlerImpl
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownDimens
import com.mikepenz.markdown.model.markdownExtendedSpans
import com.mikepenz.markdown.model.markdownPadding
import com.mikepenz.markdown.model.rememberMarkdownState
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes
import org.intellij.markdown.MarkdownElementTypes.ATX_1
import org.intellij.markdown.MarkdownElementTypes.ATX_2
import org.intellij.markdown.MarkdownElementTypes.ATX_3
import org.intellij.markdown.MarkdownElementTypes.ATX_4
import org.intellij.markdown.MarkdownElementTypes.ATX_5
import org.intellij.markdown.MarkdownElementTypes.ATX_6
import org.intellij.markdown.MarkdownElementTypes.BLOCK_QUOTE
import org.intellij.markdown.MarkdownElementTypes.CODE_BLOCK
import org.intellij.markdown.MarkdownElementTypes.CODE_FENCE
import org.intellij.markdown.MarkdownElementTypes.IMAGE
import org.intellij.markdown.MarkdownElementTypes.LINK_DEFINITION
import org.intellij.markdown.MarkdownElementTypes.ORDERED_LIST
import org.intellij.markdown.MarkdownElementTypes.PARAGRAPH
import org.intellij.markdown.MarkdownElementTypes.SETEXT_1
import org.intellij.markdown.MarkdownElementTypes.SETEXT_2
import org.intellij.markdown.MarkdownElementTypes.UNORDERED_LIST
import org.intellij.markdown.MarkdownTokenTypes.Companion.EOL
import org.intellij.markdown.MarkdownTokenTypes.Companion.HORIZONTAL_RULE
import org.intellij.markdown.MarkdownTokenTypes.Companion.TEXT
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMElementTypes.TABLE
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import com.mikepenz.markdown.model.State as InnerMarkdownState

@Stable
class BlogMarkdownScope private constructor(

    val components: MarkdownComponents,
    val flavour: MarkdownFlavourDescriptor,
    val parser: MarkdownParser,
    val referenceLinkHandler: ReferenceLinkHandler,
) {

    @Composable
    fun innerMarkdownState(
        body: String,
    ): InnerMarkdownState {
        val markdownState = rememberMarkdownState(
            content = body,
            flavour = flavour,
            parser = parser,
            referenceLinkHandler = referenceLinkHandler,
        )
        return markdownState.state.collectAsState().value
    }

    companion object {
        @Composable
        fun BlogMarkdownScope(
            content: @Composable BlogMarkdownScope.() -> Unit,
        ) {
            val bodyTypography = MaterialTheme.typography.bodyLarge
            val textStyle = remember {
                bodyTypography.copy(lineHeight = 32.sp)
            }

            val isDarkTheme = isSystemInDarkTheme()
            val highlightsBuilder = remember(isDarkTheme) {
                Highlights.Builder().theme(SyntaxThemes.default(darkMode = isDarkTheme))
            }

            val colors = markdownColor()
            val typography = markdownTypography(
                paragraph = textStyle,
                ordered = textStyle,
                bullet = textStyle,
                list = textStyle,
            )
            val padding = markdownPadding()
            val dimens = markdownDimens()
            val imageTransformer = Coil3ImageTransformerImpl
            val annotator = markdownAnnotator()
            val extendedSpans = markdownExtendedSpans()
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
            val animations = markdownAnimations()

            val flavour = GFMFlavourDescriptor()
            val parser = MarkdownParser(flavour)
            val referenceLinkHandler = ReferenceLinkHandlerImpl()

            val scope = remember {
                BlogMarkdownScope(
                    components = components,
                    flavour = flavour,
                    parser = parser,
                    referenceLinkHandler = referenceLinkHandler,
                )
            }

            with(scope) {
                CompositionLocalProvider(
                    LocalReferenceLinkHandler provides referenceLinkHandler,
                    LocalMarkdownPadding provides padding,
                    LocalMarkdownDimens provides dimens,
                    LocalMarkdownColors provides colors,
                    LocalMarkdownTypography provides typography,
                    LocalImageTransformer provides imageTransformer,
                    LocalMarkdownAnnotator provides annotator,
                    LocalMarkdownExtendedSpans provides extendedSpans,
                    LocalMarkdownComponents provides components,
                    LocalMarkdownAnimations provides animations,
                ) {
                    content()
                }
            }
        }
    }
}

internal fun BlogMarkdownScope.blogItems(
    lazyListScope: LazyListScope,
    innerMarkdownState: InnerMarkdownState,
) {
    when (innerMarkdownState) {
        is State.Error -> Unit
        is State.Loading -> Unit
        is State.Success -> {
            lazyListScope.items(
                items = innerMarkdownState.node.children,
                itemContent = { node ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        renderComponent(
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

@Composable
private fun BlogMarkdownScope.renderComponent(
    node: ASTNode,
    components: MarkdownComponents,
    content: String,
    includeSpacer: Boolean = true,
    skipLinkDefinition: Boolean = true,
) {
    val model = MarkdownComponentModel(
        content = content,
        node = node,
        typography = LocalMarkdownTypography.current,
    )
    var handled = true
    if (includeSpacer) Spacer(Modifier.height(LocalMarkdownPadding.current.block))
    when (node.type) {
        TEXT -> components.text(model)
        EOL -> components.eol(model)
        CODE_FENCE -> components.codeFence(model)
        CODE_BLOCK -> components.codeBlock(model)
        ATX_1 -> components.heading1(model)
        ATX_2 -> components.heading2(model)
        ATX_3 -> components.heading3(model)
        ATX_4 -> components.heading4(model)
        ATX_5 -> components.heading5(model)
        ATX_6 -> components.heading6(model)
        SETEXT_1 -> components.setextHeading1(model)
        SETEXT_2 -> components.setextHeading2(model)
        BLOCK_QUOTE -> components.blockQuote(model)
        PARAGRAPH -> components.paragraph(model)
        ORDERED_LIST -> components.orderedList(model)
        UNORDERED_LIST -> components.unorderedList(model)
        IMAGE -> components.image(model)
        LINK_DEFINITION -> {
            @Suppress("DEPRECATION")
            if (!skipLinkDefinition) components.linkDefinition(model)
        }

        HORIZONTAL_RULE -> components.horizontalRule(model)
        TABLE -> components.table(model)
        else -> {
            handled = components.custom?.invoke(node.type, model) != null
        }
    }

    if (!handled) {
        node.children.forEach { child ->
            renderComponent(child, components, content, includeSpacer, skipLinkDefinition)
        }
    }
}