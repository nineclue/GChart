import java.time.LocalDate

object Calc {
    def ageInMonths(bday: LocalDate, mday: LocalDate) = 
        bday.until(mday).toTotalMonths()

    def mkClipper(lower: Int, upper: Int)(v: Int) = 
        (v max lower) min upper

    def availableTypes(rs: Seq[PatientRecord]): Seq[Option[ChartType]] = {
        val rset = rs.foldLeft(Set.empty[ChartType])({ case ((s, r)) =>
            val h = r.height.map(_ => HeightChart)
            val w = r.weight.map(_ => WeightChart)
            val b = r.bmi.map(_ => BMIChart)
            s ++ Set(h, w, b).flatten
        })
        /*
        Seq(HeightChart, WeightChart, BMIChart).
            foldLeft(Seq.empty[ChartType])({ case ((s, ct)) =>
                if (rset.contains(ct)) s :+ ct else s
            })
        */
        Seq(HeightChart, WeightChart, BMIChart).map(ct => if (rset.contains(ct)) Some(ct) else None)
    }
}