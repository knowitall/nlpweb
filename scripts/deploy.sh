#!/bin/sh

# Compile the new source into an assembly
sbt clean compile assembly

cp target/nlpweb-assembly*jar nlpweb.jar

rc=$?
if [[ $rc == 0 ]] ; then
  ./scripts/start.sh
  rc=$?
  if [[ $rc == 0 ]] ; then
    echo "Try it out: http://nlpweb.cs.washington.edu" | mail -s "Automatic Notification: NlpWeb Updated" schmmd@cs.washington.edu
  else
    echo "Could not start NlpWeb on Reliable." | mail -s "Automatic Notification: NlpWeb Start Failed" schmmd@cs.washington.edu
  fi
else
  echo "Could not compile NlpWeb on Reliable." | mail -s "Automatic Notification: NlpWeb Build Failed" schmmd@cs.washington.edu
fi
