package ai.engineering.patternApplication.internal;

import ai.engineering.patternApplication.internal.extraTab.*;
import ai.engineering.patternApplication.internal.utility.*;
import com.change_vision.jude.api.gsn.model.*;
import com.change_vision.jude.api.inf.model.*;
import com.change_vision.jude.api.inf.presentation.*;
import com.change_vision.jude.api.inf.project.*;

import java.util.*;

public class LLMPatternSearchController {

    public String GenerateGPTResponse() {
        String kaosDiagram = GetPromptDiagram(false, true, false);
        String kaosDiagramUnreachedGoal = GetPromptDiagram(true, true, false);

        String prompt1 = "What are the best pattern that could be used to achieve the unreached goal of the KAOS diagram (GSN diagram)?\n" +
                "\n" +
                "#KAOS Diagram\n" +
                kaosDiagram +
                "\n" +
                "#KAOS Diagram(Unreached Goal)\n" +
                kaosDiagramUnreachedGoal +
                "\n" +
                "#Pattern CSV\n" +
                "ID, Pattern Name, Context, Problem, Solution\n" +
                "0,Selective repair,\"The model is already trained. Some classes is more important than other.,\"It is impossible to analyze the cause and consider countermeasures when deficiencies are found.\",Machine Learning repair tool\n" +
                "1,Adversarial example defense,\"The model has not been trained.,\"Response to malicious users (control by security attacks, misrecognition of images, falsification of learning data, etc.) is unknown.\",Adversarial training\n" +
                "2,Reprioritize accuracy,\"Model has been trained. Concept drift has been detected.,\"Recognition of critical (important) scenes cannot be guaranteed. New training data can reduce the recognition rate.\",Machine Learning repair tool\n" +
                "3,Training data sampling,\"Model has been trained.,\"Large amount of training and test data are needed to ensure reliability.\",Data sampling method\n" +
                "4,Model smoke testing,\"The model has just been retrained and need to be re-verified,\"The cost of re-verification and retesting of safety and reliability when changing the system is high.\",Smoke Testing\n" +
                "5,Safeguard,\"The system operates in an environment where safety cannot be fully guaranteed.,\"Safe system shutdown within the warranty period cannot be guaranteed.\",Rule-based Safety Guard Pattern\n" +
                "6,Security requirement satisfaction argument,\"Before deploying a DNN, a compelling argument for validation of security requirements must be provided.,\"Security requirements must be evaluated in a data and model driven manner.\",test data or formal verification\n" +
                "7,DNN Robustness Case Verification Pattern,\"Need to guarantee security of DNN in security-critical domains,\"A security case based on the formal verification of robustness has yet to be verified,arguments over the model verification, its inputs and model integration into the system as a whole\n" +
                "-1,Other,\"Other,\"Other\",Other\n";


        //初期のcontext,problemを手動で入力する場合
//        String prompt1 = "Please answer the pattern that corresponds to the Input.\n" +
//                "\n" +
//                "#Input\n" +
//                "Context: " + context + "\n" +
//                "Problem: " + problem + "\n" +
//                "\n" +
//                "#Pattern CSV\n" +
//                "ID, Pattern Name, Context, Problem, Solution\n" +
//                "0,Selective repair,\"The model is already trained. Some classes is more important than other.,\"It is impossible to analyze the cause and consider countermeasures when deficiencies are found.\",Machine Learning repair tool\n" +
//                "1,Adversarial example defense,\"The model has not been trained.,\"Response to malicious users (control by security attacks, misrecognition of images, falsification of learning data, etc.) is unknown.\",Adversarial training\n" +
//                "2,Reprioritize accuracy,\"Model has been trained. Concept drift has been detected.,\"Recognition of critical (important) scenes cannot be guaranteed. New training data can reduce the recognition rate.\",Machine Learning repair tool\n" +
//                "3,Training data sampling,\"Model has been trained.,\"Large amount of training and test data are needed to ensure reliability.\",Data sampling method\n" +
//                "4,Model smoke testing,\"The model has just been retrained and need to be re-verified,\"The cost of re-verification and retesting of safety and reliability when changing the system is high.\",Smoke Testing\n" +
//                "5,Safeguard,\"The system operates in an environment where safety cannot be fully guaranteed.,\"Safe system shutdown within the warranty period cannot be guaranteed.\",Rule-based Safety Guard Pattern\n" +
//                "6,Security requirement satisfaction argument,\"Before deploying a DNN, a compelling argument for validation of security requirements must be provided.,\"Security requirements must be evaluated in a data and model driven manner.\",test data or formal verification\n" +
//                "7,DNN Robustness Case Verification Pattern,\"Need to guarantee security of DNN in security-critical domains,\"A security case based on the formal verification of robustness has yet to be verified,arguments over the model verification, its inputs and model integration into the system as a whole\n" +
//                "-1,Other,\"Other,\"Other\",Other\n";

        String prompt2 = "From the above discussion, choose one ID for the appropriate pattern and output only that number";

        //入力例
        //cotext: 自動運転車の物体認識モデルの歩行者検出における誤認識が安全に大きな影響を与えるため、このクラスのみを優先的に改善する必要がある
        //problem: 歩行者検出が特定の状況で誤認識されることが判明しましたが、モデル全体を再トレーニングするリソースが限られているため、重要なクラスのみを効果的に修正する方法が求められる

        //歩行者検出の誤認識が発生した場合、自動運転車は歩行者を避けることができず、交通事故を引き起こす可能性がある

        GPTController gptController = new GPTController();
        return gptController.chat(new String[]{prompt1, prompt2});
    }

