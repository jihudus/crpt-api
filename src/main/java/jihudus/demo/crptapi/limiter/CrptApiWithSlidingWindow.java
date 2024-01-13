package jihudus.demo.crptapi.limiter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/* Я помню, что просили все в одном классе сделать, но у меня тут 2 реализации,
 * так что решил сделать интерфейс. */
public class CrptApiWithSlidingWindow implements CrptApi {

    /* For demo used local rest service */
    private final String baseUrl = "http://localhost:8811/api";
    // private final String baseUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private final WebClient webClient = WebClient.create(baseUrl);

    /* Queue for holding async requests */
    private final ConcurrentLinkedQueue<Mono<String>> queue = new ConcurrentLinkedQueue<>();

    private final SlidingWindow slidingWindow;

    private boolean serviceStarted = false;

    public CrptApiWithSlidingWindow(TimeUnit timeUnit, Integer requestLimit) {
        long slidingWindowSize = TimeUnit.MILLISECONDS.convert(1, timeUnit);
        slidingWindow = new SlidingWindow(slidingWindowSize, requestLimit);
    }

    /* Create async request, start processing if queue has been empty */
    @Override
    public void createNewRfProduct(ProductDescription productDescription, String signature) {
        Mono<String> requestMono = webClient
                .post()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                /* auth procedure is not actual for this demo */
                .header(HttpHeaders.AUTHORIZATION, signature)
                .header(HttpHeaders.HOST)
                .header(HttpHeaders.CONTENT_LENGTH)
                .body(Mono.just(productDescription), ProductDescription.class)
                .retrieve()
                .bodyToMono(String.class);
        queue.offer(requestMono);
        if (!serviceStarted) {
            serviceStarted = true;
            handle();
        }
    }

    /* Start request and write into the journal.
     * Stop service after the last request is completed */
    private void handle() {
        while (serviceStarted) {
            Long timeToWait = slidingWindow.timeToWait(System.currentTimeMillis());
            if (timeToWait > 0) {
                try {
                    /* Wait for window slide */
                    Thread.sleep(timeToWait);
                } catch (InterruptedException e) {
                    throw new CrptApiException(e);
                }
            }
            try {
                slidingWindow.addToJournal(System.currentTimeMillis());
                /* response ignored in this demo */
                queue.poll().toFuture().get();
                if (queue.isEmpty()) serviceStarted = false;
            } catch (InterruptedException | ExecutionException e) {
                throw new CrptApiException(e);
            }
        }
    }

    public static class SlidingWindow {

        private final ConcurrentLinkedDeque<Long> journal = new ConcurrentLinkedDeque<>();
        private final Long slidingWindowSize;
        private final Integer requestLimit;

        public SlidingWindow(Long slidingWindowSize, Integer requestLimit) {
            this.slidingWindowSize = slidingWindowSize;
            this.requestLimit = requestLimit;
        }

        public Long timeToWait(Long currentTime) {
            /* Delete old items */
            while (!journal.isEmpty() && currentTime - journal.peekFirst() > slidingWindowSize) {
                journal.pollFirst();
            }
            if (journal.size() < requestLimit) {
                /* Limit is not exceeded */
                return 0L;
            } else {
                /* Journal size should be equal to request limit
                 * so attempt must repeat after first item goes old */
                return slidingWindowSize - journal.peekFirst() + currentTime;
            }
        }

        public void addToJournal(Long requestTime) {
            journal.addLast(requestTime);
        }
    }

    public static class CrptApiException extends RuntimeException {
        public CrptApiException(Throwable cause) {
            super(cause);
        }
    }

    public static class ProductDescription {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private Set<Product> products;
        private String reg_date;
        private String reg_number;

        public ProductDescription() {
        }

        public ProductDescription(Description description, String doc_id, String doc_status, String doc_type, boolean importRequest, String owner_inn, String participant_inn, String producer_inn, String production_date, String production_type, Set<Product> products, String reg_date, String reg_number) {
            this.description = description;
            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participant_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.production_type = production_type;
            this.products = products;
            this.reg_date = reg_date;
            this.reg_number = reg_number;
        }

        public static class Description {
            private String participantInn;

            public Description() {
            }

