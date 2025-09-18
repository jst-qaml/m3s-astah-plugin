package ai.engineering.patternApplication.internal.extraTab;

/*
 * パッケージ名は、生成したプラグインのパッケージ名よりも
 * 下に移してください。
 * プラグインのパッケージ名=> com.example
 *   com.change_vision.astah.extension.plugin => X
 *   com.example                            　　　　　　　  => O
 *   com.example.internal                    　　　　　 => O
 *   learning                                　　　　　　　　 => X
 */


import ai.engineering.patternApplication.internal.utility.*;
import com.change_vision.jude.api.gsn.model.*;
import com.change_vision.jude.api.inf.model.*;
import com.change_vision.jude.api.inf.presentation.*;
import com.change_vision.jude.api.inf.project.*;
import com.change_vision.jude.api.inf.ui.IPluginExtraTabView;
import com.change_vision.jude.api.inf.ui.ISelectionListener;
import ai.engineering.patternApplication.internal.*;
import ai.engineering.patternApplication.internal.entity.*;
import ai.engineering.patternApplication.internal.LLMPatternSearchController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class LLMPatternSearchTab extends JPanel
        implements IPluginExtraTabView, ProjectEventListener {
    private PatternConfigManager patternConfigManager = new PatternConfigManager();
    Const constClass = new Const();

    private static JTextArea kaosInput = new JTextArea(1,20);
    private static JTextArea safetyCaseInput = new JTextArea(1,20);

    //この値によって文字の表示調整
    private int fontStyle = constClass.fontStyle;
    private int fontSize = constClass.fontSize;

    private JLabel outputLabel; // 出力用のラベル
    // コンストラクタでUIのセットアップ

    private boolean[] recommendedPatterns = new boolean[patternConfigManager.patternNames.length];

    private int patternN = -1;
    public LLMPatternSearchTab() {
        // パネルとレイアウト設定
        JPanel panel = new JPanel();
        GridBagLayout innerLayout = new GridBagLayout();
        panel.setLayout(innerLayout);
        GridBagConstraints gbc = new GridBagConstraints();

        // Kaos diagram
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel contextLabel = new JLabel("KAOS Goal Model");
        contextLabel.setFont(new Font("Arial", fontStyle, fontSize));
        panel.add(contextLabel, gbc);

        // Kaos diagram入力フィールド
        //gbc.gridy = 1;
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        kaosInput.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        kaosInput.setFont(new Font("Arial", fontStyle, fontSize));
        panel.add(kaosInput, gbc);

        // Safety case
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel problemLabel = new JLabel("Safety Case ");
        problemLabel.setFont(new Font("Arial", fontStyle, fontSize));
        panel.add(problemLabel, gbc);

        // Safety case入力フィールド
        //gbc.gridy = 3;
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        safetyCaseInput.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        safetyCaseInput.setFont(new Font("Arial", fontStyle, fontSize));
        panel.add(safetyCaseInput, gbc);

        // 送信ボタン
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JButton sendButton = new JButton("Search for applicable patterns");
        sendButton.setFont(new Font("Arial", fontStyle, fontSize));
        panel.add(sendButton, gbc);

        // 出力用ラベルを追加（初期状態は空）
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        outputLabel = new JLabel("");
        outputLabel.setFont(new Font("Arial", fontStyle, fontSize));
        panel.add(outputLabel, gbc);

        // pattern applyボタン
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JButton applyButton = new JButton("apply recommended pattern");
        applyButton.setFont(new Font("Arial", fontStyle, fontSize));
        panel.add(applyButton, gbc);
        applyButton.setVisible(false);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //String contextText = contextInput.getText();
                //String problemText = problemInput.getText();
                LLMPatternSearchController llmPatternSearchController = new LLMPatternSearchController();
                String response = llmPatternSearchController.GenerateGPTResponse();
                patternN = Integer.parseInt(response);

                //recommendedPatternsの初期化
                for(int i = 0; i < recommendedPatterns.length; i++){
                    if(i == patternN){
                        recommendedPatterns[i] = true;
                    }else{
                        recommendedPatterns[i] = false;
                    }
                }


                if (patternN == -1) {
                    outputLabel.setText("No pattern found");
                    applyButton.setVisible(false);
                }else{
                    outputLabel.setText("Recommended Pattern: " + patternConfigManager.patternNames[patternN]);
                    applyButton.setVisible(true);
                }
                //例外処理

                //パターン推薦のStep1を飛ばして、Step2に直接遷移する
                //そのパターンを推薦できるかチェックする
                //そのパターンが推薦できる場合、そのパターンを推薦する
                //そのパターンが推薦できない場合、returnする

//                SelectionSupportDataBase selectionSupportDataBase = new SelectionSupportDataBase();
//
//                PatternMatchingController patternMatchingController = new PatternMatchingController();
//                patternMatchingController.selectionSupportDataBase = selectionSupportDataBase;
//                patternMatchingController.AddRecommendationListener();


                // 出力ラベルにテキストを表示
                //outputLabel.setText("<html>Context: " + contextText + "<br>Problem: " + problemText + "</html>");

            }
        });

        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //recommendのfunctionを呼び出す
                PatternRecommendTab patternRecommendTab = new PatternRecommendTab();
                patternRecommendTab.PatternSelectionSupportMain(recommendedPatterns, true, true, patternN);

            }
        });

        add(panel);
    }

    public static String GetKaosDiagramNameText(){
        return kaosInput.getText();
    }

    public static String GetSafetyCaseNameText(){
        return safetyCaseInput.getText();
    }

    @Override
    public void projectChanged(ProjectEvent e) {
    }

    @Override
    public void projectClosed(ProjectEvent e) {
    }

    @Override
    public void projectOpened(ProjectEvent e) {
    }

    @Override
    public void addSelectionListener(ISelectionListener listener) {
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getTitle() {
        return "LLM pattern search View";
    }

    public void activated() {
    }

    public void deactivated() {
    }

}