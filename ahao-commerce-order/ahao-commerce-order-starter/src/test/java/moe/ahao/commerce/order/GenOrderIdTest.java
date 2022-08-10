package moe.ahao.commerce.order;


import moe.ahao.commerce.order.api.command.GenOrderIdCommand;
import moe.ahao.commerce.order.application.GenOrderIdAppService;
import moe.ahao.commerce.common.enums.BusinessIdentifierEnum;
import moe.ahao.commerce.order.infrastructure.enums.OrderIdTypeEnum;
import moe.ahao.commerce.order.infrastructure.publisher.DefaultProducer;
import moe.ahao.embedded.RedisExtension;
import moe.ahao.util.commons.juc.ConcurrentTestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = OrderApplication.class)
@ActiveProfiles("test")
public class GenOrderIdTest {
    @RegisterExtension
    static RedisExtension redisExtension = new RedisExtension();
    @MockBean
    private DefaultProducer defaultProducer;

    @Autowired
    private GenOrderIdAppService genOrderIdAppService;

    @Test
    public void test() throws Exception {
        int threadCount = 100;
        List<Callable<String>> taskList = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            taskList.add(() -> genOrderIdAppService.generate(OrderIdTypeEnum.SALE_ORDER, "001"));
        }
        List<String> list = ConcurrentTestUtils.concurrentCallable(threadCount, taskList);
        list.forEach(System.out::println);

        Set<String> set = new HashSet<>(list);

        Assertions.assertEquals(list.size(), set.size());
    }
}
