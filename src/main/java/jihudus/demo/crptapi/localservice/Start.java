package jihudus.demo.crptapi.localservice;

import jihudus.demo.crptapi.limiter.CrptApi;
import jihudus.demo.crptapi.limiter.CrptApiWithSlidingWindow;

import java.util.Set;

public class Start {

    private final CrptApi crptApi;
    private final CrptApiWithSlidingWindow.ProductDescription productDescription;

    public Start(CrptApi crptApi) {
        this.crptApi = crptApi;
        productDescription = new CrptApiWithSlidingWindow.ProductDescription(
                new CrptApiWithSlidingWindow.ProductDescription.Description("participantInn"),
                "doc_id",
                "doc_status",
                "doc_type",
                true,
                "owner_inn",
                "participant_inn",
                "producer_inn",
                "production_date",
                "production_type",
                Set.of(new CrptApiWithSlidingWindow.ProductDescription.Product(
                        "certificate_document",
                        "certificate_document_date",
                        "certificate_document_number",
                        "owner_inn",
                        "producer_inn",
                        "production_date",
                        "tnved_code",
                        "uit_code",
                        "uitu_code")),
                "reg_date",
                "reg_number");
    }

    public void start(String token) {
        crptApi.createNewRfProduct(productDescription, token);
    }
}
