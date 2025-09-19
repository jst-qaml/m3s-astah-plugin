package ai.engineering.patternApplication.internal;

import ai.engineering.patternApplication.internal.entity.*;
import ai.engineering.patternApplication.internal.utility.*;
import ai.engineering.patternApplication.internal.extraTab.LLMPatternSearchTab;
import com.change_vision.jude.api.gsn.editor.*;
import com.change_vision.jude.api.gsn.model.*;
import com.change_vision.jude.api.inf.editor.*;
import com.change_vision.jude.api.inf.exception.*;
import com.change_vision.jude.api.inf.model.*;
import com.change_vision.jude.api.inf.presentation.*;
import com.change_vision.jude.api.inf.project.*;
import com.change_vision.jude.api.inf.view.*;

import java.awt.*;
import java.util.*;
import java.util.List;


public class PatternMatchingController {
    private AstahAPIUtils astahAPIUtils = new AstahAPIUtils();
    private AstahUtils astahUtils = new AstahUtils();
    private Const constClass = new Const();
    private PatternConfigManager patternConfigManager = new PatternConfigManager();
    private TransformationManager transformationManager = new TransformationManager();

    public SelectionSupportDataBase selectionSupportDataBase = new SelectionSupportDataBase();


    private OCLValidator oclValidator = new OCLValidator();

    //仮
    /*
    private final ProjectEventListener projectEventListener = new ProjectEventListener() {
        @Override
        public void projectChanged(ProjectEvent projectEvent) {
            System.out.println("projectChanged");
            astahUtils.GetCurrentTime();//現在時刻表示
        }

        @Override
        public void projectOpened(ProjectEvent projectEvent) {
            //System.out.println("projectChanged");
        }

        @Override
        public void projectClosed(ProjectEvent projectEvent) {
            //System.out.println("projectChanged");
        }
    };

     */


    private final IEntitySelectionListener possibilityListener = new IEntitySelectionListener() {
        @Override
        public void entitySelectionChanged(IEntitySelectionEvent iEntitySelectionEvent) {
            astahUtils.GetCurrentTime();//現在時刻表示
            long startSelectionTime = System.currentTimeMillis();//実行時間計測開始
            //System.out.println("entitySelectionChanged");
            //Match();
            OclMatch();
            //LLMMatch();はOclMatch()の中で呼び出す
            //リスナの変更は上位で行う必要がある?
            //IDiagramViewManager diagramViewManager = astahAPIUtils.getDiagramViewManager();
            //diagramViewManager.removeEntitySelectionListener(this);
            long endSelectionTime = System.currentTimeMillis();//実行時間計測終了
            System.out.println("実行時間Selection：\n" + (endSelectionTime - startSelectionTime)  + "\nms");

        }
    };
    public void AddRecommendationListener() {
        //リスナに追加する
        IDiagramViewManager diagramViewManager = astahAPIUtils.getDiagramViewManager();
        diagramViewManager.addEntitySelectionListener(possibilityListener);


    }

    public void RemoveRecommendationListener() {
        //リスナを削除する
        IDiagramViewManager diagramViewManager = astahAPIUtils.getDiagramViewManager();
        diagramViewManager.removeEntitySelectionListener(possibilityListener);
    }

    private final IEntitySelectionListener applySelectionPatternListener = new IEntitySelectionListener() {
        @Override
        public void entitySelectionChanged(IEntitySelectionEvent iEntitySelectionEvent) {
            astahUtils.GetCurrentTime();//現在時刻表示
            long startApplicationTime = System.currentTimeMillis();//実行時間計測開始
            ApplySelectionPattern();
            //RemoveApplySelectionPatternListener();
            long endApplicationTime = System.currentTimeMillis();//実行時間計測終了
            System.out.println("実行時間Application：\n" + (endApplicationTime - startApplicationTime)  + "\nms");

        }
    };
    public void AddApplySelectionPatternListener() {
        //リスナに追加する
        IDiagramViewManager diagramViewManager = astahAPIUtils.getDiagramViewManager();
        diagramViewManager.addEntitySelectionListener(applySelectionPatternListener);
    }

    public void RemoveApplySelectionPatternListener() {
        //リスナを削除する
        IDiagramViewManager diagramViewManager = astahAPIUtils.getDiagramViewManager();
        diagramViewManager.removeEntitySelectionListener(applySelectionPatternListener);
    }