    public String GetPromptDiagram(boolean isUnreachedGoalOnly, boolean isKaosDiagram, boolean isSafetyCaseDiagram) {
        ProjectAccessor projectAccessor;
        //ITransactionManager transactionManager = null;
        AstahAPIUtils astahAPIUtils = new AstahAPIUtils();
        AstahUtils astahUtils = new AstahUtils();
        IFacet iFacet = null;

        String gsnDiagramSentence = "";
        try{
            projectAccessor = astahAPIUtils.getProjectAccessor();
            IModel iCurrentProject = projectAccessor.getProject();
            iFacet = astahAPIUtils.getGSNFacet(projectAccessor);

            //transactionManager = projectAccessor.getTransactionManager();

            String diagramNameText = "";//注意：もし指定している図がみつからない場合は、現在開いている図を取得

            if(isKaosDiagram){
                diagramNameText = LLMPatternSearchTab.GetKaosDiagramNameText();
            }
            if(isSafetyCaseDiagram){
                diagramNameText = LLMPatternSearchTab.GetSafetyCaseNameText();
            }

            IDiagram currentDiagram = astahAPIUtils.getSameNameDiagram(diagramNameText);

            //現在の図がnullの場合はエラーを出力して終了
            if(currentDiagram == null){
                System.out.println("Error: currentDiagram is null");
                return "-1";
            }

            IPresentation[] iNodePresentations = null;
            IPresentation[] iPresentations = astahUtils.getOwnedINodePresentation(currentDiagram);//プレゼンテーションを取得
            IElement topGoal = astahUtils.getTopGoal((IArgumentAsset) iPresentations[0].getModel());

            System.out.println("iPresentations[0].getLabel()は"+iPresentations[0].getLabel());
            System.out.println("topGoalは"+topGoal.getPresentations()[0].getLabel());
            gsnDiagramSentence = GetGSNDiagramSentence(topGoal, currentDiagram, isUnreachedGoalOnly);


        }catch (Exception e) {
            System.out.println("Error: " + e);
            e.printStackTrace();
        }

        return gsnDiagramSentence;



//
//        //topGoalを取得
//        //
//        String kaosDiagram =
//                "G1:Provide a reliable traffic sign classification system for level 3 ADV system\n" +
//                " G5:Classify traffic signs from camera color images\n" +
//                "  G9:Achieve very high performances on general cases\n" +
//                "   G4:Achieve  [Accuracy(Overall) >= 95.0]\n" +
//                "   G8:Achieve  [Precision(Overall) >= 95.0]\n" +
//                "  G2:Achieve reliability on important classes in suburban area\n" +
//                "   G15:Prioritized recall of traffic signs that signs dangerous objects ahead in suburban area\n" +
//                "    G13:Achieve  [Recall(Pedestrian crosswalk) >= 90.0]\n" +
//                "    G14:Achieve  [Recall(Children) >= 90.0]\n" +
//                "    G16:Achieve  [Recall(Cycles) >= 90.0]\n" +
//                "   G17:Prioritized precision of traffic signs that signals speed limits in suburban area\n" +
//                "    G18:Achieve  [Precision(Speed limit 20) >= 90.0]\n" +
//                "    G19:Achieve  [Precision(Speed limit 30) >= 90.0]\n" +
//                "    G20:Achieve  [Precision(Speed limit 50) >= 90.0]\n" +
//                "  G3:Achieve very low misclassification on highway area\n" +
//                "   G21:Minimized potential misclassification of speed limit signs into a higher one\n" +
//                "    G22:Achieve  [Misclassification(Speed limit 60, Speed limit 80) <= 2.0]\n" +
//                "    G23:Achieve  [Misclassification(Speed limit 100, Speed limit 120) <= 2.0]\n" +
//                "  G24:Ensure the input for decision on lanekeeping or overtaking is done correctly\n" +
//                "   G25:Achieve  [Accuracy(No overtaking) >= 90.0]\n" +
//                "   G26:Achieve very high recall on no overtaking signs  [Recall(No overtaking) >= 90.0]\n" +
//                " G6:Encapsulate ML model with safeguarding\n" +
//                "  G27:Ensured the quality of input data from the sensors\n" +
//                "  G28:Returned back the control to driver in out of domain operation";
//
//        return kaosDiagram;
    }

//    private String GetKaosDiagramUnreachedGoal() {
//        String kaosDiagramUnreachedGoal =
//                "G1:Provide a reliable traffic sign classification system for level 3 ADV system\n" +
//                " G5:Classify traffic signs from camera color images\n" +
//                "  G9:Achieve very high performances on general cases\n" +
//                "   G8:Achieve  [Precision(Overall) >= 95.0]\n" +
//                "  G3:Achieve very low misclassification on highway area\n" +
//                "   G21:Minimized potential misclassification of speed limit signs into a higher one\n" +
//                "    G22:Achieve  [Misclassification(Speed limit 60, Speed limit 80) <= 2.0]\n" +
//                "    G23:Achieve  [Misclassification(Speed limit 100, Speed limit 120) <= 2.0]\n";
//
//
//        return kaosDiagramUnreachedGoal;
//    }


