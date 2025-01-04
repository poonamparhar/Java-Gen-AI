import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import com.oracle.bmc.generativeaiinference.model.EmbedTextDetails;
import com.oracle.bmc.generativeaiinference.model.OnDemandServingMode;
import com.oracle.bmc.generativeaiinference.model.ServingMode;
import com.oracle.bmc.generativeaiinference.requests.EmbedTextRequest;
import com.oracle.bmc.generativeaiinference.responses.EmbedTextResponse;
import dev.langchain4j.data.embedding.Embedding;

import java.util.Collections;
import java.util.List;

/**
 * Class for generating embeddings from input content using Oracle Cloud Infrastructure's
 * Generative AI Inference service.
 */
public class EmbeddingModel {
    private final GenerativeAiInferenceClient generativeAiInferenceClient;
    private final ServingMode embeddingServingMode;

    /**
     * Constructs an instance of EmbeddingModel with the specified Generative AI Inference client.
     *
     * @param generativeAiInferenceClient the client used to interact with the Generative AI Inference service
     */
    public EmbeddingModel(GenerativeAiInferenceClient generativeAiInferenceClient) {
        this.generativeAiInferenceClient = generativeAiInferenceClient;
        embeddingServingMode = OnDemandServingMode.builder()
                .modelId("cohere.embed-english-v3.0")
                .build();
    }

    /**
     * Generates an embedding vector for the given input string content.
     *
     * @param content the text content to generate an embedding for
     * @return a LangChain4j Embedding object representing the generated embedding vector
     */
    public Embedding embedContent(String content) {
        List<String> inputs = Collections.singletonList(content);
        // Build embed text details and request from the input string
        // use the embedding model as the serving mode
        EmbedTextDetails embedTextDetails = EmbedTextDetails.builder()
                .servingMode(embeddingServingMode)
                .compartmentId(OCIGenAIEnv.getCompartmentId())
                .inputs(inputs)
                .truncate(EmbedTextDetails.Truncate.None)
                .build();
        EmbedTextRequest embedTextRequest = EmbedTextRequest.builder().embedTextDetails(embedTextDetails).build();

        // send embed text request to the AI inference client
        EmbedTextResponse embedTextResponse = generativeAiInferenceClient.embedText(embedTextRequest);

        // extract embeddings from the embed text response
        List<Float> embeddings = embedTextResponse.getEmbedTextResult().getEmbeddings().get(0);
        // put the embeddings in a float[]
        int len = embeddings.size();
        float[] embeddingsVector = new float[len];
        for (int i = 0; i < len; i++) {
            embeddingsVector[i] = embeddings.get(i);
        }
        // return Embedding of LangChain4j that wraps a float[]
        return new Embedding(embeddingsVector);
    }
}
