package moe.ahao.commerce.product;

import moe.ahao.commerce.product.api.dto.ProductSkuDTO;
import moe.ahao.commerce.product.api.query.GetProductSkuQuery;
import moe.ahao.commerce.product.api.query.ListProductSkuQuery;
import moe.ahao.commerce.product.application.ProductSkuQueryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = ProductApplication.class)
@ActiveProfiles("test")
public class ProductTest {
    @Autowired
    private ProductSkuQueryService productSkuQueryService;

    @ParameterizedTest
    @ValueSource(strings = {"10101010", "10101011"})
    public void get(String skuCode) {
        GetProductSkuQuery query = new GetProductSkuQuery();
        query.setSellerId(null);
        query.setSkuCode(skuCode);
        ProductSkuDTO sku = productSkuQueryService.query(query);
        System.out.println(sku);
        Assertions.assertNotNull(sku);
    }

    @Test
    public void list() {
        ListProductSkuQuery query = new ListProductSkuQuery();
        query.setSellerId(null);
        query.setSkuCodeList(Arrays.asList("10101010", "10101011"));
        List<ProductSkuDTO> list = productSkuQueryService.query(query);

        for (ProductSkuDTO sku : list) {
            System.out.println(sku);
        }
        Assertions.assertEquals(2, list.size());
    }
}
