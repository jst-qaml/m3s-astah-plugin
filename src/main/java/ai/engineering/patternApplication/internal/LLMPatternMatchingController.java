package ai.engineering.patternApplication.internal;

import ai.engineering.patternApplication.internal.entity.*;

import java.util.*;

public class LLMPatternMatchingController {
    private static String GSNPrompt = "";

    private PatternConfigManager patternConfigManager = new PatternConfigManager();
    //prompt2について、contextの位置やノードの繋がりと配列の順番が違う場合の処理も必要になる
    public String[] GetLLMMatchingResponse(int patternN){
        String response = GenerateGPTResponse(patternN);
        String[] responseArray = response.split(";");

        //-1の部分を""に置き換える
        for (int i = 0; i < responseArray.length; i++) {
            if ("-1".equals(responseArray[i])) {
                responseArray[i] = "";
            }
        }

        //配列の中のcontextを一番前に持ってくるコード
        int contextIndex = 4;//パターンごとの分岐が必要
        String[] newResponseArray = new String[patternConfigManager.patternParameterExplanationNames[patternN].length];//GPTの応答の要素数が多くてもエラーにならずに後ろの要素を切る
        newResponseArray[0] = responseArray[contextIndex];
        for (int i = 0; i < newResponseArray.length; i++) {
            if (i == contextIndex) {
                continue;
            }else if(i < contextIndex){
                newResponseArray[i + 1] = responseArray[i];
            }else if(i > contextIndex){
                newResponseArray[i] = responseArray[i];
            }
        }

        //配列の中身を全て出力するコード
        for (int i = 0; i < newResponseArray.length; i++) {
            System.out.println("newResponseArray[" + i + "] = " + newResponseArray[i]);
        }

        return newResponseArray;
    }

    private String GenerateGPTResponse(int patternN) {
        //return "G31;S4;-1;-1;-1;-1;-1;-1;";//contextが必要なため、冒頭に;をつけている


        String gptConcept = GenerateGPTConcept();
        String gsnDiagramPrompt = GenerateGSNDiagramPrompt();
        String patternPrompt = GeneratePatternPrompt(patternN);
        String prompt = "You are a strict sentence similarity evaluator. Apply the pattern to the following GSN diagrams, answering for each which node in the GSN diagram corresponds to which node in the pattern. If there is no correspond node, exit as -1. Let's think step by step.\n\n"
                + "#Concept\n"
                + gptConcept + "\n\n"
                + "#GSN diagram\n"
                + gsnDiagramPrompt + "\n\n"
                + "#Pattern\n"
                + patternPrompt + "\n"
                + "Do not match if:" + "\n"
                + "- The meaning is broader or narrower than the pattern node. Ensure it conveys the IDENTICAL core message and scope."+ "\n"
                + "- The node is just contextually related but does not express the same **explicit problem statement or action-oriented intent** as the pattern."+ "\n"
                + "- The node type is different from the pattern node type.";

        String prompt2 = "Based on the above discussion, please answer the nodes that match. Please output the corresponding node ID in order of the pattern, separated by ;. There should be -1, even if the node is missing. Do not write any extra text.\n\n"
                + "#Pattern\n"
                + patternPrompt
                + "\n"
                + "#Example\n"
                + "For example, if a pattern has 8 nodes and only the first 3 pattern nodes match and the rest do not, the output would be: X;Y;Z;-1;-1;-1;-1;-1;. (8 positions)";

        GPTController gptController = new GPTController();
        return gptController.chat(new String[]{prompt, prompt2});
    }

    private String GenerateGPTConcept() {
        String concept =
                "Step 1: Find a node in the GSN diagram that has exactly the same meaning and equally specific meaning　to the sentence in the first node of pattern. If there is no correspond node, set to -1. From that failure point, you use depth-first search to backtrack pattern node and continue matching the rest of the pattern.\n"
                + "Step 2: Find a child node of the node selected in Step 1 whose sentence has exactly the same meaning and equally specific meaning　to the sentence of the second node in the pattern. If there is no correspond node, set to -1. From that failure point, you use depth-first search to backtrack pattern node and continue matching the rest of the pattern.\n"
                + "Step 3: Find a child node of the node selected in Step 2 whose sentence has exactly the same meaning and equally specific meaning　to the sentence of the third node in the pattern. If there is no correspond node, set to -1. From that failure point, you use depth-first search to backtrack pattern node and continue matching the rest of the pattern.\n"
                + "Step 4: Find the node in the GSN diagram that matches the node in the pattern by repeating in the same way\n";
        return concept;
    }

