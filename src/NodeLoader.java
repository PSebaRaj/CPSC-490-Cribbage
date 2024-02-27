import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

// class for compressing strings
enum StringCompressor {
    ;
    public static byte[] compress(String text) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            OutputStream out = new DeflaterOutputStream(baos);
            out.write(text.getBytes("UTF-8"));
            out.close();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return baos.toByteArray();
    }

    public static String decompress(byte[] bytes) {
        InputStream in = new InflaterInputStream(new ByteArrayInputStream(bytes));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[8192];
            int len;
            while((len = in.read(buffer))>0)
                baos.write(buffer, 0, len);
            return new String(baos.toByteArray(), "UTF-8");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}

public class NodeLoader {

    /**
     * Get CFR Nodes from file.
     * @param fileName
     * @param cls
     * @param sample: whether to sample actions according to probabilities or argmax. Argmax performs better.
     * @return HashMap of infoset and CFRNode
     * @throws IOException
     */
    public static HashMap<String, CFRNode> getNodes(String fileName, Class<?> cls, boolean sample) throws IOException {
        HashMap<String, CFRNode> nodes = new HashMap<>();

        // Read the serialized data from the file
        InputStream input = new GZIPInputStream(new FileInputStream(fileName));
        CFRNodeOuterClass.NodeMap nodeMap = CFRNodeOuterClass.NodeMap.parseFrom(input);
        input.close();

        // Iterate over entries in the NodeMap
        for (Map.Entry<String, CFRNodeOuterClass.CFRNode> entry : nodeMap.getNodesMap().entrySet()) {
            String key = entry.getKey();
            CFRNodeOuterClass.CFRNode cfrNodeProto = entry.getValue();

            // Create a new CFRNode instance using the parsed data
            float[] regretSumArray = new float[cfrNodeProto.getRegretSumCount()];
            for (int i = 0; i < cfrNodeProto.getRegretSumCount(); i++) {
                regretSumArray[i] = cfrNodeProto.getRegretSum(i);
            }
            float[] strategyArray = new float[cfrNodeProto.getStrategyCount()];
            for (int i = 0; i < cfrNodeProto.getStrategyCount(); i++) {
                strategyArray[i] = cfrNodeProto.getStrategy(i);
            }

            float[] strategySumArray = new float[cfrNodeProto.getStrategySumCount()];
            for (int i = 0; i < cfrNodeProto.getStrategySumCount(); i++) {
                strategySumArray[i] = cfrNodeProto.getStrategySum(i);
            }

            CFRNode node = (cls == ThrowNode.class) ? new ThrowNode(sample) : new PegNode((byte) regretSumArray.length, sample);
            node.regretSum = regretSumArray;
            node.strategy = strategyArray;
            node.strategySum =  strategySumArray;

            // Put the node into the HashMap
            nodes.put(key, node);
        }

        return nodes;
    }


    public static HashMap<String, ThrowNode> getThrowNodes(String fileName, boolean sample) throws IOException {
        System.out.println("Loading ThrowNodes");
        HashMap<String, CFRNode> nodes = getNodes(fileName, ThrowNode.class, sample);
        Map<String,ThrowNode> newNodes = nodes.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> (ThrowNode)e.getValue()));
        return new HashMap<>(newNodes);
    }

    public static HashMap<String, ThrowNode> getThrowNodes(String fileName) throws IOException {
        return getThrowNodes(fileName, false);
    }

    public static HashMap<String, PegNode> getPegNodes(String fileName, boolean sample) throws IOException {
        System.out.println("Loading PegNodes");
        HashMap<String, CFRNode> nodes = getNodes(fileName, PegNode.class, sample);
        Map<String,PegNode> newNodes = nodes.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> (PegNode)e.getValue()));
        return new HashMap<>(newNodes);
    }

    public static HashMap<String, PegNode> getPegNodes(String fileName) throws IOException {
        return getPegNodes(fileName, false);
    }

    /**
     * Save nodes to a file.
     * @param fileName
     * @param nodes
     * @throws IOException
     */
    public static void saveNodes(String fileName, HashMap<String, ? extends CFRNode> nodes) throws IOException {
        CFRNodeOuterClass.NodeMap.Builder nodeMapBuilder = CFRNodeOuterClass.NodeMap.newBuilder();

        // Populate NodeMap.Builder with data from HashMap
        for (Map.Entry<String, ? extends CFRNode> entry : nodes.entrySet()) {
            CFRNode node = entry.getValue();
            CFRNodeOuterClass.CFRNode.Builder cfrNodeBuilder = CFRNodeOuterClass.CFRNode.newBuilder();

            // write the data to the builder
            for (float regretSumValue : node.regretSum) {
                cfrNodeBuilder.addRegretSum(regretSumValue);
            }
            for (float strategyValue : node.strategy) {
                cfrNodeBuilder.addStrategy(strategyValue);
            }
            for (float strategySumValue : node.strategySum) {
                cfrNodeBuilder.addStrategySum(strategySumValue);
            }
            cfrNodeBuilder.setNumActions(node.numActions);

            nodeMapBuilder.putNodes(entry.getKey(), cfrNodeBuilder.build());
        }

        CFRNodeOuterClass.NodeMap nodeMap = nodeMapBuilder.build();

        // Write the serialized data to the file
        OutputStream output = new GZIPOutputStream(new FileOutputStream(fileName));
        nodeMap.writeTo(output);
        output.close();
    }

    /**
     * Converts a CFRNode (either PegNode or ThrowNode) to JSON.
     * @param nodes
     * @return json string
     */
    private static String jsonify(HashMap<String, ? extends CFRNode> nodes) {
        HashMap<String, Object[]> nodes_new = new HashMap<>();
        for (String key : nodes.keySet()) {
            CFRNode node = nodes.get(key);
            Object[] obj = new Object[]{node.regretSum, node.strategySum, node.strategy};
            nodes_new.put(key, obj);
        }
        return new Gson().toJson(nodes_new);
    }

    private static float[] convertToFloatArr(Object obj) {
        ArrayList arr = (ArrayList) obj;
        float[] out = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            out[i] = ((Double)arr.get(i)).floatValue();
        }
        return out;
    }
}
