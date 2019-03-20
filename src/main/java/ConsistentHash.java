import org.apache.log4j.Logger;

import java.util.*;

public class ConsistentHash {

    private static final Logger logger = Logger.getLogger(ConsistentHash.class);

    private HashAlgorithm hashAlgorithm;
    /**
     * 所有虚拟节点通过散列分布到一个称为continuum的环上，其范围在0~2^32-1
     */
    private SortedMap<Long, VNode> continuum = new TreeMap<Long, VNode>();

    public ConsistentHash(HashAlgorithm hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }

    public ConsistentHash(HashAlgorithm hashAlgorithm, Collection<PNode> pNodes, int vn) {
        this(hashAlgorithm);
        for (PNode pNode : pNodes) {
            addNode(pNode, vn);
        }
    }

    /**
     * 获取物理节点的所有虚拟节点
     * @param pNode
     * @return
     */
    private List<VNode> getReplicas(PNode pNode) {
        List<VNode> vNodes = new ArrayList<>();
        for (VNode vNode : continuum.values()) {
            if (vNode.replicaOf(pNode)) {
                vNodes.add(vNode);
            }
        }
        return vNodes;
    }

    /**
     * 添加节点
     * @param pNode 物理节点
     * @param vn 虚拟节点个数
     */
    public void addNode(PNode pNode, int vn) {
        List<VNode> existingReplicas = getReplicas(pNode);
        int replicas = existingReplicas == null ? 0 : existingReplicas.size();
        for (int i = 0; i < vn; i++) {
            VNode vNode = new VNode(pNode, i + replicas);
            long hashKey = hashAlgorithm.hash(vNode.getKey());
            logger.debug("add vnode: " + vNode.getKey() + " of hashKey: " + hashKey);
            continuum.put(hashKey, vNode);
        }
    }

    /**
     * 删除节点
     * @param pNode
     */
    public void removeNode(PNode pNode) {
        List<VNode> replicas = getReplicas(pNode);
        for (VNode vNode : replicas) {
            long hashKey = hashAlgorithm.hash(vNode.getKey());
            logger.debug("remove vnode: " + vNode.getKey() + " of hashKey: " + hashKey);
            continuum.remove(hashKey);
        }
    }

    public PNode getNode(Object key) {
        if (continuum.isEmpty()) {
            return null;
        }
        long hashKey = hashAlgorithm.hash(key.toString());
        SortedMap<Long, VNode> tailMap = continuum.tailMap(hashKey);
        long hash = (tailMap != null && !tailMap.isEmpty()) ? tailMap.firstKey() : continuum.firstKey();
        return continuum.get(hash).getParent();
    }

    /**
     * 物理节点
     * 用节点的host:ip作为唯一key
     */
    static class PNode {

        private String host;
        private int port;

        public PNode(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getKey() {
            return host + ":" + port;
        }
    }

    /**
     * 虚拟节点
     * 用节点的host:ip:replicaNum作为唯一key
     */
    static class VNode {

        private PNode parent;
        private int replicaNum;

        public VNode(PNode parent, int replicaNum) {
            this.parent = parent;
            this.replicaNum = replicaNum;
        }

        public PNode getParent() {
            return parent;
        }

        public void setParent(PNode parent) {
            this.parent = parent;
        }

        public int getReplicaNum() {
            return replicaNum;
        }

        public void setReplicaNum(int replicaNum) {
            this.replicaNum = replicaNum;
        }

        public boolean replicaOf(PNode pNode) {
            if (parent.getKey().equals(pNode.getKey()))
                return true;
            return false;
        }

        public String getKey() {
            return parent.getKey() + ":" + replicaNum;
        }
    }

    public static void main(String[] args) {
        int numberOfReplicas = 4;
        PNode pNode0 = new PNode("cache0.server.com", 12345);
        PNode pNode1 = new PNode("cache1.server.com", 12345);
        PNode pNode2 = new PNode("cache2.server.com", 12345);
        PNode pNode3 = new PNode("cache3.server.com", 12345);
        ConsistentHash cc = new ConsistentHash(new MD5HashingAlg(), Arrays.asList(pNode0, pNode1, pNode2, pNode3), numberOfReplicas);
        cc.addNode(pNode0, 2); //add 2 more replicas for pNode0
        System.out.println("key: john -> node: " + cc.getNode("john").getKey());
        System.out.println("key: alice -> node: " + cc.getNode("alice").getKey());
        System.out.println("key: peter -> node: " + cc.getNode("peter").getKey());
        System.out.println("key: stone -> node: " + cc.getNode("stone").getKey());
        System.out.println("key: steve -> node: " + cc.getNode("steve").getKey());
        cc.removeNode(pNode0);
    }

}
