package com.geecommerce.core.system.query.helper;

import com.geecommerce.core.App;
import com.geecommerce.core.Char;
import com.geecommerce.core.Str;
import com.geecommerce.core.rest.pojo.Update;
import com.geecommerce.core.service.annotation.Helper;
import com.geecommerce.core.system.query.QueryNodeType;
import com.geecommerce.core.system.query.model.QueryNode;
import com.geecommerce.core.type.Id;
import com.geecommerce.core.util.Json;
import com.geecommerce.core.util.Strings;
import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

import java.util.*;
import java.util.regex.Pattern;

@Helper
public class DefaultQueryHelper implements QueryHelper {
    @Inject
    protected App app;

    protected static final String AND = "AND";
    protected static final String OR = "OR";

    protected static final Pattern DOT_PATTERN = Pattern.compile("\\.");
    protected static final Pattern SLASH_PATTERN = Pattern.compile("\\/");
    protected static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_");
    protected static final Pattern MINUS_PATTERN = Pattern.compile("\\-");

    protected static final String ATT_PREFIX = "att_";
    protected static final String HASH_SUFFIX = "_hash"; // _hash
    protected static final String IS_OPTION_SUFFIX = "_is_option"; // _is_option
    protected static final String RAW_SUFFIX = "_raw"; // _raw
    protected static final String RAW_PART = "_raw_"; // _raw_
    protected static final String SLUG_SUFFIX = "_slug"; // _slug
    protected static final String SLUG_PART = "_slug_"; // _slug_

    protected static final String IS_VISIBLE = "is_visible";
    protected static final String IS_VISIBLE_IN_PL = "is_visible_in_pl";


    @Override
    public FilterBuilder buildQuery(QueryNode queryNode) {
        if (queryNode == null)
            return null;
        if (queryNode.getType().equals(QueryNodeType.BOOLEAN)) {
            List<FilterBuilder> filterBuilders = new ArrayList<>();
            if(queryNode.getNodes() != null) {
                for (QueryNode node : queryNode.getNodes()) {
                    filterBuilders.add(buildQuery(node));
                }
                if (queryNode.getOperator().equals(AND)) {
                    FilterBuilder andFilterBuilder = FilterBuilders.andFilter(filterBuilders.toArray(new FilterBuilder[filterBuilders.size()]));
                    return andFilterBuilder;
                } else {
                    FilterBuilder orFilterBuilder = FilterBuilders.orFilter(filterBuilders.toArray(new FilterBuilder[filterBuilders.size()]));
                    return orFilterBuilder;
                }
            }
        } else {
            if (queryNode.getValue() != null && queryNode.getValue().getAttribute() != null) {
                String key = ATT_PREFIX + Strings.slugify(queryNode.getValue().getAttribute().getCode()).replace(Char.MINUS, Char.UNDERSCORE) + HASH_SUFFIX;
                if (queryNode.getValue().getOptionIds() != null && queryNode.getValue().getOptionIds().size() > 1) {
                    List<String> values = new ArrayList<>();
                    for (Id id : queryNode.getValue().getOptionIds()) {
                        values.add(Str.UNDERSCORE_2X + id + Str.UNDERSCORE_2X);
                    }
                    return FilterBuilders.termsFilter(key, values).execution(OR);
                } else if (queryNode.getValue().getOptionIds() != null && queryNode.getValue().getOptionIds().size() > 0) {
                    return FilterBuilders.termsFilter(key, Str.UNDERSCORE_2X + queryNode.getValue().getOptionId() + Str.UNDERSCORE_2X);
                } else if (queryNode.getValue().getVal() != null) {
                    if(queryNode.getValue().getAttribute().isAllowMultipleValues()){
                        key = ATT_PREFIX + Strings.slugify(queryNode.getValue().getAttribute().getCode()).replace(Char.MINUS, Char.UNDERSCORE) + RAW_SUFFIX;
                        return FilterBuilders.termsFilter(key, queryNode.getValue().getVal());
                    } else {
                        if(StringUtils.isEmpty(queryNode.getComparator()) || "is".equals(queryNode.getComparator())){
                            key = ATT_PREFIX + Strings.slugify(queryNode.getValue().getAttribute().getCode()).replace(Char.MINUS, Char.UNDERSCORE) + RAW_SUFFIX;
                            return FilterBuilders.termFilter(key, queryNode.getValue().getVal());
                        } else {
                            key = ATT_PREFIX + Strings.slugify(queryNode.getValue().getAttribute().getCode()).replace(Char.MINUS, Char.UNDERSCORE) + RAW_SUFFIX;
                            if(queryNode.getComparator().equals("gt")){
                                return FilterBuilders.rangeFilter(key).gt(queryNode.getValue().getVal());
                            }
                            if(queryNode.getComparator().equals("gte")){
                                return FilterBuilders.rangeFilter(key).gte(queryNode.getValue().getVal());
                            }
                            if(queryNode.getComparator().equals("lt")){
                                return FilterBuilders.rangeFilter(key).lt(queryNode.getValue().getVal());
                            }
                            if(queryNode.getComparator().equals("lte")){
                                return FilterBuilders.rangeFilter(key).lte(queryNode.getValue().getVal());
                            }
                        }
                    }
                } else {
                    return FilterBuilders.missingFilter(key);
                }
            }
        }
        return null;
    }

    @Override
    public QueryNode getQueryNode(Update update, String field) {
        String queryNodeJson = (String) update.getFields().get(field);
        update.getFields().remove(field);

        Map<String, Object> queryNodeMap = Json.fromJson(queryNodeJson, HashMap.class);
        QueryNode queryNode = app.model(QueryNode.class);
        queryNode.fromMap(queryNodeMap);

        return queryNode;
    }

    @Override
    public QueryNode combine(QueryNode queryNode1, QueryNode queryNode2) {
        if(queryNode1 == null && queryNode2 == null)
            return null;

        if(queryNode1 == null)
            return queryNode2;

        if(queryNode2 == null)
            return queryNode1;

        if(queryNode1.isEmpty())
            return queryNode2;

        if(queryNode2.isEmpty())
            return queryNode1;

        QueryNode queryNode = app.model(QueryNode.class);
        queryNode.setType(QueryNodeType.BOOLEAN);
        queryNode.setOperator("AND");
        queryNode.setNodes(new ArrayList<>());
        queryNode.getNodes().add(queryNode1);
        queryNode.getNodes().add(queryNode2);

        return queryNode;
    }

}
