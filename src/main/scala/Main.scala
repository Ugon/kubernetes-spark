import org.apache.spark.graphx.{GraphLoader, PartitionStrategy}
import org.apache.spark.{SparkConf, SparkContext}

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
      .edgeListFile(sparkContext, args(0), true)
      .partitionBy(PartitionStrategy.RandomVertexCut)

    /* Computing the number of triangles passing through each vertex. */
    val trianglesCount = contributionGraph.triangleCount().vertices

    /* Printing the results. */
    trianglesCount
      .collect                // Getting the mapping: UserID -> triangles count
      .filter(_._2 > 0)       // Filtering only non-zero results
      .sortBy(_._2)           // Sorting increasingly by the values
      .foreach(println(_))    // Printing each pair in the new line

    /* Finishing. */
    sparkContext.stop()

  }

}

