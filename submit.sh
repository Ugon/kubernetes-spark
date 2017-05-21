kubectl exec -i $1 -- /bin/bash -c 'cat > job.jar && /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --class Main ./job.jar' < $2
