##Overview

SDB (Mellowtech Db) is a lightweight embedded database. It is not an RDBMS nor is it an Object store 
or a traditional key/value store, but something in between. The goal with MellowDB are the following

* Speed
* Flexible
* Powerful search features
* Easy to use

The development of SDB started in 2015 so it is still very much in an early development phase and 
we are experimenting heavily with the kind of functionality it should provide and 
how to best structure its API, http://slick.typesafe.com has been a source of inspiration

As a guiding principle we are not trying to create yet another RDBMS - there are plenty out there 
that are extremely capable - but rather looking for a new kind of database.

__1 Flexibility__  
By flexible we mean a database that meets many different needs, from general purpose record type
functionality to very schema less columnar stores. Tables in SDB can

* be fully loaded in memory
* have specific columns stored in memory
* use a row table layout
* use a column table layout
* use a hybrid layout (not yet implemented)

__2 Speed__  
A guiding principle in our philosophy with SDB is to create a fast disc based database. 
Although you can instantiate pure in-memory tables the focus is disc based access. Great effort 
has gone into developing an efficient [B+Tree](http://en.wikipedia.org/wiki/B%2B_tree)
that uses memory mapped files (to a varying) degree.

SDB offers the developer a number of ways of tuning table design for speed, durability 
and memory usage.

__3 Searching__  
A major difference between SDB and a typical RDBMS is the way you find information in it. By design 
it is very similar to a NoSQL key-value store in that it has rowkey (the key) and a row (the value). 
Thus a cell in the table is always accessed by its corresponding rowkey.

Obviously this can be quite limiting in many use cases. In a normal RDBMS you would typically do 
something like CREATE INDEX index ON table (col). Instead of taking this approach we chose
to treat the problem more as a text search problem by regarding each row in a table as a unique
document.

The text search approach gives us some interesting opportunities but also some problems. Our flexibility 
increase dramatically in the way we can search for information in a table. On the other hand, we are 
using [lucene](http://lucene.apache.org/) for this which means new data will be searchable in near 
real time. Similarly deleting data might not directly be reflected in our search index. 
SDB offers a way to configure how close to real time your index should be kept - again
it is a balance between consistency and speed.

__4 Ease of use__  
Databases can be difficult to use, especially when it comes to configuring them for your
desired use case. You need to consider things such as cache size, in-memory, table layout,
etc. Then it is a matter of getting your data to-from the database.

SDB gives you fine grained control over these aspects, so that you can for example specify
how much as table as a whole can use of your ram or for that matter configure this on per
column basis. You can also configure if caches should be read only or read-write. In the end this
can be somewhat tricky. As a short hand you can instantiate pre-configured tables and columns that
are optimized for certain use cases. We are also looking into a more heuristic configuration scheme where
you don't specify absolutes but rather guiding principles, e.g. SPEED = IMPORTANT, MEMORY=SMALL, and so on.

From [slick](http://slick.typesafe.com) we have borrowed the possibility of defining your tables
using a combination of case classes and an abstract table class where you define column properties

###Download
All mellowtech.org APIs can be downloaded from our maven central or sonatype snapshots

```scala
resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
libraryDependencies += "org.mellowtech" %% "sdb" % "0.2-SNAPSHOT"
```

##Quick Start
The simplest way to get going with SDB is to use case class with SimpleTable pattern. The first thing you
need to do is to define your table model using a case class

```scala
case class MyRow(id: Option[Int], c2: Option[String])
```

Next you need to creat your actual table. If you have worked with slick this should look familiar to you

```scala
class TestSimple extends SimpleTable[Int,MyRow]("testTable") {
  val idc = column[Int]("id", O.Nullable)
  def c2 = column[String]("c2", O.Length(128,true), O.FieldSearch)
  val * = List(c2)
}
```
The idc and * functions needs to be implemented. _idc_ should return the key column while _*_ should return
a list of all other columns (in this case c2)


##Todo

##Version History