    //Listenerを単体でremoveするとastahのマルチスレッドでエラーが出るため、一時的なリスナを追加する
    private final IEntitySelectionListener tmpListener = new IEntitySelectionListener() {
        @Override
        public void entitySelectionChanged(IEntitySelectionEvent iEntitySelectionEvent) {
            System.out.println("tmpListener");
            astahUtils.GetCurrentTime();//現在時刻表示

        }
    };
    public void AddTmpListener() {
        //リスナに追加する
        IDiagramViewManager diagramViewManager = astahAPIUtils.getDiagramViewManager();
        diagramViewManager.addEntitySelectionListener(tmpListener);
    }

    public void RemoveTmpListener() {
        //リスナを削除する
        IDiagramViewManager diagramViewManager = astahAPIUtils.getDiagramViewManager();
        diagramViewManager.removeEntitySelectionListener(tmpListener);
    }








    //今は一度だけに限定する
    boolean isMatched = false;
    boolean isApplied = false;
    boolean isTmpListener = false;
    //リスナに追加する

    /*
    public void Match() {

       //今はテストで一度だけに限定
        if(isMatched){
            return;
        }

        List<IElement> selectedPresentations = astahUtils.getModelsOfSelectedPresentations(astahAPIUtils.getViewManager());
        //選択されていなければreturn
        if(selectedPresentations.isEmpty()){
            //System.out.println("No selected presentation");
            return;
        }

        IElement selectedPresentation = selectedPresentations.get(0);//0とする


        //IArgumentAssetにキャストできない時はreturn
        if(!(selectedPresentation instanceof IArgumentAsset)){
            //System.out.println("selectedPresentation is not IArgumentAsset");
            return;
        }


        //sentenceの取得
        String selectedSentence = ((IArgumentAsset)selectedPresentation).getContent();

        String selectedSentenceLower = selectedSentence.toLowerCase();

        List<Integer> matchedPatternIndex = new ArrayList<Integer>();
        for(int i = 0; i < patternConfigManager.patternNames.length; i++){
            //型のタイプのマッチング
            //現状はGoalの場合のみをマッチングする
            if(!(selectedPresentation instanceof IGoal)){
                //System.out.println("selectedPresentation is not IGoal");
                continue;
            }

            //sentenceのマッチング
            boolean isMatched = true;
            for(int j = 0; j < patternConfigManager.recommendationParameter[i].length; j++){
                //selectedSentenceLowerに対して、patternConfigManager.recommendationParameter[i][j]が含まれているかどうか
                if(!selectedSentenceLower.contains(patternConfigManager.recommendationParameter[i][j])){
                    isMatched = false;
                }
            }

            if(isMatched){
                matchedPatternIndex.add(i);
            }
        }

        //matchしたpatternについての処理
        if(matchedPatternIndex.isEmpty()){
            System.out.println("No matched pattern");
        }else{
            System.out.println("Matched pattern");
            for(int i = 0; i < matchedPatternIndex.size(); i++){
                System.out.println(patternConfigManager.patternNames[matchedPatternIndex.get(i)]);
            }

            //Recommendationの表示
            //今は一番上の部分のみをrecommendする
            //また、複数のパターンとマッチする場合も一番上のパターンのみにする
            String patternName = patternConfigManager.patternNames[matchedPatternIndex.get(0)];
            String[] inputPatternParameterNames = new String[patternConfigManager.patternParameterExplanationNames[matchedPatternIndex.get(0)].length];
            for(int i = 0; i < inputPatternParameterNames.length; i++){
                inputPatternParameterNames[i] = patternConfigManager.patternParameterExplanationNames[matchedPatternIndex.get(0)][i];
            }
            //今は1番目のみ
            inputPatternParameterNames[1] = ((IArgumentAsset) selectedPresentation).getName();
            System.out.println("inputPatternParameterNames[1] = " + inputPatternParameterNames[1]);
            int repeatN = 0;
            if(patternConfigManager.isRepeat[matchedPatternIndex.get(0)]){
                repeatN = 1;
            }
            String supportedElementString = "";
            String[][] solutionParameterValue = new String[patternConfigManager.solutionParameter[matchedPatternIndex.get(0)].length][];
            for(int i = 0; i < solutionParameterValue.length; i++){
                solutionParameterValue[i] = new String[patternConfigManager.solutionParameter[matchedPatternIndex.get(0)][i].length];
                Arrays.fill(solutionParameterValue[i], "");
            }
            transformationManager.ApplyPattern(patternName, inputPatternParameterNames, repeatN, supportedElementString, solutionParameterValue);

            isMatched = true;
        }
    }*/

