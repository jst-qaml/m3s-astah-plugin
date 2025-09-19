package ai.engineering.patternApplication.internal;

import ai.engineering.patternApplication.internal.entity.*;

import java.util.*;

public class LLMSolutionValueSupport {
    //このクラスはapplyPatternの中で呼び出されるため、ここでの処理は一度のみであることが前提
    private PatternConfigManager patternConfigManager = new PatternConfigManager();
    private String[] llmSolutionValues = null;


    public boolean isSolutionSelectionSupportPattern(int patternN){
        //ソリューションに{}のあるパターンでLLMを使って値を取得する
        if (patternN == 0||patternN == 1||patternN == 2||patternN == 4||patternN == 6) {
            return true;
        }
        return false;
    }

    public int GetRepeatN(int patternN, int repeatN){
        if(!isSolutionSelectionSupportPattern(patternN)){
            return repeatN;
        }

        String[] valueNames = new String[]{};
        //repeatは現状0,2,4のみ
        if(patternN == 0||patternN==2){
            valueNames = GetImportantClassNames(patternN);
        }else if(patternN == 4){
            valueNames = GetGeneralValueName(patternN,new String[]{"{type X smoke testing}"});
        }


        if(valueNames.length == 0){
            return 1;
        }else{
            return valueNames.length;
        }


    }

    public String ReplaceSolutionValue(int patternN, String content, int loopN){
        if(!isSolutionSelectionSupportPattern(patternN)){
            return content;
        }

        //P1,3の場合は特化したプロンプト、それ以外は汎用的なプロンプトで取得した値を使う
        if(patternN == 0||patternN==2){
            String[] importantClassNames = GetImportantClassNames(patternN);
            //String[] importantClassNames = GetGeneralValueName(patternN,new String[]{"{X[0]}"});
            //content = content.replace("RepairPriority for {important class {X[0]}} = {5}", String.valueOf("RepairPriority for {important class {X[0]}} = {"+ GetPriorityValuesFromRiskValue()[loopN]+"}"));
            //content = content.replace("PreventDegradation for {important class {X[0]}} = {5}", String.valueOf("PreventDegradation for {important class {X[0]}} = {"+ GetPreventValuesFromRiskValue()[loopN]+"}"));
            content = content.replace("{important class {X[0]}}", String.valueOf(importantClassNames[loopN]+" class"));
            return content;
        }else if(patternN == 1){
            String[] generalValueNames = GetGeneralValueName(patternN,new String[]{"{0}"});
            if(Objects.equals(generalValueNames[0], "-1")){
                return content;
            }
            content = content.replace("Value of ε = {0}", String.valueOf("Value of ε = "+generalValueNames[0]));
            return content;
        }else if(patternN == 4){
            String[] generalValueNames = GetGeneralValueName(patternN,new String[]{"{type X smoke testing}"});
            if(Objects.equals(generalValueNames[0], "-1")){
                return content;
            }
            content = content.replace("{type X smoke testing}", String.valueOf(generalValueNames[loopN]));
            return content;
        }else if(patternN == 6){
            String[] generalValueNames = GetGeneralValueName(patternN,new String[]{"{Agreement over inspection conducted by domain and formalization expert}", "{Formal verification results}"});
            if(Objects.equals(generalValueNames[0], "-1")){
                return content;
            }
            content = content.replace("{Agreement over inspection conducted by domain and formalization expert}", String.valueOf(generalValueNames[0]));
            content = content.replace("{Formal verification results}", String.valueOf(generalValueNames[1]));
            return content;
        }else{
            return content;
        }
    }

//    private int[] GetPriorityValuesFromRiskValue(){
//        int[] array = Arrays.stream(GetRiskValues())
//                .mapToInt(d -> (int) Math.floor(d)) // 小数点以下を切り捨て
//                .toArray();
//        //10以上の場合は10にして、0以下の場合は0にする
//        for(int i = 0; i < array.length; i++){
//            if(array[i] >= 10){
//                array[i] = 10;
//            }else if(array[i] <= 0){
//                array[i] = 0;
//            }
//        }
//        return array;
//    }
//
//    private int[] GetPreventValuesFromRiskValue(){
//        int[] array = Arrays.stream(GetRiskValues())
//                .mapToInt(d -> (int) Math.floor(d)) // 小数点以下を切り捨て
//                .toArray();
//        //10以上の場合は10にして、0以下の場合は0にする
//        for(int i = 0; i < array.length; i++){
//            if(array[i] >= 10){
//                array[i] = 10;
//            }else if(array[i] <= 0){
//                array[i] = 0;
//            }
//        }
//        return array;
//    }

