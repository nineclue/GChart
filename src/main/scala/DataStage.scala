import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Modality
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.control.DatePicker
import javafx.scene.control.{TableView, TableColumn}

import java.time.LocalDate
import javafx.scene.layout.{Pane, GridPane, ColumnConstraints, RowConstraints, Priority}
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.geometry.{Insets, HPos}
import java.awt.Event
import javafx.event.ActionEvent
import javafx.event.EventHandler

// import javafx.beans.property.{SimpleListProperty
import javafx.beans.property.{SimpleStringProperty, SimpleDoubleProperty}
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.{FXCollections, ListChangeListener}
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.control.RadioButton
import javafx.scene.control.ToggleGroup

case class PatientRecord(iday: LocalDate, height: Double, weight: Double) {
    private val idayProperty = new SimpleStringProperty(iday.toString)
    private val heightProperty = new SimpleDoubleProperty(height)
    private val weightProperty = new SimpleDoubleProperty(weight)

    def getIday() = idayProperty.get
    def setIday(d: String) = LocalDate.parse(d)

    def getHeight = heightProperty.get
    def setHeight(h: Double) = heightProperty.set(h)

    def getWeight = weightProperty.get
    def setWeight(w: Double) = weightProperty.set(w)
}

trait PatientInput {
    val chartLabel: Label 
    val chartInput: TextField

    val sexLabel: Label
    val maleButton: RadioButton
    val femaleButton: RadioButton

    val bdayLabel: Label 
    val bdayInput: DatePicker
    val idayLabel: Label 
    val idayInput: DatePicker 

    val heightLabel: Label 
    val heigthInput: TextField
    val weightLabel: Label 
    val weightInput: TextField
    val bmiLabel: Label 
    val bmiValue: Label 

    val commitButton: Button

    val records: TableView[PatientRecord]
}

object DataStage {
    type InputPane = (Pane, PatientInput)

    /*
    def apply(parent: Stage): Stage = {
        val ds = new Stage(StageStyle.UNDECORATED)
        ds.initOwner(parent)
        ds.initModality(Modality.APPLICATION_MODAL)
        val (box, pi) = inputPane()
        pi.commitButton.setOnAction(new EventHandler[ActionEvent] {
            def handle(e: ActionEvent) = ds.close()
        })
        ds.setScene(new Scene(box))
        ds
    }
    */

