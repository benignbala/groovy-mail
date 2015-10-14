package org.benignbala.groovy.mailer

//import org.benignbala.groovy.mailer.exceptions.*
import org.benignbala.groovy.mailer.smtp.*
import groovy.transform.CompileDynamic

class Mailer {
    def host
    def mailFrom
    List<String> rcptTos
    def conn
    
    Mailer(def server) {
	host = server
	Conversation conv = new TextConversation(host)
    }

    @CompileDynamic
    void send(def from, def to, def msg) {
	mailFrom = from
	rcptTos = to
	def status = conv.transact(from, to, msg)
    }
}
