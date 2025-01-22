import com.oracle.bmc.ClientConfiguration;
import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import com.oracle.bmc.generativeaiinference.model.BaseChatResponse;
import com.oracle.bmc.generativeaiinference.model.ChatChoice;
import com.oracle.bmc.generativeaiinference.model.ChatContent;
import com.oracle.bmc.generativeaiinference.model.GenericChatResponse;
import com.oracle.bmc.generativeaiinference.model.TextContent;
import com.oracle.bmc.generativeaiinference.responses.ChatResponse;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static java.util.stream.Collectors.joining;

/**
 * A Java-based AI troubleshooting assistant that utilizes OCI Generative AI service
 * and LangChain4j to provide helpful answers to Java troubleshooting related
 * questions.
 */
public class JavaTroubleshootingAssistant {

    private final GenerativeAiInferenceClient generativeAiInferenceClient;
    private final ChatModel chatModel;
    private final EmbeddingModel embModel;
    private final DocumentSplitter documentSplitter;
    private final ChromaEmbeddingStore embeddingStore;
    private final PromptTemplate template;

    /**
     * Constructs a new instance of the JavaTroubleshootingAssistant class.
     *
     * Initializes the necessary components required for the assistant to function,
     * including the Generative AI inference client, chat model, embedding model,
     * document splitter, embedding store, and prompt template.
     */
    public JavaTroubleshootingAssistant() {
        this.generativeAiInferenceClient = createAIClient();
        this.chatModel = new ChatModel(generativeAiInferenceClient);
        this.embModel = new EmbeddingModel(generativeAiInferenceClient);

        // Configure document splitter with desired chunk size and overlap
        this.documentSplitter = DocumentSplitters.recursive(
                800,    // Maximum chunk size in tokens
                40,    // Overlap between chunks
                null    // Default separator
        );

        // create ChromaEmbeddingStore instance running at http://localhost:8000
        this.embeddingStore = ChromaEmbeddingStore.builder()
                .baseUrl("http://localhost:8000")
                .collectionName("Java-collection")
                .build();

        // create a LangChain4j PromptTemplate
        this.template = PromptTemplate.from("""
                You are a Java Troubleshooting Assistant. Answer the question in the context of Java or HotSpot JVM.
                Always ask if the user would like to know more about the topic. Do not add signature at the end of the answer.
                Use only the following pieces of context to answer the question at the end.
                                
                Context: {{context}}
                                
                Question: {{question}}
                                
                Helpful Answer:
                """);

    }

    /**
     * The main entry point of the JavaTroubleshootingAssistant application.
     *
     * This method initializes the assistant, sets up the necessary components, and enters
     * a loop where it continuously prompts the user for input and provides responses based
     * on the user's queries.
     *
     * If the "--createVectorStore" command-line argument is provided, the method will first
     * remove any existing vector store data and recreate it by loading PDF files.
     *
     * @param args command-line arguments (optional)
     */
    public static void main(String[] args) {

        // check if we need to create the vector store
        // Pass this flag the first time the program is run.
        boolean creatVectorStore = false;
        if (args.length == 1) {
            String flag = args[0];
            if (flag.equals("--createVectorStore")) {
                creatVectorStore = true;
            }
        }

        JavaTroubleshootingAssistant javachat = new JavaTroubleshootingAssistant();

        // create vector store if requested
        if (creatVectorStore) {
            javachat.embeddingStore.removeAll();
            javachat.createVectorStore("./knowledge-docs/");
        }

        Scanner scanner = new Scanner(System.in);
        String question;

        System.out.println("Ask me a Java troubleshooting question! Type 'exit' to quit.");

        // the chat loop for the user to ask questions and generate response with LLM
        while (true) {
            System.out.print("Your question: ");
            question = scanner.nextLine();
            if (question.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            }

            // generate embedding for the question
            Embedding queryEmbedding = javachat.embModel.embedContent(question);

            // Find relevant embeddings in embedding store by semantic similarity
            int maxResults = 10;
            double minScore = 0.7;
            List<EmbeddingMatch<TextSegment>> relevantEmbeddings
                    = javachat.embeddingStore.findRelevant(queryEmbedding, maxResults, minScore);
            String context = relevantEmbeddings.stream()
                    .map(match -> match.embedded().text())
                    .collect(joining("\n\n"));

            // add the question and the retrieved context to the prompt template
            Map<String, Object> variables = Map.of(
                    "question", question,
                    "context", context
            );
            Prompt prompt = javachat.template.apply(variables);
            System.out.println(prompt.text());

            // send augmented prompt to the chat model
            ChatResponse response = javachat.chatModel.generateResponse(prompt.text());
            String answer = javachat.extractResponseText(response);

            // print the response received from the chat model
            System.out.println("Answer: " + answer);
        }

        // release resources
        javachat.closeAIClient();
        scanner.close();
    }

