import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

@Composable
@Preview
fun App() {
    var date by remember { mutableStateOf(LocalDate.now()) }
    val format = remember { DateTimeFormatter.ofPattern("dd\nMMMM\nyyyy") }
    var columnSettings by remember { mutableStateOf(0 to 0) }
    val range = remember {
        val dayOfWeek = WeekFields.of(Locale.getDefault()).dayOfWeek()
        val now = LocalDate.now()
        now.minusYears(if (now.month.ordinal <= 6) 1 else 0)
            .withMonth(9).withDayOfMonth(1).with(dayOfWeek, 1) to
        now.plusYears(if (now.month.ordinal > 6) 1 else 0)
            .withMonth(6).withDayOfMonth(30).plusDays(7).with(dayOfWeek, 7)
    }

    MaterialTheme {
        CalendarPager(date, { date = it }, Modifier.onGloballyPositioned {
            columnSettings =
                if (it.size.width > 800) 2 to 5 else
                if (it.size.width > 500) 1 to 3 else
                if (it.size.width > 350) 0 to 2 else
                0 to 1
            },
            start = range.first,
            end = range.second,
            rulers = setOf(CalendarPager.EducMonths, CalendarPager.Weeks, CalendarPager.Days),
            rulerItem = {
                Card(Modifier.weight(1f).padding(3.dp), elevation = 5.dp) { it() } }
            ,
            firstItemOffset = columnSettings.first,
            divider = {
//                Divider()
//                Spacer(Modifier.height(2.dp))
//                Divider()
            },
            stickyColumn = {
                Box(Modifier.width(50.dp).fillMaxHeight().background(Color.LightGray))
            }
        ) {
            Box(Modifier.fillParentMaxWidth(1f/columnSettings.second).fillParentMaxHeight()) {
                Text(
                    it.format(format),
                    Modifier.padding(5.dp).align(Alignment.Center),
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                    color = if (date == it) Color.Blue else Color.Unspecified
                )
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
