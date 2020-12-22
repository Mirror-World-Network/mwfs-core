#!/bin/bash
 
ids=$(ps -ef | grep -E 'org.conch.Conch|cos.jar' | grep -v 'grep' | awk '{print $2}' | wc -l)
T=$(date "+%Y-%m-%d %H:%M:%S")

cd ~
HOME_PATH=$(pwd)

if [ ${ids} = 0 ]; then
    echo "[${T}] No running instance. start the cos instance"
    if [ -e ${HOME_PATH}/mwfs/start.sh ]; then
        ${HOME_PATH}/mwfs/start.sh
    else
        /root/mwfs/start.sh
    fi
else
   running_ids=`ps -ef | grep -E 'Conch|cos' | grep -v "grep" | awk '{print $2}'`
   echo "[${T}] COS instance[${running_ids}] is running"
fi