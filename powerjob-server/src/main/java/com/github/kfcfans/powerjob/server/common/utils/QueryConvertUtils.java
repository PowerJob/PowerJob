package com.github.kfcfans.powerjob.server.common.utils;

import com.alibaba.fastjson.JSON;
import com.github.kfcfans.powerjob.common.PowerJobException;
import com.github.kfcfans.powerjob.common.PowerQuery;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.lang.reflect.Field;
import java.util.List;

/**
 * auto convert query to Specification
 *
 * @author tjq
 * @since 2021/1/15
 */
@Slf4j
@SuppressWarnings("unchecked, rawtypes")
public class QueryConvertUtils {

    public static <T> Specification<T> autoConvert(PowerQuery powerQuery) {

        return (Specification<T>) (root, query, cb) -> {
            List<Predicate> predicates = Lists.newLinkedList();
            Field[ ] fields = query.getClass().getDeclaredFields();
            try {
                for (Field field : fields) {
                    String fieldName = field.getName();
                    Object fieldValue = field.get(powerQuery);
                    if (fieldValue == null) {
                        continue;
                    }
                    if (fieldName.endsWith(PowerQuery.EQUAL)) {
                        String colName = StringUtils.substringBeforeLast(fieldName, PowerQuery.EQUAL);
                        predicates.add(cb.equal(root.get(colName), fieldValue));
                    } else if (fieldName.endsWith(PowerQuery.NOT_EQUAL)) {
                        String colName = StringUtils.substringBeforeLast(fieldName, PowerQuery.NOT_EQUAL);
                        predicates.add(cb.notEqual(root.get(colName), fieldValue));
                    } else if (fieldName.endsWith(PowerQuery.LIKE)) {
                        String colName = StringUtils.substringBeforeLast(fieldName, PowerQuery.LIKE);
                        predicates.add(cb.like(root.get(colName), (String) fieldValue));
                    } else if (fieldName.endsWith(PowerQuery.NOT_LIKE)) {
                        String colName = StringUtils.substringBeforeLast(fieldName, PowerQuery.NOT_LIKE);
                        predicates.add(cb.notLike(root.get(colName), (String) fieldValue));
                    } else if (fieldName.endsWith(PowerQuery.LESS_THAN)) {
                        String colName = StringUtils.substringBeforeLast(fieldName, PowerQuery.LESS_THAN);
                        predicates.add(cb.lessThan(root.get(colName), (Comparable)fieldValue));
                    } else if (fieldName.endsWith(PowerQuery.GREATER_THAN)) {
                        String colName = StringUtils.substringBeforeLast(fieldName, PowerQuery.GREATER_THAN);
                        predicates.add(cb.greaterThan(root.get(colName), (Comparable)fieldValue));
                    } else if (fieldName.endsWith(PowerQuery.LESS_THAN_EQUAL)) {
                        String colName = StringUtils.substringBeforeLast(fieldName, PowerQuery.LESS_THAN_EQUAL);
                        predicates.add(cb.lessThanOrEqualTo(root.get(colName), (Comparable)fieldValue));
                    } else if (fieldName.endsWith(PowerQuery.GREATER_THAN_EQUAL)) {
                        String colName = StringUtils.substringBeforeLast(fieldName, PowerQuery.GREATER_THAN_EQUAL);
                        predicates.add(cb.greaterThanOrEqualTo(root.get(colName), (Comparable)fieldValue));
                    } else if (fieldName.endsWith(PowerQuery.IN)) {
                        String colName = StringUtils.substringBeforeLast(fieldName, PowerQuery.IN);
                        predicates.add(root.get(colName).in(fieldValue));
                    } else if (fieldName.endsWith(PowerQuery.NOT_IN)) {
                        String colName = StringUtils.substringBeforeLast(fieldName, PowerQuery.NOT_IN);
                        predicates.add(cb.not(root.get(colName).in(fieldValue)));
                    } else if (fieldName.endsWith(PowerQuery.IS_NULL)) {
                        String colName = StringUtils.substringBeforeLast(fieldName, PowerQuery.IS_NULL);
                        predicates.add(cb.isNull(root.get(colName)));
                    } else if (fieldName.endsWith(PowerQuery.IS_NOT_NULL)) {
                        String colName = StringUtils.substringBeforeLast(fieldName, PowerQuery.IS_NOT_NULL);
                        predicates.add(cb.isNotNull(root.get(colName)));
                    }
                }
            } catch (Exception e) {
                log.warn("[QueryConvertUtils] convert failed for query: {}", JSON.toJSON(query));
                throw new PowerJobException("convert query object failed, maybe you should redesign your query object!");
            }

            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };
    }

    public static void main(String[] args) {
        String s = "appIdEq";
        System.out.println(StringUtils.substringBeforeLast(s, "Eq"));
    }
}
