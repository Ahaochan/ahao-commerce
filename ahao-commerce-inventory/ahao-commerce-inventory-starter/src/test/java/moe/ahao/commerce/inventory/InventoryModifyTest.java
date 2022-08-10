package moe.ahao.commerce.inventory;

import moe.ahao.commerce.inventory.api.InventoryFeignApi;
import moe.ahao.commerce.inventory.api.command.AddProductStockCommand;
import moe.ahao.commerce.inventory.api.command.ModifyProductStockCommand;
import moe.ahao.embedded.RedisExtension;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static moe.ahao.commerce.inventory.infrastructure.cache.RedisCacheSupport.SALED_STOCK;
import static moe.ahao.commerce.inventory.infrastructure.cache.RedisCacheSupport.SALE_STOCK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, classes = InventoryApplication.class)
@ActiveProfiles("test")

// @EnableAutoConfiguration(exclude = {SeataAutoConfiguration.class, RocketMQAutoConfiguration.class})
@EnableAutoConfiguration(exclude = {RocketMQAutoConfiguration.class})
class InventoryModifyTest {
    public static final String skuCode = "ahao001";
    @RegisterExtension
    static RedisExtension redisExtension = new RedisExtension();

    private MockMvc mockMvc;

    @BeforeEach
    void setup(WebApplicationContext wac) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void test() throws Exception {
        this.assertResult(null);

        BigDecimal saleStockQuantity = new BigDecimal("100");
        AddProductStockCommand addProductStockCommand = new AddProductStockCommand(skuCode, saleStockQuantity);
        mockMvc.perform(post(InventoryFeignApi.PATH + "/addProductStock")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(JSONHelper.toString(addProductStockCommand)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.obj").value(true));
        this.assertResult(saleStockQuantity);

        BigDecimal stockIncremental = new BigDecimal("20");
        ModifyProductStockCommand modifyProductStockCommand = new ModifyProductStockCommand(skuCode, stockIncremental);
        mockMvc.perform(post(InventoryFeignApi.PATH + "/modifyProductStock")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(JSONHelper.toString(modifyProductStockCommand)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.obj").value(true));
        this.assertResult(saleStockQuantity.add(stockIncremental));
    }

    void assertResult(BigDecimal saleStockQuantity) throws Exception {
        ResultActions actions = mockMvc.perform(get(InventoryFeignApi.PATH + "/getStockInfo")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .param("skuCode", InventoryModifyTest.skuCode))
            .andDo(print())
            .andExpect(status().isOk());

        if (saleStockQuantity == null) {
            actions
                .andExpect(jsonPath("$.obj.saleStockQuantity", IsNull.nullValue()))
                .andExpect(jsonPath("$.obj.saledStockQuantity", IsNull.nullValue()));
        } else {
            actions
                .andExpect(jsonPath("$.obj." + SALE_STOCK).value(saleStockQuantity.intValue()))
                .andExpect(jsonPath("$.obj." + SALED_STOCK).value(0));
        }
    }
}
