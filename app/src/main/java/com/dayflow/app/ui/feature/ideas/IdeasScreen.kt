package com.dayflow.app.ui.feature.ideas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dayflow.app.domain.model.Idea
import com.dayflow.app.ui.components.EmptyState
import com.dayflow.app.ui.components.pressable
import com.dayflow.app.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
private val dateFmt = DateTimeFormatter.ofPattern("MMM d")
private fun stamp(ms: Long): String {
    val z = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    return if (z.toLocalDate() == LocalDate.now()) z.format(timeFmt) else z.format(dateFmt)
}

/** Creative notebook: search, tag filter, pinned first, staggered paper cards. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun IdeasScreen(
    bottomPadding: Dp,
    onOpenIdea: (Long) -> Unit,
    modifier: Modifier = Modifier,
    vm: IdeasViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    SectionTheme(DaySection.IDEAS) {
        Column(modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(if (ui.showArchived) "ARCHIVE" else "IDEAS OF THE DAY", style = DfType.label, color = Df.colors.textSecondary)
                    Text("Notebook", style = DfType.serifTitle.copy(fontSize = 34.sp), color = Df.colors.textPrimary)
                }
                Text(
                    if (ui.showArchived) "Back to notes" else "Archive",
                    style = DfType.bodySmall, color = Df.section.accent,
                    modifier = Modifier.heightIn(min = 44.dp).pressable { vm.setArchived(!ui.showArchived) }.padding(horizontal = 8.dp, vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            SearchBar(ui.query, vm::setQuery, Modifier.padding(horizontal = 16.dp))
            if (ui.allTags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ui.allTags.forEach { t ->
                        val on = ui.tag == t
                        Text(
                            "#$t", style = DfType.bodySmall,
                            color = if (on) Paper.Ink else Df.colors.textSecondary,
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (on) Paper.Lavender else Color.Transparent)
                                .border(1.dp, if (on) Paper.Lavender else Df.colors.hairline, RoundedCornerShape(50))
                                .pressable { vm.setTag(t) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            if (ui.pinned.isEmpty() && ui.others.isEmpty()) {
                Box(Modifier.weight(1f).padding(bottom = bottomPadding), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Rounded.Search,
                        title = if (ui.query.isNotBlank() || ui.tag != null) "No matching ideas" else if (ui.showArchived) "Archive is empty" else "A blank page",
                        body = if (ui.query.isNotBlank() || ui.tag != null) "Try a different word or clear the tag filter." else if (ui.showArchived) "Archived ideas are kept here, out of the way." else "Tap + and capture the first thought before it slips away.",
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
                    verticalItemSpacing = 12.dp,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (ui.pinned.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) { Label("Pinned") }
                        items(ui.pinned, key = { it.id }) { NoteCard(it, { onOpenIdea(it.id) }, { vm.togglePin(it.id) }) }
                        if (ui.others.isNotEmpty()) item(span = StaggeredGridItemSpan.FullLine) { Label("Everything else") }
                    }
                    items(ui.others, key = { it.id }) { NoteCard(it, { onOpenIdea(it.id) }, { vm.togglePin(it.id) }) }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) =
    Text(text.uppercase(), style = DfType.label, color = Df.colors.textTertiary, modifier = Modifier.padding(top = 4.dp, start = 2.dp))

@Composable
private fun SearchBar(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(Df.colors.raised)
            .border(1.dp, Df.colors.hairline, RoundedCornerShape(50))
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, null, tint = Df.colors.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = DfType.body.copy(color = Df.colors.textPrimary),
            cursorBrush = SolidColor(Df.section.accent),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box { if (value.isEmpty()) Text("Search ideas and tags", style = DfType.body, color = Df.colors.textTertiary); inner() }
            },
        )
        if (value.isNotEmpty()) {
            Box(Modifier.size(40.dp).pressable(haptic = null) { onChange("") }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, "Clear search", tint = Df.colors.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun NoteCard(idea: Idea, onOpen: () -> Unit, onPin: () -> Unit) {
    val shape = RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 6.dp, bottomStart = 18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (idea.pinned) Paper.Lavender else Paper.Sheet)
            .pressable(pressedScale = 0.98f, haptic = null, onClick = onOpen)
            .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                idea.text, style = DfType.serifBody, color = Paper.Ink,
                maxLines = 8, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(top = 4.dp),
            )
            Box(Modifier.size(40.dp).pressable(haptic = null, onClick = onPin), contentAlignment = Alignment.Center) {
                Icon(
                    if (idea.pinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                    if (idea.pinned) "Unpin" else "Pin",
                    tint = if (idea.pinned) Paper.Ink else Paper.InkSoft, modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.padding(end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stamp(idea.createdAt), style = DfType.serifSmall, color = Paper.InkSoft)
            idea.convertedTo?.let {
                Spacer(Modifier.width(8.dp))
                Text("→ ${it.name.lowercase()}", style = DfType.serifSmall, color = Paper.InkSoft)
            }
        }
        if (idea.tags.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(idea.tags.joinToString("  ") { "#$it" }, style = DfType.serifSmall, color = Paper.Ink.copy(alpha = 0.75f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
