package com.accenture.springai_bootcamp_demo.service.learning;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Seeded in-memory learning guidance used as a lightweight RAG source.
 */
@Component
public class LearningKnowledgeBase {

    private final LearningRetriever learningRetriever;
    private final List<LearningTopic> topics = List.of(
            new LearningTopic(
                    "java-core",
                    "Java Core",
                    "Types, methods, records, immutability, and everyday Java 21 language features.",
                    List.of("java", "record", "class", "method", "type", "immutability", "string"),
                    """
                            Java Core is the foundation for every Spring application. Start with strong method design: keep methods small, name inputs clearly, and return typed values instead of loosely structured maps or strings. Java records are a good fit for DTOs because they make request and response shapes explicit while staying concise.

                            Pay attention to null handling and immutability. Prefer final local variables when it improves readability, use immutable collections for fixed data, and keep mutable state inside well-defined objects. In a Spring Boot app, clear Java types make validation, JSON serialization, and tests easier to reason about.

                            Practice by tracing one request DTO from controller input through service logic to response output. Explain which values are required, which are optional, and which class owns each transformation.
                            """),
            new LearningTopic(
                    "oop",
                    "Object-Oriented Design",
                    "Classes, responsibilities, encapsulation, composition, and service boundaries.",
                    List.of("oop", "class", "interface", "encapsulation", "composition", "service", "responsibility"),
                    """
                            Object-oriented design is mostly about assigning responsibilities. Controllers should translate HTTP into method calls, services should own use-case behavior, repositories should handle persistence access, and clients should isolate external systems. When each class has one clear job, code becomes easier to test and change.

                            Prefer composition over inheritance for application code. Interfaces are useful at boundaries, such as an AI client that can be replaced by a fake in tests. Encapsulation matters when state changes: expose methods that describe the behavior, not direct access to every internal detail.

                            Practice by choosing one workflow and writing a one-line responsibility statement for each class involved. If a class needs the word "and" too many times, split the behavior.
                            """),
            new LearningTopic(
                    "collections",
                    "Collections",
                    "Lists, maps, sets, ordering, defensive copies, and choosing the right collection.",
                    List.of("collection", "list", "map", "set", "ordering", "stream", "copy"),
                    """
                            Collections communicate intent. Use List when order matters, Set when uniqueness matters, and Map when lookup by key is the main operation. In APIs, returning a List is often clearer than exposing a persistence collection directly.

                            Be deliberate with mutability. For seeded reference data, List.of and unmodifiable maps prevent accidental changes. For request processing, create new lists instead of mutating inputs unless mutation is the point of the method.

                            Practice by replacing a loop with a clear stream pipeline, then decide whether the stream made the code easier to read. Streams are a tool for clarity, not a requirement.
                            """),
            new LearningTopic(
                    "exceptions",
                    "Exceptions",
                    "Domain exceptions, validation failures, error boundaries, and ProblemDetail responses.",
                    List.of("exception", "error", "validation", "problem", "handler", "failure"),
                    """
                            Exceptions should mark meaningful failure boundaries. A missing chat, unknown topic, or failed model call are different problems and deserve different exception types. That lets the global exception handler return accurate HTTP status codes.

                            Validation annotations are best for malformed input, while domain exceptions are best for valid input that cannot be fulfilled. Keep exception messages specific enough for debugging but avoid leaking secrets or internal provider details.

                            Practice by listing each failure mode for an endpoint and assigning it an HTTP response. If two failures need different user actions, they probably should not share the same exception.
                            """),
            new LearningTopic(
                    "streams",
                    "Streams",
                    "Filtering, mapping, sorting, collecting, and readable data transformations.",
                    List.of("stream", "filter", "map", "sort", "collect", "lambda", "pipeline"),
                    """
                            Streams are useful for transforming collections without manual accumulator code. A readable pipeline usually follows this shape: start with a collection, filter unwanted values, map to a new shape, sort if needed, then collect to a list or map.

                            Avoid hiding complex business rules inside long lambdas. Extract helper methods when a stream step needs a name. This is especially helpful in retrieval code where scoring, sorting, and DTO mapping are separate ideas.

                            Practice by reading a stream from left to right and saying what each step contributes. If that is hard, split the pipeline.
                            """),
            new LearningTopic(
                    "concurrency",
                    "Concurrency",
                    "Thread safety, shared state, request handling, and async boundaries in web apps.",
                    List.of("concurrency", "thread", "state", "async", "request", "safety"),
                    """
                            Spring services are usually singleton beans, so shared mutable fields can become a concurrency risk. Keep request-specific data in method variables, DTOs, or persisted entities instead of storing it on the service instance.

                            Most beginner web workflows should stay synchronous until there is a clear reason to add async behavior. External calls, such as model requests, can fail or run slowly, so keep them behind client classes and handle errors at the service boundary.

                            Practice by scanning a service for fields. Ask whether each field is configuration, a dependency, or mutable request state. Mutable request state should not live there.
                            """),
            new LearningTopic(
                    "spring-crm",
                    "Spring CRM Application",
                    "Customer management endpoints, DTO contracts, service use cases, validation, and persistence flow.",
                    List.of("spring", "crm", "customer", "contact", "controller", "service", "dto", "validation", "repository"),
                    """
                            A Spring CRM application is a good layered architecture exercise because it has clear business nouns and workflows. Start with one vertical slice: create a customer, view a customer, update customer details, and list customers. For each use case, define the HTTP endpoint, the request DTO, the response DTO, the service method, and the repository operation.

                            Controllers should stay thin. A CustomerController should validate request DTOs, call a CustomerService method, and return a response DTO. It should not decide business rules such as whether an email is unique, how a customer status changes, or how entities are assembled. Those decisions belong in the service layer.

                            DTOs protect the API contract. A CreateCustomerRequest can include fields such as name, email, phone, and company. A CustomerResponse can include id, display name, status, and timestamps. Keep DTOs separate from JPA entities so database changes do not automatically become API changes. Add validation annotations such as NotBlank, Email, Size, and custom constraints where the request boundary needs them.

                            Services should read like use cases. A createCustomer method validates business rules, builds the entity, saves it through the repository, and maps the result to a response DTO. A listCustomers method can handle filtering or sorting. A service test should be able to fake the repository and verify the business behavior without starting the whole web application.

                            Practice by implementing one CRM use case end to end. Write the DTOs first, then the controller method, then the service method, then the repository call. After that, add one validation failure test and one successful service test.
                            """),
            new LearningTopic(
                    "spring-boot",
                    "Spring Boot",
                    "Controllers, services, dependency injection, configuration, and application structure.",
                    List.of("controller", "service", "dependency", "injection", "configuration", "endpoint"),
                    """
                            Spring Boot gives the application structure: controllers expose endpoints, services orchestrate use cases, repositories access persistence, and configuration classes bind environment-specific settings. Keep each layer thin enough that its responsibility is obvious.

                            Constructor injection makes dependencies explicit and testable. Request and response DTOs protect API boundaries from persistence internals. Typed configuration properties are safer than reading raw environment variables throughout the code.

                            Practice by tracing one endpoint from annotation to service method. Identify where validation happens, where business behavior happens, and where external calls happen.
                            """),
            new LearningTopic(
                    "spring-ai",
                    "Spring AI",
                    "Chat clients, prompt design, Ollama integration, and AI workflow basics.",
                    List.of("ai", "chat", "prompt", "ollama", "agent", "workflow", "model"),
                    """
                            Spring AI ChatClient lets the app build structured prompts from system and user messages while relying on configured providers such as Ollama. Keep prompts close to the workflow they support, and keep provider-specific concerns inside client classes.

                            Multi-agent workflows do not require separate processes. A service can call the same model multiple times with different system prompts: one call diagnoses, another designs exercises, and another writes the final coaching message.

                            Practice by writing the system prompt for each role before coding. If two agents have the same job, merge them; if one prompt has two jobs, split it.
                            """),
            new LearningTopic(
                    "persistence",
                    "Persistence",
                    "JPA entities, repositories, SQLite configuration, and transaction boundaries.",
                    List.of("jpa", "repository", "entity", "sqlite", "transaction", "database"),
                    """
                            Persistence code should model the data the app owns. JPA entities represent stored state, repositories provide query access, and services define transaction boundaries. Avoid returning entities directly from controllers because that couples the API to database shape.

                            Relationships should be loaded deliberately. Entity graphs or focused repository methods make it clear when related data is needed. SQLite is convenient for demos, but the same service and repository boundaries still matter.

                            Practice by drawing the aggregate root and its child objects. Mark which service methods create, update, read, and delete that aggregate.
                            """),
            new LearningTopic(
                    "validation",
                    "Validation",
                    "Request DTO constraints, problem responses, and API input safety.",
                    List.of("validation", "constraint", "dto", "problem", "error", "request"),
                    """
                            Validation belongs at the boundary. Request DTO records can declare required fields, size limits, and numeric ranges with Jakarta validation annotations. The controller stays simple because invalid requests never reach the use-case logic.

                            A global exception handler turns validation and domain failures into consistent ProblemDetail responses. This makes frontend error handling simpler because the UI can read one predictable detail field.

                            Practice by sending one invalid request for every annotation on a DTO. Confirm the status code and message match the user action needed to fix it.
                            """),
            new LearningTopic(
                    "testing",
                    "Testing",
                    "Service tests, controller tests, fake AI clients, and Maven verification.",
                    List.of("test", "mock", "fake", "mvc", "maven", "verify"),
                    """
                            Tests should be deterministic. For AI workflows, put model calls behind an interface and use a fake implementation in service tests. That lets tests verify orchestration order, parsing, and response shape without needing a live model.

                            Controller tests should focus on HTTP behavior: status codes, validation errors, and JSON shape. Service tests should focus on business decisions. Retrieval tests should prove that selected context is ranked correctly.

                            Practice by writing one test that fails because a method call happens out of order. Then fix the service so the test documents the intended workflow.
                            """),
            new LearningTopic(
                    "frontend",
                    "Frontend",
                    "Dependency-free static UI, fetch calls, rendering state, and error handling.",
                    List.of("frontend", "static", "fetch", "javascript", "ui", "render"),
                    """
                            A static frontend can still be structured. Keep fetch helpers together, keep rendering functions small, and let the backend own validation rules. The UI should show loading, success, and error states for each request.

                            Avoid duplicating backend behavior in JavaScript. For example, the UI can limit inputs for convenience, but the server must still validate. Render returned DTOs directly so API changes are easy to spot during manual testing.

                            Practice by following one button click from event listener to fetch call to render function. Each step should have one clear responsibility.
                            """)
    );
    private final Map<String, LearningTopic> topicsById = topics.stream()
            .collect(Collectors.toUnmodifiableMap(LearningTopic::id, Function.identity()));

    public LearningKnowledgeBase(LearningRetriever learningRetriever) {
        this.learningRetriever = learningRetriever;
    }

    public List<LearningTopic> allTopics() {
        return topics;
    }

    public List<RetrievedLearningContext> retrieve(List<String> topicIds, String query) {
        return learningRetriever.retrieve(findTopics(topicIds), query);
    }

    public List<LearningTopic> findTopics(List<String> topicIds) {
        return topicIds.stream()
                .map(this::findTopic)
                .toList();
    }

    private LearningTopic findTopic(String topicId) {
        LearningTopic topic = topicsById.get(topicId);
        if (topic == null) {
            throw new LearningTopicNotFoundException(topicId);
        }
        return topic;
    }

    public record LearningTopic(
            String id,
            String title,
            String summary,
            List<String> keywords,
            String article) {

        String searchText() {
            return String.join(" ", title, summary, String.join(" ", keywords), article);
        }
    }

    public record RetrievedLearningContext(
            String topicId,
            String title,
            List<String> matchedKeywords,
            String guidance) {
    }
}