    public void OclMatch(){
        //今はテストで一度だけに限定
        if(isMatched){
            return;
        }

        //OCLを使わないでLLMのみでパターンマッチングする場合(possibilityもスキップしている)
        if(selectionSupportDataBase.isStopOCL){
            System.out.println("isLLMMatchを利用する");
            if(selectionSupportDataBase.isLLMMatch){
                int patternN = selectionSupportDataBase.llmMatchedPatternIndex;
                System.out.println("LLMMatchを利用");

                selectionSupportDataBase.createdAllIElements = new ArrayList<ArrayList<ArrayList<IElement>>>();
                for(int i = 0; i < patternConfigManager.patternNames.length; i++){
                    selectionSupportDataBase.createdAllIElements.add(new ArrayList<ArrayList<IElement>>());
                }

                //最初の色などを取得する
                IPresentation[] iNodePresentations = null;
                try {
                    //IDiagram currentDiagram = astahAPIUtils.getDiagram();
                    IDiagram currentDiagram = astahAPIUtils.getSameNameDiagram(LLMPatternSearchTab.GetSafetyCaseNameText());//注意：もし指定している図がみつからない場合は、現在開いている図を取得
                    //現在の図がnullの場合はエラーを出力して終了
                    if(currentDiagram == null){
                        System.out.println("Error: currentDiagram is null");
                        return;
                    }


                    iNodePresentations = astahUtils.getOwnedINodePresentation(currentDiagram);//現在開いている図のプレゼンテーションを取得
                }catch (Exception e){
                    e.printStackTrace();
                }

                if(iNodePresentations == null){
                    System.out.println("Error: iPresentations is null");
                    return;
                }

                //以上がnull処理などのエラー処理
                selectionSupportDataBase.SetINodePresentation(iNodePresentations);

                LLMMatch(patternN);
            }
            isMatched = true;
            ListenerChangeToApplySelectionPattern();
            return;
        }

        IPresentation[] selectedPresentationsNote = astahUtils.getSelectedPresentations(astahAPIUtils.getViewManager());
        //選択されていなければreturn
        if(selectedPresentationsNote.length == 0){
            //System.out.println("No selected presentation");
            return;
        }

        //IElement selectedPresentationIElementNote = selectedPresentations.get(0);//0とする

        if(!(selectedPresentationsNote[0] instanceof INodePresentation)){
            System.out.println("selectedPresentation is not INodePresentation");
            return;
        }

        INodePresentation note = (INodePresentation) selectedPresentationsNote[0];
        int patternN = -1;
        //緑の看板の元のノードとパターンの番号を取得
        for(int i = 0; i < patternConfigManager.patternNames.length; i++){
            if(note.getLabel().equals("Possibility\n" + patternConfigManager.patternNames[i])){
                patternN = i;
                break;
            }
        }
        if(patternN == -1){
            return;
        }
        IElement selectedPresentationIElement = (IElement) note.getLinks()[0].getTarget().getModel();
        System.out.println("selectedPresentationIElement = " + selectedPresentationIElement.getId());


        //複数パターンについてのマッチ
        ArrayList<ArrayList<ArrayList<IElement>>> matchedAllIElements = new ArrayList<ArrayList<ArrayList<IElement>>>();
        List<Integer> matchedPatternIndex = new ArrayList<Integer>();
        for(int i = 0; i < patternConfigManager.patternNames.length; i++){
            ArrayList<ArrayList<IElement>> matchedIElements = new ArrayList<ArrayList<IElement>>();
            matchedAllIElements.add(matchedIElements);
        }

        //1つのパターンについてのマッチ
        ArrayList<ArrayList<IElement>> matchedIElements = new ArrayList<ArrayList<IElement>>();

        //Graph pattern matching
//        try{
//
//            //IOCLContext selectedContext = (IOCLContext) selectedPresentationIElement;
//
//            IElement parentIElement = selectedPresentationIElement;
//            int counter = 0;
//
//            //子ノードを取得するためのノードの隣接リストを作成
//            //ArrayList<ArrayList<IElement>> SupportedByAdjacencyList = astahUtils.GetSupportedByAdjacencyList();
//
//            for(int i = 0; i < patternConfigManager.patternNames.length; i++){
//                for(int j = 0; ; j++){//int j = 0; j < patternConfigManager.oclInv[i].length; j++){
//                    if(j == 0){
//                        matchedIElements = new ArrayList<ArrayList<IElement>>();
//                        //3次元配列にもaddする
//                        matchedAllIElements.add(matchedIElements);
//
//
//                        counter = 0;
//
//                        //if((boolean)selectedContext.evaluateOCL(patternConfigManager.oclInv[i][j])){
//                        if(oclValidator.Validate(selectedPresentationIElement, patternConfigManager.oclInv[i][j])){//note.getLinks()[0].getTarget()はselectされたpresentation
//                            ArrayList<IElement> tmpList = new ArrayList<IElement>();
//                            tmpList.add(selectedPresentationIElement);
//                            matchedIElements.add(tmpList);
//
//                            matchedPatternIndex.add(i);
//                            continue;
//                        }else{
//                            ArrayList<IElement> tmpList = new ArrayList<IElement>();
//                            tmpList.add(null);
//                            matchedIElements.add(tmpList);
//                            break;
//                        }
//                    }
//
//
//
//                    parentIElement = matchedIElements.get(counter).get(matchedIElements.get(counter).size()-1);//リストの一番後ろの要素を取得
//
//
//                    //parentの下にある要素を全て取得する
//                    List<IElement> childIElements = astahUtils.getSupportedByTarget((IArgumentAsset) parentIElement);//, SupportedByAdjacencyList);
//
//                    if(childIElements.isEmpty()){
//                        counter++;
//
//                        if(counter == matchedIElements.size()){
//                            break;
//                        }else{
//                            continue;
//                        }
//                    }
//
//                    boolean isChildMatched = false;
//                    for(int k = 0; k < childIElements.size(); k++){
//                        //IOCLContext childContext = (IOCLContext) childIElements.get(k);
//
//                        //if((boolean)childContext.evaluateOCL(patternConfigManager.oclInv[i][matchedIElements.get(counter).size()])){
//                        if(oclValidator.Validate(childIElements.get(k), patternConfigManager.oclInv[i][matchedIElements.get(counter).size()])){
//                            ArrayList<IElement> copyList = new ArrayList<IElement>(matchedIElements.get(counter));
//                            copyList.add(childIElements.get(k));
//                            matchedIElements.add(copyList);
//                            isChildMatched = true;
//                        }
//                    }
//
//                    counter++;
//                    /*
//
//                    if(isChildMatched){
//                        continue;
//                    }else{
//                        break;
//                    }
//
//                     */
//                    if(counter == matchedIElements.size()){
//                        break;
//                    }
//                }
//            }
//


            /*
            IOCLContext selectedContext = (IOCLContext) selectedPresentationIElement;
            //System.out.println(selectedContext.evaluateOCL("self.identifier"));
            //model smoke testing pattern
            //System.out.println(selectedContext.evaluateOCL("self.content.toLowerCase().matches('(?=.*model)(?=.*test).*') and self.oclIsKindOf(Goal)"));

            //System.out.println(selectedContext.evaluateOCL("self.content.toLowerCase().matches('.*ee.*')"));
            //System.out.println(selectedContext.evaluateOCL("self.content.toLowerCase().matches('(?=.*model)(?=.*test).*')"));
            //System.out.println(selectedContext.evaluateOCL("self.oclIsKindOf(Goal)"));
            //System.out.println(selectedContext.evaluateOCL("self.oclIsKindOf(ArgumentAsset)"));


            IElement parentIElement = selectedPresentationIElement;

            for(int i = 0; i < patternConfigManager.patternNames.length; i++){
                if(!matchedIElements.isEmpty()){
                    break;
                }
                for(int j = 0; j < patternConfigManager.oclInv[i].length; j++){
                    if(j == 0){
                        if((boolean)selectedContext.evaluateOCL(patternConfigManager.oclInv[i][j])){
                            parentIElement = selectedPresentationIElement;
                            matchedIElements.add(selectedPresentationIElement);
                            //matchedIElements.get(i).add(new ArrayList<IElement>(Arrays.asList(null,selectedPresentationIElement)));

                            matchedPatternIndex.add(i);
                            continue;
                        }else{
                            break;
                        }
                    }

                    //parentの下にある要素を全て取得する
                    List<IElement> childIElements = astahUtils.getSupportedByTarget((IArgumentAsset) parentIElement);

                    boolean isChildMatched = false;
                    for(int k = 0; k < childIElements.size(); k++){
                        IOCLContext childContext = (IOCLContext) childIElements.get(k);
                        if((boolean)childContext.evaluateOCL(patternConfigManager.oclInv[i][j])){
                            matchedIElements.add(childIElements.get(k));
                            isChildMatched = true;
                            break;
                        }
                    }

                    if(isChildMatched){
                        continue;
                    }else{
                        break;
                    }
                }
            }

             */
//        }catch(Exception e){
//            e.printStackTrace();
//        }

        //Graph pattern matching
        GraphPatternMatchingController graphPatternMatchingController = new GraphPatternMatchingController();
        matchedIElements = graphPatternMatchingController.GraphPatternMatching(patternN, selectedPresentationIElement);
        matchedAllIElements.set(patternN, matchedIElements);

        //matchしたpatternについてのview処理
        if(false){//matchedPatternIndex.isEmpty()){
            //緑の看板が押されているため必ずマッチはしている。将来の拡張のためにコードは残す
            System.out.println("No matched pattern");
        }else{
            System.out.println("Matched pattern");

            selectionSupportDataBase.matchedAllIElements = matchedAllIElements;
            selectionSupportDataBase.createdAllIElements = new ArrayList<ArrayList<ArrayList<IElement>>>();
            for(int i = 0; i < patternConfigManager.patternNames.length; i++){
                selectionSupportDataBase.createdAllIElements.add(new ArrayList<ArrayList<IElement>>());
            }

            SelectionSupportDeleteViewOperation();

            LLMPatternMatchingController llmPatternMatchingController = new LLMPatternMatchingController();
            llmPatternMatchingController.SetGSNPrompt();//OCLのノード作成の前にダイアグラムを書き出しておかないと、OCLで作ったノードともマッチングが行われる

            for(int i = 0; i < 1/*matchedAllIElements.size()*/; i++){
                //System.out.println("matchedAllIElements["+i+"]");
                i = patternN;//今回必要なパターンのみに限定
                for(int j = 0; j < matchedAllIElements.get(i).size(); j++){
                    //System.out.println("matchedAllIElements["+i+"]["+j+"]");

                    //色分け機能のために、ここで色を決める
                    String selectionColor = DefineSelectionColor(j);


                    //System.out.println(((IArgumentAsset)matchedAllIElements.get(i).get(j).get(0)).getContent());
                    //ApplyFunctionの呼び出し
                    //Recommendationの表示
                    ShowRecommendationView(i, matchedAllIElements.get(i).get(j), true,selectionColor, false);



                }
            }





            if(selectionSupportDataBase.isLLMMatch){
                LLMMatch(patternN);
            }
            isMatched = true;
            ListenerChangeToApplySelectionPattern();

        }
    }

