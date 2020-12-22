#!/bin/bash

PRJ=mwfs
chmod -R +x ~/${PRJ}/
if [ -e ~/.${PRJ}/${PRJ}.pid ]; then
    PID=`cat ~/.${PRJ}/${PRJ}.pid`
    ps -p ${PID} > /dev/null
    STATUS=$?
    if [ ${STATUS} -eq 0 ]; then
        echo "${PRJ} server already running"
        exit 1
    fi
fi
mkdir -p ~/.${PRJ}/
DIR=`dirname "$0"`
cd "${DIR}"

# detect an del expired files
EXPIRED_DATA_FILE='./lib/ExpiredFiles.data'
if [ -e ${EXPIRED_DATA_FILE} ]; then
    JSON_STRING=$(cat ${EXPIRED_DATA_FILE})
    fileArray=(${JSON_STRING//,/ })
    cd ./lib
    for file in ${fileArray[@]}
    do
        if [ -e ${file} ]; then
            echo "Delete expired file: ${file}"
            rm -rf ${file}
        fi
    done
    cd ../
    rm -rf ${EXPIRED_DATA_FILE}
fi

if [ -e jre/bin/java ]; then
    JAVA=./jre/bin/java
else
    JAVA=${JAVA_HOME}/bin/java
fi

if [ -e cos.jar ]; then
      nohup ${JAVA} -cp lib/*:conf -Dsharder.runtime.mode=noview -Dsharder.runtime.dirProvider=org.conch.env.DefaultDirProvider -jar cos.jar > /dev/null 2>&1 &
else
    if [ -d classes ]; then
      nohup ${JAVA} -cp classes:lib/*:conf:addons/classes:addons/lib/* org.conch.Conch > /dev/null 2>&1 &
    else
       echo "can't start the ${PRJ}, because not found the cos.jar or compiled class"
    fi
fi

echo "${PRJ} server started with pid["$!"]"
echo $! > ~/.${PRJ}/${PRJ}.pid
cd - > /dev/null