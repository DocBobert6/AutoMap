package app.vela.ui.map

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import app.vela.ui.dpadHighlight
import app.vela.ui.icons.Sym
import app.vela.ui.icons.SymOutlined
import kotlinx.coroutines.flow.drop

/** A short haptic tick, the kind a notification gives as it is swiped away. */
fun tickHaptic(view: View) {
    val kind = if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CLOCK_TICK
    view.performHapticFeedback(kind)
}

/**
 * A row that swipes: toward the start to delete, toward the end to edit when [onEdit] is given
 * (issue #748). The row ticks as the swipe crosses the point where it will act and again when
 * it acts, then settles back; the caller removes the row from its list (or asks first). Every
 * swipe here has a key path on the row itself: the handle or the overflow button beside it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeActionRow(
    onDelete: () -> Unit,
    deleteLabel: String,
    onEdit: (() -> Unit)? = null,
    editLabel: String? = null,
    editIcon: androidx.compose.ui.graphics.vector.ImageVector = SymOutlined.Edit,
    modifier: Modifier = Modifier,
    /** The surface the row sits on. The row is painted in it, so the action behind shows only
     *  where the row has moved away, not through the row's icon and name (issue 772). */
    background: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val delete = rememberUpdatedState(onDelete)
    val edit = rememberUpdatedState(onEdit)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { v ->
            when (v) {
                SwipeToDismissBoxValue.EndToStart -> { tickHaptic(view); delete.value(); false }
                SwipeToDismissBoxValue.StartToEnd -> { tickHaptic(view); edit.value?.invoke(); false }
                else -> true
            }
        },
    )
    // The first tick, as the swipe passes the point of no return.
    LaunchedEffect(state) {
        snapshotFlow { state.targetValue }.drop(1).collect { if (it != SwipeToDismissBoxValue.Settled) tickHaptic(view) }
    }
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = onEdit != null,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val dir = state.dismissDirection
            val deleting = dir == SwipeToDismissBoxValue.EndToStart
            // Only with an edit to run: a row without one cannot be dragged that way, yet the box
            // reports the direction for a moment as it settles, and flashed the pencil (issue 772).
            val editing = onEdit != null && dir == SwipeToDismissBoxValue.StartToEnd
            val bg = when {
                deleting -> MaterialTheme.colorScheme.errorContainer
                editing -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.Transparent
            }
            Box(
                Modifier.fillMaxSize().background(bg).padding(horizontal = 24.dp),
                contentAlignment = if (deleting) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                if (deleting) Icon(SymOutlined.Delete, contentDescription = deleteLabel, tint = MaterialTheme.colorScheme.onErrorContainer)
                else if (editing) Icon(editIcon, contentDescription = editLabel, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        },
        content = { Box(Modifier.fillMaxWidth().background(background)) { content() } },
    )
}

/**
 * The reorder handle at the end of a list row: drag it up or down to move the row one place per
 * row height passed, with a tick per move. Focused, Up and Down move the row and OK does
 * [onActivate] (the row's editor), so a keypad has the same reach as the drag and the pencil
 * the handle replaces.
 */
@Composable
fun ReorderHandle(
    onMove: (Int) -> Unit,
    onActivate: () -> Unit,
    rowHeightPx: Float,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val move = rememberUpdatedState(onMove)
    val activate = rememberUpdatedState(onActivate)
    // Read, not keyed: a row of another height composing mid-drag would restart the gesture.
    val rowHeight = rememberUpdatedState(rowHeightPx)
    var carried by remember { mutableFloatStateOf(0f) }
    Box(
        modifier
            .size(40.dp)
            .dpadHighlight(CircleShape)
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (ev.key) {
                    Key.DirectionUp -> { move.value(-1); tickHaptic(view); true }
                    Key.DirectionDown -> { move.value(1); tickHaptic(view); true }
                    else -> false
                }
            }
            .clickable(onClick = { activate.value() })
            // Vertical drags only, so a swipe that starts on the handle still swipes the row.
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { carried = 0f; tickHaptic(view) },
                    onDragEnd = { carried = 0f },
                    onDragCancel = { carried = 0f },
                ) { change, dy ->
                    change.consume()
                    carried += dy
                    val step = rowHeight.value.coerceAtLeast(1f)
                    while (carried > step) { carried -= step; move.value(1); tickHaptic(view) }
                    while (carried < -step) { carried += step; move.value(-1); tickHaptic(view) }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Sym.DragHandle, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
    }
}
