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
import javafx.scene.layout.{HBox, VBox, AnchorPane}
import java.time.LocalDate
import javafx.scene.control.RadioButton
import javafx.scene.control.ToggleGroup
import javafx.geometry.Pos
import javafx.scene.layout.TilePane
import javafx.event.ActionEvent
import javafx.scene.paint.Color

object GChart {
    def main(as: Array[String]) = {
        // dbtest
        DB.init
        Application.launch(classOf[GChart_], as:_*)
    }

    def dbtest() = {
        println("creating tables...")
        DB.init()
        println("inserting rows...")
        DB.put(PatientRecord("123", "M", LocalDate.of(1970, 3, 5), LocalDate.now, Some(182), Some(83)))
        DB.put(PatientRecord("123", "M", LocalDate.of(1970, 3, 5), LocalDate.of(2000,1,1), Some(180), Some(75)))
        DB.put(PatientRecord("777", "F", LocalDate.of(1969, 8, 7), LocalDate.of(2000, 1, 1), Some(164), None))
        println("phase 1 - selecting chart number")
        DB.get("123").foreach(println)
        println("phase 2 - selecting chart number and date")
        DB.get("777", Some(LocalDate.of(2000, 1, 1))).foreach(println)
    }
}

class GChart_ extends Application {

    val stageWidth = 600
    val stageHeight = 600
    val fsmall = Font.loadFont(getClass.getResourceAsStream("IropkeBatangM.ttf"), 12)
    // val flarge = Font.loadFont(getClass.getResourceAsStream("IropkeBatangM.ttf"), 20)
    val (cgroup, chart) = Chart(stageWidth, stageHeight, Some(fsmall))
    val hButton = new RadioButton("신장")
    val wButton = new RadioButton("체중")
    val bButton = new RadioButton("BMI")
    val ctypes = new ToggleGroup()
    val ctButtons = Seq(hButton, wButton, bButton)
    ctButtons.foreach(_.setToggleGroup(ctypes))

    val pButton = new RadioButton("Percentile")
    val sButton = new RadioButton("SD")
    val rtypes = new ToggleGroup()
    val rtButtons = Seq(pButton, sButton)
    rtButtons.foreach(_.setToggleGroup(rtypes))

    override def start(ps: Stage) = {
        val root = new BorderPane()
        // root.setStyle(s"font-family: '${fsmall.getFamily}'")
        // root.setStyle("@font-face { font-family: 'Iropke'; src: url('IropkeBatangM.ttf') }; -fx-family: 'Iropke'")
        val (bp, cs) = DataStage.inputPane(drawChart, point)
        root.setLeft(bp)

        prepareRadioButtons

        val ctbox = new HBox(hButton, wButton, bButton)
        ctbox.setSpacing(20)
        ctbox.setAlignment(Pos.CENTER)
        val rtbox = new HBox(pButton, sButton)
        rtbox.setSpacing(20)
        rtbox.setAlignment(Pos.CENTER)
        pButton.setSelected(true)

        val anchor = new AnchorPane(ctbox, rtbox)
        val anchorinset = chart.width/15
        AnchorPane.setLeftAnchor(ctbox, anchorinset)
        AnchorPane.setRightAnchor(rtbox, anchorinset)
        val centralBox = new VBox(cgroup, anchor)
        root.setCenter(centralBox)

        val scene = new Scene(root, Color.WHITE)
        ps.setScene(scene)
        ps.setTitle("성장 곡선")
        
        /*
        val sizeChangeListener = new ChangeListener[Number] {
            var startTime: Option[Long] = None

            def changed(v: ObservableValue[_ <: Number], ov: Number, nv: Number) = {
                println(s"${nv.doubleValue} ${centralBox.getWidth}, ${centralBox.getHeight}")
                // chart.setWidth(centralBox.getWidth)
                // chart.setHeight(centralBox.getHeight)
            }
        }

        centralBox.widthProperty.addListener(sizeChangeListener)
        centralBox.heightProperty.addListener(sizeChangeListener)
        */
        ps.show

        chart.draw(Seq.empty, HeightChart, Percentile, false)

        ps.setMinWidth(scene.getWidth)
        ps.setMinHeight(scene.getHeight)
        ps.setResizable(false)
        // convertCSV()
    }

    private def prepareRadioButtons(): AnchorPane = {
        val ctbox = new HBox(hButton, wButton, bButton)
        ctbox.setSpacing(20)
        ctbox.setAlignment(Pos.CENTER)
        val rtbox = new HBox(pButton, sButton)
        rtbox.setSpacing(20)
        rtbox.setAlignment(Pos.CENTER)
        pButton.setSelected(true)

        (ctButtons ++ rtButtons).foreach(b =>
            b.setOnAction(DataStage.mkEventHandler[ActionEvent](e => 
                chart.draw(Seq.empty, getChartType.getOrElse(HeightChart), getReferenceType.getOrElse(Percentile), false)
            ))
        )

        val anchor = new AnchorPane(ctbox, rtbox)
        val anchorinset = chart.width/15
        AnchorPane.setLeftAnchor(ctbox, anchorinset)
        AnchorPane.setRightAnchor(rtbox, anchorinset)
        anchor
    }

    def drawChart(rs: Seq[PatientRecord] = Seq.empty) = {
        val available: Seq[Option[ChartType]] = Calc.availableTypes(rs)
        val combo: Seq[(RadioButton, Option[ChartType])] = ctButtons.zip(available)
        combo.foreach({ case ((btn, oct)) =>
            btn.setDisable(oct.isEmpty)
        })
        val select = combo.filterNot(_._1.isDisabled()).headOption
        select.foreach({ case ((b, ctype)) =>
            b.setSelected(true)
        })
        chart.draw(rs, select.flatMap(_._2).getOrElse(HeightChart), 
            getReferenceType().getOrElse(Percentile), true)
    }

    def point(i: Int) = {
        chart.emphasizeMeasure(i, getChartType().get, getReferenceType().get)
    }

    private def getChartType(): Option[ChartType] = {
        ctButtons.find(_.isSelected).map(_ match {
            case `hButton` => HeightChart
            case `wButton` => WeightChart
            case `bButton` => BMIChart
        })
    }

    private def getReferenceType(): Option[RefType] = {
        rtButtons.find(_.isSelected).map(_ match {
            case `pButton` => Percentile
            case `sButton` => SD
        })
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

    // setfont - root pane 이하 모든 children의 font 지정
    def setFont() = ???
}