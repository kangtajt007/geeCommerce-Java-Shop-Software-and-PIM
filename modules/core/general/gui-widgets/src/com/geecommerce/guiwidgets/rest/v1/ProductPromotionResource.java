package com.geecommerce.guiwidgets.rest.v1;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.geecommerce.core.rest.AbstractResource;
import com.geecommerce.core.rest.jersey.inject.FilterParam;
import com.geecommerce.core.rest.pojo.Filter;
import com.geecommerce.core.rest.pojo.Update;
import com.geecommerce.core.rest.service.RestService;
import com.geecommerce.core.system.ConfigurationKey;
import com.geecommerce.core.type.Id;
import com.geecommerce.core.util.Strings;
import com.geecommerce.guiwidgets.model.ProductPromotion;
import com.geecommerce.mediaassets.model.MediaAsset;
import com.geecommerce.mediaassets.service.MediaAssetService;
import com.google.inject.Inject;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataParam;

@Path("/v1/product-promotions")
public class ProductPromotionResource extends AbstractResource {
    private final RestService service;

    @Inject
    public ProductPromotionResource(RestService service) {
        this.service = service;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response getProductPromotions(@FilterParam Filter filter) {
        return ok(service.get(ProductPromotion.class, filter.getParams(), queryOptions(filter)));
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public ProductPromotion getProductPromotion(@PathParam("id") Id id) {
        return checked(service.get(ProductPromotion.class, id));
    }

    @DELETE
    @Path("{id}")
    public void removeProductPromotion(@PathParam("id") Id id) {
        ProductPromotion productPromotion = checked(service.get(ProductPromotion.class, id));
        service.remove(productPromotion);
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response createProductPromotion(Update update) {
        ProductPromotion p = app.model(ProductPromotion.class);
        p.set(update.getFields());
        // p.setQuery(attributesToQuery(p.getAttributes(),
        // p.getPromotionType()));
        setProductPromotionKey(p);
        p = service.create(p);
        return created(p);
    }

    @PUT
    @Path("{id}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public ProductPromotion updateProductPromotion(@PathParam("id") Id id, Update update) {
        if (id != null && update != null) {
            ProductPromotion p = checked(service.get(ProductPromotion.class, id));
            p.set(update.getFields());
            // p.setQuery(attributesToQuery(p.getAttributes(),
            // p.getPromotionType()));
            setProductPromotionKey(p);
            service.update(p);
        }
        return checked(service.get(ProductPromotion.class, id));
    }

    private void setProductPromotionKey(ProductPromotion productPromotion) {
        String defaultLanguage = app.cpStr_(ConfigurationKey.I18N_CPANEL_DEFAULT_EDIT_LANGUAGE);
        if (productPromotion.getKey() == null || productPromotion.getKey().isEmpty()) {
            if (productPromotion.getLabel() != null) {
                String name = productPromotion.getLabel().getClosestValue(defaultLanguage);
                if (name != null && !name.isEmpty()) {
                    productPromotion.setKey(Strings.slugify(name));
                }
            }
        }
    }

}