    public void SelectionSupportDeleteViewOperation(){
        //トランザクション処理
        ProjectAccessor projectAccessor;
        ITransactionManager transactionManager = null;
        AstahAPIUtils astahAPIUtils = new AstahAPIUtils();
        IFacet iFacet = null;

        AstahTransactionProcessing astahTransactionProcessing = new AstahTransactionProcessing();
        AstahUtils astahUtils = new AstahUtils();

        try {

            projectAccessor = astahAPIUtils.getProjectAccessor();
            IModel iCurrentProject = projectAccessor.getProject();
            iFacet = astahAPIUtils.getGSNFacet(projectAccessor);

            transactionManager = projectAccessor.getTransactionManager();

            //IDiagram currentDiagram = astahAPIUtils.getDiagram();
            IDiagram currentDiagram = astahAPIUtils.getSameNameDiagram(LLMPatternSearchTab.GetSafetyCaseNameText());//注意：もし指定している図がみつからない場合は、現在開いている図を取得

            //現在の図がnullの場合はエラーを出力して終了
            if (currentDiagram == null) {
                System.out.println("Error: currentDiagram is null");
                return;
            }

            transactionManager.beginTransaction();
            IDiagramEditorFactory diagramEditorFactory = projectAccessor.getDiagramEditorFactory();
            GsnDiagramEditor gsnDiagramEditor = diagramEditorFactory.getDiagramEditor(GsnDiagramEditor.class);
            GsnModelEditor gsnModelEditor = projectAccessor.getModelEditorFactory().getModelEditor(GsnModelEditor.class);
            gsnDiagramEditor.setDiagram(currentDiagram);

            selectionSupportDataBase.UndoColorClearedPresentations(gsnModelEditor);





            transactionManager.endTransaction();
        } catch (BadTransactionException e) {

            transactionManager.abortTransaction();

            // 処理（省略）

        } catch (Exception e) {
            System.out.println("Error: " + e);
            e.printStackTrace();
        }


        return;
    }

