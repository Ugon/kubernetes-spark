import org.apache.spark.{SparkConf, SparkContext}

/**
  * @author Wojciech Pachuta.
  */
object Main {

  val NumberOfSamples: Int = 1000000000

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Spark Pi")
    val sc = new SparkContext(conf)
    val count = sc.parallelize(1 to NumberOfSamples).filter { _ =>
      val x = math.random
      val y = math.random
      x*x + y*y < 1
    }.count()
    println("******************************************************************************************")
    println("******************************************************************************************")
    println(s"Pi is roughly ${4.0 * count / NumberOfSamples}")
    println("******************************************************************************************")
    println("******************************************************************************************")
    sc.stop()
  }

}