    def inputPane(): InputPane = {
        val pi: PatientInput = new PatientInput {
            val chartLabel: Label = new Label("차트번호")
            val chartInput: TextField = new TextField()

            val sexLabel: Label = new Label("성별")
            val maleButton: RadioButton = new RadioButton("남")
            val femaleButton: RadioButton = new RadioButton("여")

            val bdayLabel: Label = new Label("생일")
            val bdayInput: DatePicker = new DatePicker(LocalDate.of(LocalDate.now.getYear() - 10, 1, 1))
            val idayLabel: Label = new Label("측정일")
            val idayInput: DatePicker = new DatePicker(LocalDate.now)

            val heightLabel: Label = new Label("키")
            val heigthInput: TextField = new TextField()
            val weightLabel: Label = new Label("몸무게")
            val weightInput: TextField = new TextField()
            val bmiLabel: Label = new Label("BMI")
            val bmiValue: Label = new Label("0")

            val commitButton: Button = new Button("저장 및 그래프")
            val records: TableView[PatientRecord] = new TableView[PatientRecord]()
        }

        val p = new GridPane()
        val firstCol: Seq[Node] = Seq(pi.chartLabel, pi.sexLabel, pi.bdayLabel, pi.idayLabel, pi.heightLabel, pi.weightLabel, pi.bmiLabel)
        firstCol.zipWithIndex.foreach({ case (n, i) =>
            setGridPos(n, 0, i)
        })

        val sexGroup = new ToggleGroup()
        pi.maleButton.setToggleGroup(sexGroup)
        pi.maleButton.setSelected(true)
        pi.femaleButton.setToggleGroup(sexGroup)
        val sexBox = new HBox(pi.maleButton, pi.femaleButton)

        val secondCol: Seq[Node] = Seq(pi.chartInput, sexBox, pi.bdayInput, pi.idayInput, pi.heigthInput, pi.weightInput, pi.bmiValue, pi.commitButton)
        secondCol.zipWithIndex.foreach({ case (n, i) =>
            setGridPos(n, 1, i)
        })
        setGridPos(pi.records, 0, 8)
        GridPane.setColumnSpan(pi.records, 2)
        val children = firstCol ++ secondCol :+ pi.records
        children.foreach(c => GridPane.setMargin(c, new Insets(3)))
        firstCol.foreach(c => GridPane.setHalignment(c, HPos.RIGHT))
        p.getChildren.addAll(children:_*)

        val c1con = new ColumnConstraints()
        c1con.setPercentWidth(30)
        val c2con = new ColumnConstraints()
        c2con.setPercentWidth(70)
        p.getColumnConstraints.addAll(c1con, c2con)

        val listRow = new RowConstraints()
        listRow.setMaxHeight(Double.MaxValue)
        listRow.setVgrow(Priority.ALWAYS)
        p.getRowConstraints.addAll((Range(0,7).map(_ => new RowConstraints()) :+ listRow):_*)

        p.setPadding(new Insets(10))
        val rs = tableSet[PatientRecord](pi.records)
        rs.add(PatientRecord(LocalDate.now, 182.3, 82.0))
        // p.setInsets(10)
        (p, pi)
    }

    private def tableSet[A](t: TableView[A]) = {
        t.setEditable(false)
        
        val c1 = new TableColumn[A, String]("기록일")
        val c2 = new TableColumn[A, Double]("키")
        val c3 = new TableColumn[A, Double]("몸무게")
        t.getColumns.addAll(c1, c2, c3)

        Seq(c1, c2, c3).foreach({ c => 
            c.setResizable(false)
            c.setSortable(false)
            c.setReorderable(false)
        })
        t.widthProperty.addListener(new ChangeListener[Number] {
            def changed(ob: ObservableValue[_ <: Number], ov: Number, nv: Number) = {
                val tw = nv.doubleValue
                val c1pw = tw * 0.4
                val c2pw = tw * 0.3
                val c3pw = tw - (c1pw + c2pw) - 2  // table grows in mac without extraspace
                c1.setPrefWidth(c1pw)
                c2.setPrefWidth(c2pw)
                c3.setPrefWidth(c3pw)
            }
        })

        /* // auto growing table
        c1.prefWidthProperty.bind(t.widthProperty().multiply(0.4))
        c2.prefWidthProperty.bind(t.widthProperty().multiply(0.3))
        c3.prefWidthProperty.bind(t.widthProperty().multiply(0.3))
        */

        c1.setCellValueFactory(new PropertyValueFactory[A, String]("iday"))
        c2.setCellValueFactory(new PropertyValueFactory[A, Double]("height"))
        c3.setCellValueFactory(new PropertyValueFactory[A, Double]("weight"))

        val rs = FXCollections.observableArrayList[A]()
        t.setItems(rs)
        /*
        t.getSelectionModel.getSelectedIndices.addListener(
        // (obs, oldsel, newsel) => println("changed")
            new ListChangeListener[Int] {
            def onChanged(c: ListChangeListener.Change[_ >: Int]) = println("changed")
            }
        )
        */
        // t.setContextMenu(...)
        rs
    }

    private def setGridPos(n: Node, x: Int, y: Int) = {
        GridPane.setColumnIndex(n, x)
        GridPane.setRowIndex(n, y)
    }
    private def setPadSpace(p: HBox) = {
        p.setSpacing(10)
        p.setPadding(new Insets(3))
    }
}