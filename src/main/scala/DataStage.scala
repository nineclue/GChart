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

    val records: ListView[PatientRecord]
}

object DataStage {
    def apply(parent: Stage): Stage = {
        val ds = new Stage(StageStyle.UNDECORATED)
        ds.initOwner(parent)
        ds.initModality(Modality.APPLICATION_MODAL)
        ds.setScene(new Scene(inputForm()))
        ds
    }

    private def inputForm(): Pane = {
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

            val records: ListView[PatientRecord] = new ListView[PatientRecord]()
        }
        val b1 = new HBox(pi.chartLabel, pi.chartInput)
        val b2 = new HBox(pi.bdayLabel, pi.bdayInput, pi.idayLabel, pi.idayInput)
        val b3 = new HBox(pi.heightLabel, pi.heigthInput, pi.weightLabel, pi.weightInput, pi.bmiLabel, pi.bmiValue)
        new VBox(b1, b2, b3, pi.records)
    }
}