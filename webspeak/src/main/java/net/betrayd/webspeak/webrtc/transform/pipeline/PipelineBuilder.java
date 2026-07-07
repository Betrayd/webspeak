package net.betrayd.webspeak.webrtc.transform.pipeline;

import net.betrayd.webspeak.webrtc.transform.node.nodes.Node;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

// TODO: look into @DslMarker to prevent inner dsl builders from accidentally setting parent
// member variables when they overlap
public class PipelineBuilder {
    public static Node pipeline(Consumer<PipelineBuilder> block){
        PipelineBuilder pipelineBuilder = new PipelineBuilder();
        block.accept(pipelineBuilder);
        return pipelineBuilder.build();
    }

    @Nullable
    private Node head = null;
    @Nullable
    private Node tail = null;

    public void addNode(Node node){
        if (head == null){
            head = node;
        }
        /*
        * Demuxer Node Thing here
        */
        if (tail != null){
            tail.attach(node);
        }
        tail = node;
    }

    public void node(Node node, Supplier<Boolean> condition){
        if(condition.get()){
            addNode(node);
        }
    }

    /**
     * simpleNode allows the caller to pass in a block of code which takes a list of input
     * [Packet]s and returns a list of output [Packet]s to be forwarded to the next
     * [Node]
     */
    public void simpleNode(String name){
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Node build(){
        if (head == null) {
            throw new IllegalStateException("Build called on pipeline that contains no nodes");
        }
        return head;
    }
}
