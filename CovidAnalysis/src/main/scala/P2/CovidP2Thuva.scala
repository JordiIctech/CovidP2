package P2

import P2.Main.session
import P2.P2tempviews.df4
import org.apache.spark.sql.functions.{col, desc, round}

object CovidP2Thuva {
  def UsDeathDataByMonth(): Unit = {
    val MonthDayCount = Array(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    println("Loading Sum of  Covid Death Data By Month wise ...........")
    var df0 = session.spark.read.format("csv").option("header", "true").option("inferSchema", "true").load("hdfs://localhost:9000/user/hive/warehouse/time_series_covid_19_deaths_US.csv")
     df0 = df0.withColumnRenamed("Province_state", "USState")
    var LastDayOfMonth = ""
    var Month = 1
    var year = 20
    var TableColumn = Seq(col("USState"), col("Population"))
    var SumOfMap = Map("Population" -> "sum")
    while (!(Month == 5 && year == 21)) {
      LastDayOfMonth = Month + "/" + MonthDayCount(Month) + "/" + year
      SumOfMap += (LastDayOfMonth -> "sum")

      TableColumn = TableColumn :+ col(LastDayOfMonth)
      Month += 1
      if (Month == 13) {
        Month = 1
        year += 1
      }
    }
    df0 = df0
      .select(TableColumn: _*)
      .groupBy("USState")
      .agg(SumOfMap)
      .withColumnRenamed("sum(Population)", "Population")
    Month = 1
    year = 20
    while (!(Month == 5 && year == 21)) {
      LastDayOfMonth = Month + "/" + MonthDayCount(Month) + "/" + year
      df0 = df0.withColumnRenamed(s"sum(${LastDayOfMonth})", LastDayOfMonth)
      //df = df.withColumnRenamed(s"avg(${LastDayOfMonth})", LastDayOfMonth)
      Month += 1

      if (Month == 13) {
        Month = 1
        year += 1
      }
    }
    df0 = df0
      .select(TableColumn: _*)
      .orderBy(desc("Population"))

    df0.show(false)

    file.outputJson("USDeathSumByMonth", df0)

  }

  def DeathVSRecoverPercentage(): Unit = {
    val df1 = session.spark.read.option("header", "true").csv("hdfs://localhost:9000/user/hive/warehouse/covid_19_data.csv")
    df1.createOrReplaceTempView("CovidWorld")
    println("Top 10 Countries which has best Recovery Percentage against Confirmed Cases..")
    session.spark.sql("SELECT  `Country/Region` AS Country, SUM(Confirmed) AS TotalConfirmed,SUM(Recovered) AS TotalRecovered ,ROUND((SUM(Recovered) * 100 )/SUM(Confirmed))  as RecoverPercentage FROM CovidWorld GROUP BY  `Country/Region` order by  RecoverPercentage DESC").show(10)

    println("_____________________________________________________________________________")
    println("Top 10 Counries which has worst Death Percentage against Confirmed Cases..")

    session.spark.sql("SELECT  `Country/Region` AS Country, SUM(Confirmed) AS TotalConfirmed,SUM(Deaths) AS TotalDeaths ,ROUND((SUM(Deaths) * 100 )/SUM(Confirmed))  as DeathPercentage FROM CovidWorld GROUP BY  `Country/Region` order by  DeathPercentage DESC").show(10)

  }



  def TopDays(): Unit = {
    println("Load top 20 Days Recorded highest Confirmed Cases between 2020 -2021")

    val df1 = session.spark.read.option("header", "true").csv("hdfs://localhost:9000/user/hive/warehouse/covid_19_data.csv")
    df1.createOrReplaceTempView("CovidCases")

    session.spark.sql(" SELECT ObservationDate , `Province/State`, `Country/Region`, max(Confirmed) AS MaxCases " +
      " FROM CovidCases " +
      " GROUP BY ObservationDate,`Country/Region`,`Province/State` ").createTempView("Query1")

    session.spark.sql(" SELECT ObservationDate, FLOOR(sum(MaxCases)) AS CombineDailyCases" +
      " FROM Query1" +
      " GROUP BY ObservationDate" +
      " ORDER BY CombineDailyCases ASC").createTempView("Query2") // Sum Daily Cases by Date wise

    session.spark.sql(" SELECT ObservationDate, CombineDailyCases, " +
      " LAG(CombineDailyCases,1) OVER(" +
      " ORDER BY CombineDailyCases ASC) AS PreviousDailyCases" +
      " FROM Query2" +
      " ORDER BY CombineDailyCases ASC").createTempView("Query3") //Check Previus Values

    session.spark.sql(" SELECT ObservationDate as Date, CombineDailyCases - IFNULL(PreviousDailyCases,0) AS NewCases" +
      " FROM Query3" +
      " ORDER BY NewCases DESC").show(20)
  }


  def run():Unit = {
    UsDeathDataByMonth()
    DeathVSRecoverPercentage()

    TopDays()

  }

}
