trait Measures {
    val values: Seq[Seq[Seq[Double]]]
}

object Measures {
    val legends = Map(Percentile -> Seq("1st", "3rd", "5th", "10th", "15th", "25th", "50th", "75th", "85th", "90th", "95th", "97th", "99th"),
        SD -> Seq("-3SD", "-2SD", "-1SD", "0", "+1SD", "+2SD", "+3SD"))
    /*
    val selects[A]: Map[RefType, Seq[A] => Seq[A]] = 
        Map(Percentile -> (as: Seq[A]) => as.tail.init)
    */
    val marks = Map(Percentile -> Seq())
}