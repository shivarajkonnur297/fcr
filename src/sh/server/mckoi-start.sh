#!/bin/sh

if [ "$FEDORA_HOME" = "" ]; then
  echo "ERROR: Environment variable, FEDORA_HOME must be set."
  exit 1
fi

if [ ! -f "$FEDORA_HOME/server/config/fedora.fcfg" ]; then
  echo "ERROR: FEDORA_HOME does not appear correctly set."
  echo "Configuration cannot be found at $FEDORA_HOME/server/config/fedora.fcfg"
  exit 1
fi

if [ "$FEDORA_JAVA_HOME" = "" ]; then
  echo "Warning: FEDORA_JAVA_HOME not set, falling back to JAVA_HOME"
  if [ "$JAVA_HOME" = "" ]; then
    echo "ERROR: FEDORA_JAVA_HOME was not defined, nor was (the fallback) JAVA_HOME."
    exit 1
  fi 
  THIS_JAVA_HOME=$JAVA_HOME
else
  THIS_JAVA_HOME="$FEDORA_JAVA_HOME"
fi

if [ ! -f "$THIS_JAVA_HOME/bin/java" ]; then
  echo "ERROR: java was not found in $THIS_JAVA_HOME"
  echo "Make sure FEDORA_JAVA_HOME or JAVA_HOME is set correctly."
  exit 1
fi

if [ ! -f "$FEDORA_HOME/server/mckoi094/mckoidb.jar" ]; then
  echo "ERROR: No mckoidb.jar found in $FEDORA_HOME/mckoi094/"
  exit 1
fi

if [ ! -f "$FEDORA_HOME/server/mckoi094/data/DefaultDatabase.sf" ]; then
  echo "ERROR: McKoi database hasn't been initialized, run mckoi-init first."
  exit 1
fi

echo "Starting McKoi DB..."

OLD_JAVA_HOME=$JAVA_HOME
JAVA_HOME=$THIS_JAVA_HOME
export JAVA_HOME

(exec $JAVA_HOME/bin/java -Xms64m -Xmx96m -cp $FEDORA_HOME/server/mckoi094/gnu-regexp-1.1.4.jar -jar $FEDORA_HOME/server/mckoi094/mckoidb.jar -conf $FEDORA_HOME/server/mckoi094/db.conf &)

JAVA_HOME=$OLD_JAVA_HOME
export JAVA_HOME

echo "To stop the server, use mckoi-stop."


exit 0