    private void ShowRecommendationView(int matchedPatternIndex, ArrayList<IElement> matchedIElements, boolean isSelectionSupport, String selectionColor, boolean isValueSupport){
        //RecommendationViewの表示
        String patternName = patternConfigManager.patternNames[matchedPatternIndex];
        String[] inputPatternParameterNames = new String[patternConfigManager.patternParameterExplanationNames[matchedPatternIndex].length];
        for(int i = 0; i < inputPatternParameterNames.length; i++){
            inputPatternParameterNames[i] = patternConfigManager.patternParameterExplanationNames[matchedPatternIndex][i];
        }

        for(int i = 0; i < matchedIElements.size(); i++){
            //nullの場合はcontinue
            if(matchedIElements.get(i) == null){
                continue;
            }
            inputPatternParameterNames[i] = ((IArgumentAsset) matchedIElements.get(i)).getName();
            System.out.println("inputPatternParameterNames["+(i)+"] = " + inputPatternParameterNames[i]);
        }

        int repeatN = 0;
        if(patternConfigManager.isRepeat[matchedPatternIndex]){
            repeatN = 1;
        }
        String supportedElementString = "";
        String[][] solutionParameterValue = new String[patternConfigManager.solutionParameter[matchedPatternIndex].length][];
        for(int i = 0; i < solutionParameterValue.length; i++){
            solutionParameterValue[i] = new String[patternConfigManager.solutionParameter[matchedPatternIndex][i].length];
            Arrays.fill(solutionParameterValue[i], "");
        }

        SelectionSupportDataBase argSelectionSupportDataBase = selectionSupportDataBase;
        if(!isSelectionSupport){
            argSelectionSupportDataBase = null;
        }
        transformationManager.ApplyPattern(patternName, inputPatternParameterNames, repeatN, supportedElementString, solutionParameterValue, isSelectionSupport, argSelectionSupportDataBase, selectionColor, isValueSupport);

    }

