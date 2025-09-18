package ai.engineering.patternApplication.internal;

import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;

import java.time.*;
import java.util.ArrayList;


public class GPTController {
    private OpenAiService service;
    private ArrayList<ChatMessage> messages;


    // コンストラクタでOpenAIサービスの初期化
    public GPTController() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        this.service = new OpenAiService(apiKey, Duration.ofMinutes(2));// タイムアウトを2分に設定
        this.messages = new ArrayList<>();
        this.messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), "You are a helpful assistant."));
    }

    // 事前定義された質問を受け取ってGPTと会話する
    public String chat(String[] userInputs) {
        this.messages.clear();
        for (String userInput : userInputs) {
            System.out.println("User: " + userInput);
//            // byeで終了
//            if (userInput.equalsIgnoreCase("bye")) {
//                System.out.println("Assistant: Goodbye!");
//                break;
//            }
//
//            // resetで履歴リセット
//            if (userInput.equalsIgnoreCase("reset")) {
//                this.messages.clear();
//                this.messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), "You are a helpful assistant."));
//                System.out.println("Assistant: Conversation reset.");
//                continue;
//            }

            // ユーザーのメッセージを履歴に追加
            this.messages.add(new ChatMessage(ChatMessageRole.USER.value(), userInput));

            // ChatGPT APIリクエストを作成
            ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                    .model("gpt-4.1")
                    .messages(this.messages)
                    .maxTokens(1000) // 必要に応じてトークン数を調整
                    .build();

            // GPTからのレスポンスを取得して表示
            String response = service.createChatCompletion(chatRequest)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
            System.out.println("Assistant: " + response);


            // GPTの返答を履歴に追加
            this.messages.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), response));
        }
        // 最後のメッセージを取得して返す
        return this.messages.get(this.messages.size() - 1).getContent();
    }


    //test用のchatgptのコード
//    public void TestGPT() {
//        String apiKey = System.getenv("OPENAI_API_KEY");
//
//        OpenAiService service = new OpenAiService(apiKey);
//        ArrayList<ChatMessage> messages = new ArrayList<>();
//        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), "You are a helpful assistant."));
//        messages.add(new ChatMessage(ChatMessageRole.USER.value(), "Tell me a joke."));
//
//        ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
//                .model("gpt-4o")
//                .messages(messages)
//                .maxTokens(50)
//                .build();
//
//        String response = service.createChatCompletion(chatRequest).getChoices().get(0).getMessage().getContent();
//        System.out.println(response);
//    }
}

