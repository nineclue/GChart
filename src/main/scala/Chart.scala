import javafx.scene.canvas.Canvas
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.paint.Color

sealed trait ChartType
case object HeightChart extends ChartType
case object WeightChart extends ChartType
case object BMIChart extends ChartType

case class Legend(repr: String, include: Boolean, emph: Boolean)

sealed trait RefType {
    val legends: Seq[Legend]
}
case object Percentile extends RefType {
    val legends = Seq(Legend("1st", false, false), 
        Legend("3rd", true, true), Legend("5th", true, false), 
        Legend("10th", true, false), Legend("15th", false, false),
        Legend("25th", true, false), Legend("50th", true, true),
        Legend("75th", true, false), Legend("85th", false, false),
        Legend("90th", true, false), Legend("95th", true, false),
        Legend("97th", true, true), Legend("99th", false, false))
}
case object SD extends RefType {
    val legends = 
        Seq(Legend("-3SD", true, true), Legend("-2SD", true, false),
        Legend("-1SD", true, false), Legend("0", true, true),
        Legend("+1SD", true, false), Legend("+2SD", true, false),
        Legend("+3SD", true, true))
}

object Chart {
    type MapF = Double => Double
    type ChartMap = Tuple2[MapF, MapF]
    // type ChartInfo = Option[ChartMap] => Option[ChartMap]
}

