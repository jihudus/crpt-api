package jihudus.demo.crptapi.limiter;

public interface CrptApi {

    void createNewRfProduct(
            CrptApiWithSlidingWindow.ProductDescription productDescription,
            String signature);
}