    /**
     * Creates a new instance of the Generative Ai Inference client.
     *
     * This method reads configuration settings from the specified config location and profile,
     * authenticates using the provided authentication details, and establishes a connection
     * to the Generative AI Inference service at the specified endpoint.
     *
     * @return a fully configured and authenticated Generative Ai Inference client
     * @throws RuntimeException if there is an error reading the configuration file or authenticating
     */
    public GenerativeAiInferenceClient createAIClient() {
        GenerativeAiInferenceClient generativeAiInferenceClient;
        ConfigFileReader.ConfigFile configFile;
        AuthenticationDetailsProvider provider;

        // read configuration details from the config file and create a AuthenticationDetailsProvider
        try {
            configFile = ConfigFileReader.parse(OCIGenAIEnv.getConfigLocation(), OCIGenAIEnv.getConfigProfile());
            provider = new ConfigFileAuthenticationDetailsProvider(configFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Set up Generative AI client with credentials and endpoint
        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .readTimeoutMillis(240000)
                .build();
        generativeAiInferenceClient = GenerativeAiInferenceClient.builder()
                .configuration(clientConfiguration)
                .endpoint(OCIGenAIEnv.getEndpoint())
                .build(provider);

        // return the created GenerativeAiInferenceClient
        return generativeAiInferenceClient;
    }

    /**
     * Closes the Generative AI Inference client connection.
     *
     * This method releases any system resources held by the client and should be called when the
     * client is no longer needed.
     */
    public void closeAIClient() {
        this.generativeAiInferenceClient.close();
    }

    /**
     * Extracts the response text from a given ChatResponse object.
     *
     * This method handles GenericChatResponse, and extracts the corresponding
     * response text.
     *
     * @param chatResponse the ChatResponse object containing the response text
     * @return the extracted response text as a string
     * @throws RuntimeException if an unexpected ChatResponse type is encountered
     */
    private String extractResponseText(ChatResponse chatResponse) {
        // get BaseChatResponse from ChatResponse
        BaseChatResponse bcr = chatResponse
                .getChatResult()
                .getChatResponse();
        // extract text from the GenericChatResponse response type
        // GenericChatResponse represents response from llama models
        if (bcr instanceof GenericChatResponse resp) {
            List<ChatChoice> choices = resp.getChoices();
            List<ChatContent> contents = choices.getLast()
                    .getMessage()
                    .getContent();
            ChatContent content = contents.getLast();
            if (content instanceof TextContent textContent) {
                return textContent.getText();
            }
        }
        throw new RuntimeException("Unexpected ChatResponse");
    }

    /**
     * Loads PDF files from the specified file path, parses their contents using ApachePdfBoxDocumentParser,
     * and splits them into individual TextSegments.
     *
     * @param filePath the path to the directory containing the PDF files
     * @return a list of TextSegments representing the parsed and split PDF
     * text content
     */
    public List<TextSegment> chunkPDFFiles(String filePath) {
        List<Document> documents = null;
        try {
            // Load all *.pdf documents from the given directory
            PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*.pdf");
            documents = FileSystemDocumentLoader.loadDocuments(
                    filePath,
                    pathMatcher,
                    new ApachePdfBoxDocumentParser());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Split documents into TextSegments and add them to a List
        assert documents != null;
        return documents.stream().flatMap(d -> documentSplitter.split(d).stream()).toList();
    }

    /**
     * Creates a list of embeddings from a given list of TextSegments.
     *
     * This method uses the EmbeddingModel to generate an embedding for each TextSegment
     * in the provided list, resulting in a new list of Embedding objects.
     *
     * @param segments the list of TextSegments to be embedded
     * @return a list of Embedding objects representing the embedded TextSegments
     */
    public List<Embedding> createEmbeddings(List<TextSegment> segments) {
        return segments.stream().map(s -> embModel.embedContent(s.text())).toList();
    }

    /**
     * Stores a list of embeddings along with their corresponding TextSegments in the embedding store.
     *
     * This method takes two lists as input: one containing the embeddings and another containing the
     * associated TextSegments. It then adds these embeddings to the embedding store, allowing for
     * efficient retrieval and querying based on semantic similarity.
     *
     * @param embeddings the list of embeddings to be stored
     * @param segments   the list of TextSegments corresponding to the embeddings
     */
    public void storeEmbeddings(List<Embedding> embeddings, List<TextSegment> segments) {
        embeddingStore.addAll(embeddings, segments);
    }

    /**
     * Creates a vector store by loading PDF files from the specified file path,
     * parsing their contents, splitting them into individual TextSegments, generating
     * embeddings for each segment, and storing these embeddings in the embedding store.
     *
     * This method orchestrates the entire process of creating a vector store.
     *
     * @param filePath the path to the directory containing the PDF files to be loaded
     */
    public void createVectorStore(String filePath) {
        List<TextSegment> segments = chunkPDFFiles(filePath);
        List<Embedding> embeddings = createEmbeddings(segments);
        storeEmbeddings(embeddings, segments);
    }
}
