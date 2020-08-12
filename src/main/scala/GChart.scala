import javafx.application.Application
import javafx.stage.Stage
import javafx.scene.layout.BorderPane
import javafx.scene.canvas.Canvas
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.scene.control.TextArea
import javafx.scene.text.Font
import javafx.scene.control.Label
import java.io.PrintWriter
import java.awt.Toolkit
import javafx.scene.text.Text

object GChart {
    def main(as: Array[String]) =
        Application.launch(classOf[GChart_], as:_*)
}

class GChart_ extends Application {
    val stageWidth = 800
    val stageHeight = 600
    val can = new Canvas(stageWidth - 200, stageHeight)

    override def start(ps: Stage) = {
        val f = Font.loadFont(getClass.getResourceAsStream("BMEULJIROTTF.ttf"), 20)
        // val text = new TextArea()
        val root = new BorderPane(can)
        val l = new Label(s"배달의 민족 : Pi는 ${DB.test}입니다.")
        l.setFont(f)
        root.setBottom(l)
        val scene = new Scene(root, stageWidth, stageHeight, Color.WHITE)
        ps.setScene(scene)
        ps.setTitle("성장 관리")
        
        ps.show
        // test
        drawBase()
    }

    private def mapMaker(smin: Double, smax: Double, tmin: Double, tmax: Double)(v: Double): Double = 
        (v - smin) * (tmax - tmin) / (smax - smin) + tmin

    def drawBase() = {
        val inset = 30
        val (xstart, xend) = (inset, can.getWidth - inset)
        val (ystart, yend) = (inset, can.getHeight - inset)
        val alpha = 0.5
        // println(ystart, yend)
        val gc = can.getGraphicsContext2D()
        
        // TODO. determine month gap by pixel gap 
        val mrange = Range(3, 19).flatMap(y => Range(0, 6).map(y * 12 + _ * 2))
        val monthMap = mapMaker(mrange.head, mrange.last, xstart, xend) _
        gc.setGlobalAlpha(alpha)
        gc.setStroke(Color.BLACK)
        mrange.foreach({ m =>
            val mx = monthMap(m)
            if (m % 12 == 0) {
                gc.setLineWidth(2)
                gc.setGlobalAlpha(1.0)
                val mstr = (m / 12).toString
                val ms = textSize(mstr, gc.getFont())
                gc.strokeText(mstr, mx - ms._1 / 2, yend + (ms._2 + 5))
                gc.setGlobalAlpha(alpha)
            } else {
                gc.setLineWidth(1)
            }
            gc.strokeLine(mx, ystart, mx, yend)
        })        

        val msrange = (8, 20)
        val measureMap = mapMaker(80, 200, yend, ystart) _
        val measures = Range(8, 20).flatMap(u => Range(0, 6).map(u * 10 + _ * 2))
        measures.foreach({ m =>
            val my = measureMap(m)
            println(m, my)
            if (m % 10 == 0) {
                gc.setLineWidth(2)
                gc.setGlobalAlpha(1.0)
                val mstr = (m / 10).toString
                val ms = textSize(mstr, gc.getFont())
                gc.strokeText(mstr, xstart - (ms._1 + 5), my + ms._2 / 2)
                gc.setGlobalAlpha(alpha)
            } else {
                gc.setLineWidth(1)
            }
            gc.strokeLine(xstart, my, xend, my)
        })
    }

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

    def test() = {
        def helper(l: String) = {
            val ws = l.split(",").toList
            val data = ws.drop(6).take(13).map(_.trim).mkString(", ")
            (s"Seq($data)", s"// ${ws(0)} ${ws(2)}")
        }

        val folder = "/home/nineclue/lab/gchart/"
        val categories = Seq("height", "weight", "bmi")
        // val categories = Seq("height")
        val sets = categories.map(s => (s ++ "2017.csv", s))

        def helper2(ls : Seq[(String, String)]) = {
            ls.init.map(t => s"${t._1}, ${t._2}") ++ Seq(ls.last).map(t => s"${t._1} ${t._2}")
        }
        val result = new PrintWriter("GrowthData.scala")
        sets.foreach({ case (fname, cname) => 
            val f = scala.io.Source.fromFile(folder ++ fname, "euc-kr")
            val ls = f.getLines.toList.map(helper)
            val male = ls.drop(2).take(228)
            val female = ls.drop(230).take(228)
            val header = s"val $cname : Seq[Seq[Seq[Double]]] = Seq( Seq(   // MALE"
            val intrim = "), Seq(   // FEMALE"
            val footer = "))"
            val total = (((header +: helper2(male)) :+ intrim) ++ helper2(female)) :+ footer
            total.foreach(result.println)
            result.println
        })
        result.close
        
        // sample.map(helper).foreach(t => println(t))
    }

}