import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.graphx.{GraphLoader, PartitionStrategy}

/**
  * @author Wojciech Pachuta
  * @author Bartłomiej Grochal
  */
object Main {

  def main(args: Array[String]): Unit = {
    /* Creating a Spark Context - the entry point for Spark. */
    val sparkConfiguration = new SparkConf()
      .setAppName("Triangle Counting")
    val sparkContext = new SparkContext(sparkConfiguration)

    /* Loading a contribution graph from the file. */
    val contributionGraph = GraphLoader
      .edgeListFile(sparkContext, "src/main/resources/data/collaborations.txt", true)
      .partitionBy(PartitionStrategy.RandomVertexCut)

    /* Computing the number of triangles passing through each vertex. */
    val trianglesCount = contributionGraph.triangleCount().vertices

    /* Printing the results. */
    println(trianglesCount.collect      // Getting the mapping: UserID -> triangles count
      .sortWith(_._2 < _._2)		// Sorting increasingly by the values
      .filter(_._2 > 0)			// Filtering only non-zero results
      .mkString("\n"))			// Printing each pair in the new line

    /* Finishing. */
    sparkContext.stop()
  }

}