    public void ListenerChangeToApplySelectionPattern(){
        if(isTmpListener){
            RemoveTmpListener();
            isTmpListener = false;
        }
        RemoveRecommendationListener();
        AddApplySelectionPatternListener();
    }

    public void ApplySelectionPattern(){

        System.out.println("ApplySelectionPattern");
        List<IElement> selectedPresentations = astahUtils.getModelsOfSelectedPresentations(astahAPIUtils.getViewManager());
        //選択されていなければreturn
        if(selectedPresentations.isEmpty()){
            //System.out.println("No selected presentation");
            return;
        }

        IElement selectedPresentationIElement = selectedPresentations.get(0);//0とする

        //IArgumentAssetにキャストできない時はreturn
        if(!(selectedPresentationIElement instanceof IArgumentAsset)){
            //System.out.println("selectedPresentation is not IArgumentAsset");
            return;
        }

        //LLMでつくったものであるならば(色が赤の場合)
        try {
            if(Objects.equals((((IArgumentAsset)selectedPresentations.get(0)).getPresentations()[0].getProperty("fill.color")), "#FF0000")){
                LLMPatternApply();
                return;
            }
        }catch (Exception e){
            e.printStackTrace();
        }


        //選択されているプレゼンテーションがselectionで新規作成されたものであれば
        for(int i = 0; i < selectionSupportDataBase.createdAllIElements.size(); i++){
            System.out.println("matchedAllIElements["+i+"]");
            for(int j = 0; j < selectionSupportDataBase.createdAllIElements.get(i).size(); j++){
                System.out.println("matchedAllIElements["+i+"]["+j+"]");
                if(selectionSupportDataBase.createdAllIElements.get(i).get(j).get(0) == null){
                    System.out.println("null");
                }else{
                    for(int k = 0; k < selectionSupportDataBase.createdAllIElements.get(i).get(j).size(); k++){
                        if(selectionSupportDataBase.createdAllIElements.get(i).get(j).get(k) == null){
                            System.out.println("null");
                        }else{
                            //System.out.println(((IArgumentAsset)selectionSupportDataBase.matchedAllIElements.get(i).get(j).get(k)).getContent());
                            if(selectedPresentationIElement.equals(selectionSupportDataBase.createdAllIElements.get(i).get(j).get(k))){
                                System.out.println("selectedPresentationIElement is matched:"+ ((IArgumentAsset) selectedPresentationIElement).getName());
                                //削除
                                SelectionSupportDeleteViewOperation();

                                //選択された部分を正式にApplyする
                                ShowRecommendationView(i, selectionSupportDataBase.matchedAllIElements.get(i).get(j), false, "", selectionSupportDataBase.isLLMMatch);
                                RemoveApplySelectionPatternListenerTmp();
                                //return;
                            }
                            //System.out.println("createdAllIElements["+i+"]["+j+"]["+k+"] = " + ((IArgumentAsset)selectionSupportDataBase.createdAllIElements.get(i).get(j).get(k)).getContent());
                        }
                    }
                }
            }
        }


        //RemoveApplySelectionPatternListener();

    }

