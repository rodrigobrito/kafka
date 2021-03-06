/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kafka.producer

import junit.framework.{Assert, TestCase}
import kafka.utils.SystemTime
import kafka.utils.TestUtils
import kafka.server.{KafkaServer, KafkaConfig}
import org.apache.log4j.{Logger, Level}
import org.scalatest.junit.JUnitSuite
import org.junit.{After, Before, Test}
import kafka.common.MessageSizeTooLargeException
import java.util.Properties
import kafka.api.ProducerRequest
import kafka.message.{NoCompressionCodec, Message, ByteBufferMessageSet}

class SyncProducerTest extends JUnitSuite {
  private var messageBytes =  new Array[Byte](2);
  private var server: KafkaServer = null
  val simpleProducerLogger = Logger.getLogger(classOf[SyncProducer])

  @Before
  def setUp() {
    server = TestUtils.createServer(new KafkaConfig(TestUtils.createBrokerConfig(0, TestUtils.choosePort))
    {
      override val enableZookeeper = false
    })
  }

  @After
  def tearDown() {
    server.shutdown
  }

  @Test
  def testReachableServer() {
    val props = new Properties()
    props.put("host", "localhost")
    props.put("port", server.socketServer.port.toString)
    props.put("buffer.size", "102400")
    props.put("connect.timeout.ms", "500")
    props.put("reconnect.interval", "1000")
    val producer = new SyncProducer(new SyncProducerConfig(props))
    var failed = false
    val firstStart = SystemTime.milliseconds
    try {
      producer.send("test", 0, new ByteBufferMessageSet(compressionCodec = NoCompressionCodec, messages = new Message(messageBytes)))
    }catch {
      case e: Exception => failed=true
    }
    Assert.assertFalse(failed)
    failed = false
    val firstEnd = SystemTime.milliseconds
    Assert.assertTrue((firstEnd-firstStart) < 500)
    val secondStart = SystemTime.milliseconds
    try {
      producer.send("test", 0, new ByteBufferMessageSet(compressionCodec = NoCompressionCodec, messages = new Message(messageBytes)))
    }catch {
      case e: Exception => failed = true
    }
    Assert.assertFalse(failed)
    val secondEnd = SystemTime.milliseconds
    Assert.assertTrue((secondEnd-secondStart) < 500)

    try {
      producer.multiSend(Array(new ProducerRequest("test", 0, new ByteBufferMessageSet(compressionCodec = NoCompressionCodec, messages = new Message(messageBytes)))))
    }catch {
      case e: Exception => failed=true
    }
    Assert.assertFalse(failed)
  }

  @Test
  def testMessageSizeTooLarge() {
    val props = new Properties()
    props.put("host", "localhost")
    props.put("port", server.socketServer.port.toString)
    props.put("buffer.size", "102400")
    props.put("connect.timeout.ms", "300")
    props.put("reconnect.interval", "500")
    props.put("max.message.size", "100")
    val producer = new SyncProducer(new SyncProducerConfig(props))
    val bytes = new Array[Byte](101)
    var failed = false
    try {
      producer.send("test", 0, new ByteBufferMessageSet(compressionCodec = NoCompressionCodec, messages = new Message(bytes)))
    }catch {
      case e: MessageSizeTooLargeException => failed = true
    }
    Assert.assertTrue(failed)
  }
}
