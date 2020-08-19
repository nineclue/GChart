import javafx.application.Application
import javafx.stage.Stage
import javafx.scene.layout.BorderPane
import javafx.scene.canvas.Canvas
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.scene.control.Label
import java.io.PrintWriter
import javafx.scene.text.Font
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.layout.HBox
import java.time.LocalDate

object GChart {
    def main(as: Array[String]) =
        Application.launch(classOf[GChart_], as:_*)

    def test() = {
        println("creating tables...")
        DB.init()
        println("inserting rows...")
        DB.put(PatientRecord("123", true, LocalDate.of(1970, 3, 5), LocalDate.now, Some(182), Some(83)))
        DB.put(PatientRecord("123", true, LocalDate.of(1970, 3, 5), LocalDate.now, Some(182), Some(81)))
        DB.put(PatientRecord("123", true, LocalDate.of(1970, 3, 5), LocalDate.of(2000,1,1), Some(180), Some(75)))
        DB.put(PatientRecord("777", false, LocalDate.of(1969, 8, 7), LocalDate.of(2000, 1, 1), Some(164), None))
        println("phase 1 - selecting chart number")
        DB.get("123").foreach(println)
        println("phase 2 - selecting chart number and date")
        DB.get("777", Some(LocalDate.of(2000, 1, 1))).foreach(println)
    }
}

class GChart_ extends Application {
    val stageWidth = 800
    val stageHeight = 800
    val flarge = Font.loadFont(getClass.getResourceAsStream("BMEULJIROTTF.ttf"), 20)
    val fsmall = Font.loadFont(getClass.getResourceAsStream("NanumMyeongjo.ttf"), 12)
    val chart = new Chart(stageWidth, stageHeight, Some(fsmall))

    override def start(ps: Stage) = {
        val root = new BorderPane(chart)
        /*
        val l = new Label(s"배달의 민족 : Pi는 ${DB.test}입니다.")
        l.setFont(flarge)
        l.setMinHeight(40)
        l.setPrefHeight(40)
        l.setMaxHeight(40)
        root.setBottom(l)
        */
        val (bp, cs) = DataStage.inputPane()
        root.setLeft(bp)

        val scene = new Scene(root, Color.WHITE)
        ps.setScene(scene)
        ps.setTitle("성장 곡선")
        // ps.setMinWidth(stageWidth)
        // ps.setMinHeight(stageWidth)
        
        /*
        root.prefHeightProperty().bind(scene.heightProperty());
        root.prefWidthProperty().bind(scene.widthProperty());
        */
        scene.heightProperty().addListener(new ChangeListener[Number] {
            def changed(v: ObservableValue[_ <: Number] , ov: Number, nv: Number) = {
                // println(nv.doubleValue())
                
            }
        })
        /*
        scene.widthProperty().addListener(

        )
        */
        ps.show
        // root.widthProperty().addListener(ne)
        chart.draw(WeightChart, false, SD)
        /*
        val ds = DataStage.apply(ps)
        ds.show()
        */
        // convertCSV()
    }

    def convertCSV() = {
        def helper(l: String) = {
            val ws = l.split(",").toList
            val percent = ws.drop(6).take(13).map(_.trim).mkString(", ")
            val sd = ws.takeRight(7).map(_.trim).mkString(", ")
            // (s"Seq(Seq($percent), Seq($sd))", s"// ${ws(0)} ${ws(2)}")
            (s"Seq($percent)", s"Seq($sd)", s"// ${ws(0)} ${ws(2)}")
        }

        val folder = "/home/nineclue/lab/gchart/"
        val categories = Seq("height", "weight", "bmi")
        // val categories = Seq("height")
        val sets = categories.map(s => (s ++ "2017.csv", s.capitalize))

        def helper2(ls : Seq[(String, String, String)], f: Tuple3[String, String, String] => String) = {
            ls.init.map(t => s"${f(t)}, ${t._3}") :+ s"${f(ls.last)} ${ls.last._3}"
        }
        sets.foreach({ case (fname, cname) => 
            val result = new PrintWriter(s"$cname.scala")
            val f = scala.io.Source.fromFile(folder ++ fname, "euc-kr")
            val ls = f.getLines.toList.map(helper)
            val male = ls.drop(2).take(228)
            val female = ls.drop(230).take(228)
            val header1 = s"object ${cname}Percentile {\nval values: Seq[Seq[Seq[Double]]] = Seq( Seq(   // MALE"
            val header2 = s"object ${cname}SD {\nval values: Seq[Seq[Seq[Double]]] = Seq( Seq(   // MALE"
            val inter1 = "), Seq(   // FEMALE"
            val footer = "))}"
            val total = 
                Seq(header1) ++ helper2(male, _._1) ++ Seq(inter1) ++ helper2(female, _._1) ++ Seq(footer) ++  
                    Seq(header2) ++ helper2(male, _._2) ++ Seq(inter1) ++ helper2(female, _._2) ++ Seq(footer)
            total.foreach(result.println)
            result.close
        })
        /*
        val sample = scala.io.Source.fromFile(folder ++ sets.head._1, "euc-kr").getLines.toList.drop(2).take(5)
        sample.map(helper).foreach(t => println(t))
        */
    }

}