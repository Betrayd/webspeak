package net.betrayd.webspeak.webrtc.transform.node.nodes;

import net.betrayd.webspeak.webrtc.transform.PacketHandler;
import net.betrayd.webspeak.webrtc.transform.PacketInfo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Node implements PacketHandler {
    @Nullable
    private Node nextNode = null;
    private List<Node> inputNodes = new ArrayList<Node>();


    public Node(){

    }

    public Node attach(Node node){
        // Remove ourselves as an input from the node we're currently connected to
        if(nextNode != null){
            throw new RuntimeException("Attempt to replace a Node's child. If this is intentional, use detachNext first.");
        }
        nextNode = node;
        node.addParent(this);

        return node;
    }

    public void detachNext(){
        if(nextNode != null){
            nextNode.removeParent(this);
        }
        nextNode = null;
    }

    public void addParent(Node newParent){
        inputNodes.add(newParent);
    }

    public void removeParent(Node parent){
        inputNodes.remove(parent);
    }

    public List<Node> getChildren(){
        if(nextNode != null){
            return List.of(nextNode);
        }
        return Collections.emptyList();
    }

    public List<Node> getParents(){
        return Collections.unmodifiableList(inputNodes);
    }

    public void stop(){

    }

    protected void next(PacketInfo packetInfo){
        if(nextNode != null){
            nextNode.processPacket(packetInfo);
        }
    }

    protected void next(List<PacketInfo> packetInfos){
        for(PacketInfo packetInfo : packetInfos){
            next(packetInfo);
        }
    }

    @Override
    public void processPacket(PacketInfo packetInfo) {

    }
}
