package com.geecommerce.core.system.attribute.rest.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.geecommerce.core.ApplicationContext;
import com.geecommerce.core.Char;
import com.geecommerce.core.Str;
import com.geecommerce.core.rest.AbstractResource;
import com.geecommerce.core.rest.ResponseWrapper;
import com.geecommerce.core.rest.jersey.inject.FilterParam;
import com.geecommerce.core.rest.jersey.inject.ModelParam;
import com.geecommerce.core.rest.pojo.Filter;
import com.geecommerce.core.rest.pojo.Update;
import com.geecommerce.core.rest.service.RestService;
import com.geecommerce.core.system.ConfigurationKey;
import com.geecommerce.core.system.attribute.model.Attribute;
import com.geecommerce.core.system.attribute.model.AttributeInputCondition;
import com.geecommerce.core.system.attribute.model.AttributeOption;
import com.geecommerce.core.system.attribute.pojo.BatchData;
import com.geecommerce.core.system.attribute.repository.AttributeOptions;
import com.geecommerce.core.system.attribute.service.AttributeService;
import com.geecommerce.core.system.pojo.Label;
import com.geecommerce.core.type.ContextObject;
import com.geecommerce.core.type.Id;
import com.geecommerce.core.util.Json;
import com.geecommerce.core.util.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.swagger.annotations.Api;

@Api
@Singleton
@Path("/v1/attributes")
public class AttributeResource extends AbstractResource {
    protected final RestService service;
    protected final AttributeService attrService;
    protected final AttributeOptions attributeOptions;

