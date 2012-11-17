package thesmith.podiki.http

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
import java.io.IOException
import java.io.BufferedReader
import org.apache.http.HttpResponse
import java.io.InputStreamReader
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpRequestBase
import scala.collection.JavaConversions._
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.StringEntity
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpPost
import java.io.InputStream
import org.apache.http.entity.InputStreamEntity
import org.apache.http.params.HttpConnectionParams
import org.apache.http.params.HttpParams
import org.apache.http.params.BasicHttpParams
import org.apache.http.client.methods.HttpHead
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.conn.ssl.X509HostnameVerifier
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.PlainSocketFactory

class UrlFetcher(totalConnections: Int, connectionTimeoutSecs: Int = 30, socketTimeoutSecs: Int = 30){
  
  val hostnameVerifier: X509HostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER
  val registry = new SchemeRegistry()
  val socketFactory = SSLSocketFactory.getSocketFactory()
  socketFactory.setHostnameVerifier(hostnameVerifier);
  registry.register(new Scheme("https", socketFactory, 443));
  registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory, 80))

  // Set verifier     
  HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
  
  val connManager = new ThreadSafeClientConnManager(registry)
  connManager.setMaxTotal(totalConnections)
  val httpParameters: HttpParams = new BasicHttpParams();
  HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeoutSecs * 1000)
  HttpConnectionParams.setSoTimeout(httpParameters, socketTimeoutSecs * 1000)

  val client = new DefaultHttpClient(connManager, httpParameters)
  
  def head(url: String, headers: Map[String, String] = Map()) = {
    val head = new HttpHead(url)
    addHeaders(head, headers)
    execute(head, false)
  }
  
  def get(url: String, headers: Map[String, String] = Map(), getResponseContent: Boolean = true) = {
    val get = new HttpGet(url)
    addHeaders(get, headers)
    execute(get, getResponseContent)
  }
  
  def post(url: String, payload: String, headers: Map[String, String] = Map(), getResponseContent: Boolean = true) = {
    val post = new HttpPost(url)
    addHeaders(post, headers)
    post.setEntity(new StringEntity(payload, "UTF-8"))
    execute(post, getResponseContent)
  }
  
  def putFromStream(url: String, payload: InputStream, length: Long, headers: Map[String, String] = Map(), getResponseContent: Boolean = true) = {
    val put = new HttpPut(url)
    addHeaders(put, headers)
    put.setEntity(new InputStreamEntity(payload, length));
    execute(put, getResponseContent)
  }
  
  def put(url: String, payload: String, headers: Map[String, String] = Map(), getResponseContent: Boolean = true) = {
    val put = new HttpPut(url)
    addHeaders(put, headers)
    put.setEntity(new StringEntity(payload, "UTF-8"))
    execute(put, getResponseContent)
  }
  
  def delete(url: String, headers: Map[String, String] = Map(), getResponseContent: Boolean = true) = {
    val delete = new HttpDelete(url)
    addHeaders(delete, headers)
    execute(delete, getResponseContent)
  }
  
  def clearCookies() {
    if(client.getCookieStore() != null) client.getCookieStore().clear()
  }
  
  private def execute(req: HttpRequestBase, getResponseContent: Boolean): UrlFetcherResponse = {
    val response = new UrlFetcherResponse(client.execute(req))
    
    if(getResponseContent) 
      response.content
      
    response
  }
  private def addHeaders(req: HttpRequestBase, headers: Map[String,String]): Unit = {
    headers.foreach((header)=>{req.addHeader(header._1, header._2)})
  }
}

case class UrlFetcherResponse(response: HttpResponse) {
  var contentString: Option[String] = null
  
  def content: Option[String] = {
    if(contentString == null) {
      val contentStream = this.contentStream
      
      if(contentStream.isDefined) {
        val reader = new BufferedReader(new InputStreamReader(contentStream.get));
    
        try {
          contentString = Some(Iterator.continually(reader.readLine()).takeWhile(_ != null).mkString("\n"))
        } catch {
          case ioe: IOException => {
            contentString = None
            throw new RuntimeException("Exception while reading content", ioe)
          }
        } finally {
          try {
            contentStream.get.close();
          } catch {
            case ioe: IOException => throw new RuntimeException("Error while closing stream", ioe)
          }
        }
      } else {
        contentString = None  
      }
    }
    
    return contentString
  }
  
  def contentStream = {
    Option(response.getEntity()) match {
      case Some(entity) => Option(entity.getContent())
      case None => None
    }
  }
  
  def headers = {
    val headerNames = response.getAllHeaders().map(h => h.getName()).toSet
    headerNames.foldLeft(Map[String, Set[String]]())((headers, headerName) => {
      headers + (headerName -> response.getHeaders(headerName).map(header => header.getValue()).toSet)
    })
  }
  
  def statusCode = response.getStatusLine().getStatusCode()
}