    public void SetGSNPrompt() {
        LLMPatternSearchController llmPatternSearchController = new LLMPatternSearchController();
        GSNPrompt = llmPatternSearchController.GetPromptDiagram(false, false, true);
    }
    private String GenerateGSNDiagramPrompt() {
        if(GSNPrompt != null && !GSNPrompt.isEmpty()){
            return GSNPrompt;
        }

        LLMPatternSearchController llmPatternSearchController = new LLMPatternSearchController();
        return llmPatternSearchController.GetPromptDiagram(false, false, true);

//
//        String gsnDiagram =
//                "G30:Provide a reliable traffic sign classification system for level 3 ADV system\n"
//                + " S1:Ensure achievement of minimum performance of important classes\n"
//                + "  G31:Quality of ML model training is ensured\n"
//                + "   S3:Utilize data manipulation technique to improve training quality\n"
//                + "    G35:Data is augmented properly\n"
//                + "     Sn1:Utilize rotation augmentation\n"
//                + "     Sn2:Utilize saturation augmentation\n"
//                + "    G36:Data is rebalanced to emulate operational domain\n"
//                + "     Sn3:Rebalance data to increase the population of the pedestrian crosswalk class\n"
//                + "     Sn4:Rebalance data to increase the population of the children class\n"
//                + "     Sn5:Rebalance data to increase the population of the cycle class\n"
//                + "   S4:Utilize DNN repair based on degree of unsatisfaction\n"
//                + "    G37:Repair configuration is set on gap between expected and actual class\n"
//                + "     Sn8:RepairPriority(Speed limit 60 = 10)\n"
//                + "     Sn9:RepairPriority(Speed limit 100 = 5)\n"
//                + "  G32:Quality of ML model testing is ensured\n"
//                + "  G33:Quality of ML model monitoring is ensured\n"
//                + "  G34:Quality of supporting components ensured\n"
//                + "   S5:Employ suitable camera sensors\n"
//                + "    G38:Employed camera sensors has enough resolution\n"
//                + "     Sn10:Minimum resolution of cameras = xxxx\n"
//                + " S2:Ensure failure of ML model is handled by safeguard code";
//        return gsnDiagram;
    }

    public String GeneratePatternPrompt(int patternN) {
        //todo: パターンのgsnを作成
        String pattern = "";

//        if(patternN == 0){
//            //P1の場合
//            pattern =
//                "G1:Model well trained\n"
//                        + " S1:Use model repair techniques\n"
//                        + "  G2:Solving deficiencies hinder analysis and countermeasures\n"
//                        + "   C1:The model is already trained and some classes are more important than others\n"
//                        + "   S2:Use Machine Learning repair tool\n"
//                        + "    G3:Well reparation of {important class 1}\n"
//                        + "     Sn1.X:RepairPriority for {important class X} = {high}\n"
//                        + "     Sn2.X:PreventDegradation for {important class X} = {high}\n";
//
//        }else if(patternN == 5){
//            //safeguardの場合
//            pattern =
//                "G1:System is safe to operate outside expected domain\n"
//                        + " S1:Switching to a non-ML system\n"
//                        + "  G2:Solving issues that do not guarantee a safe shutdown of the system within the warranty period\n"
//                        + "   C1:The machine learning system's application domain is clearly defined\n"
//                        + "   S2:Use rule-base safeguards\n"
//                        + "    G3:ML system is safe-guarded by rule-based function\n"
//                        + "     Sn1:Define threshold-based rules to override unsafe decisions made by the ML system\n";
//        }

        int[] idCount = new int[]{1, 1, 1, 1, 1, 1};//G,S,Sn,C,J,Aの順番
//        for (int i = 1; i < patternConfigManager.patternParameterExplanationNames[patternN].length; i++) {
//            pattern += GetGSNType(patternN, i) + idCount[GetGSNTypeInt(patternN, i)] + ":" + patternConfigManager.patternParameterExplanationNames[patternN][i] + "\n";
//            idCount[GetGSNTypeInt(patternN, i)]++;
//        }
        //深さ優先探索で有向グラフを作る。stuckを利用。深さが1増えるごとに、" "を前方に一つ増やす。最後に改行
        int[][] edges = patternConfigManager.patternLinkPair[patternN];
        Stack<Integer> stack = new Stack<>();
        stack.push(1);
        int previousDepth = 0;
        int[] depthMap = new int[patternConfigManager.patternParameterExplanationNames[patternN].length];

        while (!stack.isEmpty()) {
            int currentIndex = stack.pop();
            int currentDepth = depthMap[currentIndex];

            for(int i = 0; i < currentDepth; i++){
                pattern += " ";
            }

            pattern += GetGSNType(patternN, currentIndex) + idCount[GetGSNTypeInt(patternN, currentIndex)] + ":" + patternConfigManager.patternParameterExplanationNames[patternN][currentIndex] + "\n";
            idCount[GetGSNTypeInt(patternN, currentIndex)]++;
            previousDepth = currentDepth;

            //子ノードをスタックに追加（逆順で追加して、元の順序で処理されるようにする）
            List<Integer> children = new ArrayList<>();
            for (int[] edge : edges) {
                if (edge[0] == currentIndex) {
                    children.add(edge[1]);
                    depthMap[edge[1]] = currentDepth + 1;
                }
            }
            Collections.reverse(children);
            for (int child : children) {
                stack.push(child);
            }
        }

        return pattern;
    }

    private String GetGSNType(int patternN, int index){
        String type = patternConfigManager.patternParameterTypes[patternN][index];
        if(type == "Goal"){
            return "G";
        }else if(Objects.equals(type, "Strategy")){
            return "S";
        }else if(Objects.equals(type, "Solution")){
            return "Sn";
        }else if(Objects.equals(type, "Context")){
            return "C";
        }else if(Objects.equals(type, "Justification")){
            return "J";
        }else if(Objects.equals(type, "Assumption")){
            return "A";
        }else{
            return "";
        }
    }

    private int GetGSNTypeInt(int patternN, int index){
        String type = patternConfigManager.patternParameterTypes[patternN][index];
        if(Objects.equals(type, "Goal")){
            return 0;
        }else if(Objects.equals(type, "Strategy")){
            return 1;
        }else if(Objects.equals(type, "Solution")){
            return 2;
        }else if(Objects.equals(type, "Context")){
            return 3;
        }else if(Objects.equals(type, "Justification")){
            return 4;
        }else if(Objects.equals(type, "Assumption")){
            return 5;
        }else{
            return -1;
        }
    }





}
