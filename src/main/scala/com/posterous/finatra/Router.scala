package com.posterous.finatra

import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import java.net.InetSocketAddress
import java.util.{NoSuchElementException => NoSuchElement}
import org.jboss.netty.handler.codec.http.HttpMethod._
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import com.twitter.util.Future
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.finagle.http.{Http, RichHttp, Request, Response}
import com.twitter.finagle.http.Status._
import com.twitter.finagle.http.Version.Http11
import com.twitter.finagle.http.path._
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.builder.{Server, ServerBuilder}
import com.codahale.logula.Logging

object Router extends Logging {
  case class FinatraResponse(var status: Int = 200, var mediaType: String = "text/html", var headers:ListBuffer[Tuple2[String,String]] = new ListBuffer[Tuple2[String,String]])
  
  var routes: HashSet[(String, PathPattern, Function0[Any])] = HashSet()
  var request:Request = null
  var paramsHash:Map[String, String] = Map()
  var response:FinatraResponse = FinatraResponse()

  def loadUrlParams() {
    this.request.params.foreach(xs => this.paramsHash += xs)
  }

  def parseMatchParam(xs: Tuple2[_, _]) = {
    this.paramsHash += Tuple2(xs._1.toString, xs._2.asInstanceOf[ListBuffer[String]].head.toString)
  }

  def params(name:String):String = {
    this.paramsHash.get(name) match {
      case Some(str) => str
      case None => ""
    }
  }

  def addRoute(method: String, path: String)(callback: => Any) {
    val regex = SinatraPathPatternParser(path)
    routes += Tuple3(method, regex, (() => callback))
  }

  def returnFuture(response: Response) = {
    log.info("returning response %s", response)
    Future.value(response)
  }

  def routeExists(request: Request) = {
    var thematch:Option[Map[_,_]] = None
    
    this.routes.find( route => route match {
      case (method, pattern, callback) =>
        thematch = pattern(request.path)
        if(thematch.getOrElse(null) != null && method == request.method.toString) {
          thematch.getOrElse(null).foreach(xs => parseMatchParam(xs))
          true
        } else {
          false
        }
    })
  }

  def buildResponse(output:String) = {
    val resp = Response(Http11, InternalServerError)
    resp.statusCode = this.response.status
    resp.mediaType = this.response.mediaType
    this.response.headers.foreach(xs => resp.addHeader(xs._1, xs._2))
    resp.content = copiedBuffer(output.toString, UTF_8)

    resp
  }

  def setStatus(status:Int) = {
    this.response.status = status  
  }

  def dispatch(request: Request):Response = {
    log.info("recvd request: %s %s %s", request.method, request.uri, request.headers)

    this.paramsHash = Map()
    this.request    = request
    this.response   = FinatraResponse()

    loadUrlParams()

    val result = this.routeExists(request) match {
      case Some((method, pattern,callback)) => callback()
      case none => 
        request.method = GET
        this.routeExists(request) match {
          case Some((method, patterh, callback)) => callback()
          case none =>
            setStatus(404)
            "404 Not Found"
        }
    }
    
    buildResponse(result.toString)
  }
  
  def dispatchAndReturn(request: Request) = {
    returnFuture(dispatch(request))
  }

}
