# kubernetes-spark

## What is this?
This is a guide on how to setup a Kubernetes cluser, deploy Apache Spark on it and use it to run a simple application. 
It is based on materials that can be found on the internet:
https://kubernetes.io/docs/getting-started-guides/kubeadm/
https://github.com/kubernetes/kubernetes/tree/master/examples/spark

## Deploying Kubernetes cluster
To deploy a K8s cluster you need at least two nodes (you can successfully deploy and use K8s on one node, but that's not a cluster). 
This guide uses 4 VirtualBox virtual machines running Ubuntu 16.04 Server as K8s nodes (1 for K8s master and 3 for minions).

1. Create 4 VMs and install Ubuntu 16.04 Serve OS on all of them.
2. Login to all of them as root (use ssh for convenience).
3. Install Docker and K8s on all VMs.
```
apt-get update && apt-get install -y apt-transport-https
curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key add -
cat <<EOF >/etc/apt/sources.list.d/kubernetes.list
deb http://apt.kubernetes.io/ kubernetes-xenial main
EOF
apt-get update
apt-get install -y docker-engine
apt-get install -y kubelet kubeadm kubectl kubernetes-cni
```

4. On master node run:
```
kubeadm init
``` 
kubeadm is a tool for quick and easy deployment of K8s. Make note of last line of output, you'll use it later on other nodes to join master.
```
kubeadm join --token 0f953e.8ca02dfe7d72f2d4 192.168.0.32:6443
```

5. If you want to manage your K8s cluster from remote host (other than VMs that you deployed earlier), install kubectl on that host. kubectl is a tool for managing K8s cluster. 
You can also manage your cluster from master node - if so, skip this step.
```
curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin/kubectl
```

6. Configuring kubectl to work with your cluster.
* If you are using kubectl on master node, run on master node:
```
sudo cp /etc/kubernetes/admin.conf $HOME/
sudo chown $(id -u):$(id -g) $HOME/admin.conf
export KUBECONFIG=$HOME/admin.conf

```
* If you are using kubectl on remote host, run on that host:
```
scp root@<K8S_MASTER_IP>:/etc/kubernetes/admin.conf $HOME
export KUBECONFIG=$HOME/admin.conf
```

7. If you want master node to be used for scheduling pods, run:
```
kubectl taint nodes --all node-role.kubernetes.io/master-
```

8. Install pod network:
```
kubectl apply -f https://git.io/weave-kube-1.6
```

9. To check if everything is working as expected, run:
```
kubectl get pods --all-namespaces
```
`kube-dns` pod should have `RUNNING` status.

10. On all minion VMs run `kubeadm join` command that you took note of in step 4. This will connect your nodes to K8s mater node and form a cluster.
You can confirm that nodes joined cluster by running `kubectl get nodes` command. After a minute or so all nodes should be have `RUNNING` status.

Your cluster is not up and running, you can close all your shh connections (apart from master node if you are not configuring it from remote host).

## Deploying Apache Spark on Kubernetes cluster

1. Download configuration files from K8s Spark example:
```
curl https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/spark/spark-master-controller.yaml -o spark-master-controller.yaml
curl https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/spark/spark-master-service.yaml -o spark-master-service.yaml
curl https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/spark/spark-worker-controller.yaml -o spark-worker-controller.yaml
```

<!-- 2. Modify spark-master-service.yaml - add `type: NodePort` in `spec` section. 
Services of NodePort type are accessible from outside K8s cluster under every K8s nodes IPs and port selected by K8s.
```
kind: Service
apiVersion: v1
metadata:
  name: spark-master
spec:
  type: NodePort
  ports:
    - port: 7077
      targetPort: 7077
      name: spark
    - port: 8080
      targetPort: 8080
      name: http
  selector:
    component: spark-master
``` -->

2. Start spark-master node.
```
kubectl create -f spark-master-controller.yaml
kubectl create -f spark-master-service.yaml
```

3. Start spark-worker nodes. To select how many workers should be stared, modify `replicas: 2` in spark-worker-controller.yaml.
```
kubectl create -f spark-worker-controller.yaml
```
To check if they are working, run `kubectl get pods`. You can also check this in spark web ui.

<!--
4. Find out what ports on K8s nodes was assigned to spark. On all K8s nodes those ports will be the same.
```
kubectl get svc spark-master
NAME           CLUSTER-IP       EXTERNAL-IP   PORT(S)                         AGE
spark-master   10.108.103.225   <nodes>       7077:31585/TCP,8080:32693/TCP   2m
```
In this example K8s selected `7077:31585/TCP,8080:32693/TCP` ports, which means that Spark is available on 31585 and Spark Web UI is available on 32693 port.
To access those services you can use any of your K8s nodes' IP address.
-->

4. Spark operates in internal kubernetes network. To gain access to fully functional Spark UI a connection with this network is required.
To achieve this we recommend connecting your workstation to that k8s network using `weave` (network overlay which is used by k8s).

```
sudo curl -L git.io/weave -o /usr/local/bin/weave
sudo chmod a+x /usr/local/bin/weave
sudo weave launch
sudo weave connect <NODE_IP>
sudo weave expose
```
`<NODE_IP>` is ip address of any node running k8s.

5. To display Spark UI first find out Spark master pod id and then its ip address inside k8s network.
```
kubectl get pods
kubectl describe spark-master-controller-<ID>
```
In web browser go to `<ip>:8080`.

## Submitting jobs to Spark running in Kubernetes cluster
1. Package your code in a `.jar`
```
sbt package
```

2. Use `submit.sh` 
```
submit.sh spark-master-controller-<ID> <JAR_LOCATION>
```
`<ID>` is Spark master pod id obtained in previous step.