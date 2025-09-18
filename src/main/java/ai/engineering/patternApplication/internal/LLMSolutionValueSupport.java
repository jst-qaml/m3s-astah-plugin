package ai.engineering.patternApplication.internal;

public class LLMSolutionValueSupport {
    //このクラスはapplyPatternの中で呼び出されるため、ここでの処理は一度のみであることが前提
    private String[] llmSolutionValues = null;


    public boolean isSolutionSelectionSupportPattern(int patternN){
        //現状はP1 SelectiveRepairPatternとP3のみで行う
        if (patternN == 0||patternN == 2) {
            return true;
        }
        return false;
    }

    public int GetRepeatN(int patternN, int repeatN){
        if(!isSolutionSelectionSupportPattern(patternN)){
            return repeatN;
        }

        String[] importantClassNames = GetImportantClassNames();
        if(importantClassNames.length == 0){
            return 1;
        }else{
            return importantClassNames.length;
        }
    }

    public String ReplaceSolutionValue(int patternN, String content, int loopN){
        if(!isSolutionSelectionSupportPattern(patternN)){
            return content;
        }

        //P1,3の場合
        if(patternN == 0||patternN==2){
            String[] importantClassNames = GetImportantClassNames();
            //content = content.replace("RepairPriority for {important class {X[0]}} = {5}", String.valueOf("RepairPriority for {important class {X[0]}} = {"+ GetPriorityValuesFromRiskValue()[loopN]+"}"));
            //content = content.replace("PreventDegradation for {important class {X[0]}} = {5}", String.valueOf("PreventDegradation for {important class {X[0]}} = {"+ GetPreventValuesFromRiskValue()[loopN]+"}"));
            content = content.replace("{important class {X[0]}}", String.valueOf(importantClassNames[loopN]+" class"));
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

    private String[] GetImportantClassNames(){
        //何回もLLMを使うのは時間がかかり、変動するので、一度取得したものを使う
        if(llmSolutionValues != null){
            return llmSolutionValues;
        }

        //KAOS図から取得する
        String response = GenerateGPTResponseRepairPattern();
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

    private String GenerateGPTResponseRepairPattern() {
        LLMPatternSearchController llmPatternSearchController = new LLMPatternSearchController();
        String kaosDiagram = llmPatternSearchController.GetPromptDiagram(false, true, false);
        String kaosDiagramUnreachedGoal = llmPatternSearchController.GetPromptDiagram(true, true, false);

        //selective repair patternの場合
        String prompt1 = "The following is a KAOS Diagram (GSN diagram) of a machine learning system. Which specific individual important classes name of machine learning classifications described in Unreached Goal are in need of repair? Do not include any general and performance metrics.\n" +
                "\n" +
                "#KAOS Diagram\n" +
                kaosDiagram +
                "\n" +
                "#KAOS Diagram(Unreached Goal)\n" +
                kaosDiagramUnreachedGoal +
                "\n" +
                "#Description of Repair Tools\n" +
                "It directly repairs machine learning using input-output pairs as a specification. To achieve this, the repair priority and the values for preventing degradation are set within the range of 0 to 10 for important classes. A higher value indicates higher priority. This enables the effective repair of important classes.";


        //;区切りで要素を生成
        String prompt2 = "From the above discussion, output only specific individual important classes name of machine learning classifications in need of repair. Do not include any performance metrics or descriptions. If there are multiple important classes name, output them separated by ;.";


        GPTController gptController = new GPTController();
        return gptController.chat(new String[]{prompt1, prompt2});
    }
}
