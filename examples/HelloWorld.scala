package com.posterous.finatra

object Whatever extends FinatraApp("/my") {
  get("/lol") { <h1>lol</h1> }
}


object HelloWorld extends FinatraApp {

  get("/") { 
    "<h1>asd</h1>"
  } 

  get("/error") {
    status(500)
    "error!"
  }

  get("/headertest") {
    headers("foo" -> "bar")
    contentType("text/plain")
    "check heads"
  }

  get("/foo") { 
    request.headers 
  }
  
  get("/simple") { 
    params("lol") 
  }
  
  get("/doit/:year") { 
    params("year") 
  }

}