    //深さ優先探索で記述
    public String GetGSNDiagramSentence(IElement topGoal, IDiagram currentDiagram, boolean isUnreachedGoalOnly){
        AstahUtils astahUtils = new AstahUtils();
        String gsnDiagramSentence = "";

        IPresentation[] nodeIPresentations = null;
        try {
            nodeIPresentations = astahUtils.getOwnedINodePresentation(currentDiagram);//現在開いている図のプレゼンテーションを取得
        }catch (Exception e){
            e.printStackTrace();
        }

        // 訪問済みノードを追跡する配列
        boolean[] visited = new boolean[nodeIPresentations.length];

        //Stackを利用して、深さ優先探索を行う
        //Stackには、topGoalを入れる
        Stack<Node> stack = new Stack<Node>();

        Node topNode = new Node();
        topNode.iElement = topGoal;
        topNode.depth = 0;
        stack.push(topNode);
        // スタックが空になるまでループ
        while (!stack.isEmpty()) {
            // スタックの一番上のノードを取得して削除
            Node current = stack.pop();


            //モデル上では存在するが、プレゼンテーション上では存在しない場合はスキップ
            if(GetNodeIndex(current.iElement, nodeIPresentations) == -1){
                continue;
            }

            // ノードが未訪問の場合のみ処理を行う
            if (!isVisited(current.iElement, nodeIPresentations, visited)) {
                // ノードを訪問済みとしてマーク
                visited[GetNodeIndex(current.iElement, nodeIPresentations)] = true;
                //System.out.println("Visited: " + current);

                //sentenceに書き込みを行う
                gsnDiagramSentence += WriteGSNLineSentence(current, isUnreachedGoalOnly);
                //System.out.println(gsnDiagramSentence);


                // 隣接ノードをスタックに追加（未訪問のもののみ）
                List<IElement> adjList = null;
                try{
                    adjList = astahUtils.getSupportedByInContextOfTarget((IArgumentAsset) current.iElement);
                }catch (Exception e){
                    e.printStackTrace();
                }

                if(adjList == null || adjList.isEmpty()){
                    continue;
                }
                for (IElement neighbor : adjList) {
                    if(GetNodeIndex(neighbor, nodeIPresentations) == -1){
                        continue;
                    }
                    if (!visited[GetNodeIndex(neighbor, nodeIPresentations)]) {
                        Node neighborNode = new Node();
                        neighborNode.iElement = neighbor;
                        neighborNode.depth = current.depth + 1;
                        stack.push(neighborNode);
                    }
                }
            }
        }

        return gsnDiagramSentence;
    }

    class Node{
        IElement iElement;
        int depth;
    }

    private boolean isVisited(IElement current, IPresentation[] nodeIPresentations, boolean[] visited){
        for(int i = 0; i < nodeIPresentations.length; i++){
            if(nodeIPresentations[i].getModel() == current){
                return visited[i];
            }
        }
        return false;
    }

    //モデル上のノードとプレゼンテーション上のノードの違いによりエラーが発生する場合がある
    private int GetNodeIndex(IElement current, IPresentation[] nodeIPresentations){
        for(int i = 0; i < nodeIPresentations.length; i++){
            if(nodeIPresentations[i].getModel() == current){
                return i;
            }
        }
        System.out.println("Error: GetNodeIndex");
        System.out.println("current: " + current);
        System.out.println("nodeIPresentations: " + nodeIPresentations);
        return -1;
    }

    private String WriteGSNLineSentence(Node current, boolean isUnreachedGoalOnly){
        if (isUnreachedGoalOnly){
            try {
                //色が赤ではない場合はスキップ
                System.out.println(((IArgumentAsset) current.iElement).getPresentations()[0].getProperty("fill.color"));
                if(!Objects.equals(((IArgumentAsset) current.iElement).getPresentations()[0].getProperty("fill.color"), "#FF0000")){
                    return "";
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
        String blank = "";
        for(int i = 0; i < current.depth; i++){
            blank += " ";
        }

        // content属性にpresentationの文章をセット
        IArgumentAsset iArgumentAsset = (IArgumentAsset) current.iElement;
        String content = iArgumentAsset.getContent();

        return blank + ((IArgumentAsset)current.iElement).getName() + ":" + content + "\n";

    }
}