case class Chart(width: Double, height: Double, font: Option[Font]) extends Canvas(width, height) {
    import Chart._

    private val xinset = width / 15
    private val yinset = height / 15
    private val padding = width / 100
    private val measureRange: Map[Tuple2[ChartType, Boolean], Tuple2[Int, Int]] = 
            Map((HeightChart, true) -> (80, 200), (HeightChart, false) -> (80, 180),
                (WeightChart, true) -> (10, 110), (WeightChart, false) -> (10, 90),
                (BMIChart, true) -> (12, 32), (BMIChart, false) -> (12, 31))
    private val (yearStart, yearEnd) = (3, 18)
    private val gc = getGraphicsContext2D()
    private val maxLengendWidth = Percentile.legends.map(l => textSize(l.repr, gc.getFont)._1).max
    private val bgAlpha = 0.3

    private def mapMaker(sfrom: Double, sto: Double, dfrom: Double, dto: Double)(v: Double): Double = 
        (v - sfrom) * (dto - dfrom) / (sto - sfrom) + dfrom

    private def textSize(s: String, f: Font) = {
        val t = new Text(s)
        t.setFont(f)
        val bounds = t.getBoundsInLocal
        (bounds.getWidth(), bounds.getHeight())
    }

    private def fontSizeThatCanFit(s: String, f: Font, maxWidth: Int) = {
        val fontSize = f.getSize()
        val w = textSize(s, f)._1
        if (w > maxWidth) fontSize * maxWidth / w
        else fontSize
    }

    def drawBase(ctype: ChartType, male: Boolean): ChartMap = {
        // val gc = getGraphicsContext2D()
        font.foreach(gc.setFont)
        val (xstart, xend) = (xinset, width - xinset - maxLengendWidth - padding)
        val (ystart, yend) = (yinset, height - yinset)

        // Y value range
        val valueRange = measureRange((ctype, male))
        val ylabelSize = textSize(valueRange._2.toString, gc.getFont)
        val xAxisStart = xstart + ylabelSize._1 + padding
        // pixel width of 1 year
        val yearGap: Double = (xend - xAxisStart) / (yearEnd - yearStart + 1)
        val monthInterval = Seq(2, 3, 4, 6).find(i => yearGap / (12 / i) >= 3).getOrElse(12)
        val monthRange = Range(yearStart, yearEnd + 1).flatMap(y => Range(0, 12/monthInterval).map(y * 12 + _ * monthInterval)) :+ ((yearEnd + 1)* 12)
        val monthMap = mapMaker(monthRange.head, monthRange.last, xAxisStart, xend) _
        gc.setGlobalAlpha(bgAlpha)
        gc.setStroke(Color.BLACK)
        monthRange.foreach({ m =>
            val mx = monthMap(m)
            if (m % 12 == 0) {
                gc.setLineWidth(2)
                gc.setGlobalAlpha(1.0)
                val mstr = (m / 12).toString
                val ms = textSize(mstr, gc.getFont())
                gc.fillText(mstr, mx - ms._1 / 2, yend + (ms._2 + padding))
                gc.setGlobalAlpha(bgAlpha)
            } else {
                gc.setLineWidth(1)
            }
            gc.strokeLine(mx, ystart, mx, yend)
        })        

        val valueMap = mapMaker(valueRange._1, valueRange._2, yend, ystart) _
        val yAxisLength = yend - ystart - ylabelSize._2 - padding  // last 2 is minus for x axis label space
        val yUnitGap: Double = yAxisLength / ((valueRange._2 - valueRange._1) / 10)
        val valueInterval = Seq(2, 5).find(i => yUnitGap / i >= 3).getOrElse(10)
        // println(s"yUnitGap : $yUnitGap, valueInterval : $valueInterval")
        val measures = Range(valueRange._1, valueRange._2, 10).flatMap(u => Range(0, 10 / valueInterval).map(u + _ * valueInterval)) :+ valueRange._2
        // println(measures)
        measures.foreach({ m =>
            val my = valueMap(m)
            if (m % 10 == 0) {
                gc.setLineWidth(2)
                gc.setGlobalAlpha(1.0)
                val mstr = m.toString
                val ms = textSize(mstr, gc.getFont())
                gc.fillText(mstr, xAxisStart - (ms._1 + padding), my + ms._2 / 2)
                gc.setGlobalAlpha(bgAlpha)
            } else {
                gc.setLineWidth(1)
            }
            gc.strokeLine(xAxisStart, my, xend, my)
        })
        (monthMap, valueMap)
    }

    def drawRef(ctype: ChartType, male: Boolean, rtype: RefType, maps: ChartMap): ChartMap = {
        // val gc = getGraphicsContext2D()
        val colors = Seq(Color.BLUE, Color.RED)
        val sexIndex = if (male) 0 else 1
        gc.setStroke(colors(sexIndex))
        gc.setGlobalAlpha(bgAlpha)
        font.foreach(f => gc.setFont(f))
        val (xmap, ymap) = maps

        val refs: Measures = (ctype, rtype) match {
            case (HeightChart, Percentile) => HeightPercentile
            case (HeightChart, SD) => HeightSD
            case (WeightChart, Percentile) => WeightPercentile
            case (WeightChart, SD) => WeightSD
            case (BMIChart, Percentile) => BmiPercentile
            case (BMIChart, SD) => BmiSD
        }
        val monthRange = Range(yearStart, yearEnd + 1).flatMap(y => Range(0, 12).map(y * 12 + _)) // :+ (yearEnd * 12 + 12)
        val values = refs.values(sexIndex)
        val refnums = values(monthRange.head).size
        monthRange.sliding(2, 1).foreach({ ms =>
            val (x1, x2) = (xmap(ms(0)), xmap(ms(1)))
            gc.setGlobalAlpha(bgAlpha)
            Range(0, refnums).zip(rtype.legends) foreach({ case ((ri, l)) =>
                if (l.include) {
                    if (l.emph) gc.setLineWidth(2.0)
                    else gc.setLineWidth(1)
                    gc.strokeLine(x1, ymap(values(ms(0))(ri)), 
                                x2, ymap(values(ms(1))(ri)))
                }
            })
            gc.setGlobalAlpha(1.0)
            rtype.legends.zipWithIndex.foreach({ case ((l, i)) =>
                if (l.include) {
                    /*if (l.emph) gc.setLineWidth(2.0)
                    else gc.setLineWidth(1.0)*/
                    gc.fillText(l.repr, width - xinset - maxLengendWidth, ymap(values.last.apply(i)))
                }
            })
        })
        maps
    }

    def draw(ctype: ChartType, male: Boolean, rtype: RefType) = {
        val cm = drawBase(ctype, male)
        drawRef(ctype, male, rtype, cm)
    }
}