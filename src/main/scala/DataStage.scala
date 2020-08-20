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
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.stage.WindowEvent
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.KeyEvent
import javafx.util.StringConverter
import javafx.application.Platform
import javafx.scene.control.SelectionMode
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonType

case class PatientRecord(chartno: String, sex: String, bday: LocalDate, iday: LocalDate, height: Option[Double], weight: Option[Double]) {
    val bmi: Option[Double] = height.flatMap(h => weight.map({ w =>
        val hmeter = h / 100
        w / (hmeter * hmeter)
    }))
    private val idayProperty = new SimpleStringProperty(iday.toString)
    private val heightProperty = new SimpleStringProperty(height.map(_.toString).getOrElse(""))
    private val weightProperty = new SimpleStringProperty(weight.map(_.toString).getOrElse(""))

    def getIday() = idayProperty.get
    def setIday(d: String) = LocalDate.parse(d)

    def getHeight = heightProperty.get
    def setHeight(h: String) = heightProperty.set(h)

    def getWeight = weightProperty.get
    def setWeight(w: String) = weightProperty.set(w)
}

object CalendarConverter extends StringConverter[LocalDate] {
    def fromString(s: String): LocalDate = {
        val date = raw"(\d{2,4})(\D)(\d{1,2})\2(\d{1,2})".r
        s match {
            case date(ys, _, m, d) => 
                val y = ys.toInt
                val year = 
                    if (y < 100) {
                        if (y >= (LocalDate.now.getYear % 100)) y + 1900
                        else y + 2000
                    } else y
                LocalDate.of(year, m.toInt, d.toInt)
            case _ =>
                LocalDate.now
        }
    }
    def toString(d: LocalDate): String = s"${d.getYear}년 ${d.getMonthValue}월 ${d.getDayOfMonth}일"
}

import DataStage.DrawFunction
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
    val heightInput: TextField
    val weightLabel: Label 
    val weightInput: TextField
    val bmiLabel: Label 
    val bmiValue: Label 

    val commitButton: Button

    val records: TableView[PatientRecord]
    val drawFunction: DrawFunction
}

case class Menu(title: String, disable: () => Boolean, action: (ActionEvent) => Unit)

object DataStage {
    type InputPane = (Pane, PatientInput)
    type DrawFunction = Seq[PatientRecord] => Unit
    val bmiDefault = "N/A"
    val records = FXCollections.observableArrayList[PatientRecord]()
    var loadedRecord: Option[PatientRecord] = None

    def inputPane(drawF: DrawFunction): InputPane = {
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

            val heightLabel: Label = new Label("키 (cm)")
            val heightInput: TextField = new TextField()
            val weightLabel: Label = new Label("몸무게 (Kg)")
            val weightInput: TextField = new TextField()
            val bmiLabel: Label = new Label("BMI")
            val bmiValue: Label = new Label(bmiDefault)

            val commitButton: Button = new Button("저장 및 그래프")
            val records: TableView[PatientRecord] = new TableView[PatientRecord]()

            val drawFunction: DrawFunction = drawF
        }

        // put gridpane 
        val p = setPosition(pi)
        setHandlers(pi)

