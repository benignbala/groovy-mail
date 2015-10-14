package org.benignbala.groovy.mailer.smtp

import groovy.transform.CompileDynamic

abstract class Conversation {
    def host
    def state
    def errCode
    def errText
    def mailFrom
    def rcptTos
    def msg

    def clientSock

    def debug
    abstract void init(def host, def port)
    
    String doCommand(def cmd, def cmdData, def expectedCode) {
	clientSock.withStreams {input, output ->
	    if (cmd != null && cmdData != null) {
		output << cmd.toUpperCase() + ": " + cmdData + "\r\n"
	    } else if (cmd != null) {
		output << cmd + "\r\n"
	    }
	    def reader = input.newReader()
	    def data = ""
	    def received = ""
	    while (data != null) {
		received += reader.readLine()
	    }
	}
	return received
    }
    
    abstract def transact(def from, def to, def data)
}

class TextConversation extends Conversation {
    TextConversation(def host) {
    }

    void init(def host, def port) {
	this.state = SMTPStates.initState()
	dbg("Current State: " + this.state)
	this.clientSock = new Socket(host, 25)
	this.state = SMTPStates.transition(SMTPStates.WAIT_FOR_BANNER)
	def res = doCommand(null, null, "220")
	this.state = SMTPStates.transition(SMTPStates.BANNER_RECEIVED)
    }
    
    def transact(def from, def to, def data) {
	this.mailFrom = from
	this.rcptTos = to
	this.msg = data

	init(host, "25")
	def cmd = "MAIL FROM"
	while (cmd != "DONE") {
	    def inputs = SMTPStates.getInfo(this.state)
	    def ret = doCommand(inputs?.cmd, inputs?.cmdData, inputs?.expectedCode)
	    if (ret.startsWith(expectedCode)) {
		this.state = SMTPStates.transition(this.state, 1)
	    } else {
		this.state = SMTPStates.transition(this.state, 0)
	    }
	    cmd = inputs?.cmd
	}
    }
}
	
	
class SMTPStates {
    static ArrayList states = ["WAIT_FOR_BANNER",
			       "BANNER_RECEIVED",
			       "EHLO_SENT",
			       "EHLO_COMPLETE",
			       "MAIL_FROM_SENT",
			       "MAIL_FROM_COMPLETE",
			       "RCPT_TO_SENT",
			       "RCPT_TO_COMPLETE",
			       "DATA_CMD_SENT",
			       "DATA_CMD_COMPLETE",
			       "DATA_SENT",
			       "DATA_DONE",
			       "DOT_SENT",
			       "DOT_COMPLETE",
			       "QUIT_SENT"
			      ]
			       
    static Map stateMap = ["BANNER_RECEIVED" : [cmd: "EHLO",
						expectedCode: "250",
						nextStateSucc: "EHLO_COMPLETE",
						nextStateFail: "DOT_COMPLETE"
					       ],
			   "EHLO_COMPLETE" : [cmd: "MAIL FROM",
					      expectedCode: "250",
					      nextStateSucc: "MAIL_FROM_COMPLETE",
					      nextStateFail: "SEND_QUIT"
					     ],
			   "MAIL_FROM_COMPLETE" : [cmd: "RCPT TO",
						   expectedCode: "250",
						   nextStateSucc: "RCPT_TO_SENT",
						   nextStateFail: "DOT_COMPLETE"
						  ],
			   "RCPT_TO_COMPLETE": [cmd: "DATA",
						expectedCode: "354",
						nextStateSucc: "DATA_CMD_COMPLETE",
						nextStateFail: "DOT_COMPLETE"
					       ],
			   "DATA_CMD_COMPLETE": [cmd: null,
						 expectedCode: "250",
						 nextStateSucc: "DATA_DONE",
						 nextStateFail: "DOT_COMPLETE"
						],
			   "DATA_DONE": [cmd: "QUIT",
					 expectedCode: "421",
					 nextStateSucc: "DONE",
					 nextStateFail: "DONE"
					],
			   "DONE": [cmd: "DONE",
				    expectedCode: null
				    ]
						 
	    
			  ]
			  
    public static String initState() {
	return states[0]
    }

    public static Map getInfo(def state) {
	return stateMap."${state}"
    }

    public static String transition(def state, def succ) {
	def nextState = succ ? "nextStateSucc" : "nextStateFail"
	return stateMap."${state}"."$nextState"
    }
}
