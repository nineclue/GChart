import javafx.scene.control.Dialog
import javafx.scene.control.ButtonType
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.layout.GridPane
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.control.ToggleGroup
import javafx.scene.control.skin.RadioButtonSkin
import javafx.scene.control.RadioButton
import javafx.scene.layout.HBox
import javafx.scene.control.DatePicker
import javafx.scene.control.DialogPane
import javafx.util.Callback
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.scene.control.Toggle

object ModifyDialog {
    val updateButton = new ButtonType("수정", ButtonData.APPLY)
    val cancelButton = new ButtonType("취소", ButtonData.CANCEL_CLOSE)

    def apply(r: PatientRecord): Dialog[Option[PatientRecord]] = {
        val d = new Dialog[Option[PatientRecord]]()
        d.setTitle("수 정")
        val (dp, harvestF) = dPane(r)
        d.setDialogPane(dp)
        // d.setResult(None)
        d.setResultConverter(new Callback[ButtonType, Option[PatientRecord]] {
            def call(bt: ButtonType) = bt match {
                case `updateButton` => 
                    val nrecord = harvestF()
                    Some(nrecord)
                case `cancelButton` => None
            }
        })
        d
    }
    
    private def dPane(r: PatientRecord) = {
        val gp = new GridPane()

        val cLabel = new Label("차트번호")
        gp.add(cLabel, 0, 0)
        val ciInput = new TextField(r.chartno)
        gp.add(ciInput, 1, 0)

        val sLabel = new Label("성별")
        gp.add(sLabel, 0, 1)
        val sGroup = new ToggleGroup()
        val mButton = new RadioButton("남")
        val fButton = new RadioButton("여")
        mButton.setToggleGroup(sGroup)
        fButton.setToggleGroup(sGroup)

        if (r.sex == "M") mButton.setSelected(true)
        else fButton.setSelected(true)
        val hbox = new HBox(mButton, fButton)
        gp.add(hbox, 1, 1)

        val bLabel = new Label("생년월일")
        gp.add(bLabel, 0, 2)
        val bPicker = new DatePicker(r.bday)
        bPicker.focusedProperty.addListener(DataStage.focusedListener(bPicker))
        bPicker.setConverter(CalendarConverter)
        gp.add(bPicker, 1, 2)

        val dp = new DialogPane()
        dp.setContent(gp)
        dp.getButtonTypes().addAll(cancelButton, updateButton)

        def sexString() = if (mButton.isSelected()) "M" else "F"
        def harvest(): PatientRecord = r.copy(chartno = ciInput.getText,
            sex = sexString(),
            bday = bPicker.getValue())
        
        def checkButtonEnable(r: PatientRecord) = {
            val ncn = ciInput.getText().trim
            val modified = (ncn.nonEmpty && ncn != r.chartno) ||
                sexString() != r.sex ||
                bPicker.getValue() != r.bday
            dp.lookupButton(updateButton).setDisable(!modified)
        }

        ciInput.textProperty().addListener(new ChangeListener[String] {
            def changed(obv: ObservableValue[_ <: String], ov: String, nv: String) = {
                checkButtonEnable(r)
            }
        })
        sGroup.selectedToggleProperty().addListener(new ChangeListener[Toggle] {
            def changed(obv: ObservableValue[_ <: Toggle], ov: Toggle, nv: Toggle) = {
                checkButtonEnable(r)
            }
        })

        (dp, harvest _)
    }
}