package moe.ahao.commerce.product.application;

import moe.ahao.commerce.common.enums.ProductTypeEnum;
import moe.ahao.commerce.product.api.dto.ProductSkuDTO;
import moe.ahao.commerce.product.api.query.GetProductSkuQuery;
import moe.ahao.commerce.product.api.query.ListProductSkuQuery;
import moe.ahao.commerce.product.infrastructure.exception.ProductExceptionEnum;
import moe.ahao.commerce.product.infrastructure.repository.impl.mybatis.data.ProductSkuDO;
import moe.ahao.commerce.product.infrastructure.repository.impl.mybatis.mapper.ProductSkuMapper;
import moe.ahao.util.commons.io.JSONHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductSkuQueryService {
    @Autowired
    private ProductSkuMapper productSkuMapper;

    public ProductSkuDTO query(GetProductSkuQuery query) {
        String skuCode = query.getSkuCode();
        if (StringUtils.isEmpty(skuCode)) {
            throw ProductExceptionEnum.SKU_CODE_IS_NULL.msg();
        }

        ProductSkuDO data = productSkuMapper.selectOneBySkuCode(skuCode);
        if (data == null) {
            return null;
        }

        ProductSkuDTO dto = new ProductSkuDTO();
        dto.setProductId(data.getProductId());
        dto.setProductType(data.getProductType());
        dto.setSkuCode(data.getSkuCode());
        dto.setProductName(data.getProductName());
        dto.setProductImg(data.getProductImg());
        dto.setProductUnit(data.getProductUnit());
        dto.setSalePrice(data.getSalePrice());
        dto.setPurchasePrice(data.getPurchasePrice());
        dto.setPreSaleInfo( this.parsePreSaleInfoDTO(data));
        return dto;
    }

    public List<ProductSkuDTO> query(ListProductSkuQuery query) {
        List<String> skuCodeList = query.getSkuCodeList();
        if (CollectionUtils.isEmpty(skuCodeList)) {
            throw ProductExceptionEnum.SKU_CODE_IS_NULL.msg();
        }

        List<ProductSkuDO> dataList = productSkuMapper.selectListBySkuCodeList(skuCodeList);

        List<ProductSkuDTO> dtoList = new ArrayList<>();
        for (ProductSkuDO data : dataList) {
            ProductSkuDTO dto = new ProductSkuDTO();
            dto.setProductId(data.getProductId());
            dto.setProductType(data.getProductType());
            dto.setSkuCode(data.getSkuCode());
            dto.setProductName(data.getProductName());
            dto.setProductImg(data.getProductImg());
            dto.setProductUnit(data.getProductUnit());
            dto.setSalePrice(data.getSalePrice());
            dto.setPurchasePrice(data.getPurchasePrice());

            dtoList.add(dto);
        }
        return dtoList;
    }

    /**
     * 设置预售商品信息
     */
    private ProductSkuDTO.PreSaleInfoDTO parsePreSaleInfoDTO(ProductSkuDO productSku) {
        Integer productType = productSku.getProductType();
        boolean isPreSale = ProductTypeEnum.PRE_SALE.getCode().equals(productType);
        if (!isPreSale) {
            // 不是预售商品就不设置预售信息了
            return null;
        }
        String extJson = productSku.getExtJson();
        if(StringUtils.isEmpty(extJson)) {
            throw ProductExceptionEnum.PRE_SALE_INFO_IS_NULL.msg();
        }
        ProductSkuDTO.PreSaleInfoDTO preSaleInfoDTO = JSONHelper.parse(extJson, ProductSkuDTO.PreSaleInfoDTO.class);
        return preSaleInfoDTO;
    }
}
