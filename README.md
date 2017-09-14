# kubernetes-spark

## What is this?
This is a guide on how to setup a Kubernetes cluser, deploy the Apache Spark on it and use it to run a simple application. 
It is based on materials that can be found on the Internet: [the official Kubernetes guide](https://kubernetes.io/docs/getting-started-guides/kubeadm/) and [the Kubernetes GitHub](https://github.com/kubernetes/kubernetes/tree/master/examples/spark).

## Deploying a Kubernetes cluster
To deploy a K8s cluster you need at least two nodes (you can successfully deploy and use K8s on one node, but that's not a cluster). 
This guide uses 4 VirtualBox VMs running the Ubuntu 16.04 Server as K8s nodes (1 VM for K8s master and 3 VMs for minions).

1. Create 4 VMs and install the Ubuntu 16.04 Server OS on all of them. Please note that this guide is specific to the Ubuntu distribution. If you want to use another LINUX/UNIX distribution, it may be necessary to use some different commands than presented in this tutorial (e.g. there are some issues related to the Docker installation with the Debian-Jessie distribution). While creating the VMs, please also take into account that each VM should have different host name.

2. Login to all of the VMs as a root (use ssh for convenience).

3. Install Docker and K8s on all VMs:
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
Please note that the `docker0` interface is configured by default during the Docker installation in the network addressed by: 
`172.17.0.0/16`. This may cause problems when the Ubuntu VMs are placed in the same network. To workaround this issue, remove the
`docker0` entry in the routing table by typing:
```
sudo ip route del 172.17.0.0/16 dev docker0
```

4. On the master node run:
```
kubeadm init
``` 
The kubeadm is a tool for quick and easy deployment of K8s. Make note of the last line of the command output - you'll need to use it later on other nodes to join the master:
```
kubeadm join --token 0f953e.8ca02dfe7d72f2d4 192.168.0.32:6443
```

5. If you want to manage your K8s cluster from a remote host (other than VMs that you deployed earlier), install the kubectl on that host:
```
curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin/kubectl
```
The kubectl is a tool for managing K8s cluster. You can also manage your cluster from the master node - if so, please skip this step.

6. Configuring the kubectl to work with your cluster:
* If you are using the kubectl on the master node, run on it:
```
sudo cp /etc/kubernetes/admin.conf $HOME/
sudo chown $(id -u):$(id -g) $HOME/admin.conf
export KUBECONFIG=$HOME/admin.conf

```
* If you are using the kubectl on a remote host, run on it:
```
scp root@<K8S_MASTER_IP>:/etc/kubernetes/admin.conf $HOME
export KUBECONFIG=$HOME/admin.conf
```

7. If you want the master node to be used for scheduling pods, run:
```
kubectl taint nodes --all node-role.kubernetes.io/master-
```

8. Install a pod network:
```
kubectl apply -f https://git.io/weave-kube-1.6
```

9. To check whether everything is working as expected, run:
```
kubectl get pods --all-namespaces
```
Note that the `kube-dns` pod should have the `RUNNING` status.

10. On all minion VMs run a `kubeadm join` command that you took note of in the step 4. This will connect your nodes to the K8s master node and form a cluster.
You can confirm that nodes joined a cluster by running the `kubectl get nodes` command. After a minute or so all nodes should  have the `RUNNING` status.

Your cluster is now up and running - you can close all your shh connections (apart from the master node if you are not configuring it from a remote host).

## Deploying the Apache Spark on a Kubernetes cluster

1. Download configuration files from the K8s Spark example:
```
curl https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/spark/spark-master-controller.yaml -o spark-master-controller.yaml
curl https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/spark/spark-master-service.yaml -o spark-master-service.yaml
curl https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/spark/spark-worker-controller.yaml -o spark-worker-controller.yaml
```

2. Start the spark-master node.
```
kubectl create -f spark-master-controller.yaml
kubectl create -f spark-master-service.yaml
```

3. Start spark-worker nodes. To select how many workers should be stared, modify the `replicas` value in the `spark-worker-controller.yaml`.
```
kubectl create -f spark-worker-controller.yaml
```
To check whether they are working, run the `kubectl get pods` command. You can also check this in a Spark Web UI.

4. Spark operates in an internal Kubernetes network. To gain an access to the fully functional Spark UI, a connection with this network is required.
To achieve this we recommend connecting your workstation to that K8s network using the `weave` (network overlay which is used by K8s):

```
sudo curl -L git.io/weave -o /usr/local/bin/weave
sudo chmod a+x /usr/local/bin/weave
sudo weave launch
sudo weave connect <NODE_IP>
sudo weave expose
```
Where the `<NODE_IP>` is an IP address of any node running K8s.

5. To display the Spark UI, firstly find out the Spark master pod ID and then its IP address inside the K8s network:
```
kubectl get pods
kubectl describe pod spark-master-controller-<ID>
```
Open the `<IP>:8080` address in a web browser.

## Installing and using the Zeppelin UI
1. Download configuration files from the K8s Spark example: 
```
curl https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/spark/zeppelin-controller.yaml -o zeppelin-controller.yaml
curl https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/spark/zeppelin-service.yaml -o zeppelin-service.yaml
```
2. Start the ZeppelinUI controller and service:
```
kubectl create -f zeppelin-controller.yaml
kubectl create -f zeppelin-service.yaml
```
3. Check whether the ZeppelinUI is running:
```
kubectl get pods
```

4. The ZeppelinUI service runs in K8s in the `NodePort` mode. Therefore the ZeppelinUI is available via the public IP of the K8s node hosting the ZeppelinUI service, on a port determined by K8s. To find out that port, type:
```
kubectl get svc zeppelin
NAME       CLUSTER-IP      EXTERNAL-IP   PORT(S)        AGE
zeppelin   10.97.252.171   <pending>     80:32627/TCP   3d
```
In the example above, the port number is 32627.

5. The ZeppelinUI can take jobs written in Scala or Python languages. To create a job, firstly create a Note (multiple concurrent Notes are allowed), write your job code in it and press Shift+Enter keys to run it.

A Python example:
```
%pyspark
import random

NUM_SAMPLES = 50000000
def inside(p):
    x, y = random.random(), random.random()
    return x*x + y*y < 1

count = sc.parallelize(xrange(0, NUM_SAMPLES)) \
             .filter(inside).count()
print "Pi is roughly %f" % (4.0 * count / NUM_SAMPLES)
```

A Scala example:
```
%spark
val NumSamples = 50000000
val count = sc.parallelize(1 to NumSamples).filter { _ =>
  val x = math.random
  val y = math.random
  x*x + y*y < 1
}.count()
println(s"Pi is roughly ${4.0 * count / NumSamples}")
```

## Submitting jobs to the Spark running in a Kubernetes cluster
1. Package your code in a `*.jar` archive:
```
sbt package
```

2. Use the `submit.sh` script:
```
submit.sh <JAR_LOCATION> <DATA_FILE_LOCATION> <JOB_ARGUMENTS>
```

## Sample application
This project contains a sample Apache Spark application built on top of the [GraphX library](http://spark.apache.org/graphx/). The
application counts a number of triangles in a directed graph, passing through each node of this graph. A vertex is a part of a triangle when it has two adjacent vertices with an edge between them.

The input file comes from the [Stanford Large Network Dataset Collection](https://snap.stanford.edu) and is the [collaboration network of Arxiv High Energy Physics Theory](https://snap.stanford.edu/data/ca-HepTh.html). This network consists of 9'877 nodes and 25'998 edges. The detailed description of this network is available under the link above. The graph file consits of two columns - the first one is the ID of a researcher, who co-authored a paper with a researcher having the ID placed in the second column. If a paper is co-authored by more than one researcher, these authors form a completely connected clique.

