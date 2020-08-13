import javafx.scene.canvas.Canvas
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.paint.Color

sealed trait ChartType
case object HeightChart extends ChartType
case object WeightChart extends ChartType
case object BMIChart extends ChartType

sealed trait RefType
case object Percentile extends RefType
case object SD extends RefType

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
            Map((HeightChart, true) -> (80, 190), (HeightChart, false) -> (80, 180),
                (WeightChart, true) -> (10, 90), (WeightChart, false) -> (10, 80),
                (BMIChart, true) -> (13, 28), (BMIChart, false) -> (13, 27))
    private val (yearStart, yearEnd) = (3, 18)

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
        val (xstart, xend) = (xinset, width - xinset)
        val (ystart, yend) = (yinset, height - yinset)

        val alpha = 0.3
        val gc = getGraphicsContext2D()
        font.foreach(gc.setFont)
        
        val valueRange = measureRange((ctype, male))
        val ylabelSize = textSize(valueRange._2.toString, gc.getFont)
        val xAxisStart = xstart + ylabelSize._1 + padding
        val yearGap: Double = (xend - xAxisStart) / (yearEnd - yearStart + 1)
        val monthInterval = Seq(2, 3, 4, 6).find(i => yearGap / (12 / i) >= 3).getOrElse(12)
        val monthRange = Range(yearStart, yearEnd + 1).flatMap(y => Range(0, 12/monthInterval).map(y * 12 + _ * monthInterval)) :+ (yearEnd * 12 + 12)
        // println(monthRange.toList)
        val monthMap = mapMaker(monthRange.head, monthRange.last, xAxisStart, xend) _
        gc.setGlobalAlpha(alpha)
        gc.setStroke(Color.BLACK)
        monthRange.foreach({ m =>
            val mx = monthMap(m)
            if (m % 12 == 0) {
                gc.setLineWidth(2)
                gc.setGlobalAlpha(1.0)
                val mstr = (m / 12).toString
                val ms = textSize(mstr, gc.getFont())
                gc.strokeText(mstr, mx - ms._1 / 2, yend + (ms._2 + padding))
                gc.setGlobalAlpha(alpha)
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
        println(measures)
        measures.foreach({ m =>
            val my = valueMap(m)
            if (m % 10 == 0) {
                gc.setLineWidth(2)
                gc.setGlobalAlpha(1.0)
                val mstr = m.toString
                val ms = textSize(mstr, gc.getFont())
                gc.strokeText(mstr, xAxisStart - (ms._1 + padding), my + ms._2 / 2)
                gc.setGlobalAlpha(alpha)
            } else {
                gc.setLineWidth(1)
            }
            gc.strokeLine(xAxisStart, my, xend, my)
        })
        (monthMap, valueMap)
    }

    def drawRef(ctype: ChartType, male: Boolean, rtype: RefType, maps: ChartMap): ChartMap = {
        val gc = getGraphicsContext2D()
        val colors = Seq(Color.BLUE, Color.RED)
        val sexIndex = if (male) 0 else 1
        gc.setStroke(colors(sexIndex))
        gc.setGlobalAlpha(0.5)
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
            Range(0, refnums).foreach({ ri =>
                gc.strokeLine(x1, ymap(values(ms(0))(ri)), 
                              x2, ymap(values(ms(1))(ri)))
            })
        })
        maps
    }
}