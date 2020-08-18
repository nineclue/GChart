import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import doobie.util.fragments.whereAndOpt
import doobie.implicits.legacy.localdate._
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
        // createMeasures.transact(xa).unsafeRunSync
    }

    def get(cno: String, iday: Option[LocalDate] = None) = {
        val idayf = iday.map(d => fr"measuredate = $d")
        val q = sql""" 
        SELECT p.chartno, sex, birthday, date(measuredate, 'unixepoch'), height, weight 
        FROM person p, measures m WHERE p.chartno = $cno AND
        p.chartno = m.chartno
        """ ++ whereAndOpt(idayf) ++ fr"""ORDER BY measuredate"""
        q.query[PatientRecord].to[List].transact(xa).unsafeRunSync
    }

    def put(r: PatientRecord) = {
        val m = if (r.male) "M" else "F"
        val pinsert = sql"""
        INSERT OR IGNORE INTO person VALUES (${r.chartno}, $m, ${r.bday})
        """.update.run
        val mupsert = sql"""
        INSERT INTO measures VALUES (${r.chartno}, ${r.iday}, ${r.height}, ${r.weight})
        ON CONFLICT (chartno, measuredate) 
        DO UPDATE SET height = ${r.height}, weight = ${r.weight}
        WHERE chartno = ${r.chartno} AND measuredate = ${r.iday}
        """.update.run
        (pinsert, mupsert).mapN(_ + _).transact(xa).unsafeRunSync
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