        tableSet(pi)
        (p, pi)
    }

    private def setPosition(pi: PatientInput): Pane = {
        val p = new GridPane()
        val firstCol: Seq[Node] = Seq(pi.chartLabel, pi.sexLabel, pi.bdayLabel, pi.idayLabel, pi.heightLabel, pi.weightLabel, pi.bmiLabel)
        firstCol.zipWithIndex.foreach({ case (n, i) =>
            setGridPos(n, 0, i)
        })

        val sexGroup = new ToggleGroup()
        pi.maleButton.setToggleGroup(sexGroup)
        pi.femaleButton.setToggleGroup(sexGroup)
        pi.maleButton.setSelected(true)
        val sexBox = new HBox(pi.maleButton, pi.femaleButton)
        
        val secondCol: Seq[Node] = Seq(pi.chartInput, sexBox, pi.bdayInput, pi.idayInput, pi.heightInput, pi.weightInput, pi.bmiValue, pi.commitButton)
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
        p
    }

    private def content(i: TextField) = i.getText.trim

    private def setHandlers(pi: PatientInput) = {
        // 현재 저장가능한지
        def savable() = {
            val cnoOk = content(pi.chartInput).nonEmpty
            val heightOk = content(pi.heightInput).toDoubleOption.nonEmpty
            val weightOk = content(pi.weightInput).toDoubleOption.nonEmpty
            cnoOk && (heightOk || weightOk)
        }
        // 환자 기본 정보 수정 가능 여부
        def notModifiable() = {
            val cnoOk = content(pi.chartInput).nonEmpty
            val iday = pi.idayInput.getValue
            val bday = pi.bdayInput.getValue
            !(cnoOk && (iday.compareTo(bday) > 0))
        }

        // 저장 그래프 버트 활성화
        val inputHandler = mkEventHandler[KeyEvent](e => pi.commitButton.setDisable(!savable()))
        pi.chartInput.setOnKeyTyped(inputHandler)
        pi.heightInput.setOnKeyTyped(inputHandler)
        pi.weightInput.setOnKeyTyped(inputHandler)

        // 신장, 체중에 숫자만 입력 & bmi update
        val bmiListener = new ChangeListener[String] {
            def changed(obv: ObservableValue[_ <: String], ov: String, nv: String) = {
                if (content(pi.heightInput).nonEmpty && content(pi.weightInput).nonEmpty) {
                    val bmi = 
                        pi.heightInput.getText.toDoubleOption.flatMap(h =>
                            pi.weightInput.getText.toDoubleOption.map({ w =>
                                val hmeter = h / 100
                                w / (hmeter * hmeter)
                            })
                        )
                    pi.bmiValue.setText(bmi.map("%.2f".format(_)).getOrElse(bmiDefault))
                } else {
                    pi.bmiValue.setText(bmiDefault)
                }
            }
        }
        pi.heightInput.textProperty().addListener(mkNumberInputHandler(pi.heightInput))
        pi.heightInput.textProperty().addListener(bmiListener)
        pi.weightInput.textProperty().addListener(mkNumberInputHandler(pi.weightInput))
        pi.weightInput.textProperty().addListener(bmiListener)
        pi.commitButton.setDisable(true)

        // 생일, 측정일 basic parser
        pi.bdayInput.setConverter(CalendarConverter)
        pi.idayInput.setConverter(CalendarConverter)

        // 생일, 측정일 focus시 기존 입력이 모두 선택되도록 
        def focused(p: DatePicker) = new ChangeListener[java.lang.Boolean] {
            def changed(obv: ObservableValue[_ <: java.lang.Boolean], ov: java.lang.Boolean, nv: java.lang.Boolean) = {
                if (nv) { 
                    Platform.runLater(new Runnable {
                        def run() = p.getEditor.selectAll
                    })
                }
            }
        }
        pi.bdayInput.focusedProperty().addListener(focused(pi.bdayInput))
        pi.idayInput.focusedProperty().addListener(focused(pi.idayInput))

        val modifyMenu = Seq(Menu("수정", notModifiable, (_) => println("수정!!!")))
        pi.chartLabel.setOnContextMenuRequested(mkMenuHandler(modifyMenu))

        pi.chartInput.setOnAction(mkEventHandler[ActionEvent](e => loadPatient(pi)))

        pi.commitButton.setOnAction(mkEventHandler[ActionEvent](e => saveAndGraph(pi)))
    }

    private def saveAndGraph(pi: PatientInput) = {
        val r = harvest(pi)
        DB.put(r)
        // table reload ?
        val rs = loadPatient(pi)
        pi.drawFunction(rs)
    }

    private def harvest(pi: PatientInput) = 
        PatientRecord(content(pi.chartInput), 
            if (pi.maleButton.isSelected) "M" else "F", 
            pi.bdayInput.getValue, pi.idayInput.getValue,
            pi.heightInput.getText.toDoubleOption, 
            pi.weightInput.getText.toDoubleOption)

    // 차트번호에서 Enter 치면 실행되는 루틴
    // 기존 기록 읽고 없으면 환자 기록 수정 가능 설정
    private def loadPatient(pi: PatientInput): Seq[PatientRecord] = {
        val rs = DB.get(content(pi.chartInput))
        // 처음 loading시는 새로운 자료를 입력 받는 걸로
        // setPatientRelatedFields( -> updateControls)에서 fields update 함
        loadedRecord = None
        
        records.setAll(rs:_*)
        setPatientRelatedFields(pi, rs)        
        rs
    }

    private def setPatientRelatedFields(pi: PatientInput, records: Seq[PatientRecord]) = {
        val enable = records.isEmpty
        // println(s"${records.length} => $enable")
        records.headOption.foreach(r => updateControls(pi, r, true))
        pi.maleButton.setDisable(!enable)
        pi.femaleButton.setDisable(!enable)
        pi.bdayInput.setEditable(enable)
        pi.bdayInput.setDisable(!enable)

        //
        pi.idayInput.setValue(LocalDate.now)
        pi.heightInput.setText("")
        pi.weightInput.setText("")
    }

    private def updateControls(pi: PatientInput, r: PatientRecord, basicOnly: Boolean) = {
        if (r.sex == "M") pi.maleButton.setSelected(true) 
        else pi.femaleButton.setSelected(true)

        pi.bdayInput.setValue(r.bday)

        if (!basicOnly) {
            pi.idayInput.setValue(r.iday)

            pi.heightInput.setText(if (r.height.nonEmpty) r.height.get.toString else "")
            pi.weightInput.setText(if (r.weight.nonEmpty) r.weight.get.toString else "")
        }
    }

    private def tableSet(pi: PatientInput) = {
        val t = pi.records
        t.setEditable(false)
        t.getSelectionModel.setSelectionMode(SelectionMode.SINGLE)        

        val c1 = new TableColumn[PatientRecord, String]("기록일")
        val c2 = new TableColumn[PatientRecord, String]("키")
        val c3 = new TableColumn[PatientRecord, String]("몸무게")
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

        c1.setCellValueFactory(new PropertyValueFactory[PatientRecord, String]("iday"))
        c2.setCellValueFactory(new PropertyValueFactory[PatientRecord, String]("height"))
        c3.setCellValueFactory(new PropertyValueFactory[PatientRecord, String]("weight"))

        t.setPlaceholder(new Label("이전 자료가 없습니다."))
        t.setItems(records)
        t.getSelectionModel.getSelectedIndices.addListener(
            new ListChangeListener[Integer] {
                def onChanged(c: ListChangeListener.Change[_ <: Integer]) = {
                    while (c.next) {
                        println(s"ListChangeListener: $c")
                        if (c.wasAdded()) 
                            updateControls(pi, records.get(t.getSelectionModel().getSelectedIndex()), false)
                    }
                }
            }
        )
        
        def tableNotSelected() = t.getSelectionModel().isEmpty
        val m1 = Seq(Menu("delete", tableNotSelected, (_) => deleteRecord(pi)))
        t.setOnContextMenuRequested(mkMenuHandler(m1))
    }

    private def deleteRecord(pi: PatientInput) = {
        val cno = content(pi.chartInput)
        val iday = pi.idayInput.getValue()
        val alert = new Alert(AlertType.CONFIRMATION, s"$iday 기록을 삭제합니다.", ButtonType.NO, ButtonType.OK)
        val choice = alert.showAndWait()
        if (choice.isPresent() && choice.get() == ButtonType.OK) {
            DB.deleteMeasure(cno, iday)
            loadPatient(pi)
        }
    }

    private def mkEventHandler[A <: javafx.event.Event](action: A => Unit): EventHandler[A] = 
        new EventHandler[A] {
            def handle(e: A) = action(e)
        }

    private def mkMenuHandler[A, B](menus: Seq[Menu]): EventHandler[ContextMenuEvent] = {
        val m = new ContextMenu()
        val ms = menus.map({ case mi => 
            val itm = new MenuItem(mi.title)
            itm.setOnAction(new EventHandler[ActionEvent] {
                def handle(e: ActionEvent) = mi.action(e)
            })
            itm
        })
        m.getItems.addAll(ms:_*)
        new EventHandler[ContextMenuEvent] {
            def handle(e: ContextMenuEvent) = {
                menus.zip(ms).foreach({ case ((m, mi)) => 
                    mi.setDisable(m.disable())
                })                
                m.show(e.getSource.asInstanceOf[Node], e.getScreenX(), e.getScreenY)
            }
        }
    }

    private def mkNumberInputHandler(s: TextField) = new ChangeListener[String] {
        val nums = raw"[0-9]+[.]?[0-9]*"
        def changed(obv: ObservableValue[_ <: String], ov: String, nv: String) = {
            if (!nv.matches(nums)) {
                s.setText(nv.init)  // ignore last input
            }
        }
    }

    private def setGridPos(n: Node, x: Int, y: Int) = {
        GridPane.setColumnIndex(n, x)
        GridPane.setRowIndex(n, y)
    }

    private def runLater(action: => Unit) = 
        Platform.runLater(
            new Runnable {
                def run() = action               
            }
        )
}