    public void RemoveApplySelectionPatternListenerTmp(){
        RemoveApplySelectionPatternListener();
        //Astahのマルチスレッドにより単体でRemoveするとエラーが出るため、TmpListenerを追加
        AddTmpListener();
        isTmpListener = true;
    }

    public String DefineSelectionColor(int number){
        //10パターンまで対応。その後はデフォルト値
        if(number == 0) {
            return "#f0f8ff";
        }else if(number == 1) {
            return "#87cefa";
        }else if(number == 2) {
            return "#6495ed";
        }else if (number == 3) {
            return "#ffa07a";
        }else if (number == 4) {
            return "#ffc0cb";
        }else if (number == 5) {
            return "#3cb371";
        }else if (number == 6) {
            return "#adff2f";
        }else if (number == 7) {
            return "#bdb76b";
        }else if (number == 8) {
            return "#ffa500";
        }else if (number == 9) {
            return "#da70d6";
        }
        return constClass.SELECTION_DEFAULT_COLOR;
    }

    public void LLMMatch(int patternN){
        //以下はOCLMatchと同じ
        //今はテストで一度だけに限定
        if(isMatched){
            return;
        }

        IPresentation[] selectedPresentationsNote = astahUtils.getSelectedPresentations(astahAPIUtils.getViewManager());
        INodePresentation note = null;
        //選択されていなければreturn
        if(selectedPresentationsNote.length != 0){
            if((selectedPresentationsNote[0] instanceof INodePresentation)){
                System.out.println("selectedPresentation is not INodePresentation");
                note = (INodePresentation) selectedPresentationsNote[0];
            }
        }

//        int patternN = -1;
//        //緑の看板の元のノードとパターンの番号を取得
//        for(int i = 0; i < patternConfigManager.patternNames.length; i++){
//            if(note.getLabel().equals("Possibility\n" + patternConfigManager.patternNames[i])){
//                patternN = i;
//                break;
//            }
//        }
        if(patternN == -1){
            return;
        }
//        IElement selectedPresentationIElement = (IElement) note.getLinks()[0].getTarget().getModel();
//        System.out.println("selectedPresentationIElement = " + selectedPresentationIElement.getId());


        //ここまではOCLMatchと同じ

        //ここからLLMMatch
        LLMPatternMatchingController llmPatternMatchingController = new LLMPatternMatchingController();
        String[] llmMatchedElement = llmPatternMatchingController.GetLLMMatchingResponse(patternN);


        //ShowRecommendの関数を参照してつくっている
        String patternName = patternConfigManager.patternNames[patternN];
//        String[] inputPatternParameterNames = new String[patternConfigManager.patternParameterExplanationNames[patternN].length];
//        for(int i = 0; i < inputPatternParameterNames.length; i++){
//            inputPatternParameterNames[i] = patternConfigManager.patternParameterExplanationNames[patternN][i];
//        }
//
//        for(int i = 0; i < matchedIElements.size(); i++){
//            inputPatternParameterNames[i+1] = ((IArgumentAsset) matchedIElements.get(i)).getName();
//            System.out.println("inputPatternParameterNames["+(i+1)+"] = " + inputPatternParameterNames[i+1]);
//        }

        int repeatN = 0;
        if(patternConfigManager.isRepeat[patternN]){
            repeatN = 1;
        }
        String supportedElementString = "";
        String[][] solutionParameterValue = new String[patternConfigManager.solutionParameter[patternN].length][];
        for(int i = 0; i < solutionParameterValue.length; i++){
            solutionParameterValue[i] = new String[patternConfigManager.solutionParameter[patternN][i].length];
            Arrays.fill(solutionParameterValue[i], "");
        }

        SelectionSupportDataBase argSelectionSupportDataBase = selectionSupportDataBase;
        boolean isSelectionSupport = true;
        if(!isSelectionSupport){
            argSelectionSupportDataBase = null;
        }

        String selectionColor = "#ff0000";//red
        boolean isValueSupport = false;

        selectionSupportDataBase.llmMatchedPatternIndex = patternN;
        selectionSupportDataBase.llmMatchedElementStrings = llmMatchedElement;

        //何もマッチしていない場合の対応
        //llmMatchedElementの配列の中身の要素が全て""である場合をfor文で確かめる
        boolean isAllEmpty = true;
        for(String element : llmMatchedElement){
            if(!element.equals("")){
                isAllEmpty = false;
                break;
            }
        }

        if(isAllEmpty){
            System.out.println("LLM did not match");
            //noteと同じ場所にLLM did not matchのノートを作る
            CreateNoteNoMatchedLLM(note);
        }else{
            transformationManager.ApplyPattern(patternName, llmMatchedElement, repeatN, supportedElementString, solutionParameterValue, isSelectionSupport, argSelectionSupportDataBase, selectionColor, isValueSupport);
        }




        //ShowRecommendationView(patternN, llmMatchedElement, true,"#ff0000", false);
    }

