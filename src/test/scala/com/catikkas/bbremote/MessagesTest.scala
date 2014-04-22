package com.catikkas.bbremote

import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith
import Messages._
import akka.util.ByteString

@RunWith(classOf[JUnitRunner])
class MessagesTest extends Specification {
  
  "ksb keys" should {
    "parse single character" in {
      val KeyboardString(keys) = ByteString("""{type:'ksb',state:3,letter:'F'}""")
      keys.mkString must_== "F"
    }
    
    "parse two characters" in {
      val KeyboardString(keys) = ByteString("""{type:'ksb',state:3,letter:'F--o'}""")
      keys.mkString must_== "Fo"
    }
    
    "parse -" in {
      val KeyboardString(keys) = ByteString("""{type:'ksb',state:3,letter:'-'}""")
      keys.mkString must_== "-"
    }
    
    "parse - in multiple keys" in {
      val KeyboardString(keys) = ByteString("""{type:'ksb',state:3,letter:'F-----o'}""")
      keys.mkString must_== "F-o"
    }
    
    "parse special character 'space'" in {
      val KeyboardString(keys) = ByteString("""{type:'ksb',state:3,letter:'C--e--m--space--C--a--t'}""")
      keys.mkString must_== "Cem Cat"
    }
  }
}
