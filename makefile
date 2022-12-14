JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
		$(JC) $(JFLAGS) $*.java

CLASSES = Server.java Client.java Util.java Message.java ClientObj.java DHClient.java DHServer.java Helper.java KeyedHash.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
		$(RM) *.class

