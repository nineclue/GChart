import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Modality
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.control.DatePicker
import javafx.scene.control.ListView
import java.{util => ju}
import java.time.LocalDate
import javafx.scene.layout.Pane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.geometry.Insets
import java.awt.Event
import javafx.event.ActionEvent
import javafx.event.EventHandler

case class PatientRecord(iday: LocalDate, height: Double, weight: Double)

trait PatientInput {
    val chartLabel: Label 
    val chartInput: TextField

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

    val records: ListView[PatientRecord]
}

object DataStage {
    type InputPane = (Pane, PatientInput)
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

    private def inputPane(): InputPane = {
        val pi = new PatientInput {
            val chartLabel: Label = new Label("차트번호")
            val chartInput: TextField = new TextField()

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
            val records: ListView[PatientRecord] = new ListView[PatientRecord]()
        }

        val b1 = new HBox(pi.chartLabel, pi.chartInput)
        val b2 = new HBox(pi.bdayLabel, pi.bdayInput)
        val b3 = new HBox(pi.idayLabel, pi.idayInput)
        val b4 = new HBox(pi.heightLabel, pi.heigthInput)
        val b5 = new HBox(pi.weightLabel, pi.weightInput)
        val b6 = new HBox(pi.bmiLabel, pi.bmiValue)
        val b7 = new HBox(pi.commitButton)
 
        Seq(b1,b2,b3,b4,b5,b6,b7).foreach(setPadSpace)
        (new VBox(b1, b2, b3, b4, b5, b6, b7, pi.records), pi)
    }

    private def setPadSpace(p: HBox) = {
        p.setSpacing(10)
        p.setPadding(new Insets(3))
    }
}