import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import com.oracle.bmc.generativeaiinference.model.BaseChatResponse;
import com.oracle.bmc.generativeaiinference.model.ChatChoice;
import com.oracle.bmc.generativeaiinference.model.ChatContent;
import com.oracle.bmc.generativeaiinference.model.ChatDetails;
import com.oracle.bmc.generativeaiinference.model.CohereChatResponse;
import com.oracle.bmc.generativeaiinference.model.GenericChatRequest;
import com.oracle.bmc.generativeaiinference.model.GenericChatResponse;
import com.oracle.bmc.generativeaiinference.model.Message;
import com.oracle.bmc.generativeaiinference.model.OnDemandServingMode;
import com.oracle.bmc.generativeaiinference.model.ServingMode;
import com.oracle.bmc.generativeaiinference.model.TextContent;
import com.oracle.bmc.generativeaiinference.model.UserMessage;
import com.oracle.bmc.generativeaiinference.requests.ChatRequest;
import com.oracle.bmc.generativeaiinference.responses.ChatResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A utility class that encapsulates the functionality of generating responses to user input using
 * Oracle Cloud Infrastructure's Generative AI Inference service.
 */
public class ChatModel {
    private final GenerativeAiInferenceClient generativeAiInferenceClient;
    private final ServingMode chatServingMode;

    private List<ChatChoice> chatMemory;
    public ChatModel(GenerativeAiInferenceClient generativeAiInferenceClient) {
        this.generativeAiInferenceClient = generativeAiInferenceClient;

        // use meta.llama-3.1-405b-instruct model vailable in OCI Gen AI
        chatServingMode = OnDemandServingMode.builder()
                    .modelId("meta.llama-3.1-405b-instruct")
                    .build();

    }

    /**
     * Generates a response to a given user prompt using the OCI's Generative
     * AI Inference client.
     *
     * @param prompt the user input or question to which a response will be generated
     * @return a ChatResponse object containing the generated response from the LLM
     * model
     */
    public ChatResponse generateResponse(String prompt) {
        // create ChatContent and UserMessage using the given prompt string
        ChatContent content = TextContent.builder()
                .text(prompt)
                .build();
        List<ChatContent> contents = List.of(content);
        Message message = UserMessage.builder()
                .content(contents)
                .build();

        // put the message into a List
        // List<Message> messages = List.of(message);

        // messages below holds previous messages from the conversation
        List<Message> messages = chatMemory == null ? new ArrayList<>() :
                        chatMemory.stream()
                        .map(ChatChoice::getMessage)
                        .collect(Collectors.toList());
        // add the current query message to list of history messages.
        messages.add(message);

        // create a GenericChatRequest including the current and previous messages,
        // and the parameters for the LLM model
        GenericChatRequest genericChatRequest = GenericChatRequest.builder()
                .messages(messages)
                .maxTokens(1000)
                .numGenerations(1)
                .frequencyPenalty(0.0)
                .topP(1.0)
                .topK(1)
                .temperature(0.75)
                .isStream(false)
                .build();

        // create ChatDetails and ChatRequest providing it with the compartment ID
        // and the LLM model info
       ChatDetails details = ChatDetails.builder()
                .chatRequest(genericChatRequest)
                .compartmentId(OCIGenAIEnv.getCompartmentId())
                .servingMode(chatServingMode)
                .build();
       ChatRequest request = ChatRequest.builder()
                .chatDetails(details)
                .build();

       // send chat request to the AI inference client and receive response
       ChatResponse response = generativeAiInferenceClient.chat(request);
       // save the response to the chat memory
       saveChatResponse(response);

       return response;
    }

    /**
     * Saves the message history from a given ChatResponse into the chat memory.
     *
     * @param chatResponse the ChatResponse object containing the result of the LLM model
     */
    private void saveChatResponse(ChatResponse chatResponse) {
        BaseChatResponse bcr = chatResponse.getChatResult().getChatResponse();
        if (bcr instanceof GenericChatResponse resp) {
            chatMemory = resp.getChoices();
        }
    }
}
