package MachineLearning.Preprocessing
import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.feature.{OneHotEncoderEstimator, StandardScaler, StringIndexer, VectorAssembler}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.LogisticRegression
object PipelinesOps {
  def main(args: Array[String]): Unit = {
    ///////////////////// LOG SEVİYESİNİ AYARLAMA /////////////////////
    Logger.getLogger("org").setLevel(Level.ERROR)

    ///////////////////// SPARK SESSION OLUŞTURMA /////////////////////
    val spark = SparkSession.builder()
      .appName("StringIndexerOps")
      .master("local[4]")
      .config("spark.driver.memory", "2g")
      .config("spark.executor.memory", "4g")
      .getOrCreate()

    val sc = spark.sparkContext

    ///////////////////// VERİ OKUMA ///////////////////////////////////
    val df = spark.read.format("csv")
      .option("header","true")
      .option("inferSchema","true")
      .option("sep",",")
      .load("D:\\Datasets\\simple_data.csv")

    println("Orjinal DF:")
    df.show()


    ///////////////////// VERİ SETİNE ETİKET EKLEME ///////////////////////////////////
    // Sınıflandırma hedef değişken (etiket-label) yaratmak adına
    // Geliri 7000 üstü olanların ekonomik_durumu iyi diyelim.

    val df1 = df.withColumn("ekonomik_durum",
      when(col("aylik_gelir").gt(7000),"iyi")
        .otherwise("kötü")
    )

    // DF'i BÖL
    val Array(trainDF, testDF) = df1.randomSplit(Array(0.8, 0.2), 142L)

    val meslekIndexer = new StringIndexer()
      .setInputCol("meslek")
      .setOutputCol("meslekIndex")
      .setHandleInvalid("skip")

    val sehirIndexer = new StringIndexer()
      .setInputCol("sehir")
      .setOutputCol("sehirIndex")
      .setHandleInvalid("skip")

    val encoder = new OneHotEncoderEstimator()
      .setInputCols(Array[String]("meslekIndex","sehirIndex"))
      .setOutputCols(Array[String]("meslekIndexEncoded","sehirIndexEncoded"))

    val vectorAssembler = new VectorAssembler()
      .setInputCols(Array[String]("meslekIndexEncoded","sehirIndexEncoded","yas","aylik_gelir"))
      .setOutputCol("vectorizedFeatures")

    val labelIndexer = new StringIndexer()
      .setInputCol("ekonomik_durum")
      .setOutputCol("label")

    val scaler = new StandardScaler()
      .setInputCol("vectorizedFeatures")
      .setOutputCol("features")

    val classifier = new LogisticRegression()
      .setLabelCol("label")
      .setFeaturesCol("features")
      .setPredictionCol("prediction")

    val pipelineObj = new Pipeline()
      .setStages(Array(meslekIndexer, sehirIndexer, encoder, vectorAssembler, labelIndexer, scaler, classifier))

    val pipelineModel = pipelineObj.fit(trainDF)
    pipelineModel.transform(testDF).show(false)


  }
}