    public void LLMPatternApply(){
        int patternN = selectionSupportDataBase.llmMatchedPatternIndex;
        String[] llmMatchedElement = selectionSupportDataBase.llmMatchedElementStrings;

        if(patternN == -1){
            return;
        }
        String patternName = patternConfigManager.patternNames[patternN];

        int repeatN = 0;
        if(patternConfigManager.isRepeat[patternN]){
            repeatN = 1;
        }
        String supportedElementString = "";
        String[][] solutionParameterValue = new String[patternConfigManager.solutionParameter[patternN].length][];
        for(int i = 0; i < solutionParameterValue.length; i++){
            solutionParameterValue[i] = new String[patternConfigManager.solutionParameter[patternN][i].length];
            Arrays.fill(solutionParameterValue[i], "");
        }

        SelectionSupportDataBase argSelectionSupportDataBase = selectionSupportDataBase;
        boolean isSelectionSupport = false;
        if(!isSelectionSupport){
            argSelectionSupportDataBase = null;
        }

        String selectionColor = "";//なし
        boolean isValueSupport = selectionSupportDataBase.isLLMMatch;

        //削除
        SelectionSupportDeleteViewOperation();
        transformationManager.ApplyPattern(patternName, llmMatchedElement, repeatN, supportedElementString, solutionParameterValue, isSelectionSupport, argSelectionSupportDataBase, selectionColor, isValueSupport);

        RemoveApplySelectionPatternListenerTmp();
    }

    private void CreateNoteNoMatchedLLM(INodePresentation possibilityNote){
        //トランザクション処理
        ProjectAccessor projectAccessor;
        ITransactionManager transactionManager = null;
        AstahAPIUtils astahAPIUtils = new AstahAPIUtils();
        IFacet iFacet = null;

        AstahTransactionProcessing astahTransactionProcessing = new AstahTransactionProcessing();
        AstahUtils astahUtils = new AstahUtils();

        try {

            projectAccessor = astahAPIUtils.getProjectAccessor();
            IModel iCurrentProject = projectAccessor.getProject();
            iFacet = astahAPIUtils.getGSNFacet(projectAccessor);

            transactionManager = projectAccessor.getTransactionManager();

            //IDiagram currentDiagram = astahAPIUtils.getDiagram();
            IDiagram currentDiagram = astahAPIUtils.getSameNameDiagram(LLMPatternSearchTab.GetSafetyCaseNameText());//注意：もし指定している図がみつからない場合は、現在開いている図を取得

            //現在の図がnullの場合はエラーを出力して終了
            if (currentDiagram == null) {
                System.out.println("Error: currentDiagram is null");
                return;
            }

            transactionManager.beginTransaction();
            IDiagramEditorFactory diagramEditorFactory = projectAccessor.getDiagramEditorFactory();
            GsnDiagramEditor gsnDiagramEditor = diagramEditorFactory.getDiagramEditor(GsnDiagramEditor.class);
            gsnDiagramEditor.setDiagram(currentDiagram);

            //default値
            int positionX = 100;
            int positionY = 100;
            if(possibilityNote != null){
                positionX = (int)(possibilityNote.getLocation().getX());
                positionY = (int)(possibilityNote.getLocation().getY());
            }

            INodePresentation note = gsnDiagramEditor.createNote("LLM did not match", new Point(positionX, positionY));
            selectionSupportDataBase.clearedPresentations.add(note);

            note.setProperty("fill.color", "#ff0000");//赤色にする


            transactionManager.endTransaction();
        } catch (BadTransactionException e) {

            transactionManager.abortTransaction();

            // 処理（省略）

        } catch (Exception e) {
            System.out.println("Error: " + e);
            e.printStackTrace();
        }

        return;
    }
}