            public Description(String participantInn) {
                this.participantInn = participantInn;
            }

            public String getParticipantInn() {
                return participantInn;
            }

            public void setParticipantInn(String participantInn) {
                this.participantInn = participantInn;
            }
        }

        public static class Product {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String tnved_code;
            private String uit_code;
            private String uitu_code;

            public Product() {
            }

            public Product(String certificate_document, String certificate_document_date, String certificate_document_number, String owner_inn, String producer_inn, String production_date, String tnved_code, String uit_code, String uitu_code) {
                this.certificate_document = certificate_document;
                this.certificate_document_date = certificate_document_date;
                this.certificate_document_number = certificate_document_number;
                this.owner_inn = owner_inn;
                this.producer_inn = producer_inn;
                this.production_date = production_date;
                this.tnved_code = tnved_code;
                this.uit_code = uit_code;
                this.uitu_code = uitu_code;
            }

            public String getCertificate_document() {
                return certificate_document;
            }

            public void setCertificate_document(String certificate_document) {
                this.certificate_document = certificate_document;
            }

            public String getCertificate_document_date() {
                return certificate_document_date;
            }

            public void setCertificate_document_date(String certificate_document_date) {
                this.certificate_document_date = certificate_document_date;
            }

            public String getCertificate_document_number() {
                return certificate_document_number;
            }

            public void setCertificate_document_number(String certificate_document_number) {
                this.certificate_document_number = certificate_document_number;
            }

            public String getOwner_inn() {
                return owner_inn;
            }

            public void setOwner_inn(String owner_inn) {
                this.owner_inn = owner_inn;
            }

            public String getProducer_inn() {
                return producer_inn;
            }

            public void setProducer_inn(String producer_inn) {
                this.producer_inn = producer_inn;
            }

            public String getProduction_date() {
                return production_date;
            }

            public void setProduction_date(String production_date) {
                this.production_date = production_date;
            }

            public String getTnved_code() {
                return tnved_code;
            }

            public void setTnved_code(String tnved_code) {
                this.tnved_code = tnved_code;
            }

            public String getUit_code() {
                return uit_code;
            }

            public void setUit_code(String uit_code) {
                this.uit_code = uit_code;
            }

            public String getUitu_code() {
                return uitu_code;
            }

            public void setUitu_code(String uitu_code) {
                this.uitu_code = uitu_code;
            }
        }

        public Description getDescription() {
            return description;
        }

        public void setDescription(Description description) {
            this.description = description;
        }

        public String getDoc_id() {
            return doc_id;
        }

        public void setDoc_id(String doc_id) {
            this.doc_id = doc_id;
        }

        public String getDoc_status() {
            return doc_status;
        }

        public void setDoc_status(String doc_status) {
            this.doc_status = doc_status;
        }

        public String getDoc_type() {
            return doc_type;
        }

        public void setDoc_type(String doc_type) {
            this.doc_type = doc_type;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public void setImportRequest(boolean importRequest) {
            this.importRequest = importRequest;
        }

        public String getOwner_inn() {
            return owner_inn;
        }

        public void setOwner_inn(String owner_inn) {
            this.owner_inn = owner_inn;
        }

        public String getParticipant_inn() {
            return participant_inn;
        }

        public void setParticipant_inn(String participant_inn) {
            this.participant_inn = participant_inn;
        }

        public String getProducer_inn() {
            return producer_inn;
        }

        public void setProducer_inn(String producer_inn) {
            this.producer_inn = producer_inn;
        }

        public String getProduction_date() {
            return production_date;
        }

        public void setProduction_date(String production_date) {
            this.production_date = production_date;
        }

        public String getProduction_type() {
            return production_type;
        }

        public void setProduction_type(String production_type) {
            this.production_type = production_type;
        }

        public Set<Product> getProducts() {
            return products;
        }

        public void setProducts(Set<Product> products) {
            this.products = products;
        }

        public String getReg_date() {
            return reg_date;
        }

        public void setReg_date(String reg_date) {
            this.reg_date = reg_date;
        }

        public String getReg_number() {
            return reg_number;
        }

        public void setReg_number(String reg_number) {
            this.reg_number = reg_number;
        }
    }

}
