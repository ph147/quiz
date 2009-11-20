#!/bin/bash
# Takes file "$from" of format
#       Question
#       Answer
#       Question
#       ...
#
# and writes every pair into its own file, namely
#       1
#       2
#       ...

from="questions.de"


i=1
j=1
cat "$from" | while read line; do
    if [ $i -eq 1 ]; then
        echo $line > "$j"
    else
        echo $line >> "$j"
        i=0
        j=$[ $j + 1 ]
    fi
    i=$[ $i + 1 ]
done
