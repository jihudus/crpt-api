package jihudus.demo.crptapi.limiter;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/* Эта реализация лимитирует интервал между запросами */
public class CrptApiWithGuava implements CrptApi {

    /* For demo used local rest service */
    private final String baseUrl = "http://localhost:8811/api";
    // private final String baseUrl = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private final RateLimiter rateLimiter;

    public CrptApiWithGuava(TimeUnit timeUnit, int requestLimit) {
        double seconds = TimeUnit.SECONDS.convert(1, timeUnit);
        rateLimiter = RateLimiter.create(requestLimit/seconds);
    }

    @Override
    public void createNewRfProduct(CrptApiWithSlidingWindow.ProductDescription productDescription,
                                   String signature) {
        WebClient webClient = getWebClient(signature);
        rateLimiter.acquire();
        webClient
                .post()
                .header(HttpHeaders.HOST)
                .header(HttpHeaders.CONTENT_LENGTH)
                .body(Mono.just(productDescription), CrptApiWithSlidingWindow.ProductDescription.class)
                .retrieve()
                .bodyToMono(String.class)
                .toFuture();
    }

    private WebClient getWebClient(String signature) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, signature)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
