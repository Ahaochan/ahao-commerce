package moe.ahao.commerce.address;

import moe.ahao.commerce.address.api.dto.AddressDTO;
import moe.ahao.commerce.address.api.dto.AddressFullDTO;
import moe.ahao.commerce.address.api.query.AddressQuery;
import moe.ahao.commerce.address.application.AddressQueryService;
import moe.ahao.exception.BizException;
import moe.ahao.util.commons.lang.reflect.ReflectHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = AddressApplication.class)
@ActiveProfiles("test")
class AddressApiTest {
    @Autowired
    private AddressQueryService addressQueryService;

    @Test
    void provinces() {
        List<AddressDTO> provinces = addressQueryService.listProvince();
        for (AddressDTO province : provinces) {
            System.out.println(province);
        }
        Assertions.assertTrue(provinces.size() > 0);
    }

    @Test
    public void success() throws Exception {
        List<AddressQuery> queryList = new ArrayList<>();
        AddressQuery source = new AddressQuery("110000", "北京", "110100", "北京市", "110101", "东城区", "110101001", "东华门街道");
        queryList.add(source);
        queryList.addAll(this.getQueryList(source, 0, null));
        queryList.removeIf(q -> q.getProvince() == null && q.getProvinceCode() == null && q.getCity() == null && q.getCityCode() == null && q.getArea() == null && q.getAreaCode() == null && q.getStreet() == null && q.getStreetCode() == null);

        for (AddressQuery query : queryList) {
            AddressFullDTO dto = addressQueryService.queryFullAddress(query);
            System.out.println(dto);
            if (dto.getProvince() != null) {
                Assertions.assertEquals("北京", dto.getProvince());
            }
            if (dto.getProvinceCode() != null) {
                Assertions.assertEquals("110000", dto.getProvinceCode());
            }
            if (dto.getCity() != null) {
                Assertions.assertEquals("北京市", dto.getCity());
            }
            if (dto.getCityCode() != null) {
                Assertions.assertEquals("110100", dto.getCityCode());
            }
            if (dto.getArea() != null) {
                Assertions.assertEquals("东城区", dto.getArea());
            }
            if (dto.getAreaCode() != null) {
                Assertions.assertEquals("110101", dto.getAreaCode());
            }
            if (dto.getStreet() != null) {
                Assertions.assertEquals("东华门街道", dto.getStreet());
            }
            if (dto.getStreetCode() != null) {
                Assertions.assertEquals("110101001", dto.getStreetCode());
            }
        }
    }

    @Test
    public void failure() throws Exception {
        List<AddressQuery> queryList = new ArrayList<>();
        AddressQuery source = new AddressQuery("110000", "北京", "110100", "北京市", "110101", "东城区", "110101001", "东华门街道");
        queryList.add(new AddressQuery());
        queryList.addAll(this.getQueryList(source, 0, "错误地址"));

        for (AddressQuery query : queryList) {
            Assertions.assertThrows(BizException.class, () -> addressQueryService.queryFullAddress(query)).printStackTrace();
        }
    }

    private List<AddressQuery> getQueryList(AddressQuery source, int index, String value) {
        Field[] declaredFields = AddressQuery.class.getDeclaredFields();
        if (index >= declaredFields.length) {
            return Collections.emptyList();
        }
        List<AddressQuery> queryList = new ArrayList<>();
        for (int i = index; i < declaredFields.length; i++) {
            AddressQuery query = new AddressQuery(source);
            ReflectHelper.setValue(query, declaredFields[i], value);

            queryList.addAll(this.getQueryList(query, i + 1, value));

            queryList.add(query);
        }
        return queryList;
    }
}
