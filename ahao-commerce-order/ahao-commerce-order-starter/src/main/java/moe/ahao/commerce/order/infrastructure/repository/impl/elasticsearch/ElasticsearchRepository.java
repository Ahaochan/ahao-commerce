package moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch;

import moe.ahao.commerce.order.infrastructure.repository.impl.elasticsearch.data.EsBaseDO;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BulkOptions;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;

import java.util.ArrayList;
import java.util.List;

public abstract class ElasticsearchRepository {
    protected final ElasticsearchRestTemplate elasticsearchRestTemplate;

    public ElasticsearchRepository(ElasticsearchRestTemplate elasticsearchRestTemplate) {
        this.elasticsearchRestTemplate = elasticsearchRestTemplate;
    }

    public <T extends EsBaseDO> void saveBatch(List<T> list, Class<T> clazz) {
        this.saveBatch(list, clazz, -1);
    }

    public <T extends EsBaseDO> void saveBatch(List<T> list, Class<T> clazz, long timestamp) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        List<IndexQuery> indexQueryList = new ArrayList<>(list.size());
        for (T item : list) {
            IndexQueryBuilder builder = new IndexQueryBuilder();
            builder.withObject(item);
            if (timestamp > -1) {
                builder.withVersion(timestamp);
            }
            indexQueryList.add(builder.build());
        }
        IndexCoordinates indexCoordinates = elasticsearchRestTemplate.getIndexCoordinatesFor(clazz);
        List<String> ids = elasticsearchRestTemplate.bulkIndex(indexQueryList, BulkOptions.defaultOptions(), indexCoordinates);
    }

    public BoolQueryBuilder range(BoolQueryBuilder boolQueryBuilder, String name, Object from, Object to) {
        if (from != null || to != null) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(name);
            if(from != null) {
                rangeQueryBuilder = rangeQueryBuilder.gte(from);
            }
            if(to != null) {
                rangeQueryBuilder = rangeQueryBuilder.lte(to);
            }
            boolQueryBuilder = boolQueryBuilder.must(rangeQueryBuilder);
        }
        return boolQueryBuilder;
    }
}
