package net.betrayd.webspeak.webrtc.transform.node;

import net.betrayd.webspeak.webrtc.transform.node.nodes.Node;

import java.util.*;

public class NodeVisitors {
    /**
     * Produces a set of all notes in the tree.
     */
    public static class NodeSetVisitor extends NodeVisitor {
        private final Set<Node> nodeSet;
        public NodeSetVisitor() {
            nodeSet = new HashSet<>();
        }
        public NodeSetVisitor(Set<Node> nodeSet) {
            this.nodeSet = nodeSet;
        }

        @Override
        public void doWork(Node node) {
            nodeSet.add(node);
        }
    }

    public static class NodeTearDownVisitor extends NodeVisitor {
        @Override
        public void visit(Node node) {
            for(Node child : node.getChildren()){
                visit(child);
            }
            doWork(node);
        }

        @Override
        public void reverseVisit(Node node) {
            // We can't use the default reverseVisit method, because in doWork
            // we modify the parents list, so we'll get a ConcurrentModificationException.
            // So instead we override the method here and make a copy of the parents
            // and use that to iterate over so we can modify the real one
            List<Node> parentsCopy = new ArrayList<>(node.getParents());
            for(Node parent : parentsCopy){
                reverseVisit(parent);
            }
            doWork(node);
        }

        @Override
        public void doWork(Node node) {
            node.stop();
            //if (node instanceof DemuxerNode) {
            //    ((DemuxerNode) node).removePacketPaths();
            //} else {
            node.detachNext();
            //}
        }
    }
}
