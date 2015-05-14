package io.muoncore.stream

import io.muoncore.Muon
import io.muoncore.extension.amqp.AmqpTransportExtension
import io.muoncore.extension.amqp.discovery.AmqpDiscovery
import reactor.rx.Streams
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.Executors

class MultiSubscribeLoadedSpec extends Specification {

  def "A loaded channel remains available."() {

    given:
    def muon = new Muon(new AmqpDiscovery("amqp://localhost"))
    muon.serviceIdentifer = "eventsource"
    new AmqpTransportExtension("amqp://localhost").extend(muon)

    def pub = Streams.defer()
        .capacity(5000)

    def consumes = []
    pub.consume {
      consumes << it
    }

    muon.start()

    muon.streamSource("/core", Map, pub)

    Thread.sleep(3500)

    int messages = 10000
    def numSubscribers = 20
    def errors = []
    def items = []

    numSubscribers.times {
      def localitems = []
      items << localitems
      def stream = Streams.defer()

      stream.consume {
        localitems << it
      }
      stream.subscribe(new MySubscriber(errors: errors))
      muon.subscribe("muon://eventsource/core", Map, stream)
    }

    and:
    Thread.sleep(6000)

    when:
    def cl = 0
    messages.times {
      if(++cl % 100 == 0) println "$cl"
      pub.accept(["message":"is awesome"])
    }

    then:
    new PollingConditions(timeout: 10).eventually {
      def size = items*.size()
      def consumessize = consumes.size()
      consumessize == messages
      size.sum() == messages * numSubscribers
    }
    cleanup:
    println "There were ${errors.size()} errors in the streams"
    println errors
  }
}

