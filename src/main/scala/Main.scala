import org.apache.spark.{SparkConf, SparkContext}

import scala.io.Source

/**
  * @author Wojciech Pachuta.
  */
object Main {

  val NumberOfSamples: Int = 1000000

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Spark Pi")
    println("******************************************************************************************")
    println("******************************************************************************************")
    println(s"args:")
    println(args.mkString(" "))
    Source.fromInputStream(getClass.getResourceAsStream("data.csv")).getLines().foreach(println(_))
    println(s"from file:")
    println("******************************************************************************************")
    println("******************************************************************************************")

    println("******************************************************************************************")
    println("******************************************************************************************")
    println(s"from internet:")
    println(scala.io.Source.fromURL("http://www.agh.edu.pl").mkString)
    println("******************************************************************************************")
    println("******************************************************************************************")

    val sc = new SparkContext(conf)
    val count = sc.parallelize(1 to NumberOfSamples).filter { _ =>
      val x = math.random
      val y = math.random
      x * x + y * y < 1
    }.count()
    println("******************************************************************************************")
    println("******************************************************************************************")
    println(s"Pi is roughly ${4.0 * count / NumberOfSamples}")
    println("******************************************************************************************")
    println("******************************************************************************************")
    sc.stop()
  }

}
