import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sign

// actual for desktop/jvm
fun Modifier.horizontalSnapHelper(state: LazyListState): Modifier = composed {
    val scope = rememberCoroutineScope()
    draggable(rememberDraggableState {
        scope.launch { state.scrollBy(-it) }
    }, Orientation.Horizontal, onDragStopped = {
        val dx = if (state.firstVisibleItemScrollOffset >
            state.layoutInfo.visibleItemsInfo.first().size / 2) 1 else 0
        scope.launch { state.animateScrollToItem(state.firstVisibleItemIndex + dx) }
    }).scrollable(rememberScrollableState {
        if (!state.isScrollInProgress)
            scope.launch { state.animateScrollToItem(max(0, state.firstVisibleItemIndex - it.sign.toInt())) }
        0f
    }, Orientation.Vertical, flingBehavior = ScrollableDefaults.flingBehavior())
}