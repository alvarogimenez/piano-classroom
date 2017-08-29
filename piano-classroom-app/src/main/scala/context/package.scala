import java.io.{File, FileNotFoundException, PrintWriter}

import io.contracts.{SessionContract, initializeEmptySesionContract}
import io.{fromJson, toJson}

import scala.io.Source
import scala.util.{Failure, Success, Try}

package object context {
  def readSessionSettings(): SessionContract = {
    val sessionContract =
      Try(Source.fromFile("session.json").mkString)
        .map(fromJson[SessionContract])
        .recoverWith {
          case e: FileNotFoundException =>
            println(s"No session.json file found in the working directory. Creating an empty one")
            val emptySession = initializeEmptySesionContract()
            Try(toJson(emptySession))
              .map { source =>
                val w = new PrintWriter(new File("session.json"))
                w.write(source)
                w.close()
              }
              .map(_ => emptySession)
              .recoverWith {
                case e: Exception =>
                  throw new Exception(s"Error while writing session.json: '${e.getMessage}'")
              }
          case e: Exception =>
            throw new Exception(s"Error while reading session.json: '${e.getMessage}'")
        }

    sessionContract match {
      case Success(contract) => contract
      case Failure(exception) => throw exception
    }
  }

  def writeSessionSettings(session: SessionContract): Unit = {
    try {
      val w = new PrintWriter(new File("session.json"))
      w.write(toJson(session))
      w.close()
    } catch {
      case e: Exception =>
        println(s"Error writing session.json file: '${e.getMessage}'")
    }
  }
}
