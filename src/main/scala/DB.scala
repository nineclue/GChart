import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import doobie.util.fragments.whereAndOpt
// import doobie.implicits.legacy.localdate._
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import java.sql.DriverManager
import java.time.LocalDate

object DB {
    // lazy val connection = DriverManager.getConnection("jdbc:sqlite:gchart.db")
    implicit val cs = IO.contextShift(ExecutionContexts.synchronous)
    val xa = Transactor.fromDriverManager[IO]("org.sqlite.JDBC", "jdbc:sqlite:gchart.db", "", "")

    implicit val localDateMeta: Meta[LocalDate] = Meta[String].imap(LocalDate.parse)(_.toString)

    val createPerson = sql"""
    CREATE TABLE IF NOT EXISTS person (
        chartno TEXT PRIMARY KEY,
        sex TEXT NOT NULL,
        birthday TEXT NOT NULL
    )
    """.update.run

    val createMeasures = sql"""
    CREATE TABLE IF NOT EXISTS measures (
        chartno TEXT NOT NULL,
        measuredate TEXT NOT NULL,
        height REAL,
        weight REAL, 
        UNIQUE (chartno, measuredate) ON CONFLICT IGNORE,
        FOREIGN KEY (chartno) REFERENCES person(charno)
    )
    """.update.run

    def init() = {
        (createPerson, createMeasures).mapN(_ + _).transact(xa).unsafeRunSync
    }

    def get(cno: String, iday: Option[LocalDate] = None) = {
        val chartf = Some(fr"p.chartno = $cno")
        val idayf = iday.map(d => fr"measuredate = $d")
        val q = sql""" 
        SELECT p.chartno, sex, birthday, measuredate, height, weight 
        FROM person p INNER JOIN measures m on p.chartno = m.chartno
        """ ++ whereAndOpt(chartf, idayf) ++ fr"""ORDER BY measuredate"""
        // println(q.toString())
        q.query[PatientRecord].to[List].transact(xa).unsafeRunSync
    }

    def put(r: PatientRecord) = {
        val pinsert = sql"""
        INSERT OR IGNORE INTO person VALUES (${r.chartno}, ${r.sex}, ${r.bday})
        """.update.run
        val mupsert = sql"""
        INSERT INTO measures VALUES (${r.chartno}, ${r.iday}, ${r.height}, ${r.weight})
        ON CONFLICT (chartno, measuredate) 
        DO UPDATE SET height = ${r.height}, weight = ${r.weight}
        WHERE chartno = ${r.chartno} AND measuredate = ${r.iday}
        """.update.run
        (pinsert, mupsert).mapN(_ + _).transact(xa).unsafeRunSync
    }

    def deleteMeasure(cno: String, iday: LocalDate) = {
        sql"""
        DELETE FROM measures WHERE chartno = $cno AND measuredate = $iday
        """.update.run.transact(xa).unsafeRunSync
    }

    def changePerson(o: PatientRecord, n: PatientRecord) = {
        sql"""
        UPDATE person SET chartno = ${o.chartno}, sex = ${o.sex}, birthday = ${o.bday}
        WHERE chartno = ${n.chartno}, sex = ${n.sex}, birthday = ${n.bday}
        """.update.run.transact(xa).unsafeRunSync()
    }

    /*
    def test() = {
        val createPi = sql"""
        CREATE TABLE IF NOT EXISTS pi (
            nums TEXT
        )
        """.update.run

        createPi.transact(xa).unsafeRunSync()

        Seq("3.1", "451", "592", "654").foreach({ n =>
            val insert = sql"""
                INSERT INTO pi values($n)
            """.update.run
            insert.transact(xa).unsafeRunSync()
        })

        val nums = sql"""
            SELECT nums FROM pi
        """.query[String].to[List].transact(xa).unsafeRunSync()

        val dropPi = sql"""
        DROP TABLE pi
        """.update.run

        dropPi.transact(xa).unsafeRunSync()

        nums.mkString
    }
    */
}