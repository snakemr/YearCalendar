import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

private val minDate = LocalDate.of(1, 1, 1)
private val maxDate = LocalDate.of(2999, 12, 31)
private val days = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
private val months = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")

enum class CalendarPager { Days, Weeks, Months, EducMonths, Years }

@Composable
fun CalendarPager(
    current: LocalDate? = null,
    onChange: (LocalDate)->Unit = {},
    modifier: Modifier = Modifier,
    start: LocalDate = minDate,
    end: LocalDate = maxDate,
    firstItemOffset: Int = 0,
    rulers: Set<CalendarPager> = CalendarPager.values().toSet() - CalendarPager.EducMonths,
    rulerItem: (@Composable RowScope.(@Composable ()->Unit) -> Unit)? = null,
    divider: @Composable () -> Unit = { Divider() },
    stickyColumn: @Composable RowScope.() -> Unit = {},
    content: @Composable LazyItemScope.(LocalDate) -> Unit
) = Column(modifier) {
    val size = remember(start, end) { ChronoUnit.DAYS.between(start, end).toInt() + 1 }
    val scope = rememberCoroutineScope()
    val dayState = rememberLazyListState()
    val weekState = rememberLazyListState()
    val yearState = rememberLazyListState()
    var scrolling by remember { mutableStateOf(true) }
    var thin by remember { mutableStateOf(false) }

    val (weeks, years) = remember(start, end, rulers) {
        if (CalendarPager.Weeks in rulers || CalendarPager.Years in rulers) {
            val allDays = List(size) { start.plusDays(it.toLong()) }
            Pair(
                if (CalendarPager.Weeks in rulers) {
                    val weekOfYear = WeekFields.of(Locale.getDefault()).weekOfYear()
                    allDays.groupBy { it.year to it[weekOfYear] }.map { it.value.first() }.filter { it.dayOfWeek == DayOfWeek.MONDAY }
                } else null,
                if (CalendarPager.Years in rulers) {
                    allDays.groupBy { it.year }.map { it.value.first().withDayOfYear(1) }
                } else null
            )
        } else Pair(null, null)
    }

    fun scrollYears(new: LocalDate, animate: Boolean = true) = scope.launch {
        if (years != null) {
            val offset = years.indexOf(new.withDayOfYear(1))
            if (offset < yearState.firstVisibleItemIndex && offset >= 0 ||
                offset >= yearState.firstVisibleItemIndex + yearState.layoutInfo.visibleItemsInfo.size - 1
            ) if (animate) yearState.animateScrollToItem(offset) else yearState.scrollToItem(offset)
        }
    }

    fun scrollWeeks(new: LocalDate, animate: Boolean = true) = scope.launch {
        if (weeks != null) {
            val dayOfWeek = WeekFields.of(Locale.getDefault()).dayOfWeek()
            val offset = weeks.indexOf(new.with(dayOfWeek, 1))
            if (offset < weekState.firstVisibleItemIndex && offset >= 0 ||
                offset >= weekState.firstVisibleItemIndex + weekState.layoutInfo.visibleItemsInfo.size - 1
                ) if (animate) weekState.animateScrollToItem(offset) else weekState.scrollToItem(offset)
        }
    }

    fun scrollDays(new: LocalDate, animate: Boolean = true) = scope.launch {
        val first = min(firstItemOffset, new.dayOfWeek.ordinal)
        val offset = ChronoUnit.DAYS.between(start, new).toInt() - first
        scrolling = true
        if (offset < dayState.firstVisibleItemIndex ||
            offset >= dayState.firstVisibleItemIndex + dayState.layoutInfo.visibleItemsInfo.size - 1 - first
            ) if (animate) dayState.animateScrollToItem(offset) else dayState.scrollToItem(offset)
        scrolling = false
    }

    fun LocalDate.inRange() = if (this < start) start else if (this > end) end else this

    LaunchedEffect(Unit) {
        (current ?: LocalDate.now()).let {
            dayState.scrollToItem(
                ChronoUnit.DAYS.between(start, it).toInt() - min(firstItemOffset, it.dayOfWeek.ordinal)
            )
            scrollWeeks(it, false)
            scrollYears(it, false)
            delay(100)
            scrolling = false
        }
    }

    val index = remember(current) { ChronoUnit.DAYS.between(start, current ?: LocalDate.now()).toInt() }
    LaunchedEffect(dayState.firstVisibleItemIndex, dayState.firstVisibleItemScrollOffset) {
        if (!scrolling) current?.let {
            val first = dayState.firstVisibleItemIndex + firstItemOffset +
                    if (dayState.firstVisibleItemScrollOffset > dayState.layoutInfo.visibleItemsInfo.first().size / 2)
                        1 else 0
            if (first != index) {
                val new = it.plusDays((first - index).toLong())
                onChange(new)
                scrollYears(new)
                scrollWeeks(new)
            }
        }
    }

    if (years != null) {
        val year by remember(current) { derivedStateOf {
            current?.withDayOfYear(1)
        } }
        LazyRow(Modifier.fillMaxWidth().horizontalSnapHelper(yearState), yearState) {
            itemsIndexed(years) {index, it ->
                val back by animateColorAsState(
                    if (it == year) MaterialTheme.colors.secondary else Color.Unspecified
                )
                Text(it.year.toString(), Modifier.background(back).clickable {
                    val new = (current ?: LocalDate.now()).withYear(it.year).inRange()
                    if (new != current) {
                        onChange(new)
                        scrollWeeks(new)
                        scrollDays(new)
                    }
                }.padding(horizontal = 15.dp, vertical = 4.dp))
                if (index < years.size-1) Box(Modifier.background(Color.Gray).size(1.dp, 26.dp))
            }
        }
        divider()
    }

    if (CalendarPager.Months in rulers || CalendarPager.EducMonths in rulers) {
        @Composable fun List<String>.months(start: Int) = forEachIndexed { index, name ->
            val back by animateColorAsState(
                if (current?.month?.ordinal == (start + index) % 12)
                    MaterialTheme.colors.secondary else Color.Unspecified
            )
            Text(name,
                Modifier.weight(1f).background(back).clickable {
                    val it = current ?: LocalDate.now()
                    val new = it.withMonth((start + index) % 12 + 1).plusYears(
                        if (start + index >= 12 && it.month.ordinal > 6) 1
                        else if (start + index < 12 && it.month.ordinal <= 6 && CalendarPager.EducMonths in rulers) -1
                        else 0
                    ).inRange()
                    if (new != current) {
                        onChange(new)
                        scrollYears(new)
                        scrollWeeks(new)
                        scrollDays(new)
                    }
                }.padding(4.dp),
                textAlign = TextAlign.Center
            )
            if (index < this.size-1) Box(Modifier.background(Color.Gray).width(1.dp).fillMaxHeight())
        }
        if (CalendarPager.EducMonths in rulers) {
            if (thin) Column {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) { (months.drop(8) + months.first()).months(8) }
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) { months.subList(1, 6).months(13) }
            } else
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) { (months.drop(8) + months.take(6)).months(8) }
        } else {
            if (thin) Column {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) { months.take(6).months(0) }
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) { months.drop(6).months(6) }
            } else
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) { months.months(0) }
        }
        divider()
    }

    if (weeks != null) {
        val dayOfWeek = remember { WeekFields.of(Locale.getDefault()).dayOfWeek() }
        val format = remember { DateTimeFormatter.ofPattern("dd MMM") }
        val week by remember(current) { derivedStateOf {
            current?.with(dayOfWeek, 1)
        } }
        LazyRow(Modifier.fillMaxWidth().horizontalSnapHelper(weekState), weekState) {
            itemsIndexed(weeks) {index, it ->
                val back by animateColorAsState(
                    if (it == week) MaterialTheme.colors.secondary else Color.Unspecified
                )
                Text(
                    if (thin)
                        it.format(format)
                    else
                        it.format(format) + "– " + it.plusDays(6).format(format),
                    Modifier.background(back).clickable {
                        val new = it.with(dayOfWeek, current?.dayOfWeek?.ordinal?.plus(1)?.toLong() ?: 1L).inRange()
                        if (new != current) {
                            onChange(new)
                            scrollYears(new)
                            scrollDays(new)
                        }
                    }.padding(horizontal = 15.dp, vertical = 4.dp))
                if (index < weeks.size-1) Box(Modifier.background(Color.Gray).size(1.dp, 26.dp))
            }
        }
        divider()
    }

    if (CalendarPager.Days in rulers) {
        val dayOfWeek = remember { WeekFields.of(Locale.getDefault()).dayOfWeek() }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            days.forEachIndexed { index, name ->
                val back by animateColorAsState(
                    if (current?.dayOfWeek?.ordinal == index) MaterialTheme.colors.secondary else Color.Unspecified
                )
                @Composable fun Item() = Text(
                    name,
                    Modifier.weight(1f).background(back).clickable {
                        val new = (current ?: LocalDate.now()).with(dayOfWeek, (index + 1).toLong()).inRange()
                        onChange(new)
                        scrollYears(new)
                        scrollWeeks(new)
                        scrollDays(new)
                    }.padding(4.dp),
                    color = if (index < 5) Color.Unspecified else MaterialTheme.colors.error,
                    textAlign = TextAlign.Center
                )
                if (rulerItem != null)
                    rulerItem { Item() }
                else {
                    Item()
                    if (index < 6) Box(Modifier.background(Color.Gray).width(1.dp).fillMaxHeight())
                }
            }
        }
        divider()
    }

    Row {
        stickyColumn()
        LazyRow(Modifier.fillMaxWidth().onGloballyPositioned {
            thin = it.size.width < 500
        }.horizontalSnapHelper(dayState), dayState) {
            items(size) {
                val date by remember {
                    derivedStateOf {
                        start.plusDays(it.toLong())
                    }
                }
                content(date)
            }
        }
    }
}

@Composable
fun Modifier.horizontalSnapHelper(state: LazyListState): Modifier {
    val scope = rememberCoroutineScope()
    return draggable(rememberDraggableState {
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