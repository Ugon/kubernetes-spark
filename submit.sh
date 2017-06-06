RANDOMNUMBER=$((RANDOM))
DATAFILE="/tmp/data$RANDOMNUMBER"
JARFILE="/tmp/job$RANDOMNUMBER.jar"
for POD in `kubectl get pods -o name | sed -e "s/^pods//" | cut -c 2- | grep spark`;
do
kubectl exec -i $POD -- /bin/bash -c "cat > "$DATAFILE < $2
done

SPARKMASTER=`kubectl get pods -o name | sed -e "s/^pods//" | cut -c 2- | grep spark-master`
kubectl exec -i $SPARKMASTER -- /bin/bash -c "cat > $JARFILE && /opt/spark/bin/spark-submit --deploy-mode client --master spark://spark-master:7077 --class Main $JARFILE $DATAFILE ${*:3}" < $1
