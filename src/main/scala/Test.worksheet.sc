def mapMaker(smin: Double, smax: Double, tmin: Double, tmax: Double)(v: Double): Double = 
    (v - smin) * (tmax - tmin) / (smax - smin) + tmin

val mp = mapMaker(80, 200, 570, 30) _
Range(80, 200, 2).map(i => (i, mp(i.toDouble)))

import java.time.LocalDate

val bday = LocalDate.of(1970, 3, 5)
val tday = LocalDate.now

val d = bday.until(tday)
d.getMonths
d.toTotalMonths

Set(Some(1), None, Some(2)).flatten