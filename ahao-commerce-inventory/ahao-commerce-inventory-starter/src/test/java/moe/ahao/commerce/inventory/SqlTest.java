package moe.ahao.commerce.inventory;

import moe.ahao.commerce.common.enums.ProductTypeEnum;
import org.junit.jupiter.api.Test;

class SqlTest {
    @Test
    void prepareProductSQL() {
        String template = "INSERT INTO `product_sku` VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s', %s, %s, '%s', '%s');";
        for (int i = 1; i <= 100; i++) {
            int id = i;
            String productId = "product" + i;
            int productType = ProductTypeEnum.NORMAL.getCode();
            String skuCode = "skuCode" + i;
            String productName = "压测数据" + i;
            String productImg = "demo.img";
            String productUnit = "个";
            int salePrice = 1000;
            int purchasePrice = 500;
            String createTime = "2021-12-17 11:02:25";
            String updateTime = "2021-12-17 11:02:25";

            String sql = String.format(template, id, productId, productType, skuCode, productName, productImg, productUnit, salePrice, purchasePrice, createTime, updateTime);
            System.out.println(sql);
        }
    }

    @Test
    void prepareInventorySQL() {
        String template = "INSERT INTO `inventory_product_stock` VALUES ('%s', '%s', '%s', '%s', '%s', '%s');";
        for (int i = 1; i <= 100; i++) {
            int id = i;
            String skuCode = "skuCode" + i;
            int saleStockQuantity = 1000000000;
            int saledStockQuantity = 0;
            String createTime = "2021-12-17 11:02:25";
            String updateTime = "2021-12-17 11:02:25";

            String sql = String.format(template, id, skuCode, saleStockQuantity, saledStockQuantity, createTime, updateTime);
            System.out.println(sql);
        }
    }

    private static String getNo(Integer i) {
        return String.valueOf(i);
    }
}