    private String[] GetGeneralValueName(int patternN, String[] replaceValues){
        //何回もLLMを使うのは時間がかかり、変動するので、一度取得したものを使う
        if(llmSolutionValues != null){
            return llmSolutionValues;
        }

        //KAOS図から取得する
        String response = GenerateGPTResponseGeneralPattern(patternN, replaceValues);
        String[] valueNames = response.split(";");

        llmSolutionValues = valueNames;

        return valueNames;

    }


    private String[] GetImportantClassNames(int patternN){
        //何回もLLMを使うのは時間がかかり、変動するので、一度取得したものを使う
        if(llmSolutionValues != null){
            return llmSolutionValues;
        }

        //KAOS図から取得する
        String response = GenerateGPTResponseRepairPattern(patternN);
        String[] importantClassNames = response.split(";");

        llmSolutionValues = importantClassNames;


        //String[] importantClassNames = {"Speed limit 60", "Speed limit 100"};
        return importantClassNames;

        //return VersionFetcher.GetLabels(false);
//        String[] importantClassNames = {"Mobility", "Drivable", "Obstacle"};
//        return importantClassNames;
    }

//    private double[] GetRiskValues(){
//        double[] riskValues = {6.5, 2.3, 12.4, 3.7, 1.0, 9.8};
//        return riskValues;
//    }


    private String GenerateGPTResponseGeneralPattern(int patternN, String[] replaceValues) {
        LLMPatternSearchController llmPatternSearchController = new LLMPatternSearchController();
        LLMPatternMatchingController llmPatternMatchingController = new LLMPatternMatchingController();

        String kaosDiagram = llmPatternSearchController.GetPromptDiagram(false, true, false);
        String kaosDiagramUnreachedGoal = llmPatternSearchController.GetPromptDiagram(true, true, false);

        String patternPrompt = llmPatternMatchingController.GeneratePatternPrompt(patternN);

        String valuesString = "";
        for(int i = 0; i < replaceValues.length; i++){
            valuesString += replaceValues[i];
            if(i != replaceValues.length - 1){
                valuesString += ", ";
            }
        }

        //Generalなパターンの場合のプロンプト
        String prompt1 = "The following is a KAOS diagram (GSN diagram) for a machine learning system. Refer to this KAOS diagram and the KAOS diagram for unmet objectives, and consider which words fits into the "+ valuesString+" in the pattern description. If no appropriate character exists, output -1. Do not include general metrics or performance indicators. Let's think step by step.\n" +
                "\n" +
                "#KAOS Diagram\n" +
                kaosDiagram +
                "\n" +
                "#KAOS Diagram(Unreached Goal)\n" +
                kaosDiagramUnreachedGoal +
                "\n" +
                "#"+patternConfigManager.patternNames[patternN]+" pattern description\n" +
                patternPrompt;

        //;区切りで要素を生成
        String prompt2 = "From the above discussion, output only the specific words that should be replaced in the "+valuesString+". If there are multiple words, output them separated by ;. If there are no appropriate words, output -1.";


        GPTController gptController = new GPTController();
        return gptController.chat(new String[]{prompt1, prompt2});
    }

    private String GenerateGPTResponseRepairPattern(int patternN) {
        LLMPatternSearchController llmPatternSearchController = new LLMPatternSearchController();
        LLMPatternMatchingController llmPatternMatchingController = new LLMPatternMatchingController();

        String kaosDiagram = llmPatternSearchController.GetPromptDiagram(false, true, false);
        String kaosDiagramUnreachedGoal = llmPatternSearchController.GetPromptDiagram(true, true, false);

        String patternPrompt = llmPatternMatchingController.GeneratePatternPrompt(patternN);
        //selective repair patternの場合
        String prompt1 = "The following is a KAOS Diagram (GSN diagram) of a machine learning system. Which specific individual important classes name of machine learning classifications described in Unreached Goal are in need of repair? Do not include any general and performance metrics.\n" +
                "\n" +
                "#KAOS Diagram\n" +
                kaosDiagram +
                "\n" +
                "#KAOS Diagram(Unreached Goal)\n" +
                kaosDiagramUnreachedGoal +
                "\n" +
                "#"+patternConfigManager.patternNames[patternN]+" pattern description\n" +
                patternPrompt +
                "\n" +
                "#Description of Repair Tools\n" +
                "It directly repairs machine learning using input-output pairs as a specification. To achieve this, the repair priority and the values for preventing degradation are set within the range of 0 to 10 for important classes. A higher value indicates higher priority. This enables the effective repair of important classes.";


        //;区切りで要素を生成
        String prompt2 = "From the above discussion, output only specific individual important classes name of machine learning classifications in need of repair. Do not include any performance metrics or descriptions. If there are multiple important classes name, output them separated by ;.";


        GPTController gptController = new GPTController();
        return gptController.chat(new String[]{prompt1, prompt2});
    }
}
