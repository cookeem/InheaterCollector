package com.inheater.collector.actor

import akka.actor._
import akka.routing.{RoundRobinPool, DefaultResizer}
import org.openqa.selenium.{Keys, JavascriptExecutor, By, Dimension}
import org.openqa.selenium.firefox.{FirefoxDriver, FirefoxProfile}

/**
  * Created by cookeem on 16/5/10.
  */

case class FetchRequest(url: String, isWeixin: Int, isScroll: Int)
case class FetchResponse(source: String, connectError: String)

//Firefox路由管理Actor
class FirefoxManager(routercountMin: Int, routercountMax: Int) extends Actor with ActorLogging {
  implicit val logs = log
  val resizer = DefaultResizer(lowerBound = routercountMin, upperBound = routercountMax)
  val router = context.actorOf(RoundRobinPool(routercountMin, Some(resizer)).props(FirefoxActor.props), "router")

  override def postStop() = {
    log.warning("FirefoxManager begin to stop!")
  }

  def receive = {
    case FetchRequest(url, isWeixin, isScroll) =>
      router forward FetchRequest(url, isWeixin, isScroll)
    case _ =>
      log.error(s"Unacceptable message type")
  }
}

object FirefoxManager {
  def props(routercountMin: Int, routercountMax: Int) = Props(new FirefoxManager(routercountMin, routercountMax))
}

//Firefox爬虫Actor
class FirefoxActor extends Actor with ActorLogging {
  implicit val logs = log
  val firefoxProfile = new FirefoxProfile
  firefoxProfile.setPreference("permissions.default.image", 2)
  firefoxProfile.setPreference("plugin.state.flash", 0)
  firefoxProfile.setPreference("permissions.default.stylesheet", 2)
  firefoxProfile.setPreference("permissions.default.subdocument", 2)
  val driver = new FirefoxDriver(firefoxProfile)
  driver.manage().window().setSize(new Dimension(800, 640))

  override def postStop() = {
    driver.quit()
    log.warning("FirefoxActor begin to stop!")
  }

  def receive = {
    case FetchRequest(url, isWeixin, isScroll) =>
      var source = ""
      var connectError = ""
      try {
        if (isScroll == 1 || isWeixin == 1) {
          driver.get(url)
          val scroll = driver.findElement(By.tagName("body"))
          val screenHeight = 500
          var currentScroll = 0
          var pageHeight = scroll.getSize.getHeight
          Thread.sleep(1000)
          //无需进行scroll处理,以下代码无效
          if (isWeixin == -1) {
            try {
              //向浏览器注入Javascript隐藏无用信息,微信公众号文章
              val jse = driver.asInstanceOf[JavascriptExecutor]
              jse.executeScript(
                """
                var element1 = document.getElementById("sg_tj");
                element1.parentNode.removeChild(element1);
                var element2 = document.getElementsByClassName("rich_media_area_extra");
                var i;
                for (i = 0; i < element2.length; i++) {
                    element2[i].parentNode.removeChild(element2[i]);
                }
                """)
              while (currentScroll < pageHeight) {
                Thread.sleep(300)
                scroll.sendKeys(Keys.PAGE_DOWN)
                currentScroll += screenHeight
                pageHeight = scroll.getSize.getHeight
              }
            } catch {
              case e: Throwable =>
                log.warning(s"JavascriptExecutor error: ${e.getMessage}, ${e.getCause}")
            }
          }
          source = driver.getPageSource
        } else {
          driver.get(url)
          source = driver.getPageSource
        }
      } catch {
        case e: Throwable =>
          connectError = s"FirefoxActor error: ${e.getMessage}, ${e.getCause}"
          log.error(connectError)
      }
      sender ! FetchResponse(source, connectError)
    case _ =>
      val connectError = s"Unacceptable message type"
      log.error(connectError)
  }
}

object FirefoxActor {
  def props = Props(new FirefoxActor)
}