    @Inject
    public AttributeResource(RestService service, AttributeService attrService, AttributeOptions attributeOptions) {
        this.service = service;
        this.attrService = attrService;
        this.attributeOptions = attributeOptions;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getAttributes(@FilterParam Filter filter) {

        List<Attribute> attributes = service.get(Attribute.class, filter.getParams(), queryOptions(filter));

        return ok(attributes);
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Attribute getAttribute(@PathParam("id") Id id) {
        return checked(service.get(Attribute.class, id));
    }

    @GET
    @Path("{id}/options")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getAttributeOptions(@PathParam("id") Id attributeId, @FilterParam Filter filter) {
        // Check if attribute exists.
        checked(service.get(Attribute.class, attributeId));

        // If attribute exists, we can continue to get options for it.
        if (filter == null) {
            filter = new Filter();
        }

        // Make sure that we get the options for the right attribute.
        filter.append(AttributeOption.Col.ATTRIBUTE_ID, attributeId);

        // Sort by position by default.
        if (!filter.hasSortFields()) {
            filter.setSortField(AttributeOption.Col.POSITION);
        }

        // We do a query instead of attribute.getOptions() so that we also get
        // the total count.
        List<AttributeOption> attributeOptions = service.get(AttributeOption.class, filter.getParams(), queryOptions(filter));

        return ok(attributeOptions);
    }

    @GET
    @Path("{id}/options/map")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getAttributeOptions(@PathParam("id") Id attributeId, @QueryParam("term") String term,
        @QueryParam("lang") String language, @QueryParam("limit") Integer limit, @QueryParam("matchCase") boolean isMatchCase, @QueryParam("matchExact") boolean isMatchExact) {

        // Check if attribute exists.
        checked(service.get(Attribute.class, attributeId));

        List<AttributeOption> attrOptions = attributeOptions.havingLabel(attributeId, term, language, limit, isMatchCase, isMatchExact);

        List<Label> result = new ArrayList<>();

        ApplicationContext appCtx = app.context();

        for (AttributeOption attributeOption : attrOptions) {
            if (attributeOption.getLabel() != null && attributeOption.getLabel().hasEntryFor(appCtx.getLanguage())) {
                result.add(new Label(attributeOption.getId(), attributeOption.getLabel()));
            }
        }

        return ok("options", result);
    }

    @GET
    @Path("{id}/options/{optionId}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public AttributeOption getAttributeOption(@PathParam("id") Id id, @PathParam("optionId") Id optionId) {
        return checked(service.get(AttributeOption.class, optionId));
    }

    @GET
    @Path("{id}/options/tags")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public List<String> getAttributeOptionTags(@PathParam("id") Id id) {
        return attrService.getOptionTags(id);
    }

    @PUT
    @Path("{id}/options/positions")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public void updateOptionPositions(@PathParam("id") Id id, HashMap<String, Integer> positionsMap) {
        if (id != null && positionsMap != null && positionsMap.size() > 0) {
            Attribute attribute = checked(service.get(Attribute.class, id));

            Set<String> keys = positionsMap.keySet();

            for (String key : keys) {
                Id optionId = Id.valueOf(key);
                Integer pos = positionsMap.get(key);

                AttributeOption attributeOption = checked(service.get(AttributeOption.class, optionId));

                if (attributeOption.getAttributeId().equals(attribute.getId())) {
                    attributeOption.setPosition(pos);
                    service.update(attributeOption);
                }
            }
        }
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response createAttribute(@ModelParam Attribute attribute) {
        setAttributeCode(attribute);
        return created(service.create(attribute));
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response updateAttribute(@PathParam("id") Id id, Update update) {
        if (id != null && update != null) {
            Attribute a = checked(service.get(Attribute.class, id));
            a.set(update.getFields());
            setAttributeCode(a);
            service.update(a);

            return ok(a);
        }
        return notFound();
    }

    private void setAttributeCode(Attribute attribute) {
        String defaultLanguage = app.cpStr_(ConfigurationKey.I18N_CPANEL_DEFAULT_EDIT_LANGUAGE);
        if (attribute.getCode() == null || attribute.getCode().isEmpty()) {
            if (attribute.getBackendLabel() != null) {
                String name = attribute.getBackendLabel().getClosestValue(defaultLanguage);
                if (name != null && !name.isEmpty()) {
                    String code = Strings.slugify2(name).replace(Char.MINUS, Char.UNDERSCORE);

                    while (code.indexOf(Str.UNDERSCORE_2X) != -1)
                        code = code.replace(Str.UNDERSCORE_2X, Str.UNDERSCORE);

                    if (code.endsWith(Str.UNDERSCORE))
                        code = code.substring(0, code.length() - 1);

                    attribute.setCode(code);
                }
            }
        }
    }

    @DELETE
    @Path("{id}")
    public void removeAttribute(@PathParam("id") Id id) {
        Attribute attribute = checked(service.get(Attribute.class, id));

        List<AttributeOption> options = attribute.getOptions();

        if (options.size() > 1000) {
            throwInternalServerError(
                "Cannot delete an attribute with more than 1000 options. Consult the administrator.");
        }

        for (AttributeOption attributeOption : options) {
            service.remove(attributeOption);
        }

        service.remove(attribute);
    }

    @POST
    @Path("{id}/options")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response createAttributeOptions(@PathParam("id") Id id, @ModelParam List<AttributeOption> attributeOptions) {
        checked(service.get(Attribute.class, id));

        if (attributeOptions != null && attributeOptions.size() > 0) {
            for (AttributeOption attributeOption : attributeOptions) {
                if (attributeOption != null && attributeOption.getLabel() != null
                    && attributeOption.getLabel().isValid()) {
                    service.create(attributeOption);
                }
            }
        }

        return created();
    }

    @POST
    @Path("{id}/option")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response createAttributeOption(@PathParam("id") Id id, @ModelParam AttributeOption attributeOption, @QueryParam("ifNotExists") boolean createIfNotExists,
        @QueryParam("matchCase") boolean isMatchCase) {
        checked(service.get(Attribute.class, id));

        AttributeOption createdOption = null;
        AttributeOption existingOption = null;

        if (createIfNotExists) {
            ContextObject<String> labels = attributeOption.getLabel();

            for (Map<String, Object> map : labels) {
                String lang = (String) map.get(ContextObject.LANGUAGE);
                String label = (String) map.get(ContextObject.VALUE);

                List<AttributeOption> attrOptions = attributeOptions.havingLabel(id, label, lang, 1, isMatchCase, true);

                if (attrOptions != null && !attrOptions.isEmpty()) {
                    AttributeOption ao = attrOptions.get(0);

                    if (existingOption == null && ao != null) {
                        existingOption = ao;
                    } else if (existingOption != null && ao != null && !ao.getId().equals(existingOption.getId())) {
                        existingOption = null;
                        break;
                    }
                }
            }

            if (existingOption != null) {
                return existing(existingOption);
            }
        }

        if (attributeOption != null && attributeOption.getLabel() != null && attributeOption.getLabel().isValid()) {
            createdOption = service.create(attributeOption);
        }

        return createdOption != null ? created(createdOption) : this.internalServerError("Attribute option could not be created");
    }

    @PUT
    @Path("{id}/options")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public void updateAttributeOptions(@PathParam("id") Id id, List<Update> updates) {
        checked(service.get(Attribute.class, id));

        if (updates != null && updates.size() > 0) {
            for (Update update : updates) {
                if (update != null && update.getId() != null) {
                    AttributeOption ao = service.get(AttributeOption.class, update.getId());
                    ao.set(update.getFields());

                    service.update(ao);
                }
            }
        }
    }

    @DELETE
    @Path("{id}/options/{optionId}")
    public void removeAttributeOption(@PathParam("id") Id id, @PathParam("optionId") Id optionId) {
        Attribute a = checked(service.get(Attribute.class, id));
        AttributeOption ao = checked(service.get(AttributeOption.class, optionId));

        if (ao != null && ao.getAttributeId().equals(a.getId())) {
            service.remove(ao);
        }
    }

    @GET
    @Path("input-conditions")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getAttributeInputConditions(@FilterParam Filter filter) {
        if (filter == null) {
            filter = new Filter();
        }

        List<AttributeInputCondition> inputConditions = service.get(AttributeInputCondition.class, filter.getParams(),
            queryOptions(filter));

        return ok(inputConditions);
    }

    @GET
    @Path("{id}/input-conditions")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getAttributeInputConditions(@PathParam("id") Id attributeId, @FilterParam Filter filter) {
        long start = System.currentTimeMillis();

        // Check if attribute exists.
        checked(service.get(Attribute.class, attributeId));

        // If attribute exists, we can continue to get input conditions for it.

        if (filter == null) {
            filter = new Filter();
        }

        // Make sure that we get the options for the right attribute.
        filter.append(AttributeInputCondition.Col.SHOW_ATTRIBUTE_ID, attributeId);

        List<AttributeInputCondition> inputConditions = service.get(AttributeInputCondition.class, filter.getParams(),
            queryOptions(filter));

        return ok(inputConditions);
    }

    @POST
    @Path("{id}/input-conditions")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public void createAttributeInputConditions(@PathParam("id") Id id,
        @ModelParam List<AttributeInputCondition> attributeInputConditions) {
        checked(service.get(Attribute.class, id));

        if (attributeInputConditions != null && attributeInputConditions.size() > 0) {
            for (AttributeInputCondition attributeInputCondition : attributeInputConditions) {
                if (attributeInputCondition != null && attributeInputCondition.getWhenAttributeId() != null
                    && attributeInputCondition.getShowAttributeId() != null) {
                    service.create(attributeInputCondition);
                }
            }
        }
    }

    @PUT
    @Path("{id}/input-conditions")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public void updateAttributeInputConditions(@PathParam("id") Id id, List<Update> updates) {
        checked(service.get(Attribute.class, id));

        if (updates != null && updates.size() > 0) {
            for (Update update : updates) {
                if (update != null && update.getId() != null) {
                    AttributeInputCondition aic = service.get(AttributeInputCondition.class, update.getId());
                    aic.set(update.getFields());

                    service.update(aic);
                }
            }
        }
    }

    @DELETE
    @Path("{id}/input-conditions/{inputConditionId}")
    public void removeAttributeInputCondition(@PathParam("id") Id id,
        @PathParam("inputConditionId") Id inputConditionId) {
        Attribute a = checked(service.get(Attribute.class, id));
        AttributeInputCondition aic = checked(service.get(AttributeInputCondition.class, inputConditionId));

        if (aic != null && aic.getShowAttributeId().equals(a.getId())) {
            service.remove(aic);
        }
    }

    @GET
    @Path("{id}/suggestions/{lang}/{query}/{collection}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getAttributeSuggestions(@PathParam("id") Id id, @PathParam("lang") String lang,
        @PathParam("query") String query, @PathParam("collection") String collection)
        throws ClassNotFoundException {
        List<String> suggestions = attrService.getSuggestions(id, collection, lang, query);
        if (suggestions.size() > 20)
            suggestions = suggestions.subList(0, 20);
        return ok(suggestions);
    }

    @PUT
    @Path("batch")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public void batchUpdateAttributeValues(@QueryParam("forType") String forType, Update update) {

        System.out.println("forType=" + forType + ", update=" + update);

        attrService.processBatchUpdate(BatchData.from(forType, update));

    }

}
